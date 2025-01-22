package io.edurt.datacap.plugin;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.spi.PluginService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressFBWarnings(value = {"EI_EXPOSE_REP"})
public class Db2Service
        implements PluginService
{
    @Override
    public String validator()
    {
        return "SELECT\n" +
                "  replace(SERVICE_LEVEL, 'DB2 v', '') AS version\n" +
                "FROM\n" +
                "  SYSIBMADM.ENV_INST_INFO";
    }

    @Override
    public String driver()
    {
        return "com.ibm.db2.jcc.DB2Driver";
    }

    @Override
    public String connectType()
    {
        return "db2";
    }
}
