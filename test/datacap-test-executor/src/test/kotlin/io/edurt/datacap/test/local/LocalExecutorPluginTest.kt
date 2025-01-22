package io.edurt.datacap.test.local

import io.edurt.datacap.plugin.PluginConfigure
import io.edurt.datacap.plugin.PluginManager
import io.edurt.datacap.plugin.utils.PluginPathUtils
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalExecutorPluginTest
{
    private val pluginManager: PluginManager
    private val pluginName = "LocalExecutor"

    init
    {
        val projectRoot = PluginPathUtils.findProjectRoot()
        val config = PluginConfigure.builder()
            .pluginsDir(projectRoot.resolve("executor/datacap-executor-local"))
            .build()

        pluginManager = PluginManager(config).apply { start() }
    }

    @Test
    fun test()
    {
        assertNotNull(pluginManager.getPlugin(pluginName))
    }
}
