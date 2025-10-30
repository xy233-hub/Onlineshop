package com.example.onlineshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${media.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        String path = "file:" + base + (base.endsWith(File.separator) ? "" : File.separator);
        registry.addResourceHandler("/media/**")
                .addResourceLocations(path)
                .setCachePeriod(3600);
    }
}
