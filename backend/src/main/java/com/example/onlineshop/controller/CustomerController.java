// backend/src/main/java/com/example/onlineshop/controller/CustomerController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.CustomerLoginRequest;
import com.example.onlineshop.dto.request.CustomerRegisterRequest;
import com.example.onlineshop.dto.request.CustomerCancelOrderRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.CustomerService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private PurchaseIntentService purchaseIntentService;

    // 客户注册
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody CustomerRegisterRequest request) {
        try {
            // 调用服务注册客户
            Customer customer = customerService.registerCustomer(
                    request.getUsername(),
                    request.getPassword(),
                    request.getPhone(),
                    request.getDefault_address()
            );

            if (customer != null) {
                // 生成token
                String token = JwtUtil.generateCustomerToken(customer.getCustomerId(), customer.getUsername());

                // 构造返回数据
                Map<String, Object> result = new HashMap<>();
                result.put("token", token);

                Map<String, Object> customerInfo = new HashMap<>();
                customerInfo.put("customer_id", customer.getCustomerId());
                customerInfo.put("username", customer.getUsername());
                customerInfo.put("phone", customer.getPhone());
                customerInfo.put("default_address", customer.getDefaultAddress());
                customerInfo.put("created_at", customer.getCreatedAt());
                customerInfo.put("updated_at", customer.getUpdatedAt());

                result.put("customer_info", customerInfo);

                return ResponseEntity.ok(new ApiResponse(200, "注册成功", result));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "注册失败，用户名或手机号已存在", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "注册失败: " + e.getMessage(), null));
        }
    }

    // 客户登录
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody CustomerLoginRequest request) {
        try {
            Customer customer = customerService.login(request.getUsername(), request.getPassword());

            if (customer != null) {
                // 生成token
                String token = JwtUtil.generateCustomerToken(customer.getCustomerId(), customer.getUsername());

                // 构造返回数据
                Map<String, Object> result = new HashMap<>();
                result.put("token", token);

                Map<String, Object> customerInfo = new HashMap<>();
                customerInfo.put("customer_id", customer.getCustomerId());
                customerInfo.put("username", customer.getUsername());
                customerInfo.put("phone", customer.getPhone());
                customerInfo.put("default_address", customer.getDefaultAddress());
                customerInfo.put("created_at", customer.getCreatedAt());
                customerInfo.put("updated_at", customer.getUpdatedAt());

                result.put("customer_info", customerInfo);

                return ResponseEntity.ok(new ApiResponse(200, "登录成功", result));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "用户名或密码错误", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "登录失败: " + e.getMessage(), null));
        }
    }

    // 客户：查看自己的购买意向记录
    @GetMapping("/{customer_id}/purchase-intents")
    public ResponseEntity<ApiResponse> getCustomerPurchaseIntents(
            @RequestHeader("Authorization") String token,
            @PathVariable("customer_id") Integer customer_id,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "purchase_status", required = false) String purchase_status) {
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

    // 客户取消订单
    @PostMapping("/{customer_id}/purchase-intents/{purchase_id}/cancel")
    public ResponseEntity<ApiResponse> customerCancelOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable("customer_id") Integer customerId,
            @PathVariable("purchase_id") Integer purchaseId,
            @RequestBody CustomerCancelOrderRequest request) {
        // 鉴权校验
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        // 验证路径参数customerId与token中的customerId是否一致
        if (!customerIdFromToken.equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(403, "无权操作其他客户的订单", null));
        }

        try {
            ApiResponse response = purchaseIntentService.customerCancelOrder(
                    purchaseId, request.getCancelReason(), request.getCancelNotes(), customerId);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细异常信息
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "取消订单失败: " + e.getMessage(), null));
        }
    }

    /**
     * 客户确认收货
     */
    @PostMapping("/{customer_id}/purchase-intents/{purchase_id}/confirm-received")
    public ResponseEntity<ApiResponse> customerConfirmReceived(
            @RequestHeader("Authorization") String token,
            @PathVariable("customer_id") Integer customerId,
            @PathVariable("purchase_id") Integer purchaseId) {
        // 鉴权校验
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        // 验证路径参数customerId与token中的customerId是否一致
        if (!customerIdFromToken.equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(403, "无权操作其他客户的订单", null));
        }

        try {
            ApiResponse response = purchaseIntentService.customerConfirmReceived(purchaseId, customerId);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "确认收货失败: " + e.getMessage(), null));
        }
    }
}
