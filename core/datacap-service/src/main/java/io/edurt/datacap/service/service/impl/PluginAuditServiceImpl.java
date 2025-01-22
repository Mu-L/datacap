package io.edurt.datacap.service.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.common.utils.DateUtils;
import io.edurt.datacap.convert.ConvertService;
import io.edurt.datacap.convert.model.ConvertRequest;
import io.edurt.datacap.convert.model.ConvertResponse;
import io.edurt.datacap.fs.FsRequest;
import io.edurt.datacap.fs.FsResponse;
import io.edurt.datacap.fs.FsService;
import io.edurt.datacap.plugin.PluginManager;
import io.edurt.datacap.service.activity.HeatmapActivity;
import io.edurt.datacap.service.entity.PluginAuditEntity;
import io.edurt.datacap.service.entity.UserEntity;
import io.edurt.datacap.service.initializer.InitializerConfigure;
import io.edurt.datacap.service.itransient.ContributionRadar;
import io.edurt.datacap.service.repository.PluginAuditRepository;
import io.edurt.datacap.service.security.UserDetailsService;
import io.edurt.datacap.service.service.PluginAuditService;
import io.edurt.datacap.spi.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressFBWarnings(value = {"DM_BOXED_PRIMITIVE_FOR_PARSING", "DM_DEFAULT_ENCODING", "EI_EXPOSE_REP2"})
public class PluginAuditServiceImpl
        implements PluginAuditService
{
    private final PluginAuditRepository pluginAuditRepository;
    private final InitializerConfigure initializer;
    private final PluginManager pluginManager;

    public PluginAuditServiceImpl(PluginAuditRepository pluginAuditRepository, InitializerConfigure initializer, PluginManager pluginManager)
    {
        this.pluginAuditRepository = pluginAuditRepository;
        this.initializer = initializer;
        this.pluginManager = pluginManager;
    }

    @Override
    public CommonResponse<Long> count()
    {
        return CommonResponse.success(this.pluginAuditRepository.countByUser(UserDetailsService.getUser()));
    }

    @Override
    public CommonResponse<List<HeatmapActivity>> getAllContribution()
    {
        UserEntity user = UserDetailsService.getUser();
        return CommonResponse.success(this.pluginAuditRepository.countByCreateTimeAndFindByUser(user));
    }

    @Override
    public CommonResponse<List<ContributionRadar>> getContributionRadar()
    {
        UserEntity user = UserDetailsService.getUser();
        List<ContributionRadar> contributionRadars = new ArrayList<>();
        List<Map<String, Object>> list = this.pluginAuditRepository.selectRadarByUserId(user.getId());
        Long count = list.stream().mapToLong(v -> Long.valueOf(String.valueOf(v.get("dataOfCount")))).sum();
        list.forEach(v -> {
            ContributionRadar radar = new ContributionRadar();
            radar.setLabel(String.valueOf(v.get("dataOfLabel")));
            radar.setCount(Long.valueOf(String.valueOf(v.get("dataOfCount"))));
            BigDecimal left = new BigDecimal(radar.getCount());
            BigDecimal right = new BigDecimal(count);
            BigDecimal divide = left.divide(right, 2, RoundingMode.HALF_UP);
            radar.setPercentage(Float.valueOf(divide.toString()) * 100);
            contributionRadars.add(radar);
        });
        return CommonResponse.success(contributionRadars);
    }

    @Override
    public CommonResponse<PluginAuditEntity> getById(Long id)
    {
        return pluginAuditRepository.findById(id)
                .map(CommonResponse::success)
                .orElse(CommonResponse.failure(String.format("PluginAudit [ %s ] not found", id)));
    }

    @Override
    public CommonResponse<Response> getData(String code)
    {
        return this.pluginAuditRepository.findByCode(code)
                .map(value -> {
                    Response response = new Response();
                    FsRequest fsRequest = FsRequest.builder()
                            .access(initializer.getFsConfigure().getAccess())
                            .secret(initializer.getFsConfigure().getSecret())
                            .endpoint(value.getHome())
                            .bucket(initializer.getFsConfigure().getBucket())
                            .fileName("result")
                            .build();
                    // If it is OSS third-party storage, rebuild the default directory
                    if (!initializer.getFsConfigure().getType().equals("LocalFs")) {
                        fsRequest.setEndpoint(initializer.getFsConfigure().getEndpoint());
                        fsRequest.setFileName(String.join(File.separator, value.getUser().getCode(), DateUtils.formatYMD(), String.join(File.separator, "adhoc", code), "result"));
                    }
                    pluginManager.getPlugin(initializer.getFsConfigure().getType())
                            .ifPresent(plugin -> {
                                FsService fsService = plugin.getService(FsService.class);
                                FsResponse fsResponse = fsService.reader(fsRequest);

                                if (!fsResponse.isSuccessful()) {
                                    response.setIsSuccessful(false);
                                    response.setMessage(fsResponse.getMessage());
                                    return;
                                }

                                pluginManager.getPlugin(value.getFormat())
                                        .ifPresent(it -> {
                                            ConvertRequest request = new ConvertRequest();
                                            request.setStream(fsResponse.getContext());
                                            ConvertService convertService = it.getService(ConvertService.class);

                                            ConvertResponse _response = convertService.formatStream(request);
                                            if (Boolean.TRUE.equals(_response.getSuccessful())) {
                                                response.setHeaders(_response.getHeaders()
                                                        .stream()
                                                        .map(String::valueOf)
                                                        .collect(Collectors.toList()));
                                                response.setColumns(_response.getColumns());
                                                response.setIsSuccessful(true);
                                            }
                                            else {
                                                response.setIsSuccessful(false);
                                                response.setMessage(_response.getMessage());
                                            }
                                        });
                            });
                    return CommonResponse.success(response);
                })
                .orElseGet(() -> CommonResponse.failure(String.format("Not found [ %s ] history", code)));
    }
}
