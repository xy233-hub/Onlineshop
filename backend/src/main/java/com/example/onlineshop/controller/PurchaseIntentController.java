package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 添加Product类的导入
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.dto.request.PurchaseRequest;
import com.example.onlineshop.util.ResponseUtil;

/**
 * 购买意向管理控制器 卖家查看、处理购买意向信息、更新意向状态
 */
@RestController
@RequestMapping("/api")
public class PurchaseIntentController {

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    // 注入ProductService
    @Autowired
    private ProductService productService;

    // 在PurchaseIntentController中添加以下方法
    @PostMapping("/purchase-intents")
    public Object submitPurchaseIntent(@RequestBody PurchaseRequest request) {
        try {
            Product product = productService.getProductById(request.getProduct_id());
            if (product == null) {
                return ResponseUtil.error("商品不存在");
            }
            if (!"online".equals(product.getProductStatus())) {
                return ResponseUtil.error("商品当前不可购买");
            }

            // 检查库存
            if (product.getStockQuantity() < (request.getQuantity() != null ? request.getQuantity() : 1)) {
                return ResponseUtil.error("库存不足");
            }

            PurchaseIntent purchaseIntent = new PurchaseIntent();
            purchaseIntent.setProductId(request.getProduct_id());
            purchaseIntent.setCustomerId(request.getCustomer_id());
            purchaseIntent.setCustomerName(request.getCustomer_name());
            purchaseIntent.setCustomerPhone(request.getCustomer_phone());
            purchaseIntent.setCustomerAddress(request.getCustomer_address());
            purchaseIntent.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
            purchaseIntent.setTotalAmount(product.getPrice().multiply(
                    new java.math.BigDecimal(request.getQuantity() != null ? request.getQuantity() : 1)));
            purchaseIntent.setPurchaseStatus("pending");

            if (purchaseIntentService.createPurchaseIntent(purchaseIntent)) {
                return ResponseUtil.success("提交成功", purchaseIntent);
            } else {
                return ResponseUtil.error("提交失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("提交失败: " + e.getMessage());
        }
    }

    // 卖家：查看所有购买意向
    @GetMapping("/seller/purchase-intents")
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
    @PutMapping("/seller/purchase-intents/{purchase_id}/status")
    public ResponseEntity<ApiResponse> updateIntentStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer purchase_id,
            @RequestBody Map<String, Object> req
    ) {
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }
        try {
            String newStatus = req.get("new_status").toString();
            String sellerNotes = req.get("seller_notes") != null ? req.get("seller_notes").toString() : "";

            // 更新卖家备注
            PurchaseIntent intent = purchaseIntentService.getPurchaseIntentById(purchase_id);
            if (intent != null) {
                intent.setSellerNotes(sellerNotes);
                // 这里应该调用一个更新备注的方法，但目前没有，暂时跳过
            }

            boolean updated = purchaseIntentService.updatePurchaseStatus(purchase_id, newStatus);
            if (updated) {
                PurchaseIntent updatedIntent = purchaseIntentService.getPurchaseIntentById(purchase_id);
                return ResponseEntity.ok(new ApiResponse(200, "状态更新成功", updatedIntent));
            } else {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(404, "意向不存在", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "状态更新失败: " + e.getMessage(), null));
        }
    }

    // 客户：查看自己的购买意向记录
    @GetMapping("/customers/{customer_id}/purchase-intents")
    public ResponseEntity<ApiResponse> getCustomerPurchaseIntents(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer customer_id,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String purchase_status) {
        // 鉴权校验
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null || !customerIdFromToken.equals(customer_id)) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("customer_id", customer_id);
            params.put("page", page);
            params.put("size", size);
            params.put("purchase_status", purchase_status);

            List<PurchaseIntent> intents = purchaseIntentService.getPurchaseIntentsByCondition(params);
            int total = purchaseIntentService.countPurchaseIntentsByCondition(params);

            Map<String, Object> result = new HashMap<>();
            result.put("page", page);
            result.put("size", size);
            result.put("total", total);
            result.put("items", intents);

            return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败: " + e.getMessage(), null));
        }
    }
}
