// backend/src/main/java/com/example/onlineshop/controller/ProductController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.PurchaseRequest;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    // 获取商品列表，支持关键词、分类、状态、分页、排序等筛选
    @GetMapping("")
    public Object getProductList(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer category_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String sort_by,
            @RequestParam(required = false) String order) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", q);
            params.put("categoryId", category_id);
            params.put("status", status);
            params.put("page", page);
            params.put("size", size);
            params.put("sortBy", sort_by);
            params.put("order", order);

            Object result = productService.getProductsByCondition(params);
            return ResponseUtil.success("获取成功", result);
        } catch (Exception e) {
            return ResponseUtil.error("获取商品列表失败: " + e.getMessage());
        }
    }

    // 获取单个商品的详细信息
    @GetMapping("/{product_id}")
    public Object getProductById(@PathVariable Integer product_id) {
        try {
            Product product = productService.getProductById(product_id);
            if (product == null) {
                return ResponseUtil.custom(404, "商品不存在", null);
            }
            return ResponseUtil.success("获取成功", product);
        } catch (Exception e) {
            return ResponseUtil.error("获取商品失败: " + e.getMessage());
        }
    }

}
