package io.edurt.datacap.plugin.influxdb

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.edurt.datacap.plugin.PluginConfigure
import io.edurt.datacap.plugin.PluginManager
import io.edurt.datacap.plugin.utils.PluginPathUtils
import io.edurt.datacap.spi.PluginService
import io.edurt.datacap.spi.model.Configure
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger
import org.testcontainers.containers.Network
import org.testcontainers.lifecycle.Startables
import org.testcontainers.shaded.org.awaitility.Awaitility.given
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_INFERRED", "SA_LOCAL_SELF_ASSIGNMENT"])
class InfluxdbServiceTest
{
    private val log = getLogger(this.javaClass)
    private val pluginManager: PluginManager
    private var container: InfluxdbContainer
    private val pluginName = "Influxdb"

    init
    {
        val network = Network.newNetwork()

        container = InfluxdbContainer()
            .withNetwork(network)
            .withNetworkAliases(this.javaClass.simpleName)

        container.portBindings = listOf(String.format("%s:%s", InfluxdbContainer.PORT, InfluxdbContainer.DOCKER_PORT))

        Startables.deepStart(java.util.stream.Stream.of(container))
            .join()

        given().ignoreExceptions()
            .await()
            .atMost(1, TimeUnit.MINUTES)
        log.info("Influxdb container started")

        val configFile = this::class
            .java
            .classLoader
            .getResource("influxdb-config.properties")
        assertNotNull(configFile)
        log.info("Specified config file: {}", configFile)

        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve(Path.of(configFile.path)))
            .build()

        log.info("Initializing plugin manager")
        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        val plugin = pluginManager.getPlugin(pluginName).get()
        assertNotNull(plugin)

        val configure: Configure = Configure.builder()
            .host(container.host)
            .port(InfluxdbContainer.PORT)
            .username(Optional.of(InfluxdbContainer.USERNAME))
            .password(Optional.of(InfluxdbContainer.PASSWORD))
            .database(Optional.of(InfluxdbContainer.BUCKET))
            .plugin(plugin)
            .pluginManager(pluginManager)
            .env(
                Optional.of(
                    mapOf(
                        "useEncryption" to false,
                        "useHTTP2" to false,
                        "org" to "TestOrg"
                    )
                )
            )
            .build()

        val pluginService = plugin.getService(PluginService::class.java)
        val response = pluginService.execute(configure, pluginService.validator())
        assertNotNull(response)
    }
}
