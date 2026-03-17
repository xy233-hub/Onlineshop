package com.example.onlineshop.util;

import com.example.onlineshop.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResponseUtil {

    public static ResponseEntity<ApiResponse> success(Object data) {
        return ResponseEntity.ok(new ApiResponse(200, "操作成功", data));
    }

    public static ResponseEntity<ApiResponse> success(String message, Object data) {
        return ResponseEntity.ok(new ApiResponse(200, message, data));
    }

    public static ResponseEntity<ApiResponse> error(int code, String message) {
        return ResponseEntity.status(code).body(new ApiResponse(code, message, null));
    }

    public static ResponseEntity<ApiResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ApiResponse(400, message, null));
    }

    public static ResponseEntity<ApiResponse> unauthorized(String message) {
        return ResponseEntity.status(401).body(new ApiResponse(401, message, null));
    }

    public static ResponseEntity<ApiResponse> forbidden(String message) {
        return ResponseEntity.status(403).body(new ApiResponse(403, message, null));
    }

    public static ResponseEntity<ApiResponse> notFound(String message) {
        return ResponseEntity.status(404).body(new ApiResponse(404, message, null));
    }

    public static ResponseEntity<ApiResponse> serverError(String message) {
        return ResponseEntity.status(500).body(new ApiResponse(500, message, null));
    }
     // 兼容旧代码的单参数 error 方法（默认返回 500）
    public static ResponseEntity<ApiResponse> error(String message) {
        return ResponseEntity.status(500).body(new ApiResponse(500, message, null));
    }

    public static ResponseEntity<ApiResponse> custom(int code, String message, Object data) {
        return ResponseEntity.status(code).body(new ApiResponse(code, message, data));
    }

    public static Map<String, Object> buildPageResult(int page, int size, int total, List<?> items) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("items", items);
        return result;
    }

    public static Map<String, Object> buildCustomerInfo(com.example.onlineshop.entity.Customer customer) {
        if (customer == null) {
            return null;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("customer_id", customer.getCustomerId());
        info.put("username", customer.getUsername());
        info.put("phone", customer.getPhone());
        info.put("default_address", customer.getDefaultAddress());
        info.put("created_at", customer.getCreatedAt());
        info.put("updated_at", customer.getUpdatedAt());
        return info;
    }

    public static Map<String, Object> buildSellerInfo(com.example.onlineshop.entity.Seller seller) {
        if (seller == null) {
            return null;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("seller_id", seller.getSellerId());
        info.put("username", seller.getUsername());
        info.put("create_time", seller.getCreateTime());
        return info;
    }
}