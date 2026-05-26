package com.example.onlineshop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baidu.nlp")
public class BaiduMapConfig {
    private String apiKey;
    private String secretKey;
}