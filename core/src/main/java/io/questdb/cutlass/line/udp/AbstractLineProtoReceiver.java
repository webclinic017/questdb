/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2020 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.cutlass.line.udp;

import io.questdb.cairo.CairoEngine;
import io.questdb.cutlass.line.CairoLineProtoParser;
import io.questdb.cutlass.line.LineProtoLexer;
import io.questdb.log.Log;
import io.questdb.log.LogFactory;
import io.questdb.mp.SOCountDownLatch;
import io.questdb.mp.SynchronizedJob;
import io.questdb.mp.WorkerPool;
import io.questdb.network.NetworkError;
import io.questdb.network.NetworkFacade;
import io.questdb.std.Misc;
import io.questdb.std.Os;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLineProtoReceiver extends SynchronizedJob implements Closeable {
    private static final Log LOG = LogFactory.getLog(AbstractLineProtoReceiver.class);
    private final LineProtoLexer lexer;
    protected final CairoLineProtoParserListener listener;
    protected final CairoLineProtoParser parser;
    protected final NetworkFacade nf;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final SOCountDownLatch started = new SOCountDownLatch(1);
    private final SOCountDownLatch halted = new SOCountDownLatch(1);
    private final LineUdpReceiverConfiguration configuration;
    protected long fd;
    protected int commitRate;
    protected long totalCount = 0;
    protected final int commitMode;

    public AbstractLineProtoReceiver(
            LineUdpReceiverConfiguration configuration,
            CairoEngine engine,
            WorkerPool workerPool
    ) {
        this.configuration = configuration;
        this.commitMode = configuration.getCommitMode();
        nf = configuration.getNetworkFacade();
        fd = nf.socketUdp();
        if (fd < 0) {
            int errno = nf.errno();
            LOG.error().$("cannot open UDP socket [errno=").$(errno).$(']').$();
            throw NetworkError.instance(errno, "Cannot open UDP socket");
        }

        try {
            // when listening for multicast packets bind address must be 0
            bind(configuration);
            this.commitRate = configuration.getCommitRate();

            if (configuration.getReceiveBufferSize() != -1 && nf.setRcvBuf(fd, configuration.getReceiveBufferSize()) != 0) {
                LOG.error().$("cannot set receive buffer size [fd=").$(fd).$(", size=").$(configuration.getReceiveBufferSize()).$(']').$();
            }

            lexer = new LineProtoLexer(configuration.getMsgBufferSize());
            listener = new CairoLineProtoParserListener(
                    engine,
                    configuration.getCairoSecurityContext(),
                    configuration.getTimestampAdapter(),
                    configuration.getDefaultPartitionBy()
            );
            parser = new CairoLineProtoParser(lexer, listener);
//            lexer.withParser(parser);

            if (!configuration.ownThread()) {
                workerPool.assign(this);
                logStarted(configuration);
            }
        } catch (Throwable e) {
            close();
            throw e;
        }
    }

    @Override
    public void close() {
        if (fd > -1) {
            halt();
            if (nf.close(fd) != 0) {
                LOG.error().$("could not close [fd=").$(fd).$(", errno=").$(nf.errno()).$(']').$();
            } else {
                LOG.info().$("closed [fd=").$(fd).$(']').$();
            }
            if (listener != null) {
                listener.commitAll(commitMode);
                listener.close();
            }
            Misc.free(lexer);
            LOG.info().$("closed [fd=").$(fd).$(']').$();
            fd = -1;
        }
    }

    protected void halt() {
        if (running.compareAndSet(true, false)) {
            started.await();
            halted.await();
        }
    }

    public void start() {
        if (configuration.ownThread() && running.compareAndSet(false, true)) {
            new Thread(() -> {
                started.countDown();
                if (configuration.ownThreadAffinity() != -1) {
                    Os.setCurrentThreadAffinity(configuration.ownThreadAffinity());
                }
                logStarted(configuration);
                while (running.get()) {
                    runSerially();
                }
                LOG.info().$("shutdown").$();
                halted.countDown();
            }).start();
        }
    }

    private void bind(LineUdpReceiverConfiguration configuration) {
        if (nf.bindUdp(fd, configuration.isUnicast() ? configuration.getBindIPv4Address() : 0, configuration.getPort())) {
            if (!configuration.isUnicast() && !nf.join(fd, configuration.getBindIPv4Address(), configuration.getGroupIPv4Address())) {
                throw NetworkError.instance(nf.errno())
                        .put("cannot join group ")
                        .put("[fd=").put(fd)
                        .put(", bind=").put(configuration.getBindIPv4Address())
                        .put(", group=").put(configuration.getGroupIPv4Address())
                        .put(']');
            }
        } else {
            throw NetworkError.instance(nf.errno()).couldNotBindSocket("udp-line-server", configuration.getBindIPv4Address(), configuration.getPort());
        }
    }

    private void logStarted(LineUdpReceiverConfiguration configuration) {
        if (configuration.isUnicast()) {
            LOG.info()
                    .$("receiving unicast on ")
                    .$ip(configuration.getBindIPv4Address())
                    .$(':')
                    .$(configuration.getPort())
                    .$(" [fd=").$(fd)
                    .$(", commitRate=").$(commitRate)
                    .$(']').$();
        } else {
            LOG.info()
                    .$("receiving multicast from ")
                    .$ip(configuration.getGroupIPv4Address())
                    .$(':')
                    .$(configuration.getPort())
                    .$(" via ")
                    .$ip(configuration.getBindIPv4Address())
                    .$(" [fd=").$(fd)
                    .$(", commitRate=").$(commitRate)
                    .$(']').$();
        }
    }
}
