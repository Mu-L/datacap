package io.edurt.datacap.spi.generator.table;

import io.edurt.datacap.spi.generator.Column;
import io.edurt.datacap.spi.generator.Index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CreateTable
{
    private final String name;
    private final List<String> columns = new ArrayList<>();
    private final List<String> primaryKeys = new ArrayList<>();
    private final List<Index> indexes = new ArrayList<>();
    private String engine = "";
    private String charset = "utf8mb4";
    private String collate = "utf8mb4_general_ci";
    private String comment;
    private String rowFormat;

    private CreateTable(String name)
    {
        this.name = name;
    }

    public static CreateTable create(String name)
    {
        return new CreateTable(name);
    }

    public CreateTable addColumn(Column column)
    {
        columns.add(column.build());
        return this;
    }

    public CreateTable addPrimaryKey(String... columns)
    {
        primaryKeys.addAll(Arrays.asList(columns));
        return this;
    }

    public CreateTable addIndex(Index index)
    {
        indexes.add(index);
        return this;
    }

    public CreateTable engine(String engine)
    {
        this.engine = engine;
        return this;
    }

    public CreateTable charset(String charset)
    {
        this.charset = charset;
        return this;
    }

    public CreateTable collate(String collate)
    {
        this.collate = collate;
        return this;
    }

    public CreateTable comment(String comment)
    {
        this.comment = comment;
        return this;
    }

    public CreateTable rowFormat(String rowFormat)
    {
        this.rowFormat = rowFormat;
        return this;
    }

    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(name).append("` (\n");

        // 添加列定义
        sql.append(columns.stream()
                .map(col -> "  " + col)
                .collect(Collectors.joining(",\n")));

        // 添加主键
        if (!primaryKeys.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (")
                    .append(primaryKeys.stream()
                            .map(col -> "`" + col + "`")
                            .collect(Collectors.joining(", ")))
                    .append(")");
        }

        // 添加索引
        for (Index index : indexes) {
            sql.append(",\n  ").append(index.build());
        }

        sql.append("\n)");

        // 添加表选项
        sql.append(" ENGINE=").append(engine);

        if (charset != null) {
            sql.append(" DEFAULT CHARSET=").append(charset);
        }

        if (collate != null) {
            sql.append(" COLLATE=").append(collate);
        }

        if (rowFormat != null) {
            sql.append(" ROW_FORMAT=").append(rowFormat);
        }

        if (comment != null) {
            sql.append(" COMMENT='").append(comment).append("'");
        }

        sql.append(";");
        return sql.toString();
    }
}
