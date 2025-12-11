// OnlineShopApplication.java
package com.example.onlineshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@MapperScan("com.example.onlineshop.mapper")
public class OnlineShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineShopApplication.class, args);
    }
}
