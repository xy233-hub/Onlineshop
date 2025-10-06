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
     * POST /api/seller/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Validated @RequestBody SellerLoginRequest request
    ) {
        try {
            TokenResponse tokenResponse = sellerService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(
                    new ApiResponse(200, "登录成功", tokenResponse)
            );
        } catch (IllegalArgumentException e) {
            // 用户名不存在或密码错误
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "用户名或密码错误", null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "登录失败：" + e.getMessage(), null));
        }
    }

    /**
     * 卖家修改密码接口
     * PUT /api/seller/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse> updatePassword(
            @RequestHeader("Authorization") String token,
            @Validated @RequestBody SellerPasswordUpdateRequest request
    ) {
        try {
            Integer sellerId = JwtUtil.getSellerIdFromToken(token);
            if (sellerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }
            sellerService.updatePassword(sellerId, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(
                    new ApiResponse(200, "修改成功", null)
            );
        } catch (IllegalArgumentException e) {
            // 原密码错误等
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "密码修改失败：" + e.getMessage(), null));
        }
    }
}

