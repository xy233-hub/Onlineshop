package com.example.shop.controller;

import com.example.shop.dto.request.SellerLoginRequest;
import com.example.shop.dto.request.SellerPasswordUpdateRequest;
import com.example.shop.dto.response.ApiResponse;
import com.example.shop.dto.response.TokenResponse;
import com.example.shop.service.SellerService;
import com.example.shop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 卖家认证控制器
 * 处理登录、密码修改接口
 */
@RestController
@RequestMapping("/api/seller")
public class SellerAuthController {

    @Autowired
    private SellerService sellerService;

    /**
     * 卖家登录接口
     * 接口：POST /api/seller/login
     * 无需认证（公开接口）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Validated @RequestBody SellerLoginRequest request
    ) {
        try {
            // 调用Service验证登录，返回Token和卖家ID
            TokenResponse tokenResponse = sellerService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(
                    new ApiResponse(200, "登录成功", tokenResponse)
            );
        } catch (IllegalArgumentException e) {
            // 用户名不存在、密码错误等业务异常
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "登录失败：" + e.getMessage(), null));
        }
    }

    /**
     * 卖家修改密码接口
     * 接口：PUT /api/seller/password
     * 需要认证（携带Token）
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse> updatePassword(
            @RequestHeader("Authorization") String token,  // 认证Token
            @Validated @RequestBody SellerPasswordUpdateRequest request
    ) {
        try {
            // 1. 验证Token有效性（解析卖家ID）
            Integer sellerId = JwtUtil.getSellerIdFromToken(token);  // 从Token中提取卖家ID
            if (sellerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "Token无效或已过期", null));
            }

            // 2. 调用Service修改密码（验证旧密码，更新新密码）
            sellerService.updatePassword(sellerId, request.getOldPassword(), request.getNewPassword());

            return ResponseEntity.ok(
                    new ApiResponse(200, "密码修改成功", null)
            );
        } catch (IllegalArgumentException e) {
            // 旧密码错误、新密码不符合规则等
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "密码修改失败：" + e.getMessage(), null));
        }
    }
}
