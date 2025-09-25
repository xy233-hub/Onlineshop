package com.example.shop.service;

import com.example.shop.dto.response.TokenResponse;

public interface SellerService {

    /**
     * 卖家登录验证
     * @param username 用户名
     * @param password 明文密码
     * @return TokenResponse（包含Token、卖家ID、用户名）
     * @throws IllegalArgumentException 用户名不存在/密码错误时抛出
     */
    TokenResponse login(String username, String password);

    /**
     * 修改卖家密码
     * @param sellerId 卖家ID（从Token中解析）
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     * @throws IllegalArgumentException 旧密码错误/新密码无效时抛出
     */
    void updatePassword(Integer sellerId, String oldPassword, String newPassword);
}
