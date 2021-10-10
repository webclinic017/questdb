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

package io.questdb.cutlass.line;

public interface LineProtoParser {
    int EVT_MEASUREMENT = 1;
    int EVT_TAG_VALUE = 2;
    int EVT_FIELD_VALUE = 3;
    int EVT_TAG_NAME = 4;
    int EVT_FIELD_NAME = 5;
    int EVT_TIMESTAMP = 6;

    int ERROR_EXPECTED = 1;
    int ERROR_ENCODING = 2;
    int ERROR_EMPTY = 3;

    void onError(int position, int state, int code);

    void onEvent(CachedCharSequence token, int type, CharSequenceCache cache);

    void onLineEnd(CharSequenceCache cache);
}
