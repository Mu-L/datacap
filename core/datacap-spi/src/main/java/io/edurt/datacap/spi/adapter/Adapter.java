package io.edurt.datacap.spi.adapter;

import com.google.common.base.Preconditions;
import io.edurt.datacap.convert.ConvertFilter;
import io.edurt.datacap.convert.model.ConvertRequest;
import io.edurt.datacap.convert.model.ConvertResponse;
import io.edurt.datacap.plugin.PluginManager;
import io.edurt.datacap.spi.model.Response;

import java.util.List;

public interface Adapter
{
    Response handlerExecute(String content);

    default List<Object> handlerFormatter(PluginManager pluginManager, String format, List<String> headers, List<Object> columns)
    {
        return ConvertFilter.filter(pluginManager, format)
                .map(file -> {
                    ConvertRequest request = new ConvertRequest();
                    request.setHeaders(headers);
                    request.setColumns(columns);

                    ConvertResponse response = file.format(request);
                    Preconditions.checkArgument(Boolean.TRUE.equals(response.getSuccessful()), response.getMessage(), format);
                    return response.getColumns();
                })
                .orElseThrow(() -> new IllegalArgumentException("Unsupported format: " + format));
    }
}
