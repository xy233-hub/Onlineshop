// backend/src/main/java/com/example/shop/controller/SellerProductController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.ProductRequest;
import com.example.onlineshop.dto.request.ProductIdRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.service.SellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
public class SellerProductController {

    @Autowired
    private SellerService sellerService;

    // 发布新商品
    @PostMapping("/product")
    public ApiResponse publishProduct(@RequestBody ProductRequest request) {
        return sellerService.publishProduct(request);
    }

    // 查看历史商品
    @GetMapping("/products")
    public ApiResponse listProducts() {
        return sellerService.listProducts();
    }

    // 冻结商品
    @PutMapping("/product/freeze")
    public ApiResponse freezeProduct(@RequestBody ProductIdRequest request) {
        return sellerService.freezeProduct(request.getProductId());
    }

    // 恢复商品上线
    @PutMapping("/product/unfreeze")
    public ApiResponse unfreezeProduct(@RequestBody ProductIdRequest request) {
        return sellerService.unfreezeProduct(request.getProductId());
    }

    // 标记商品为已售出
    @PutMapping("/product/mark-sold")
    public ApiResponse markProductSold(@RequestBody ProductIdRequest request) {
        return sellerService.markProductSold(request.getProductId());
    }
}
