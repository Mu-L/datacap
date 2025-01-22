package io.edurt.datacap.service.service.impl;

import com.google.common.io.Files;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.common.enums.ServiceState;
import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.plugin.PluginManager;
import io.edurt.datacap.plugin.PluginType;
import io.edurt.datacap.service.adapter.PageRequestAdapter;
import io.edurt.datacap.service.body.FilterBody;
import io.edurt.datacap.service.body.SharedSourceBody;
import io.edurt.datacap.service.body.SourceBody;
import io.edurt.datacap.service.common.ConfigureUtils;
import io.edurt.datacap.service.common.PluginUtils;
import io.edurt.datacap.service.configure.IConfigure;
import io.edurt.datacap.service.configure.IConfigureField;
import io.edurt.datacap.service.entity.PageEntity;
import io.edurt.datacap.service.entity.PluginEntity;
import io.edurt.datacap.service.entity.ScheduledHistoryEntity;
import io.edurt.datacap.service.entity.SourceEntity;
import io.edurt.datacap.service.entity.UserEntity;
import io.edurt.datacap.service.repository.BaseRepository;
import io.edurt.datacap.service.repository.ScheduledHistoryRepository;
import io.edurt.datacap.service.repository.SourceRepository;
import io.edurt.datacap.service.repository.UserRepository;
import io.edurt.datacap.service.security.UserDetailsService;
import io.edurt.datacap.service.service.SourceService;
import io.edurt.datacap.service.wrapper.ResponseWrapper;
import io.edurt.datacap.spi.PluginService;
import io.edurt.datacap.spi.model.Configure;
import io.edurt.datacap.spi.model.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
@SuppressFBWarnings(value = {"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "REC_CATCH_EXCEPTION", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "EI_EXPOSE_REP2"})
public class SourceServiceImpl
        implements SourceService
{
    private final SourceRepository sourceRepository;
    private final UserRepository userRepository;
    private final ScheduledHistoryRepository scheduledHistoryRepository;
    private final PluginManager pluginManager;
    private final Environment environment;

    public SourceServiceImpl(
            SourceRepository sourceRepository,
            UserRepository userRepository,
            ScheduledHistoryRepository scheduledHistoryRepository,
            PluginManager pluginManager,
            Environment environment
    )
    {
        this.sourceRepository = sourceRepository;
        this.userRepository = userRepository;
        this.scheduledHistoryRepository = scheduledHistoryRepository;
        this.pluginManager = pluginManager;
        this.environment = environment;
    }

    @Override
    public CommonResponse<PageEntity<SourceEntity>> getAll(BaseRepository<SourceEntity, Long> repository, FilterBody filter)
    {
        UserEntity user = UserDetailsService.getUser();
        Page<SourceEntity> page = this.sourceRepository.findAllByUserOrPublishIsTrue(user, PageRequestAdapter.of(filter));
        // Populate pipeline configuration information
        page.getContent().forEach(item -> {
            IConfigure fromConfigure = PluginUtils.loadYamlConfigure(item.getType(), item.getType(), item.getType(), environment);
            if (fromConfigure != null) {
                item.setPipelines(fromConfigure.getPipelines());
            }
        });
        return CommonResponse.success(PageEntity.build(page));
    }

    @SneakyThrows
    @Override
    public CommonResponse<Long> delete(Long id)
    {
        Optional<SourceEntity> entityOptional = this.sourceRepository.findById(id);
        if (entityOptional.isPresent()) {
            SourceEntity source = entityOptional.get();
            if (source.isUsedConfig()) {
                String configHome = environment.getProperty("datacap.config.data");
                if (StringUtils.isEmpty(configHome)) {
                    configHome = String.join(File.separator, System.getProperty("user.dir"), "config");
                }
                String destination = String.join(File.separator, configHome, source.getUser().getUsername(), source.getType(), String.valueOf(source.getId()));
                FileUtils.deleteDirectory(new File(destination));
            }
            this.sourceRepository.deleteById(id);
        }
        return CommonResponse.success(id);
    }

    @Override
    public CommonResponse<SourceEntity> getByCode(BaseRepository<SourceEntity, Long> repository, String code)
    {
        return repository.findByCode(code)
                .map(entity -> {
                    SourceBody configure = new SourceBody();
                    configure.setId(entity.getId());
                    configure.setName(entity.getType());
                    configure.setType(entity.getProtocol());
                    // Load default configure
                    IConfigure iConfigure = PluginUtils.loadYamlConfigure(configure.getType(), configure.getName(), configure.getName(), environment);
                    configure.setConfigure(ConfigureUtils.preparedConfigure(iConfigure, entity));
                    entity.setSchema(iConfigure);
                    return CommonResponse.success(entity);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Source [ %s ] not found", code)));
    }

    @Override
    public CommonResponse<List<PluginEntity>> getPlugins()
    {
        List<PluginEntity> plugins = Lists.newArrayList();
        pluginManager.getPluginInfos()
                .stream()
                .filter(v -> v.getType().equals(PluginType.CONNECTOR))
                .forEach(plugin -> {
                    PluginEntity entity = new PluginEntity();
                    entity.setName(plugin.getName());
                    entity.setDescription(plugin.getName());
                    entity.setType(plugin.getType().getName());
                    entity.setConfigure(PluginUtils.loadYamlConfigure("JDBC", plugin.getName(), plugin.getName(), environment));
                    plugins.add(entity);
                });
        return CommonResponse.success(plugins);
    }

    @Override
    public CommonResponse<Long> count()
    {
        return CommonResponse.success(this.sourceRepository.countByUserOrPublishIsTrue(UserDetailsService.getUser()));
    }

    @Override
    public CommonResponse<Object> shared(SharedSourceBody configure)
    {
        Optional<SourceEntity> sourceOptional = this.sourceRepository.findById(configure.getSourceId());
        if (!sourceOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.SOURCE_NOT_FOUND);
        }

        Optional<UserEntity> userOptional = this.userRepository.findById(configure.getUserId());
        if (!userOptional.isPresent()) {
            return CommonResponse.failure(ServiceState.USER_NOT_FOUND);
        }

        SourceEntity source = sourceOptional.get();
        source.setUser(userOptional.get());
        source.setPublish(configure.getPublish());
        return CommonResponse.success(this.sourceRepository.save(source));
    }

    @Override
    public CommonResponse<ResponseWrapper> testConnection(SourceBody configure)
    {
        return pluginManager.getPlugin(configure.getType())
                .<CommonResponse<ResponseWrapper>>map(plugin -> {
                    // Check configure
                    IConfigure iConfigure = PluginUtils.loadYamlConfigure(configure.getType(), configure.getType(), configure.getType(), environment);
                    if (ObjectUtils.isEmpty(iConfigure) || iConfigure.getConfigures().size() != configure.getConfigure().getConfigures().size()) {
                        return CommonResponse.failure(ServiceState.PLUGIN_CONFIGURE_MISMATCH);
                    }

                    // Filter required
                    List<IConfigureField> requiredMismatchConfigures = configure.getConfigure().getConfigures()
                            .stream()
                            .filter(IConfigureField::isRequired)
                            .filter(v -> ObjectUtils.isEmpty(v.getValue()))
                            .collect(Collectors.toList());
                    if (!requiredMismatchConfigures.isEmpty()) {
                        return CommonResponse.failure(ServiceState.PLUGIN_CONFIGURE_REQUIRED, ConfigureUtils.preparedMessage(requiredMismatchConfigures));
                    }

                    // The filter parameter value is null data
                    PluginService pluginService = plugin.getService(PluginService.class);
                    List<IConfigureField> applyConfigures = ConfigureUtils.filterNotEmpty(configure.getConfigure().getConfigures());
                    Configure _configure = ConfigureUtils.preparedConfigure(applyConfigures);
                    _configure.setPluginManager(pluginManager);
                    _configure.setPlugin(plugin);

                    // Adapter file configure
                    if (_configure.isUsedConfig()) {
                        String cacheHome = environment.getProperty("datacap.cache.data");
                        if (StringUtils.isEmpty(cacheHome)) {
                            cacheHome = String.join(File.separator, System.getProperty("user.dir"), "cache");
                        }
                        _configure.setHome(cacheHome);
                        _configure.setUsername(Optional.of(UserDetailsService.getUser().getUsername()));
                    }

                    Response response = pluginService.execute(_configure, pluginService.validator());
                    if (response.getIsSuccessful()) {
                        return CommonResponse.success(ResponseWrapper.from(response));
                    }
                    return CommonResponse.failure(ServiceState.PLUGIN_EXECUTE_FAILED, response.getMessage());
                })
                .orElse(CommonResponse.failure(ServiceState.PLUGIN_NOT_FOUND));
    }

    @Override
    public CommonResponse<SourceEntity> saveOrUpdate(BaseRepository<SourceEntity, Long> repository, SourceEntity configure)
    {
        return pluginManager.getPlugin(configure.getName())
                .<CommonResponse<SourceEntity>>map(plugin -> {
                    // Check configure
                    IConfigure iConfigure = PluginUtils.loadYamlConfigure(configure.getType(), configure.getType(), configure.getType(), environment);
                    if (ObjectUtils.isEmpty(iConfigure) || iConfigure.getConfigures().size() != configure.getConfigure().getConfigures().size()) {
                        return CommonResponse.failure(ServiceState.PLUGIN_CONFIGURE_MISMATCH);
                    }

                    // Filter required
                    List<IConfigureField> requiredMismatchConfigures = configure.getConfigure()
                            .getConfigures()
                            .stream()
                            .filter(IConfigureField::isRequired)
                            .filter(v -> ObjectUtils.isEmpty(v.getValue()))
                            .collect(Collectors.toList());
                    if (!requiredMismatchConfigures.isEmpty()) {
                        return CommonResponse.failure(ServiceState.PLUGIN_CONFIGURE_REQUIRED, ConfigureUtils.preparedMessage(requiredMismatchConfigures));
                    }

                    // The filter parameter value is null data
                    List<IConfigureField> applyConfigures = ConfigureUtils.filterNotEmpty(configure.getConfigure().getConfigures());
                    SourceEntity source = ConfigureUtils.preparedSourceEntity(applyConfigures);
                    source.setProtocol(configure.getType());
                    source.setType(configure.getName());
                    source.setUser(UserDetailsService.getUser());
                    source.setId(configure.getId());
                    source.setCode(configure.getCode());

                    if (StringUtils.isNotEmpty(configure.getVersion())) {
                        source.setVersion(configure.getVersion());
                        source.setAvailable(true);
                    }
                    else {
                        source.setAvailable(false);
                    }

                    this.sourceRepository.save(source);

                    // Copy file to local data
                    if (source.isUsedConfig()) {
                        String cacheHome = environment.getProperty("datacap.cache.data");
                        if (StringUtils.isEmpty(cacheHome)) {
                            cacheHome = String.join(File.separator, System.getProperty("user.dir"), "cache");
                        }
                        String configHome = environment.getProperty("datacap.config.data");
                        if (StringUtils.isEmpty(configHome)) {
                            configHome = String.join(File.separator, System.getProperty("user.dir"), "config");
                        }
                        File sourceFile = new File(String.join(File.separator, cacheHome, source.getUser().getUsername(), source.getType()));
                        String destination = String.join(File.separator, configHome, source.getUser().getUsername(), source.getType(), String.valueOf(source.getId()));
                        File directory = new File(destination);
                        try {
                            if (!directory.exists()) {
                                directory.mkdirs();
                            }
                            for (File file : requireNonNull(sourceFile.listFiles())) {
                                Files.copy(file, new File(String.join(File.separator, destination, file.getName())));
                            }
                            FileUtils.deleteDirectory(sourceFile);
                        }
                        catch (Exception e) {
                            return CommonResponse.failure("Copy failed: " + e.getMessage());
                        }
                    }

                    return CommonResponse.success(source);
                })
                .orElse(CommonResponse.failure(ServiceState.PLUGIN_NOT_FOUND));
    }

    @Override
    public CommonResponse<PageEntity<ScheduledHistoryEntity>> getHistory(String code, FilterBody filter)
    {
        return sourceRepository.findByCode(code)
                .map(entity -> {
                    Pageable pageable = PageRequestAdapter.of(filter);
                    return CommonResponse.success(PageEntity.build(this.scheduledHistoryRepository.findAllBySource(entity, pageable)));
                })
                .orElse(CommonResponse.failure(String.format("Source [ %s ] not found", code)));
    }
}
