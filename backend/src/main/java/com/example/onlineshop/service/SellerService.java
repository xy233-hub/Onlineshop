package com.example.onlineshop.service;

import com.example.onlineshop.dto.request.ProductRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.TokenResponse;

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

    /**
     * 发布新商品
     * @param request 商品发布参数
     * @return ApiResponse（包含新商品信息，状态码200/401等）
     */
    ApiResponse publishProduct(ProductRequest request) ;

    /**
     * 查看历史商品列表
     * @return ApiResponse（包含商品列表，状态码200/401等）
     */
    ApiResponse listProducts();

    /**
     * 冻结指定商品
     * @param productId 商品ID
     * @return ApiResponse（包含冻结后商品信息，状态码200/404等）
     */
    ApiResponse freezeProduct(Long productId);

    /**
     * 恢复商品上线
     * @param productId 商品ID
     * @return ApiResponse（包含恢复后商品信息，状态码200/404等）
     */
    ApiResponse unfreezeProduct(Long productId);

    /**
     * 标记商品为已售出
     * @param productId 商品ID
     * @return ApiResponse（包含售出后商品信息，状态码200/404等）
     */
    ApiResponse markProductSold(Long productId);
}