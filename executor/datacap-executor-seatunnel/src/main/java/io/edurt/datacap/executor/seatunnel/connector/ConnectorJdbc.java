package io.edurt.datacap.executor.seatunnel.connector;

import com.google.common.collect.Maps;
import io.edurt.datacap.executor.configure.ExecutorConfigure;
import io.edurt.datacap.spi.model.Configure;

import java.util.Map;
import java.util.Properties;

public class ConnectorJdbc
        extends Connector
{
    public ConnectorJdbc(String type, ExecutorConfigure configure)
    {
        super(type, configure, configure.getSupportOptions());
    }

    @Override
    protected Properties formatToProperties(Configure originalConfigure)
    {
        Properties properties = new Properties();
        return properties;
    }

    @Override
    public Map<String, Object> formatToMap()
    {
        Map<String, Object> node = Maps.newConcurrentMap();
        node.put("Jdbc", formatToProperties(configure.getConfigure()));
        return node;
    }
}
