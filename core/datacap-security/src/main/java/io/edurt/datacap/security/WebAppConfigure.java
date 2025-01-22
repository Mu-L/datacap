package io.edurt.datacap.security;

import io.edurt.datacap.service.initializer.InitializerConfigure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebAppConfigure
        implements WebMvcConfigurer
{
    private final InitializerConfigure initializer;

    public WebAppConfigure(InitializerConfigure initializer)
    {
        this.initializer = initializer;
        log.info("WebAppConfigure initialized with ObjectMapper");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + initializer.getDataHome() + "/");
    }
}
