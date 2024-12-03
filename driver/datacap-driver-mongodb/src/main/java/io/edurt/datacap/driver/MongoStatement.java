package io.edurt.datacap.driver;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

@Slf4j
public class MongoStatement
        implements Statement
{
    private final MongoConnection connection;
    private boolean isClosed = false;

    // Constructor
    // 构造函数
    public MongoStatement(MongoConnection connection)
    {
        this.connection = connection;
    }

    // Execute query and return ResultSet
    // 执行查询并返回ResultSet
    @Override
    public ResultSet executeQuery(String sql)
            throws SQLException
    {
        checkClosed();
        try {
            // Parse SQL to MongoDB query
            // 将SQL解析为MongoDB查询
            MongoQueryParser parser = new MongoQueryParser(sql);
            String collectionName = parser.getCollection();
            Document query = parser.getQuery();
            log.debug("Executing query: {}", query);

            MongoCollection<Document> collection = connection.getDatabase().getCollection(collectionName);
            FindIterable<Document> result = collection.find(query);

            return new MongoResultSet(result);
        }
        catch (Exception e) {
            throw new SQLException("Failed to execute query", e);
        }
    }

    // Execute update statement
    // 执行更新语句
    @Override
    public int executeUpdate(String sql)
            throws SQLException
    {
        checkClosed();
        try {
            MongoQueryParser parser = new MongoQueryParser(sql);
            String collectionName = parser.getCollection();
            Document update = parser.getUpdate();

            MongoCollection<Document> collection = connection.getDatabase().getCollection(collectionName);
            return (int) collection.updateMany(parser.getQuery(), update).getModifiedCount();
        }
        catch (Exception e) {
            throw new SQLException("Failed to execute update", e);
        }
    }

    // Check if statement is closed
    // 检查语句是否已关闭
    private void checkClosed()
            throws SQLException
    {
        if (isClosed) {
            throw new SQLException("Statement is closed");
        }
    }

    // Close the statement
    // 关闭语句
    @Override
    public void close()
            throws SQLException
    {
        isClosed = true;
    }

    @Override
    public int getMaxFieldSize()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max)
            throws SQLException
    {}

    @Override
    public int getMaxRows()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setMaxRows(int max)
            throws SQLException
    {}

    @Override
    public void setEscapeProcessing(boolean enable)
            throws SQLException
    {}

    @Override
    public int getQueryTimeout()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds)
            throws SQLException
    {}

    @Override
    public void cancel()
            throws SQLException
    {}

    @Override
    public SQLWarning getWarnings()
            throws SQLException
    {
        return null;
    }

    @Override
    public void clearWarnings()
            throws SQLException
    {}

    @Override
    public void setCursorName(String name)
            throws SQLException
    {}

    @Override
    public boolean execute(String sql)
            throws SQLException
    {
        return false;
    }

    @Override
    public ResultSet getResultSet()
            throws SQLException
    {
        return null;
    }

    @Override
    public int getUpdateCount()
            throws SQLException
    {
        return 0;
    }

    @Override
    public boolean getMoreResults()
            throws SQLException
    {
        return false;
    }

    @Override
    public void setFetchDirection(int direction)
            throws SQLException
    {}

    @Override
    public int getFetchDirection()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void setFetchSize(int rows)
            throws SQLException
    {}

    @Override
    public int getFetchSize()
            throws SQLException
    {
        return 0;
    }

    @Override
    public int getResultSetConcurrency()
            throws SQLException
    {
        return 0;
    }

    @Override
    public int getResultSetType()
            throws SQLException
    {
        return 0;
    }

    @Override
    public void addBatch(String sql)
            throws SQLException
    {}

    @Override
    public void clearBatch()
            throws SQLException
    {}

    @Override
    public int[] executeBatch()
            throws SQLException
    {
        return new int[0];
    }

    @Override
    public Connection getConnection()
            throws SQLException
    {
        return null;
    }

    @Override
    public boolean getMoreResults(int current)
            throws SQLException
    {
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys()
            throws SQLException
    {
        return null;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException
    {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes)
            throws SQLException
    {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames)
            throws SQLException
    {
        return 0;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys)
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes)
            throws SQLException
    {
        return false;
    }

    @Override
    public boolean execute(String sql, String[] columnNames)
            throws SQLException
    {
        return false;
    }

    @Override
    public int getResultSetHoldability()
            throws SQLException
    {
        return 0;
    }

    @Override
    public boolean isClosed()
            throws SQLException
    {
        return false;
    }

    @Override
    public void setPoolable(boolean poolable)
            throws SQLException
    {}

    @Override
    public boolean isPoolable()
            throws SQLException
    {
        return false;
    }

    @Override
    public void closeOnCompletion()
            throws SQLException
    {}

    @Override
    public boolean isCloseOnCompletion()
            throws SQLException
    {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface)
            throws SQLException
    {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
            throws SQLException
    {
        return false;
    }
}
