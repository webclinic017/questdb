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

package io.questdb.cairo.sql;

import io.questdb.cairo.TableStructureChangesException;
import io.questdb.cairo.TableWriter;
import io.questdb.griffin.SqlException;
import io.questdb.tasks.TableWriterTask;

public interface AlterStatement {
    short DO_NOTHING = 1;
    short ADD_COLUMN = 3;
    short DROP_PARTITION = 4;
    short ATTACH_PARTITION = 5;
    short ADD_INDEX = 6;
    short ADD_SYMBOL_CACHE = 7;
    short REMOVE_SYMBOL_CACHE = 8;
    short DROP_COLUMN = 9;
    short RENAME_COLUMN = 10;
    short SET_PARAM_MAX_UNCOMMITTED_ROWS = 11;
    short SET_PARAM_COMMIT_LAG = 12;

    void apply(TableWriter tableWriter, boolean acceptStructureChange) throws SqlException, TableStructureChangesException;
    CharSequence getTableName();

    int getTableNamePosition();
    void serialize(TableWriterTask event);
}