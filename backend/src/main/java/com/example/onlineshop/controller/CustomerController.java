// backend/src/main/java/com/example/onlineshop/controller/CustomerController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.CustomerLoginRequest;
import com.example.onlineshop.dto.request.CustomerRegisterRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.service.CustomerService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

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
}
