package io.edurt.datacap.spi.generator.table;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectTable
        extends AbstractTable
{
    private final List<String> whereConditions = new ArrayList<>();
    private final List<String> groupByColumns = new ArrayList<>();
    private final List<String> orderByColumns = new ArrayList<>();
    private String havingClause;
    private Long limit;
    private Long offset;

    private SelectTable(String database, String name)
    {
        super(database, name);
    }

    public static SelectTable create(String database, String name)
    {
        return new SelectTable(database, name);
    }

    public SelectTable where(String condition)
    {
        whereConditions.add(condition);
        return this;
    }

    public SelectTable groupBy(String... columns)
    {
        for (String column : columns) {
            groupByColumns.add("`" + column + "`");
        }
        return this;
    }

    public SelectTable having(String condition)
    {
        this.havingClause = condition;
        return this;
    }

    public SelectTable orderBy(String... columns)
    {
        for (String column : columns) {
            orderByColumns.add("`" + column + "`");
        }
        return this;
    }

    public SelectTable limit(long limit)
    {
        this.limit = limit;
        return this;
    }

    public SelectTable offset(long offset)
    {
        this.offset = offset;
        return this;
    }

    @Override
    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        // 添加列
        if (columns.isEmpty()) {
            sql.append("*");
        }
        else {
            sql.append(columns.stream()
                    .collect(Collectors.joining(", ")));
        }

        // 添加表名
        sql.append("\nFROM `")
                .append(database).append("`.`")
                .append(name)
                .append("`");

        // 添加 WHERE 条件
        if (!whereConditions.isEmpty()) {
            sql.append("\nWHERE ")
                    .append(String.join("\nAND ", whereConditions));
        }

        // 添加 GROUP BY
        if (!groupByColumns.isEmpty()) {
            sql.append("\nGROUP BY ")
                    .append(String.join(", ", groupByColumns));
        }

        // 添加 HAVING
        if (havingClause != null && !havingClause.isEmpty()) {
            sql.append("\nHAVING ")
                    .append(havingClause);
        }

        // 添加 ORDER BY
        if (!orderByColumns.isEmpty()) {
            sql.append("\nORDER BY ")
                    .append(String.join(", ", orderByColumns));
        }

        // 添加 LIMIT 和 OFFSET
        if (limit != null) {
            sql.append("\nLIMIT ").append(limit);
            if (offset != null) {
                sql.append(" OFFSET ").append(offset);
            }
        }

        sql.append(";");
        return sql.toString();
    }
}
