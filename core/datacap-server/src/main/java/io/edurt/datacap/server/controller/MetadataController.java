package io.edurt.datacap.server.controller;

import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.service.service.MetadataService;
import io.edurt.datacap.spi.model.Response;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping(value = "/api/v1/metadata")
public class MetadataController
{
    private final MetadataService service;

    public MetadataController(MetadataService service)
    {
        this.service = service;
    }

    @PostMapping(value = "database/{code}")
    public CommonResponse<Response> fetchByDatabase(@PathVariable String code)
    {
        return this.service.getDatabaseSchema(code);
    }
}
