package io.edurt.datacap.spi.generator;

public class Column
{
    private final String name;
    private final DataType type;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private boolean nullable = true;
    private String defaultValue;
    private String comment;
    private boolean autoIncrement;
    private String charset;
    private String collate;

    private Column(String name, DataType type)
    {
        this.name = name;
        this.type = type;
    }

    public static Column create(String name, DataType type)
    {
        return new Column(name, type);
    }

    public Column length(int length)
    {
        this.length = length;
        return this;
    }

    public Column precision(int precision, int scale)
    {
        this.precision = precision;
        this.scale = scale;
        return this;
    }

    public Column notNull()
    {
        this.nullable = false;
        return this;
    }

    public Column defaultValue(String value)
    {
        this.defaultValue = value;
        return this;
    }

    public Column comment(String comment)
    {
        this.comment = comment;
        return this;
    }

    public Column autoIncrement()
    {
        this.autoIncrement = true;
        return this;
    }

    public Column charset(String charset)
    {
        this.charset = charset;
        return this;
    }

    public Column collate(String collate)
    {
        this.collate = collate;
        return this;
    }

    public String build()
    {
        StringBuilder sql = new StringBuilder();
        sql.append("`").append(name).append("` ");

        if (length != null) {
            sql.append(type.withLength(length));
        }
        else if (precision != null) {
            sql.append(type.withPrecision(precision, scale));
        }
        else {
            sql.append(type.getValue());
        }

        if (charset != null) {
            sql.append(" CHARACTER SET ").append(charset);
        }

        if (collate != null) {
            sql.append(" COLLATE ").append(collate);
        }

        if (!nullable) {
            sql.append(" NOT NULL");
        }

        if (defaultValue != null) {
            if (defaultValue.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                sql.append(" DEFAULT ").append(defaultValue);
            }
            else {
                sql.append(" DEFAULT '").append(defaultValue).append("'");
            }
        }

        if (autoIncrement) {
            sql.append(" AUTO_INCREMENT");
        }

        if (comment != null) {
            sql.append(" COMMENT '").append(comment).append("'");
        }

        return sql.toString();
    }
}
