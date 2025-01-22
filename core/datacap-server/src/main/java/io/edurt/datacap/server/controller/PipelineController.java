package io.edurt.datacap.server.controller;

import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.service.body.PipelineBody;
import io.edurt.datacap.service.entity.PipelineEntity;
import io.edurt.datacap.service.repository.PipelineRepository;
import io.edurt.datacap.service.service.PipelineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping(value = "/api/v1/pipeline")
public class PipelineController
        extends BaseController<PipelineEntity>
{
    private final PipelineRepository repository;
    private final PipelineService service;

    public PipelineController(PipelineRepository repository, PipelineService service)
    {
        super(repository, service);
        this.repository = repository;
        this.service = service;
    }

    @PostMapping(value = "/submit")
    public CommonResponse<Object> submit(@RequestBody PipelineBody configure)
    {
        return service.submit(configure);
    }

    @GetMapping(value = "/log/{code}")
    public CommonResponse<List<String>> log(@PathVariable String code)
    {
        return service.log(code);
    }
}
