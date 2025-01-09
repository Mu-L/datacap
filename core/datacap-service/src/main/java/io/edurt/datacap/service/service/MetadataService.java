package io.edurt.datacap.service.service;

import io.edurt.datacap.common.response.CommonResponse;
import io.edurt.datacap.common.sql.configure.SqlBody;
import io.edurt.datacap.spi.model.Response;

public interface MetadataService
{
    CommonResponse<Response> getDatabases(String code);

    CommonResponse<Response> getTables(String code, String database);

    CommonResponse<Response> getColumns(String code, String database, String table);

    CommonResponse<Response> getDatabase(String code, String database);

    CommonResponse<Response> getTable(String code, String database, String table);

    CommonResponse<Response> updateAutoIncrement(String code, SqlBody configure, String database, String table);
}
