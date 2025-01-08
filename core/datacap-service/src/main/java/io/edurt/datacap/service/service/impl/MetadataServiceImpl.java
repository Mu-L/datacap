package io.edurt.datacap.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.plugin.PluginManager;
import io.edurt.datacap.service.repository.SourceRepository;
import io.edurt.datacap.service.service.MetadataService;
import io.edurt.datacap.spi.PluginService;
import io.edurt.datacap.spi.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class MetadataServiceImpl
        implements MetadataService
{
    private final PluginManager pluginManager;
    private final SourceRepository repository;

    public MetadataServiceImpl(PluginManager pluginManager, SourceRepository repository)
    {
        this.pluginManager = pluginManager;
        this.repository = repository;
    }

    @Override
    public CommonResponse<Response> getDatabases(String code)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getDatabases(entity.toConfigure(pluginManager, plugin)));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<Response> getTables(String code, String database)
    {
        return repository.findByCode(code)
                .map(entity -> pluginManager.getPlugin(entity.getType())
                        .map(plugin -> {
                            PluginService service = plugin.getService(PluginService.class);
                            return CommonResponse.success(service.getTablesForTable(entity.toConfigure(pluginManager, plugin), database));
                        })
                        .orElseGet(() -> CommonResponse.failure(String.format("Plugin [ %s ] not found", entity.getType()))))
                .orElseGet(() -> CommonResponse.failure(String.format("Resource [ %s ] not found", code)));
    }
}
