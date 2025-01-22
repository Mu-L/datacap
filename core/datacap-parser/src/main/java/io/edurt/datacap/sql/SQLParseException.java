package io.edurt.datacap.sql;

public class SQLParseException
        extends RuntimeException
{
    public SQLParseException(String message)
    {
        super(message);
    }

    public SQLParseException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
