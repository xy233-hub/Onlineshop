package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.service.SellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 卖家信息控制器
 * 处理卖家信息相关接口
 */
@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    @Autowired
    private SellerService sellerService;

    /**
     * 根据卖家ID获取卖家信息
     * GET /api/seller/{seller_id}
     */
    @GetMapping("/{seller_id}")
    public ResponseEntity<ApiResponse> getSellerById(
            @PathVariable("seller_id") Integer sellerId
    ) {
        try {
            com.example.onlineshop.entity.Seller seller = sellerService.getSellerById(sellerId);
            if (seller == null) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(404, "卖家不存在", null));
            }
            // 构建响应，不返回密码
            java.util.Map<String, Object> sellerInfo = new java.util.HashMap<>();
            sellerInfo.put("seller_id", seller.getSellerId());
            sellerInfo.put("username", seller.getUsername());
            sellerInfo.put("create_time", seller.getCreateTime());
            return ResponseEntity.ok(
                    new ApiResponse(200, "查询成功", sellerInfo)
            );
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }
}
