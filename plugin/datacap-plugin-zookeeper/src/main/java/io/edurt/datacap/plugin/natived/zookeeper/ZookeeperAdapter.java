package io.edurt.datacap.plugin.natived.zookeeper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.spi.adapter.NativeAdapter;
import io.edurt.datacap.spi.model.Configure;
import io.edurt.datacap.spi.model.Response;
import io.edurt.datacap.spi.model.Time;
import io.edurt.datacap.sql.node.element.SelectElement;
import io.edurt.datacap.sql.statement.SelectStatement;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "REC_CATCH_EXCEPTION"},
        justification = "I prefer to suppress these FindBugs warnings")
public class ZookeeperAdapter
        extends NativeAdapter
{
    protected ZookeeperConnection zookeeperConnection;
    private final ZookeeperParser parser;

    public ZookeeperAdapter(ZookeeperConnection zookeeperConnection, ZookeeperParser parser)
    {
        super(zookeeperConnection, parser);
        this.zookeeperConnection = zookeeperConnection;
        this.parser = parser;
    }

    @Override
    public Response handlerExecute(String content)
    {
        Time processorTime = new Time();
        processorTime.setStart(new Date().getTime());
        Response response = this.zookeeperConnection.getResponse();
        Configure configure = this.zookeeperConnection.getConfigure();
        if (response.getIsConnected()) {
            List<String> headers = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<Object> columns = new ArrayList<>();
            try {
                SelectStatement statement = (SelectStatement) this.parser.getStatement();
                ZkClient client = this.zookeeperConnection.getClient();
                if (!statement.getSelectElements().isEmpty()) {
                    headers.addAll(statement.getSelectElements()
                            .stream()
                            .map(SelectElement::getColumn)
                            .collect(Collectors.toList()));
                }
                else {
                    headers.add("*");
                }
                types.add("String");
                client.getChildren(this.parser.getExecuteContext())
                        .forEach(column -> columns.add(Collections.singletonList(column)));
                response.setIsSuccessful(Boolean.TRUE);
            }
            catch (Exception ex) {
                log.error("Execute content failed content {} exception ", content, ex);
                response.setIsSuccessful(Boolean.FALSE);
                response.setMessage(ex.getMessage());
            }
            finally {
                response.setHeaders(headers);
                response.setTypes(types);
                response.setColumns(handlerFormatter(configure.getPluginManager(), configure.getFormat(), headers, columns));
            }
        }
        processorTime.setEnd(new Date().getTime());
        response.setProcessor(processorTime);
        return response;
    }
}
