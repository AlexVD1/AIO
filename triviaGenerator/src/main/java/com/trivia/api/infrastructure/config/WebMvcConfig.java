package com.trivia.api.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configuración de Spring MVC para servir archivos estáticos (PNGs generados) desde /assets/**.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${trivia.storage.path:./storage}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUri = Paths.get(storagePath).toAbsolutePath().toUri().toString();
        if (!absoluteUri.endsWith("/")) {
            absoluteUri += "/";
        }

        registry.addResourceHandler("/assets/**")
                .addResourceLocations(absoluteUri);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
