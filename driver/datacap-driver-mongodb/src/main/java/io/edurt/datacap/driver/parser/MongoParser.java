package io.edurt.datacap.driver.parser;

import io.edurt.datacap.sql.SQLParser;
import io.edurt.datacap.sql.statement.SQLStatement;
import io.edurt.datacap.sql.statement.SelectStatement;
import io.edurt.datacap.sql.statement.ShowStatement;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;

import java.util.List;

@Getter
public class MongoParser
{
    protected Document query;
    protected Document update;
    protected Document sort;
    protected Document filter;
    protected List<String> fields;
    protected String collection;
    protected String command;
    protected ShowStatement.ShowType showType;
    protected int limit = -1;
    protected int skip = -1;

    @Setter
    protected String database;

    // Parse SQL statement
    // 解析SQL语句
    public static MongoParser createParser(String sql)
    {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }

        SQLStatement statement = SQLParser.parse(sql.trim());
        if (statement instanceof SelectStatement) {
            return new MongoSelectParser((SelectStatement) statement);
        }
        else if (statement instanceof ShowStatement) {
            return new MongoShowParser((ShowStatement) statement);
        }
        throw new IllegalArgumentException("Unsupported SQL operation: " + sql);
    }
}
