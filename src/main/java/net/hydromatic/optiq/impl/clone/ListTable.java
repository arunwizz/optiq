/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq.impl.clone;

import net.hydromatic.linq4j.*;
import net.hydromatic.linq4j.expressions.Expression;
import net.hydromatic.optiq.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Implementation of table that reads rows from a read-only list and returns
 * an enumerator of rows. Each row is object (if there is just one column) or
 * an object array (if there are multiple columns).
 */
class ListTable<T>
    extends BaseQueryable<T>
    implements Table<T>
{
    private final Schema schema;
    private final List<T> list;

    public ListTable(
        Schema schema, Type elementType, Expression expression, List<T> list)
    {
        super(schema.getQueryProvider(), elementType, expression);
        this.schema = schema;
        this.list = list;
    }

    public DataContext getDataContext() {
        return schema;
    }

    @Override
    public Enumerator<T> enumerator() {
        return Linq4j.enumerator(list);
    }
}

// End ListTable.java
