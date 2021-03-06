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
package net.hydromatic.optiq.impl.java;

import net.hydromatic.linq4j.QueryProvider;
import net.hydromatic.linq4j.expressions.Expression;
import net.hydromatic.linq4j.expressions.Expressions;

import net.hydromatic.optiq.*;
import net.hydromatic.optiq.jdbc.OptiqConnection;

import org.eigenbase.reltype.RelDataType;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Implementation of {@link Schema} backed by a {@link HashMap}.
 *
 * @author jhyde
 */
public class MapSchema implements MutableSchema {

    protected final Map<String, Table<Object>> tableMap =
        new HashMap<String, Table<Object>>();

    protected final Map<String, List<TableFunction>> membersMap =
        new HashMap<String, List<TableFunction>>();

    protected final Map<String, Schema> subSchemaMap =
        new HashMap<String, Schema>();

    private final QueryProvider queryProvider;
    protected final JavaTypeFactory typeFactory;
    private final Expression expression;

    /**
     * Creates a MapSchema.
     *
     * @param queryProvider Query provider
     * @param typeFactory Type factory
     * @param expression Expression for schema
     */
    public MapSchema(
        QueryProvider queryProvider,
        JavaTypeFactory typeFactory,
        Expression expression)
    {
        this.queryProvider = queryProvider;
        this.typeFactory = typeFactory;
        this.expression = expression;

        assert expression != null;
        assert typeFactory != null;
        assert queryProvider != null;
    }

    /**
     * Creates a MapSchema within another schema.
     *
     * @param optiqConnection Connection to Optiq (also a query provider)
     * @param parentSchema Parent schema
     * @param name Name of new schema
     * @return New MapSchema
     */
    public static MapSchema create(
        OptiqConnection optiqConnection,
        MutableSchema parentSchema,
        String name)
    {
        MapSchema schema =
            new MapSchema(
                optiqConnection,
                optiqConnection.getTypeFactory(),
                parentSchema.getSubSchemaExpression(name, Object.class));
        parentSchema.addSchema(name, schema);
        return schema;
    }

    public Expression getExpression() {
        return expression;
    }

    public QueryProvider getQueryProvider() {
        return queryProvider;
    }

    public <T> Table<T> getTable(String name, Class<T> elementType) {
        // TODO: check elementType matches table.elementType
        assert elementType != null;
        return getTable(name);
    }

    public Collection<String> getTableNames() {
        return tableMap.keySet();
    }

    public Table getTable(String name) {
        // First look for a table.
        Table<Object> table = tableMap.get(name);
        if (table != null) {
            return table;
        }
        // Then look for a table-function with no arguments.
        List<TableFunction> tableFunctions = membersMap.get(name);
        if (tableFunctions != null) {
            for (TableFunction tableFunction : tableFunctions) {
                if (tableFunction.getParameters().isEmpty()) {
                    return tableFunction.apply(Collections.emptyList());
                }
            }
        }
        return null;
    }

    public Map<String, List<TableFunction>> getTableFunctions() {
        return membersMap;
    }

    public List<TableFunction> getTableFunctions(String name) {
        List<TableFunction> members = membersMap.get(name);
        if (members != null) {
            return members;
        }
        return Collections.emptyList();
    }

    public Collection<String> getSubSchemaNames() {
        return subSchemaMap.keySet();
    }

    public Schema getSubSchema(String name) {
        return subSchemaMap.get(name);
    }

    public void addTableFunction(String name, TableFunction tableFunction) {
        putMulti(membersMap, name, tableFunction);
    }

    public void addTable(String name, Table table) {
        tableMap.put(name, table);
    }

    public void addSchema(String name, Schema schema) {
        subSchemaMap.put(name, schema);
    }

    public Expression getSubSchemaExpression(String name, Class type) {
        // (Type) schemaExpression.getSubSchema("name")
        Expression call =
            Expressions.call(
                getExpression(),
                BuiltinMethod.GET_SUB_SCHEMA.method,
                Collections.<Expression>singletonList(
                    Expressions.constant(name)));
        if (type != null && !type.isAssignableFrom(Schema.class)) {
            return Expressions.convert_(call, type);
        }
        return call;
    }

    private Type deduceType(Object schemaObject) {
        // REVIEW: Can we remove the dependency on RelDataType and work in
        //   terms of Class?
        if (schemaObject instanceof Member) {
            RelDataType type = ((Member) schemaObject).getType();
            return typeFactory.getJavaClass(type);
        }
        if (schemaObject instanceof Schema) {
            return schemaObject.getClass();
        }
        return null;
    }

    protected static <K, V> void putMulti(
        Map<K, List<V>> map, K k, V v)
    {
        List<V> list = map.put(k, Collections.singletonList(v));
        if (list == null) {
            return;
        }
        if (list.size() == 1) {
            list = new ArrayList<V>(list);
        }
        list.add(v);
        map.put(k, list);
    }
}

// End MapSchema.java
