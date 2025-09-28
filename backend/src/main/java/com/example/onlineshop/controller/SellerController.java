// controller/SellerController.java
package com.example.onlineshop.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.onlineshop.dto.LoginRequestDTO;
import com.example.onlineshop.dto.ProductRequestDTO;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.Seller;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.service.SellerService;
import com.example.onlineshop.util.ResponseUtil;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    private static final Map<String, Integer> tokenStore = new HashMap<>();

    // 卖家登录
    @PostMapping("/login")
    public Object login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            Seller seller = sellerService.findByUsername(loginRequest.getUsername());
            if (seller == null) {
                return ResponseUtil.error("用户名或密码错误");
            }

            // 简单密码验证（实际应该使用BCrypt等加密方式）
            if (!seller.getPassword().equals(loginRequest.getPassword())) {
                return ResponseUtil.error("用户名或密码错误");
            }

            String token = "token_" + seller.getSeller_id();
            tokenStore.put(token, seller.getSeller_id());

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);

            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("seller_id", seller.getSeller_id());
            sellerInfo.put("username", seller.getUsername());
            sellerInfo.put("create_time", seller.getCreate_time());
            result.put("seller_info", sellerInfo);

            return ResponseUtil.success("登录成功", result);
        } catch (Exception e) {
            return ResponseUtil.error("登录失败: " + e.getMessage());
        }
    }

    // 发布新商品
    @PostMapping("/product")
    public Object createProduct(@RequestHeader("Authorization") String authorization,
            @RequestBody ProductRequestDTO productRequest) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            Integer sellerId = getSellerIdFromToken(authorization);

            Product product = new Product();
            // 手动复制属性而不是使用BeanUtils，因为名称不匹配
            product.setProduct_name(productRequest.getProduct_name());
            product.setProduct_desc(productRequest.getProduct_desc());
            product.setImage_url(productRequest.getImage_url());
            product.setPrice(productRequest.getPrice());
            product.setSeller_id(sellerId);
            product.setProduct_status("online");

            if (productService.createProduct(product)) {
                Map<String, Object> result = new HashMap<>();
                result.put("product_id", product.getProduct_id());
                result.put("product_status", product.getProduct_status());
                result.put("created_at", product.getCreated_at());
                return ResponseUtil.success("商品发布成功", result);
            } else {
                return ResponseUtil.error("商品发布失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("商品发布失败: " + e.getMessage());
        }
    }

    // 查看历史商品
    @GetMapping("/products")
    public Object getHistoryProducts(@RequestHeader("Authorization") String authorization) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            Integer sellerId = getSellerIdFromToken(authorization);
            List<Product> products = productService.getHistoryProducts(sellerId);
            return ResponseUtil.success("获取成功", products);
        } catch (Exception e) {
            return ResponseUtil.error("获取失败: " + e.getMessage());
        }
    }

    // 冻结商品
    @PutMapping("/product/freeze")
    public Object freezeProduct(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Integer> request) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            Integer productId = request.get("productId");
            if (productService.updateProductStatus(productId, "frozen")) {
                Product product = productService.getProductById(productId);
                Map<String, Object> result = new HashMap<>();
                result.put("product_id", product.getProduct_id());
                result.put("product_status", product.getProduct_status());
                result.put("updated_at", product.getUpdated_at());
                return ResponseUtil.success("商品已冻结", result);
            } else {
                return ResponseUtil.error("操作失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("操作失败: " + e.getMessage());
        }
    }

    // 恢复商品上线
    @PutMapping("/product/unfreeze")
    public Object unfreezeProduct(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Integer> request) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            Integer productId = request.get("productId");
            if (productService.updateProductStatus(productId, "online")) {
                Product product = productService.getProductById(productId);
                Map<String, Object> result = new HashMap<>();
                result.put("product_id", product.getProduct_id());
                result.put("product_status", product.getProduct_status());
                result.put("updated_at", product.getUpdated_at());
                return ResponseUtil.success("商品已恢复上线", result);
            } else {
                return ResponseUtil.error("操作失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("操作失败: " + e.getMessage());
        }
    }

    // 标记商品为已售出
    @PutMapping("/product/mark-sold")
    public Object markProductSold(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Integer> request) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            Integer productId = request.get("productId");
            if (productService.updateProductStatus(productId, "sold")) {
                Product product = productService.getProductById(productId);
                Map<String, Object> result = new HashMap<>();
                result.put("product_id", product.getProduct_id());
                result.put("product_status", product.getProduct_status());
                result.put("updated_at", product.getUpdated_at());
                return ResponseUtil.success("商品已标记为已售出", result);
            } else {
                return ResponseUtil.error("操作失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("操作失败: " + e.getMessage());
        }
    }

    // 查看购买意向信息
    @GetMapping("/purchase-intents")
    public Object getPurchaseIntents(@RequestHeader("Authorization") String authorization) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            List<PurchaseIntent> purchaseIntents = purchaseIntentService.getAllPurchaseIntents();
            return ResponseUtil.success("获取成功", purchaseIntents);
        } catch (Exception e) {
            return ResponseUtil.error("获取失败: " + e.getMessage());
        }
    }

    // 更新购买意向状态
    @PutMapping("/purchase-intents/update-status")
    public Object updatePurchaseStatus(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> request) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            Integer purchaseId = (Integer) request.get("purchaseId");
            String newStatus = (String) request.get("newStatus");

            if (purchaseIntentService.updatePurchaseStatus(purchaseId, newStatus)) {
                PurchaseIntent purchaseIntent = purchaseIntentService.getPurchaseIntentById(purchaseId);
                Map<String, Object> result = new HashMap<>();
                result.put("purchase_id", purchaseIntent.getPurchase_id());
                result.put("purchase_status", purchaseIntent.getPurchase_status());
                result.put("updated_at", purchaseIntent.getUpdated_at());
                return ResponseUtil.success("状态更新成功", result);
            } else {
                return ResponseUtil.error("操作失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("操作失败: " + e.getMessage());
        }
    }

    // 修改密码
    @PutMapping("/password")
    public Object updatePassword(@RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> request) {
        try {
            if (!validateToken(authorization)) {
                return ResponseUtil.error("未授权访问");
            }

            Integer sellerId = getSellerIdFromToken(authorization);
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");

            Seller seller = sellerService.findById(sellerId);
            if (!seller.getPassword().equals(oldPassword)) {
                return ResponseUtil.error("原密码错误");
            }

            if (sellerService.updatePassword(sellerId, newPassword)) {
                return ResponseUtil.success("密码修改成功", null);
            } else {
                return ResponseUtil.error("密码修改失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("密码修改失败: " + e.getMessage());
        }
    }

    // 验证token
    private boolean validateToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        String token = authorization.substring(7);
        return tokenStore.containsKey(token);
    }

    // 从token获取卖家ID
    private Integer getSellerIdFromToken(String authorization) {
        String token = authorization.substring(7);
        return tokenStore.get(token);
    }
}
