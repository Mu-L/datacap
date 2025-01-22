package io.edurt.datacap.spi.connection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.spi.model.Configure;
import io.edurt.datacap.spi.model.Response;
import io.edurt.datacap.spi.model.Time;

import java.util.Date;

@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public abstract class Connection
{
    protected final Configure configure;
    protected final Response response;
    protected Object connection;

    public Connection(Configure configure, Response response)
    {
        this.configure = configure;
        this.response = response;
        Time connectionTime = new Time();
        connectionTime.setStart(new Date().getTime());
        this.connection = this.openConnection();
        connectionTime.setEnd(new Date().getTime());
        this.response.setConnection(connectionTime);
    }

    protected abstract java.sql.Connection openConnection();

    public Object getConnection()
    {
        return this.connection;
    }

    public Response getResponse()
    {
        return this.response;
    }

    public Configure getConfigure()
    {
        return this.configure;
    }

    public abstract void destroy();
}
