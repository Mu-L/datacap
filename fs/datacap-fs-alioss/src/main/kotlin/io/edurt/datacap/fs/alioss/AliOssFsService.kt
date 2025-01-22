package io.edurt.datacap.fs.alioss

import io.edurt.datacap.fs.FsRequest
import io.edurt.datacap.fs.FsResponse
import io.edurt.datacap.fs.FsService
import io.edurt.datacap.fs.alioss.IOUtils.Companion.copy
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.lang.String.join

class AliOssFsService : FsService
{
    private val log = getLogger(AliOssFsService::class.java)

    override fun writer(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("AliOssFs writer origin path [ {} ]", request.fileName)
        val targetPath = join(File.separator, request.endpoint, request.bucket, request.fileName)
        val response = FsResponse.builder()
            .origin(request.fileName)
            .remote(targetPath)
            .successful(true)
            .build()
        log.info("AliOssFs writer target path [ {} ]", request.fileName)
        try
        {
            val key = copy(request, request.stream, request.fileName)
            response.remote = key
            log.info("AliOssFs writer [ {} ] successfully", key)
        }
        catch (e: Exception)
        {
            log.error("AliOssFs writer error", e)
            response.isSuccessful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        log.info("AliOssFs reader origin path [ {} ]", request.fileName)
        val response = FsResponse.builder()
            .remote(request.fileName)
            .successful(true)
            .build()
        try
        {
            response.context = IOUtils.reader(request)
            log.info("AliOssFs reader [ {} ] successfully", request.fileName)
        }
        catch (e: java.lang.Exception)
        {
            log.error("AliOssFs reader error", e)
            response.isSuccessful = false
            response.message = e.message
        }
        return response
    }

    override fun delete(request: FsRequest?): FsResponse
    {
        requireNotNull(request) { "request must not be null" }

        try
        {
            val status = IOUtils.delete(request)
            log.info("AliOssFs delete [ {} ] successfully", request.fileName)
            return FsResponse.builder()
                .successful(status)
                .build()
        }
        catch (e: java.lang.Exception)
        {
            log.error("AliOssFs delete error", e)
            return FsResponse.builder()
                .successful(false)
                .message(e.message)
                .build()
        }
    }
}
