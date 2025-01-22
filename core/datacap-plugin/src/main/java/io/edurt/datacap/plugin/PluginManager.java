package io.edurt.datacap.plugin;

import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.edurt.datacap.common.utils.DateUtils;
import io.edurt.datacap.common.utils.EnvironmentUtils;
import io.edurt.datacap.plugin.loader.PluginClassLoader;
import io.edurt.datacap.plugin.loader.PluginLoaderFactory;
import io.edurt.datacap.plugin.loader.TarPluginLoader;
import io.edurt.datacap.plugin.utils.PluginClassLoaderUtils;
import io.edurt.datacap.plugin.utils.PluginPathUtils;
import io.edurt.datacap.plugin.utils.VersionUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "EI_EXPOSE_REP2"})
public class PluginManager
{
    // 插件配置
    // Plugin configuration
    private final PluginConfigure config;

    // 插件存储映射
    // Plugin storage mapping
    private final Map<String, PluginMetadata> plugins;

    // 插件类加载器映射
    // Plugin class loader mapping
    private final Map<String, PluginClassLoader> pluginClassLoaders;

    // 运行状态标志
    // Running state flag
    @Getter
    private volatile boolean running;

    // 插件安装锁
    // Plugin installation lock
    private final Object installLock = new Object();

    // 临时目录前缀
    // Temporary directory prefix
    private static final String TEMP_DIR_PREFIX = "plugin_install_";

    // 默认版本
    // Default version
    private static final String DEFAULT_VERSION = "1.0.0";

    // 插件监视器
    // Plugin watcher
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "plugin-watcher-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });

    public PluginManager(PluginConfigure config)
    {
        this.config = config;
        this.plugins = Maps.newConcurrentMap();
        this.pluginClassLoaders = Maps.newConcurrentMap();
    }

    // 启动插件管理器
    // Start plugin manager
    public void start()
    {
        running = true;
        createPluginsDirectoryIfNotExists();
        loadPlugins();

        if (config.isAutoReload()) {
            startPluginWatcher();
        }
    }

    // 停止插件管理器
    // Stop plugin manager
    public void stop()
    {
        running = false;

        // 关闭所有插件的类加载器
        // Close class loaders for all plugins
        plugins.values().forEach(this::closePluginClassLoader);

        // 清理映射
        // Clean up mappings
        pluginClassLoaders.clear();
        plugins.clear();
    }

    // 创建插件目录（如果不存在）
    // Create plugins directory if not exists
    private void createPluginsDirectoryIfNotExists()
    {
        if (!config.getPluginsDir().toFile().isFile()) {
            try {
                Files.createDirectories(config.getPluginsDir());
            }
            catch (IOException e) {
                log.warn("Failed to create plugins directory", e);
            }
        }
    }

