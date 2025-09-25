package com.example.shop.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    private static BCryptPasswordEncoder encoder;

    // 初始化BCrypt加密器（项目启动时执行）
    @jakarta.annotation.PostConstruct
    public void init() {
        encoder = new BCryptPasswordEncoder(); // 恢复BCrypt加密器
    }

    /**
     * 对明文密码进行BCrypt加密
     */
    public static String encrypt(String rawPassword) {
        return encoder.encode(rawPassword); // 恢复加密
    }

    /**
     * 校验明文密码与密文是否匹配（核心：用BCrypt的matches方法）
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword); // 恢复密文校验
    }
}