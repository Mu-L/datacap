package io.edurt.datacap.spi;

import io.edurt.datacap.plugin.Service;
import io.edurt.datacap.spi.adapter.Adapter;
import io.edurt.datacap.spi.adapter.HttpAdapter;
import io.edurt.datacap.spi.adapter.JdbcAdapter;
import io.edurt.datacap.spi.adapter.NativeAdapter;
import io.edurt.datacap.spi.connection.Connection;
import io.edurt.datacap.spi.connection.JdbcConnection;
import io.edurt.datacap.spi.model.Configure;
import io.edurt.datacap.spi.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface PluginService
        extends Service
{
    ThreadLocal<Connection> local = new ThreadLocal<>();
    Logger log = LoggerFactory.getLogger(PluginService.class);

    default String validator()
    {
        return "SELECT version() AS version";
    }

    default PluginType type()
    {
        return PluginType.JDBC;
    }

    default String name()
    {
        return this.getClass().getSimpleName().replace("Plugin", "");
    }

    default String description()
    {
        return String.format("Integrate %s data sources", this.name());
    }

    default String driver()
    {
        return "io.edurt.datacap.JdbcDriver";
    }

    default String connectType()
    {
        return "datacap";
    }

    default void connect(Configure configure)
    {
        Response response = new Response();
        try {
            configure.setDriver(this.driver());
            configure.setType(this.connectType());
            configure.setUrl(Optional.of(url(configure)));
            local.set(new JdbcConnection(configure, response));
        }
        catch (Exception ex) {
            response.setIsConnected(Boolean.FALSE);
            response.setMessage(ex.getMessage());
            log.error("Error connecting :", ex);
        }
    }

    /**
     * 构建驱动路径
     * Build the driver path
     *
     * @param configure 配置信息 | Configuration information
     * @return 驱动路径 | Driver path
     */
    default String url(Configure configure)
    {
        if (configure.getUrl().isPresent()) {
            return configure.getUrl().get();
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:");
        buffer.append(configure.getType());
        buffer.append("://");
        buffer.append(configure.getHost());
        buffer.append(":");
        buffer.append(configure.getPort());
        if (configure.getDatabase().isPresent()) {
            buffer.append("/");
            buffer.append(configure.getDatabase().get());
        }
        if (configure.getSsl().isPresent()) {
            buffer.append(String.format("?ssl=%s", configure.getSsl().get()));
        }
        if (configure.getEnv().isPresent()) {
            Map<String, Object> env = configure.getEnv().get();
            List<String> flatEnv = env.entrySet()
                    .stream()
                    .map(value -> String.format("%s=%s", value.getKey(), value.getValue()))
                    .collect(Collectors.toList());
            if (configure.getSsl().isEmpty()) {
                buffer.append("?");
            }
            else {
                if (configure.getIsAppendChar()) {
                    buffer.append("&");
                }
            }
            buffer.append(String.join("&", flatEnv));
        }
        return buffer.toString();
    }

    @Deprecated
    default Response execute(String content)
    {
        Connection connection = local.get();
        Response response = new Response();
        response.setContent(content);
        if (connection != null) {
            log.info("Execute [ {} ] plugin started", this.name());
            Adapter adapter;
            if (type().equals(PluginType.JDBC)) {
                adapter = new JdbcAdapter(connection);
            }
            else if (type().equals(PluginType.NATIVE)) {
                adapter = new NativeAdapter(connection);
            }
            else {
                adapter = new HttpAdapter(connection);
            }
            response = adapter.handlerExecute(content);
            log.info("Execute [ {} ] plugin end", this.name());
        }
        destroy();
        return response;
    }

    default Response execute(Configure configure, String content)
    {
        this.connect(configure);
        return this.execute(content);
    }

    default Response getDatabases(Configure configure)
    {
        String sql = "SELECT\n" +
                "    SCHEMA_NAME AS object_name,\n" +
                "    'DATABASE' AS object_type,\n" +
                "    '' AS object_comment\n" +
                "FROM information_schema.SCHEMATA;";
        return this.execute(configure, sql);
    }

    /**
     * 根据数据库获取数据表结构
     * Get table structure by database
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @return 数据表结构 | Table structure
     */
    default Response getTablesForTable(Configure configure, String database)
    {
        String sql = "SELECT\n" +
                "    CASE\n" +
                "        WHEN type = 'BASE TABLE' THEN '表'\n" +
                "        WHEN type = 'VIEW' THEN '视图'\n" +
                "        WHEN type = 'FUNCTION' THEN '函数'\n" +
                "        WHEN type = 'PROCEDURE' THEN '存储过程'\n" +
                "    END AS type_name,\n" +
                "    object_name,\n" +
                "    object_comment\n" +
                "FROM (\n" +
                "    -- 表\n" +
                "    SELECT\n" +
                "        'BASE TABLE' as type,\n" +
                "        TABLE_NAME as object_name,\n" +
                "        TABLE_COMMENT as object_comment\n" +
                "    FROM information_schema.TABLES\n" +
                "    WHERE TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_TYPE = 'BASE TABLE'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 视图\n" +
                "    SELECT\n" +
                "        'VIEW' as type,\n" +
                "        TABLE_NAME as object_name,\n" +
                "        TABLE_COMMENT as object_comment\n" +
                "    FROM information_schema.TABLES\n" +
                "    WHERE TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_TYPE = 'VIEW'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 函数\n" +
                "    SELECT\n" +
                "        'FUNCTION' as type,\n" +
                "        ROUTINE_NAME as object_name,\n" +
                "        ROUTINE_COMMENT as object_comment\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_SCHEMA = '{0}'\n" +
                "        AND ROUTINE_TYPE = 'FUNCTION'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 存储过程（查询）\n" +
                "    SELECT\n" +
                "        'PROCEDURE' as type,\n" +
                "        ROUTINE_NAME as object_name,\n" +
                "        ROUTINE_COMMENT as object_comment\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_SCHEMA = '{0}'\n" +
                "        AND ROUTINE_TYPE = 'PROCEDURE'\n" +
                ") AS combined_objects\n" +
                "ORDER BY\n" +
                "    FIELD(type, 'BASE TABLE', 'VIEW', 'FUNCTION', 'PROCEDURE'),\n" +
                "    object_name;";

        return this.execute(configure, sql.replace("{0}", database));
    }

    default void destroy()
    {
        Connection connection = local.get();
        if (connection != null) {
            connection.destroy();
            local.remove();
        }
    }
}
