package io.edurt.datacap.file.none

import com.google.common.base.Preconditions.checkState
import io.edurt.datacap.common.utils.DateUtils
import io.edurt.datacap.file.File
import io.edurt.datacap.file.FileConvert
import io.edurt.datacap.file.model.FileRequest
import io.edurt.datacap.file.model.FileResponse
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory.getLogger
import java.io.IOException

class NoneFile : File
{
    private val log = getLogger(this::class.java)

    override fun format(request: FileRequest): FileResponse
    {
        val response = FileResponse()
        try
        {
            log.info("${name()} format start time [ ${DateUtils.now()} ]")
            response.headers = request.headers
            val columns = mutableListOf<Any>()
            request.columns
                .forEach { columns.add(it) }

            response.columns = columns

            log.info("${name()} format end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: IOException)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun writer(request: FileRequest): FileResponse
    {
        val response = FileResponse()
        try
        {
            log.info("${name()} writer origin path [ ${request.path} ]")
            log.info("${name()} writer start time [ ${DateUtils.now()} ]")
            val file = FileConvert.formatFile(request, name())
            log.info("${name()} writer file absolute path [ ${file.absolutePath} ]")

            request.headers
                .let { FileUtils.writeStringToFile(file, it.toString(), Charsets.UTF_8) }

            request.columns
                .forEach {
                    FileUtils.writeStringToFile(file, "\n", Charsets.UTF_8, true)
                    when (it)
                    {
                        is List<*> -> FileUtils.writeStringToFile(file, it.toString(), Charsets.UTF_8, true)
                        else -> FileUtils.writeStringToFile(file, it.toString(), Charsets.UTF_8, true)
                    }
                }

            log.info("${name()} writer end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: Exception)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    override fun reader(request: FileRequest): FileResponse
    {
        val response = FileResponse()
        try
        {
            log.info("${name()} reader origin path [ ${request.path} ]")
            log.info("${name()} reader start time [ ${DateUtils.now()} ]")
            val file = FileConvert.formatFile(request, name())
            log.info("${name()} reader file absolute path [ ${file.absolutePath} ]")

            val lines = FileUtils.readLines(file, Charsets.UTF_8)
            checkState(lines.isNotEmpty(), "The file is empty")
            log.info("${name()} reader file line count [ ${lines.size} ]")

            response.headers = parseLine(lines.first())

            val columns = lines.drop(1)
                .map { parseLine(it) }
            response.columns = columns

            log.info("${name()} reader end time [ ${DateUtils.now()} ]")
            response.successful = true
        }
        catch (e: Exception)
        {
            response.successful = false
            response.message = e.message
        }
        return response
    }

    private fun parseLine(line: String): List<String>
    {
        return line.trim()
            .removeSurrounding("[", "]")
            .split(", ")
            .map { it.trim() }
    }
}
