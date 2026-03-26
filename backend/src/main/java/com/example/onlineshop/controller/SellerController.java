package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.service.SellerService;
import com.example.onlineshop.service.CustomerService;
import com.example.onlineshop.service.ProductService;
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

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductService productService;

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
            if (seller != null) {
                // 构建响应，不返回密码
                java.util.Map<String, Object> sellerInfo = new java.util.HashMap<>();
                sellerInfo.put("seller_id", seller.getSellerId());
                sellerInfo.put("username", seller.getUsername());
                sellerInfo.put("create_time", seller.getCreateTime());
                return ResponseEntity.ok(
                        new ApiResponse(200, "查询成功", sellerInfo)
                );
            } else {
                // 尝试从顾客表中获取信息
                com.example.onlineshop.entity.Customer customer = customerService.findById(sellerId);
                if (customer != null) {
                    // 构建响应，使用顾客信息作为卖家信息
                    java.util.Map<String, Object> sellerInfo = new java.util.HashMap<>();
                    sellerInfo.put("seller_id", sellerId);
                    sellerInfo.put("username", customer.getUsername());
                    sellerInfo.put("create_time", customer.getCreatedAt());
                    return ResponseEntity.ok(
                            new ApiResponse(200, "查询成功", sellerInfo)
                    );
                } else {
                    return ResponseEntity.status(404)
                            .body(new ApiResponse(404, "卖家不存在", null));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * 根据商品ID获取商品发布者信息
     * GET /api/sellers/product/{product_id}
     */
    @GetMapping("/product/{product_id}")
    public ResponseEntity<ApiResponse> getProductPublisherById(
            @PathVariable("product_id") Integer productId
    ) {
        try {
            // 获取商品信息
            com.example.onlineshop.entity.Product product = productService.getProductById(productId);
            if (product == null) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(404, "商品不存在", null));
            }

            Integer publisherId = product.getSellerId();

            // 尝试从顾客表中获取信息（优先，因为买家发布的商品seller_id是customer_id）
            com.example.onlineshop.entity.Customer customer = customerService.findById(publisherId);
            if (customer != null) {
                // 构建响应，使用顾客信息作为卖家信息
                java.util.Map<String, Object> sellerInfo = new java.util.HashMap<>();
                sellerInfo.put("seller_id", publisherId);
                sellerInfo.put("username", customer.getUsername());
                sellerInfo.put("create_time", customer.getCreatedAt());
                return ResponseEntity.ok(
                        new ApiResponse(200, "查询成功", sellerInfo)
                );
            } else {
                // 尝试从卖家表中获取信息
                com.example.onlineshop.entity.Seller seller = sellerService.getSellerById(publisherId);
                if (seller != null) {
                    // 构建响应，使用卖家信息
                    java.util.Map<String, Object> sellerInfo = new java.util.HashMap<>();
                    sellerInfo.put("seller_id", seller.getSellerId());
                    sellerInfo.put("username", seller.getUsername());
                    sellerInfo.put("create_time", seller.getCreateTime());
                    return ResponseEntity.ok(
                            new ApiResponse(200, "查询成功", sellerInfo)
                    );
                } else {
                    return ResponseEntity.status(404)
                            .body(new ApiResponse(404, "发布者不存在", null));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }
}
