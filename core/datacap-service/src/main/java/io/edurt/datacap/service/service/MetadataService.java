package io.edurt.datacap.service.service;

import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.spi.model.Response;

public interface MetadataService
{
    CommonResponse<Response> getDatabaseSchema(String code);
}
