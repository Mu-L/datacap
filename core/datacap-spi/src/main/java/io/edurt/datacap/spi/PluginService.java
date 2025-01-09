package io.edurt.datacap.spi;

import com.google.common.collect.Lists;
import io.edurt.datacap.common.sql.SqlBuilder;
import io.edurt.datacap.common.sql.configure.SqlBody;
import io.edurt.datacap.common.sql.configure.SqlType;
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

    /**
     * 获取数据库列表
     * Get database list
     *
     * @param configure 配置信息 | Configuration information
     * @return 数据库列表 | Database list
     */
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
    default Response getTablesForDatabase(Configure configure, String database)
    {
        String sql = "SELECT\n" +
                "    CASE\n" +
                "        WHEN type = 'BASE TABLE' THEN 'table'\n" +
                "        WHEN type = 'VIEW' THEN 'view'\n" +
                "        WHEN type = 'FUNCTION' THEN 'function'\n" +
                "        WHEN type = 'PROCEDURE' THEN 'procedure'\n" +
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

    /**
     * 根据数据库，数据表获取列结构
     * Get column structure by database, table
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @param table 数据表 | Table
     * @return 数据列结构 | Column structure
     */
    default Response getColumnsForTable(Configure configure, String database, String table)
    {
        String sql = "SELECT detail.*\n" +
                "FROM (\n" +
                "    -- 列信息\n" +
                "    SELECT\n" +
                "        'column' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        COLUMN_TYPE as object_data_type,\n" +
                "        IS_NULLABLE as object_nullable,\n" +
                "        COLUMN_DEFAULT as object_default_value,\n" +
                "        COLUMN_COMMENT as object_comment,\n" +
                "        ORDINAL_POSITION as object_position,\n" +
                "        '' as object_definition\n" +
                "    FROM\n" +
                "        information_schema.COLUMNS\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}' -- 数据库名参数\n" +
                "        AND TABLE_NAME = '{1}' -- 表名参数\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 主键信息\n" +
                "    SELECT\n" +
                "        'primary' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT('PRIMARY KEY on (',\n" +
                "            GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION),\n" +
                "            ')'\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.KEY_COLUMN_USAGE\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "        AND CONSTRAINT_NAME = 'PRIMARY'\n" +
                "    GROUP BY\n" +
                "        TABLE_SCHEMA, TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 索引信息\n" +
                "    SELECT DISTINCT\n" +
                "        'index' as type_name,\n" +
                "        COLUMN_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT(\n" +
                "            CASE NON_UNIQUE\n" +
                "                WHEN 1 THEN 'Non-unique'\n" +
                "                ELSE 'Unique'\n" +
                "            END,\n" +
                "            ' index on (',\n" +
                "            GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX),\n" +
                "            ')'\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.STATISTICS\n" +
                "    WHERE\n" +
                "        TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "    GROUP BY\n" +
                "        TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, NON_UNIQUE\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 触发器信息\n" +
                "    SELECT\n" +
                "        'trigger' as type_name,\n" +
                "        TRIGGER_NAME as object_name,\n" +
                "        '' as object_data_type,\n" +
                "        '' as object_nullable,\n" +
                "        '' as object_default_value,\n" +
                "        '' as object_comment,\n" +
                "        0 as object_position,\n" +
                "        CONCAT(\n" +
                "            'TRIGGER ',\n" +
                "            ACTION_TIMING, ' ',\n" +
                "            EVENT_MANIPULATION\n" +
                "        ) as object_definition\n" +
                "    FROM\n" +
                "        information_schema.TRIGGERS\n" +
                "    WHERE\n" +
                "        EVENT_OBJECT_SCHEMA = '{0}'\n" +
                "        AND EVENT_OBJECT_TABLE = '{1}'\n" +
                ") detail\n" +
                "ORDER BY\n" +
                "    FIELD(type_name, 'column', 'primary', 'index', 'trigger'),\n" +
                "    object_position,\n" +
                "    object_name;";

        return this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
    }

    /**
     * 根据数据库获取数据库信息
     * Get database information by database
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @return 数据库信息 | Database information
     */
    default Response getDatabaseInfo(Configure configure, String database)
    {
        String sql = "SELECT \n" +
                "    s.SCHEMA_NAME as object_name,\n" +
                "    s.DEFAULT_CHARACTER_SET_NAME as object_charset,\n" +
                "    s.DEFAULT_COLLATION_NAME as object_collation,\n" +
                "    db_stats.object_create_time,\n" +
                "    db_stats.object_update_time,\n" +
                "    db_stats.object_data_size,\n" +
                "    db_stats.object_index_size,\n" +
                "    db_stats.object_total_size,\n" +
                "    db_stats.object_table_count,\n" +
                "    db_stats.object_column_count,\n" +
                "    db_stats.object_index_count,\n" +
                "    db_stats.object_view_count,\n" +
                "    db_stats.object_procedure_count,\n" +
                "    db_stats.object_trigger_count,\n" +
                "    db_stats.object_foreign_key_count,\n" +
                "    db_stats.object_total_rows\n" +
                "FROM\n" +
                "    information_schema.SCHEMATA s,\n" +
                "    (\n" +
                "        SELECT\n" +
                "            TABLE_SCHEMA,\n" +
                "            MIN(CREATE_TIME) as object_create_time,\n" +
                "            MAX(UPDATE_TIME) as object_update_time,\n" +
                "            ROUND(SUM(DATA_LENGTH)/1024/1024, 2) as object_data_size,\n" +
                "            ROUND(SUM(INDEX_LENGTH)/1024/1024, 2) as object_index_size,\n" +
                "            ROUND(SUM(DATA_LENGTH + INDEX_LENGTH)/1024/1024, 2) as object_total_size,\n" +
                "            COUNT(*) as object_table_count,\n" +
                "            SUM(TABLE_ROWS) as object_total_rows,\n" +
                "            (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '{0}') as object_column_count,\n" +
                "            (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = '{0}') as object_index_count,\n" +
                "            (SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA = '{0}') as object_view_count,\n" +
                "            (SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = '{0}' AND ROUTINE_TYPE = 'PROCEDURE') as object_procedure_count,\n" +
                "            (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = '{0}') as object_trigger_count,\n" +
                "            (SELECT COUNT(DISTINCT CONSTRAINT_NAME) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = '{0}') as object_foreign_key_count\n" +
                "        FROM\n" +
                "            information_schema.TABLES\n" +
                "        WHERE\n" +
                "            TABLE_SCHEMA = '{0}'\n" +
                "            AND TABLE_TYPE = 'BASE TABLE'\n" +
                "        GROUP BY\n" +
                "            TABLE_SCHEMA\n" +
                "    ) as db_stats\n" +
                "WHERE\n" +
                "    s.SCHEMA_NAME = '{0}';";

        return this.execute(
                configure,
                sql.replace("{0}", database)
        );
    }

    /**
     * 根据表获取表信息
     * Get table information by table data
     *
     * @param configure 配置信息 | Configuration information
     * @param database 数据库 | Database
     * @param table 表 | Table
     * @return 表信息 | Table information
     */
    default Response getTableInfo(Configure configure, String database, String table)
    {
        String sql = "WITH object_info AS (\n" +
                "    -- 获取表和视图类型\n" +
                "    SELECT\n" +
                "        TABLE_NAME as name,\n" +
                "        CASE\n" +
                "            WHEN TABLE_TYPE = 'BASE TABLE' THEN 'table'\n" +
                "            WHEN TABLE_TYPE = 'VIEW' THEN 'view'  \n" +
                "        END as type\n" +
                "    FROM information_schema.TABLES\n" +
                "    WHERE TABLE_SCHEMA = '{0}'\n" +
                "        AND TABLE_NAME = '{1}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 获取函数和存储过程类型\n" +
                "    SELECT\n" +
                "        ROUTINE_NAME as name,\n" +
                "        LOWER(ROUTINE_TYPE) as type\n" +
                "    FROM information_schema.ROUTINES\n" +
                "    WHERE ROUTINE_SCHEMA = '{0}'\n" +
                "        AND ROUTINE_NAME = '{1}'\n" +
                ")\n" +
                "SELECT d.*\n" +
                "FROM object_info i\n" +
                "LEFT JOIN (\n" +
                "    -- 表的详细信息\n" +
                "    SELECT\n" +
                "        'table' as object_type,\n" +
                "        t.TABLE_NAME as object_name,\n" +
                "        t.ENGINE as object_engine,\n" +
                "        t.TABLE_COLLATION as object_collation,\n" +
                "        t.TABLE_COMMENT as object_comment,\n" +
                "        t.CREATE_TIME as object_create_time,\n" +
                "        t.UPDATE_TIME as object_update_time,\n" +
                "        ROUND(t.DATA_LENGTH/1024/1024, 2) as object_data_size,\n" +
                "        ROUND(t.INDEX_LENGTH/1024/1024, 2) as object_index_size,\n" +
                "        t.TABLE_ROWS as object_rows,\n" +
                "        (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = t.TABLE_SCHEMA AND TABLE_NAME = t.TABLE_NAME) as object_column_count,\n" +
                "        (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = t.TABLE_SCHEMA AND TABLE_NAME = t.TABLE_NAME) as object_index_count,\n" +
                "        'table' as type_name,\n" +
                "        t.ROW_FORMAT as object_format,\n" +
                "        t.AVG_ROW_LENGTH as object_avg_row_length,\n" +
                "        t.AUTO_INCREMENT as object_auto_increment\n" +
                "    FROM information_schema.TABLES t\n" +
                "    WHERE t.TABLE_SCHEMA = '{0}'\n" +
                "        AND t.TABLE_NAME = '{1}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 视图的详细信息\n" +
                "    SELECT\n" +
                "        'view' as object_type,\n" +
                "        TABLE_NAME as object_name,\n" +
                "        'view' as object_engine,\n" +
                "        '' as object_collation,\n" +
                "        '' as object_comment,\n" +
                "        NULL as object_create_time,\n" +
                "        NULL as object_update_time,\n" +
                "        0 as object_data_size,\n" +
                "        0 as object_index_size,\n" +
                "        0 as object_rows,\n" +
                "        (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = v.TABLE_SCHEMA AND TABLE_NAME = v.TABLE_NAME) as object_column_count,\n" +
                "        0 as object_index_count,\n" +
                "        'view' as type_name,\n" +
                "        NULL as object_format,\n" +
                "        0 as object_avg_row_length,\n" +
                "        NULL as object_auto_increment\n" +
                "    FROM information_schema.VIEWS v\n" +
                "    WHERE v.TABLE_SCHEMA = '{0}'\n" +
                "        AND v.TABLE_NAME = '{1}'\n" +
                "\n" +
                "    UNION ALL\n" +
                "\n" +
                "    -- 函数或存储过程的详细信息\n" +
                "    SELECT\n" +
                "        LOWER(r.ROUTINE_TYPE) as object_type,\n" +
                "        r.ROUTINE_NAME as object_name,\n" +
                "        LOWER(r.ROUTINE_TYPE) as object_engine,\n" +
                "        '' as object_collation,\n" +
                "        '' as object_comment,\n" +
                "        r.CREATED as object_create_time,\n" +
                "        r.LAST_ALTERED as object_update_time,\n" +
                "        0 as object_data_size,\n" +
                "        0 as object_index_size,\n" +
                "        0 as object_rows,\n" +
                "        0 as object_column_count,\n" +
                "        0 as object_index_count,\n" +
                "        LOWER(r.ROUTINE_TYPE) as type_name,\n" +
                "        NULL as object_format,\n" +
                "        0 as object_avg_row_length,\n" +
                "        NULL as object_auto_increment\n" +
                "    FROM information_schema.ROUTINES r\n" +
                "    WHERE r.ROUTINE_SCHEMA = '{0}'\n" +
                "        AND r.ROUTINE_NAME = '{1}'\n" +
                ") d ON i.type = d.type_name;";

        return this.execute(
                configure,
                sql.replace("{0}", database)
                        .replace("{1}", table)
        );
    }

    /**
     * 更新表的自增值
     * Update table auto increment value
     *
     * @param configure 配置信息 | Configuration information
     * @param body SQL 语句 | SQL statement
     * @return 执行结果 | Execution result
     */
    default Response updateAutoIncrement(Configure configure, SqlBody body)
    {
        body.setType(SqlType.ALTER);
        String sql = new SqlBuilder(body)
                .getSql();

        return this.getResponse(sql, configure, body);
    }

    private Response getResponse(String sql, Configure configure, SqlBody value)
    {
        Response response;
        if (value.isPreview()) {
            response = Response.builder()
                    .isSuccessful(true)
                    .isConnected(true)
                    .headers(Lists.newArrayList())
                    .columns(Lists.newArrayList())
                    .types(Lists.newArrayList())
                    .content(sql)
                    .build();
        }
        else {
            response = this.execute(configure, sql);
            response.setContent(sql);
        }
        return response;
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
