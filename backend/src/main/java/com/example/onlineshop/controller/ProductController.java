// controller/ProductController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.entity.Product;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    // 获取当前在售商品
    @GetMapping("/product")
    public Object getOnlineProducts() {
        try {
            List<Product> products = productService.getOnlineProducts();
            if (products.isEmpty()) {
                return ResponseUtil.success("暂无在售商品", null);
            }
            return ResponseUtil.success("获取成功", products.get(0)); // 只卖一个商品
        } catch (Exception e) {
            return ResponseUtil.error("获取商品失败");
        }
    }
}
