package io.edurt.datacap.plugin.natived.zookeeper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.spi.connection.Connection;
import io.edurt.datacap.spi.model.Configure;
import io.edurt.datacap.spi.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class ZookeeperConnection
        extends Connection
{
    private Configure configure;
    private Response response;
    private ZkClient client;

    public ZookeeperConnection(Configure configure, Response response)
    {
        super(configure, response);
    }

    @Override
    protected java.sql.Connection openConnection()
    {
        try {
            this.configure = getConfigure();
            this.response = getResponse();
            this.client = new ZkClient(this.configure.getHost(), 60000, 5000);
            response.setIsConnected(Boolean.TRUE);
        }
        catch (Exception ex) {
            log.error("Connection failed ", ex);
            response.setIsConnected(Boolean.FALSE);
            response.setMessage(ex.getMessage());
        }
        return null;
    }

    @Override
    public void destroy()
    {
        this.client.close();
        log.info("Connection close successful");
    }

    public ZkClient getClient()
    {
        return client;
    }
}
