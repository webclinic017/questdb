/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2022 QuestDB
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

package io.questdb.griffin.engine.functions.groupby;

import io.questdb.griffin.AbstractGriffinTest;
import org.junit.Test;

public class AvgLongVecGroupByFunctionFactoryTest extends AbstractGriffinTest {

    @Test
    public void testAddColumn() throws Exception {
        assertQuery(
                "avg\n" +
                        "5261.376146789\n",
                "select round(avg(f),9) avg from tab",
                "create table tab as (select rnd_int(-55, 9009, 2) f from long_sequence(131))",
                null,
                "alter table tab add column b long",
                "avg\n" +
                        "5261.376146789\n",
                false,
                true,
                true
        );

        assertQuery(
                "avg\tavg2\n" +
                        "2633.684612\t52790.018932\n",
                "select round(avg(f),6) avg, round(avg(b),6) avg2 from tab",
                "insert into tab select rnd_int(2, 10, 2), rnd_long(16772, 88965, 4) from long_sequence(78057)",
                null,
                false,
                true,
                true
        );
    }

    @Test
    public void testAllNullThenOne() throws Exception {
        assertQuery(
                "avg\n" +
                        "NaN\n",
                "select avg(f) from tab",
                "create table tab as (select cast(null as long) f from long_sequence(33))",
                null,
                "insert into tab select 123L from long_sequence(1)",
                "avg\n" +
                        "123.0\n",
                false,
                true,
                true
        );
    }

    @Test
    public void testSimple() throws Exception {
        assertQuery(
                "avg\n" +
                        "4289.100917431193\n",
                "select avg(f) from tab",
                "create table tab as (select rnd_long(-55, 9009, 2) f from long_sequence(131))",
                null,
                false,
                true,
                true
        );
    }
}