package io.edurt.datacap.spi.generator.table;

import com.fasterxml.jackson.databind.JsonNode;
import io.edurt.datacap.spi.generator.Filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateTable
        extends AbstractTable
{
    private final List<Filter> filters = new ArrayList<>();
    private final Map<String, Object> updates = new HashMap<>();
    private final List<BatchUpdate> batchUpdates = new ArrayList<>();
    private Long limit;

    private UpdateTable(String database, String name)
    {
        super(database, name);
    }

    public static UpdateTable create(String database, String name)
    {
        return new UpdateTable(database, name);
    }

    /**
     * 从 JsonNode 添加过滤条件
     *
     * @param node JsonNode 对象
     * @return UpdateTable实例
     */
    public UpdateTable addFilters(JsonNode node)
    {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (!value.isNull()) {
                Object processedValue;
                if (value.isTextual()) {
                    processedValue = value.asText().replace("'", "''");
                }
                else if (value.isNumber()) {
                    processedValue = value.isInt() || value.isLong() ?
                            value.asLong() : value.asDouble();
                }
                else if (value.isBoolean()) {
                    processedValue = value.asBoolean();
                }
                else if (value.isArray()) {
                    Object[] arrayValues = new Object[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        JsonNode element = value.get(i);
                        if (element.isTextual()) {
                            arrayValues[i] = element.asText().replace("'", "''");
                        }
                        else if (element.isNumber()) {
                            arrayValues[i] = element.isInt() || element.isLong() ?
                                    element.asLong() : element.asDouble();
                        }
                        else if (element.isBoolean()) {
                            arrayValues[i] = element.asBoolean();
                        }
                        else if (element.isNull()) {
                            arrayValues[i] = null;
                        }
                        else {
                            arrayValues[i] = element.toString();
                        }
                    }
                    processedValue = arrayValues;
                }
                else if (value.asText().equalsIgnoreCase("null")) {
                    processedValue = null;
                }
                else {
                    processedValue = value.toString();
                }
                addFilter(Filter.create(key, Filter.Operator.EQ, processedValue));
            }
        }
        return this;
    }

    public UpdateTable addFilter(Filter filter)
    {
        filters.add(filter);
        return this;
    }

    /**
     * 添加要更新的列和值
     *
     * @param column 列名
     * @param value 更新值
     * @return UpdateTable实例
     */
    public UpdateTable addUpdate(String column, Object value)
    {
        updates.put(column, value);
        return this;
    }

    /**
     * 从 JsonNode 添加要更新的列和值
     *
     * @param node JsonNode 对象
     * @return UpdateTable实例
     */
    public UpdateTable addUpdates(JsonNode node)
    {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            Object processedValue;
            if (value.isNull()) {
                processedValue = null;
            }
            else if (value.isTextual()) {
                processedValue = value.asText().replace("'", "''");
            }
            else if (value.isNumber()) {
                processedValue = value.isInt() || value.isLong() ?
                        value.asLong() : value.asDouble();
            }
            else if (value.isBoolean()) {
                processedValue = value.asBoolean();
            }
            else if (value.isArray()) {
                Object[] arrayValues = new Object[value.size()];
                for (int i = 0; i < value.size(); i++) {
                    JsonNode element = value.get(i);
                    if (element.isTextual()) {
                        arrayValues[i] = element.asText().replace("'", "''");
                    }
                    else if (element.isNumber()) {
                        arrayValues[i] = element.isInt() || element.isLong() ?
                                element.asLong() : element.asDouble();
                    }
                    else if (element.isBoolean()) {
                        arrayValues[i] = element.asBoolean();
                    }
                    else if (element.isNull()) {
                        arrayValues[i] = null;
                    }
                    else {
                        arrayValues[i] = element.toString();
                    }
                }
                processedValue = arrayValues;
            }
            else {
                processedValue = value.toString();
            }
            addUpdate(key, processedValue);
        }
        return this;
    }

    public UpdateTable limit(long limit)
    {
        this.limit = limit;
        return this;
    }

    /**
     * 添加批量更新条件（基于单列条件）
     *
     * @param column 条件列名
     * @param values 条件值列表
     * @param updateColumn 要更新的列名
     * @param updateValue 更新的值
     * @return UpdateTable实例
     */
    public UpdateTable addBatchUpdate(String column, List<String> values,
            String updateColumn, Object updateValue)
    {
        batchUpdates.add(new SingleColumnBatchUpdate(column, values,
                updateColumn, updateValue));
        return this;
    }

    /**
     * 添加批量更新条件（基于多列条件）
     *
     * @param columns 条件列名列表
     * @param valuesList 条件值组合列表
     * @param updates 要更新的列和值的映射
     * @return UpdateTable实例
     */
    public UpdateTable addBatchUpdate(List<String> columns,
            List<List<String>> valuesList, Map<String, Object> updates)
    {
        batchUpdates.add(new MultiColumnBatchUpdate(columns, valuesList, updates));
        return this;
    }

    private String formatValue(Object value)
    {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            return "(" + String.join(", ", formatArrayValues(array)) + ")";
        }
        return String.valueOf(value);
    }

    private String[] formatArrayValues(Object[] values)
    {
        String[] formattedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                formattedValues[i] = "NULL";
            }
            else if (values[i] instanceof String) {
                formattedValues[i] = "'" + values[i] + "'";
            }
            else {
                formattedValues[i] = String.valueOf(values[i]);
            }
        }
        return formattedValues;
    }

    @Override
    public String build()
    {
        if (updates.isEmpty() && batchUpdates.isEmpty()) {
            throw new IllegalStateException("No update values specified");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE `")
                .append(database).append("`.`")
                .append(name)
                .append("`");

        // 添加 SET 子句
        if (!updates.isEmpty()) {
            sql.append("\nSET ");
            sql.append(updates.entrySet().stream()
                    .map(entry -> "`" + entry.getKey() + "` = " +
                            formatValue(entry.getValue()))
                    .collect(Collectors.joining(",\n    ")));
        }

        // 添加 WHERE 条件
        List<String> allConditions = new ArrayList<>();

        // 添加普通过滤条件
        if (!filters.isEmpty()) {
            allConditions.add(filters.stream()
                    .map(Filter::build)
                    .collect(Collectors.joining("\nAND ")));
        }

        // 添加批量更新条件
        if (!batchUpdates.isEmpty()) {
            allConditions.addAll(batchUpdates.stream()
                    .map(BatchUpdate::build)
                    .collect(Collectors.toList()));
        }

        if (!allConditions.isEmpty()) {
            sql.append("\nWHERE ")
                    .append(String.join("\nAND ", allConditions));
        }

        // 添加 LIMIT
        if (limit != null) {
            sql.append("\nLIMIT ").append(limit);
        }

        sql.append(";");
        return sql.toString();
    }

    private interface BatchUpdate
    {
        String build();
    }

    private class SingleColumnBatchUpdate
            implements BatchUpdate
    {
        private final String column;
        private final List<String> values;
        private final String updateColumn;
        private final Object updateValue;

        public SingleColumnBatchUpdate(String column, List<String> values,
                String updateColumn, Object updateValue)
        {
            this.column = column;
            this.values = values;
            this.updateColumn = updateColumn;
            this.updateValue = updateValue;
        }

        @Override
        public String build()
        {
            // 如果没有在主更新中设置更新值，则在这里设置
            if (!updates.containsKey(updateColumn)) {
                updates.put(updateColumn, updateValue);
            }

            return "`" + column + "` IN (" +
                    String.join(", ", values) + ")";
        }
    }

    private class MultiColumnBatchUpdate
            implements BatchUpdate
    {
        private final List<String> columns;
        private final List<List<String>> valuesList;
        private final Map<String, Object> batchUpdates;

        public MultiColumnBatchUpdate(List<String> columns,
                List<List<String>> valuesList, Map<String, Object> updates)
        {
            this.columns = columns;
            this.valuesList = valuesList;
            this.batchUpdates = updates;
        }

        @Override
        public String build()
        {
            // 将批量更新的值添加到主更新中
            updates.putAll(batchUpdates);

            String columnStr = columns.stream()
                    .map(col -> "`" + col + "`")
                    .collect(Collectors.joining(", "));

            String valuesStr = valuesList.stream()
                    .map(values -> "(" + String.join(", ", values) + ")")
                    .collect(Collectors.joining(", "));

            return "(" + columnStr + ") IN (" + valuesStr + ")";
        }
    }
}