    // 验证目录名称合法性
    // Validate directory name
    private boolean isValidDirectoryName(String name)
    {
        return name.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]*$");
    }

    // 安装新插件
    // Install new plugin
    public boolean installPlugin(Path sourcePath, String targetDirectory)
    {
        // 参数验证
        // Parameter validation
        if (sourcePath == null || targetDirectory == null || targetDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Source path and target directory cannot be null or empty");
        }

        // 对于本地文件才验证存在性
        // Only verify existence for local files
        if (!sourcePath.toString().startsWith("http") && !sourcePath.toString().startsWith("https")) {
            if (!Files.exists(sourcePath)) {
                log.error("Source plugin path does not exist: {}", sourcePath);
                return false;
            }
        }

        // 验证目标目录名称合法性
        // Validate target directory name
        if (!isValidDirectoryName(targetDirectory)) {
            log.error("Invalid target directory name: {}", targetDirectory);
            return false;
        }

        synchronized (installLock) {
            Path tempDir = null;
            boolean installed;

            try {
                // 如果是IDE环境，或者插件目录是文件，则使用默认目录
                // Use default directory for IDE environment or if plugins directory is a file
                if (EnvironmentUtils.isIdeEnvironment() || Files.isRegularFile(config.getPluginsDir())) {
                    config.setPluginsDir(PluginPathUtils.appendPath("plugins"));
                }

                // 创建临时目录
                // Create temporary directory
                tempDir = Files.createTempDirectory(config.getPluginsDir(), TEMP_DIR_PREFIX);

                // 根据插件类型处理安装
                // Handle installation based on plugin type
                installed = processPluginInstallation(sourcePath, tempDir);

                if (installed) {
                    // 创建最终目标目录
                    // Create final target directory
                    Path targetPath = config.getPluginsDir().resolve(targetDirectory);

                    // 如果目标目录已存在，先备份
                    // Backup existing target directory if it exists
                    if (Files.exists(targetPath)) {
                        backupExistingPlugin(targetPath);
                    }

                    // 将临时目录移动到最终位置
                    // Move temporary directory to final location
                    moveDirectory(tempDir, targetPath);

                    // 加载新安装的插件
                    // Load newly installed plugin
                    loadPluginFromDirectory(targetPath);

                    log.info("Successfully installed plugin from {} to {}", sourcePath, targetPath);
                    return true;
                }

                return false;
            }
            catch (Exception e) {
                log.error("Failed to install plugin from: {} to {}", sourcePath, targetDirectory, e);
                return false;
            }
            finally {
                // 清理临时目录
                // Clean up temporary directory
                if (tempDir != null) {
                    deleteDirectory(tempDir);
                }
            }
        }
    }

    // 处理插件安装
    // Process plugin installation
    private boolean processPluginInstallation(Path sourcePath, Path tempDir)
            throws IOException
    {
        String extension = FilenameUtils.getExtension(sourcePath.toString()).toLowerCase();
        AtomicBoolean installed = new AtomicBoolean(false);

        // 检测并处理插件类型
        // Detect and handle plugin type
        try {
            // 检测并处理插件类型
            // Detect and handle plugin type
            if ("tar".equals(extension)
                    || "tar.gz".equals(extension)
                    || "tgz".equals(extension)
                    || "gz".equals(extension)
            ) {
                installed.set(installTarPlugin(sourcePath, tempDir));
            }
            // Properties 插件
            // Properties plugin
            if ("properties".equals(extension)) {
                installed.set(installPropertiesPlugin(sourcePath, tempDir));
            }
            // POM 插件
            // POM plugin
            else if (isPomPlugin(sourcePath)) {
                installed.set(installPomPlugin(sourcePath, tempDir));
            }
            // 编译后的插件
            // Compiled plugin
            else if ("class".equals(extension) || "jar".equals(extension)) {
                installed.set(installCompiledPlugin(sourcePath, tempDir));
            }
            // 目录类型插件
            // Directory plugin
            else if (Files.isDirectory(sourcePath)) {
                installed.set(installDirectoryPlugin(sourcePath, tempDir));
            }
            // SPI 类型插件
            // SPI plugin
            else if (isSpiPlugin(sourcePath)) {
                installed.set(installSpiPlugin(sourcePath, tempDir));
            }
            else {
                log.warn("Unsupported plugin type for: {}", sourcePath);
            }

            // 验证安装后的插件结构
            // Validate installed plugin structure
            if (installed.get()) {
                validatePluginStructure(tempDir);
            }
        }
        catch (Exception e) {
            log.error("Failed to process plugin installation: {}", sourcePath, e);
            throw e;
        }

        return installed.get();
    }

    // 备份现有插件
    // Backup existing plugin
    private void backupExistingPlugin(Path pluginPath)
            throws IOException
    {
        if (Files.exists(pluginPath)) {
            String timestamp = DateUtils.formatYMDHMS();
            Path backupPath = pluginPath.getParent().resolve(pluginPath.getFileName() + ".backup." + timestamp);

            try {
                Files.move(pluginPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Created backup of existing plugin: {}", backupPath);
            }
            catch (IOException e) {
                log.error("Failed to create backup of existing plugin: {}", pluginPath, e);
                throw e;
            }
        }
    }

    // 移动目录
    // Move directory
    private void moveDirectory(Path source, Path target)
            throws IOException
    {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (FileAlreadyExistsException e) {
            // 如果移动失败，尝试复制
            // If move fails, try copy
            copyDirectory(source, target);
        }
    }

    // 删除目录
    // Delete directory
    private void deleteDirectory(Path directory)
    {
        try {
            if (Files.exists(directory)) {
                try (Stream<Path> pathStream = Files.walk(directory)) {
                    pathStream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(file -> {
                                if (!file.delete()) {
                                    log.warn("Failed to delete file: {}", file.getAbsolutePath());
                                    if (file.exists()) {
                                        file.deleteOnExit();
                                    }
                                }
                            });
                }
            }
        }
        catch (IOException e) {
            log.warn("Failed to delete directory: {}", directory, e);
        }
    }

    // 验证插件结构
    // Validate plugin structure
    private void validatePluginStructure(Path pluginPath)
            throws IOException
    {
        // 验证基本文件结构
        // Validate basic file structure
        if (!Files.exists(pluginPath)) {
            throw new IOException("Plugin path does not exist: " + pluginPath);
        }

        boolean hasRequiredFiles = Files.exists(pluginPath.resolve("pom.xml")) ||
                Files.exists(pluginPath.resolve("plugin.properties")) ||
                Files.exists(pluginPath.resolve("META-INF/services"));

        if (hasRequiredFiles) {
            return;
        }

        // 检查是否存在 class 或 jar 文件
        // Check for class or jar files
        try (Stream<Path> paths = Files.list(pluginPath)) {
            boolean hasClassOrJar = paths.anyMatch(path -> {
                String pathStr = path.toString();
                return pathStr.endsWith(".class") || pathStr.endsWith(".jar");
            });

            if (!hasClassOrJar) {
                throw new IOException("Invalid plugin structure in: " + pluginPath);
            }
        }
    }

    // 检查是否为 POM 插件
    // Check if it's a POM plugin
    private boolean isPomPlugin(Path path)
    {
        return path.toString().endsWith("pom.xml") ||
                (Files.isDirectory(path) && Files.exists(path.resolve("pom.xml")));
    }

    // 检查是否为 SPI 插件
    // Check if it's SPI plugin
    private boolean isSpiPlugin(Path path)
    {
        if (!Files.isDirectory(path)) {
            return false;
        }

        Path servicesPath = path.resolve("META-INF/services");
        if (!Files.exists(servicesPath)) {
            return false;
        }

        try (Stream<Path> serviceFiles = Files.list(servicesPath)) {
            return serviceFiles.anyMatch(file -> file.toString().endsWith(Plugin.class.getName()));
        }
        catch (IOException e) {
            log.debug("Failed to check SPI plugin at path: {}", path, e);
            return false;
        }
    }

    // 安装 Properties 类型插件
    // Install Properties type plugin
    private boolean installPropertiesPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        Files.copy(sourcePath, targetPath.resolve("plugin.properties"),
                StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    // 安装 POM 类型插件
    // Install POM type plugin
    private boolean installPomPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        if (Files.isDirectory(sourcePath)) {
            copyDirectory(sourcePath, targetPath);
        }
        else {
            Files.copy(sourcePath, targetPath.resolve("pom.xml"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    // 安装编译后的插件
    // Install compiled plugin
    private boolean installCompiledPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        if (Files.isDirectory(sourcePath)) {
            copyDirectory(sourcePath, targetPath);
        }
        else {
            Files.copy(sourcePath, targetPath.resolve(sourcePath.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    // 安装目录类型插件
    // Install directory type plugin
    private boolean installDirectoryPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        copyDirectory(sourcePath, targetPath);
        return true;
    }

    // 安装 SPI 类型插件
    // Install SPI type plugin
    private boolean installSpiPlugin(Path sourcePath, Path targetPath)
            throws IOException
    {
        copyDirectory(sourcePath, targetPath);
        return true;
    }

    // 安装 Tar 类型插件
    // Install Tar type plugin
    private boolean installTarPlugin(Path sourcePath, Path tempDir)
            throws IOException
    {
        // 直接使用 TarPluginLoader 处理
        // Directly use TarPluginLoader to handle
        TarPluginLoader tarPluginLoader = new TarPluginLoader();
        // 先加载插件到临时目录
        // Load plugins to temporary directory
        List<Plugin> plugins = tarPluginLoader.load(sourcePath, tempDir, config.getParentClassLoaderPackages());
        if (!plugins.isEmpty()) {
            // 查找解压后的子目录
            // Find extracted subdirectory
            try (Stream<Path> paths = Files.list(tempDir)) {
                Optional<Path> subDir = paths
                        .filter(Files::isDirectory)
                        .findFirst();

                if (subDir.isPresent()) {
                    // 如果存在子目录，将其内容移动到临时目录根目录
                    // If subdirectory exists, move its contents to temp directory root
                    Path source = subDir.get();
                    try (Stream<Path> files = Files.list(source)) {
                        files.forEach(file -> {
                            try {
                                Path target = tempDir.resolve(file.getFileName());
                                Files.move(file, target);
                            }
                            catch (IOException e) {
                                log.error("Failed to move file: {} to {}", file, tempDir, e);
                            }
                        });
                    }
                    // 删除空的子目录
                    // Delete empty subdirectory
                    Files.delete(source);
                }
            }
            return true;
        }
        return false;
    }

    // 复制目录
    // Copy directory
    private void copyDirectory(Path source, Path target)
            throws IOException
    {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    }
                    else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                catch (IOException e) {
                    log.error("Failed to copy file: {} to {}", sourcePath, target, e);
                }
            });
        }
    }

    // 加载所有插件
    // Load all plugins
    private void loadPlugins()
    {
        try {
            Path pluginsPath = config.getPluginsDir();

            if (Files.isRegularFile(pluginsPath)) {
                log.debug("Loading plugin from file: {}", pluginsPath);
                // 如果是文件直接加载
                // Load plugin from file
                loadPluginFromDirectory(pluginsPath);
            }
            else {
                // 如果是目录则遍历加载
                // Load plugins from directory
                try (Stream<Path> paths = Files.walk(pluginsPath, config.getScanDepth() == 0 ? 1 : config.getScanDepth())) {
                    paths.filter(Files::isDirectory)
                            .peek(path -> log.debug("Scanning plugin directory: {}", path))
                            .filter(path -> !path.equals(pluginsPath))
                            .forEach(this::loadPluginFromDirectory);
                }
            }
        }
        catch (IOException e) {
            log.error("Failed to scan plugins directory", e);
        }
    }

    // 从目录加载插件
    // Load plugin from directory
    private void loadPluginFromDirectory(Path pluginDir)
    {
        try {
            // 从目录名获取插件基本信息
            // Get plugin basic information from directory name
            String pluginBaseName = pluginDir.getFileName().toString();
            log.debug("Found plugin directory: {}", pluginDir);
            log.debug("Found plugin: {}", pluginBaseName);

            // 获取插件版本(可以从配置文件或清单文件中读取)
            // Get plugin version (can be read from config or manifest file)
            String pluginVersion = VersionUtils.determinePluginVersion(pluginDir);
            log.debug("Found plugin version: {}", pluginVersion);

            PluginClassLoader loader;
            if (config.isShareClassLoaderWhenSameDir()) {
                log.info("Use shared ClassLoader for plugin: {} at {}", pluginBaseName, pluginDir);
                // 多个插件在同一目录下，使用同一个类加载器
                // Multiple plugins in the same directory, use the same class loader
                loader = pluginClassLoaders.computeIfAbsent(
                        pluginDir.toString(),
                        k -> {
                            try {
                                return PluginClassLoaderUtils.createClassLoader(
                                        pluginDir,
                                        pluginBaseName,
                                        pluginVersion,
                                        true,
                                        config.getParentClassLoaderPackages()
                                );
                            }
                            catch (Exception e) {
                                log.error("Failed to create ClassLoader for plugin: {} at {}", pluginBaseName, pluginDir, e);
                                return null;
                            }
                        }
                );
            }
            else {
                log.info("Use independent ClassLoader for plugin: {} at {}", pluginBaseName, pluginDir);
                // 创建插件专用类加载器
                // Create plugin-specific class loader
                loader = PluginClassLoaderUtils.createClassLoader(
                        pluginDir,
                        pluginBaseName,
                        pluginVersion,
                        true,
                        config.getParentClassLoaderPackages()
                );
            }

            if (loader == null) {
                log.error("Failed to create ClassLoader for plugin: {} at {} skipped", pluginBaseName, pluginDir);
                return;
            }

            List<Plugin> modules = PluginContextManager.runWithClassLoader(loader, () -> PluginLoaderFactory.loadPlugins(pluginDir, config.getParentClassLoaderPackages()));

            for (Plugin module : modules) {
                PluginContextManager.runWithClassLoader(loader, () -> {
                    log.debug("Loader version: {}", loader.getPluginVersion());
                    log.debug("Module loader version: {}", module.getPluginClassLoader().getPluginVersion());

                    // 为每个插件模块创建独立的注入器
                    // Create separate injector for each plugin module
                    Injector pluginInjector = Guice.createInjector(module);
                    module.setInjector(pluginInjector);

                    String pluginName = module.getName();
                    // 保存类加载器信息
                    // Save class loader information
                    if (config.isShareClassLoaderWhenSameDir()) {
                        pluginClassLoaders.putIfAbsent(pluginName, loader);
                    }
                    else {
                        pluginClassLoaders.put(pluginName, loader);
                    }

                    PluginMetadata pluginMetadata = PluginMetadata.builder()
                            .name(pluginName)
                            .version(Objects.equals(module.getVersion(), DEFAULT_VERSION) ? module.getPluginClassLoader().getPluginVersion() : module.getVersion())
                            .location(pluginDir)
                            .state(PluginState.CREATED)
                            .classLoader(module.getPluginClassLoader() == null ? loader : module.getPluginClassLoader())
                            .loaderName(module.getClassLoader())
                            .instance(module)
                            .type(module.getType())
                            .key(module.getKey())
                            .loadTimestamp(System.currentTimeMillis())
                            .loadTime(DateUtils.formatYMDHMSWithInterval())
                            .build();

                    // 移除旧版本插件
                    // Remove old version plugin
                    PluginMetadata oldPlugin = plugins.remove(pluginName);
                    if (oldPlugin != null) {
                        closePluginClassLoader(oldPlugin);
                    }

                    plugins.put(pluginName, pluginMetadata);

                    log.info("Install plugin: [ {} ] type [ {} ] version [ {} ] loader [ {} ] from source [ {} ] loader name [ {} ]",
                            pluginName, module.getType().getName(), module.getVersion(), pluginMetadata.getLoaderName(), pluginDir, loader.getName());

                    return null;
                });
            }
        }
        catch (Exception e) {
            log.error("Failed to load plugin from directory: {}", pluginDir, e);
        }
    }

    // 关闭插件类加载器
    // Close plugin class loader
    private void closePluginClassLoader(PluginMetadata pluginMetadata)
    {
        try {
            String pluginName = pluginMetadata.getName();

            // 从映射中移除
            // Remove from mapping
            PluginClassLoader removed = pluginClassLoaders.remove(pluginName);

            if (removed != null) {
                removed.close();
                log.info("Closed class loader for plugin: {}", pluginName);
            }

            // 如果不是 PluginClassLoader，尝试关闭普通的 URLClassLoader
            // If not PluginClassLoader, try to close normal URLClassLoader
            if (pluginMetadata.getClassLoader() instanceof PluginClassLoader) {
                ((PluginClassLoader) pluginMetadata.getClassLoader()).close();
            }
        }
        catch (IOException e) {
            log.error("Failed to close plugin classloader: {}", pluginMetadata.getName(), e);
        }
    }

    // 启动插件监视器线程
    // Start plugin watcher thread
    private void startPluginWatcher()
    {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                loadPlugins();
            }
            catch (Exception e) {
                log.error("Failed to load plugins during watch cycle", e);
            }
        }, 0, config.getScanInterval(), TimeUnit.MILLISECONDS);

        log.info("Started plugin watcher with scan interval: {}ms", config.getScanInterval());
    }

    // 停止插件监视器线程
    // Stop plugin watcher thread
    private void stopPluginWatcher()
    {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(config.getScanInterval(), TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    log.warn("Plugin watcher scheduler did not terminate");
                }
            }
        }
        catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while shutting down plugin watcher", e);
        }
    }

    @PreDestroy
    public void destroy()
    {
        stopPluginWatcher();
    }

    // 获取指定名称的插件
    // Get plugin by name
    public Optional<Plugin> getPlugin(String name)
    {
        return Optional.ofNullable(plugins.get(name))
                .map(info -> (Plugin) info.getInstance());
    }

    // 获取插件的类加载器
    // Get plugin's class loader
    public Optional<ClassLoader> getPluginClassLoader(String pluginName)
    {
        return Optional.ofNullable(pluginClassLoaders.get(pluginName));
    }

    // 获取当前已加载的所有插件类加载器
    // Get all currently loaded plugin class loaders
    public Map<String, PluginClassLoader> getPluginClassLoaders()
    {
        return Maps.newHashMap(pluginClassLoaders);
    }

    // 获取所有插件信息
    // Get all plugin information
    public List<PluginMetadata> getPluginInfos()
    {
        return new ArrayList<>(plugins.values());
    }

    // 卸载指定名称的插件
    // Unload plugin by name
    public boolean uninstallPlugin(String name)
    {
        if (name == null || name.trim().isEmpty()) {
            log.warn("Plugin name cannot be null or empty");
            return false;
        }

        synchronized (installLock) {
            try {
                // 获取插件元数据
                // Get plugin metadata
                PluginMetadata pluginMetadata = plugins.get(name);
                if (pluginMetadata == null) {
                    log.warn("Plugin not found: {}", name);
                    return false;
                }

                // 检查插件状态
                // Check plugin state
                if (pluginMetadata.getState() == PluginState.UNLOADED) {
                    log.warn("Plugin {} is already unloaded", name);
                    return false;
                }

                // 创建卸载备份
                // Create unload backup
                Path pluginLocation = pluginMetadata.getLocation();
                if (pluginLocation != null && Files.exists(pluginLocation)) {
                    createUnloadBackup(pluginLocation);
                }

                // 清理插件资源
                // Clean up plugin resources
                try {
                    // 关闭类加载器
                    // Close class loader
                    closePluginClassLoader(pluginMetadata);

                    // 清理注入器
                    // Clean up injector
                    Plugin plugin = (Plugin) pluginMetadata.getInstance();
                    if (plugin != null && plugin.getInjector() != null) {
                        // Close any resources managed by the injector
                        try {
                            plugin.getInjector().getInstance(AutoCloseable.class);
                        }
                        catch (Exception ignored) {
                            // Ignore if no AutoCloseable is bound
                        }
                    }

                    // 更新插件状态
                    // Update plugin state
                    pluginMetadata.setState(PluginState.UNLOADED);

                    // 从插件映射中移除
                    // Remove from plugin mapping
                    plugins.remove(name);

                    // 清理插件文件（如果配置了自动清理）
                    // Clean up plugin files (if auto cleanup is configured)
                    if (config.isAutoCleanup()) {
                        deletePluginFiles(pluginLocation);
                    }

                    log.info("Successfully unloaded plugin: {}", name);
                    return true;
                }
                catch (Exception e) {
                    log.error("Error while unloading plugin: {}", name, e);
                    // 尝试强制移除
                    // Try force removal
                    plugins.remove(name);
                    return false;
                }
            }
            catch (Exception e) {
                log.error("Failed to unload plugin: {}", name, e);
                return false;
            }
        }
    }

    // 创建卸载备份
    // Create unload backup
    private void createUnloadBackup(Path pluginLocation)
    {
        try {
            String timestamp = DateUtils.formatYMDHMS();
            Path backupPath = pluginLocation.getParent()
                    .resolve(pluginLocation.getFileName() + ".unload." + timestamp);

            if (Files.isDirectory(pluginLocation)) {
                copyDirectory(pluginLocation, backupPath);
            }
            else {
                Files.copy(pluginLocation, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Created unload backup at: {}", backupPath);
        }
        catch (IOException e) {
            log.warn("Failed to create unload backup for: {}", pluginLocation, e);
        }
    }

    private void deletePluginFiles(Path pluginLocation)
    {
        if (pluginLocation == null || !Files.exists(pluginLocation)) {
            return;
        }

        try {
            // 删除插件目录或文件
            // Delete plugin directory or file
            if (Files.isDirectory(pluginLocation)) {
                deleteDirectory(pluginLocation);
            }
            else {
                // 如果非IDE环境，或者插件目录是文件，才进行删除
                // Delete only if not in IDE environment, or if the plugin directory is a file
                if (!EnvironmentUtils.isIdeEnvironment() || !Files.isRegularFile(config.getPluginsDir())) {
                    Files.delete(pluginLocation);
                }
            }

            // 删除备份文件
            // Delete backup files
            Path parentDir = pluginLocation.getParent();
            String baseName = pluginLocation.getFileName().toString();
            try (Stream<Path> paths = Files.list(parentDir)) {
                paths.filter(path -> {
                            String fileName = path.getFileName().toString();
                            // 匹配 .backup.* 和 .unload.* 格式的备份文件
                            // Match backup files with .backup.* and .unload.* format
                            return fileName.startsWith(baseName + ".backup.") || fileName.startsWith(baseName + ".unload.");
                        })
                        .forEach(backupPath -> {
                            try {
                                if (Files.isDirectory(backupPath)) {
                                    deleteDirectory(backupPath);
                                }
                                else {
                                    Files.delete(backupPath);
                                }
                                log.info("Deleted backup file: {}", backupPath);
                            }
                            catch (IOException e) {
                                log.warn("Failed to delete backup file: {}", backupPath, e);
                            }
                        });
            }
            log.info("Deleted plugin files at: {}", pluginLocation);
        }
        catch (IOException e) {
            log.warn("Failed to delete plugin files at: {}", pluginLocation, e);
        }
    }

    // 强制卸载插件
    // Force unload plugin
    public boolean forceUnloadPlugin(String name)
    {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        synchronized (installLock) {
            try {
                PluginMetadata pluginMetadata = plugins.remove(name);
                if (pluginMetadata != null) {
                    try {
                        closePluginClassLoader(pluginMetadata);
                    }
                    catch (Exception e) {
                        log.warn("Error while force closing plugin classloader: {}", name, e);
                    }

                    try {
                        if (config.isAutoCleanup()) {
                            deletePluginFiles(pluginMetadata.getLocation());
                        }
                    }
                    catch (Exception e) {
                        log.warn("Error while force deleting plugin files: {}", name, e);
                    }

                    log.info("Force unloaded plugin: {}", name);
                    return true;
                }
                return false;
            }
            catch (Exception e) {
                log.error("Failed to force unload plugin: {}", name, e);
                return false;
            }
        }
    }
}
