package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping("/active")
    public ApiResponse active(@RequestParam(value = "promotion_type", required = false) String promotionType,
                              @RequestParam(value = "product_id", required = false) Integer productId) {
        return new ApiResponse(200, "查询成功", promotionService.listActivePromotions(promotionType, productId));
    }
}

