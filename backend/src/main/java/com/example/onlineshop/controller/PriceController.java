package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.PriceChangeRequest;
import com.example.onlineshop.dto.response.PriceHistoryResponse;
import com.example.onlineshop.dto.response.PriceSummaryResponse;
import com.example.onlineshop.service.PriceService;
import com.example.onlineshop.util.JwtUtil;
import com.example.onlineshop.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PriceController {

    @Autowired
    private PriceService priceService;

    @GetMapping("/products/{product_id}/price-history")
    public Object getPriceHistory(
            @PathVariable("product_id") Integer productId,
            @RequestParam(value = "days", required = false) Integer days,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "50") Integer size) {
        try {
            if (productId == null) {
                return ResponseUtil.custom(400, "product_id 必填", null);
            }

            PriceHistoryResponse response = priceService.getPriceHistory(
                    productId, days, startDate, endDate, page, size);

            return ResponseUtil.success("查询成功", response);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/products/{product_id}/price-summary")
    public Object getPriceSummary(@PathVariable("product_id") Integer productId) {
        try {
            if (productId == null) {
                return ResponseUtil.custom(400, "product_id 必填", null);
            }

            PriceSummaryResponse response = priceService.getPriceSummary(productId);

            return ResponseUtil.success("查询成功", response);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + e.getMessage());
        }
    }

        @PutMapping("/seller/products/{product_id}/price")
    public Object updateProductPrice(
            @PathVariable("product_id") Integer productId,
            @RequestBody PriceChangeRequest request,
            HttpServletRequest httpRequest) {
        try {
            if (productId == null) {
                return ResponseUtil.custom(400, "product_id 必填", null);
            }

            if (request.getNewPrice() == null) {
                return ResponseUtil.custom(400, "new_price 必填", null);
            }

            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer sellerId = JwtUtil.getSellerIdFromToken(token);
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            Integer publisherId = customerId != null ? customerId : sellerId;
            
            if (publisherId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            Map<String, Object> result = priceService.updateProductPrice(productId, request, publisherId);

            return ResponseUtil.success("价格修改成功", result);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("修改失败: " + e.getMessage());
        }
    }
}