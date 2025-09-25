package com.example.shop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回的Token信息
 */
@Data
@AllArgsConstructor
public class TokenResponse {
    private String token;       // JWT令牌
    private Integer sellerId;   // 卖家ID（唯一）
    private String username;    // 卖家用户名
}