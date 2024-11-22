package io.edurt.datacap.plugin.loader;

import io.edurt.datacap.plugin.Plugin;
import io.edurt.datacap.plugin.PluginContextManager;
import io.edurt.datacap.plugin.SpiType;
import io.edurt.datacap.plugin.utils.PluginClassLoaderUtils;
import io.edurt.datacap.plugin.utils.VersionUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class SpiPluginLoader
        implements PluginLoader
{
    @Override
    public SpiType getType()
    {
        return SpiType.SPI;
    }

    @Override
    public List<Plugin> load(Path path)
    {
        try {
            // 检查路径是否有效
            // Check if the path is valid
            if (!isValidDirectory(path)) {
                log.debug("Skipping excluded or invalid directory: {}", path);
                return List.of();
            }

            // 获取目录名作为插件名
            // Get directory name as plugin name
            String pluginName = path.getFileName().toString();
            String version = VersionUtils.determinePluginVersion(path);

            // 创建插件专用类加载器
            // Create plugin-specific class loader
            PluginClassLoader classLoader = PluginClassLoaderUtils.createClassLoader(
                    path,
                    pluginName,
                    version
            );

            return PluginContextManager.runWithClassLoader(classLoader, () -> {
                ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
                List<Plugin> plugins = StreamSupport.stream(serviceLoader.spliterator(), false)
                        .filter(plugin -> {
                            // 检查插件类是否来自排除目录
                            // Check if the plugin class is from an excluded directory
                            String className = plugin.getClass().getName();
                            Path classPath = Path.of(className.replace('.', File.separatorChar) + ".class");
                            return !isExcludedPath(classPath);
                        })
                        .collect(Collectors.toList());

                // 设置插件的类加载器
                // Set class loader for plugins
                plugins.stream()
                        .peek(plugin -> log.debug("Loaded SPI plugin: {} (version: {})", plugin.getClass().getName(), version))
                        .forEach(plugin -> plugin.setPluginClassLoader(classLoader));

                return plugins;
            });
        }
        catch (Exception e) {
            log.error("Failed to load plugins using SPI from: {}", path, e);
            return List.of();
        }
    }
}
