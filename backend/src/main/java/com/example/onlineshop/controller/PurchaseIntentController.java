package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购买意向管理控制器
 * 卖家查看、处理购买意向信息、更新意向状态
 */
@RestController
@RequestMapping("/api/seller/purchase-intents")
public class PurchaseIntentController {

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    // 卖家：查看所有购买意向
    @GetMapping("")
    public ResponseEntity<ApiResponse> getAllPurchaseIntents(
            @RequestHeader("Authorization") String token
    ) {
        // 可选：鉴权校验
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }
        try {
            List<PurchaseIntent> list = purchaseIntentService.getAllPurchaseIntents();
            return ResponseEntity.ok(new ApiResponse(200, "查询成功", list));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败: " + e.getMessage(), null));
        }
    }

    // 卖家：更新购买意向状态
    @PutMapping("/update-status")
    public ResponseEntity<ApiResponse> updateIntentStatus(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> req
    ) {
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }
        try {
            Integer purchaseId = Integer.parseInt(req.get("purchase_id").toString());
            String newStatus = req.get("new_status").toString();
            boolean updated = purchaseIntentService.updatePurchaseStatus(purchaseId, newStatus);
            if (updated) {
                PurchaseIntent intent = purchaseIntentService.getPurchaseIntentById(purchaseId);
                return ResponseEntity.ok(new ApiResponse(200, "状态更新成功", intent));
            } else {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(404, "意向不存在", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "状态更新失败: " + e.getMessage(), null));
        }
    }
}


