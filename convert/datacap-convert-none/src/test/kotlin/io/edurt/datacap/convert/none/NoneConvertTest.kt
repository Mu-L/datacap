package io.edurt.datacap.convert.none

import com.google.inject.Guice.createInjector
import com.google.inject.Injector
import io.edurt.datacap.convert.ConvertFilter
import io.edurt.datacap.convert.ConvertManager
import io.edurt.datacap.convert.model.ConvertRequest
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertTrue

class NoneConvertTest
{
    private val log = getLogger(this::class.java)
    private val name = "None"
    private var injector: Injector? = null
    private val request: ConvertRequest = ConvertRequest()

    @Before
    fun before()
    {
        injector = createInjector(ConvertManager())

        request.name = "test"
        request.path = System.getProperty("user.dir")
        request.headers = listOf("name", "age")

        val l1 = listOf("Test", 12)
        val l2 = listOf("Test1", 121)
        request.columns = listOf(l1, l2)
    }

    @Test
    fun testFormat()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    val response = file.format(request)
                    log.info("headers: [ ${response.headers} ]")
                    response.columns
                        .let { columns ->
                            columns.forEachIndexed { index, line ->
                                log.info("index: [ $index ], line: [ $line ]")
                            }
                        }

                    assertTrue {
                        response.successful == true
                    }
                }
        }
    }

    @Test
    fun testFormatStream()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    request.stream = FileInputStream(File("${System.getProperty("user.dir")}/${request.name}.none"))
                    val response = file.formatStream(request)
                    log.info("headers: [ ${response.headers} ]")
                    response.columns
                        .let { columns ->
                            columns.forEachIndexed { index, line ->
                                log.info("index: [ $index ], line: [ $line ]")
                            }
                        }
                    assertTrue {
                        response.successful == true
                    }
                }
        }
    }

    @Test
    fun testWriter()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    assertTrue {
                        file.writer(request)
                            .successful == true
                    }
                }
        }
    }

    @Test
    fun testReader()
    {
        injector?.let { injector ->
            ConvertFilter.filter(injector, name)
                .ifPresent { file ->
                    val response = file.reader(request)
                    log.info("headers: [ ${response.headers} ]")
                    response.columns
                        .let { columns ->
                            columns.forEachIndexed { index, line ->
                                log.info("index: [ $index ], line: [ $line ]")
                            }
                        }
                    assertTrue {
                        response.successful == true
                    }
                }
        }
    }
}
