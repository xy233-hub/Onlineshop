// backend/src/main/java/com/example/shop/controller/SellerProductController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.ProductRequest;
import com.example.onlineshop.dto.request.ProductIdRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.SellerService;
import com.example.onlineshop.service.CustomerService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller")
public class SellerProductController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    // 发布新商品
    @PostMapping("/product")
    public ApiResponse publishProduct(@RequestBody ProductRequest request) {
        return sellerService.publishProduct(request);
    }

    // 批量发布商品
    @PostMapping("/products")
    public ApiResponse publishProducts(@RequestBody List<ProductRequest> requests) {
        return sellerService.publishProducts(requests);
    }

    // 查看历史商品
    @GetMapping("/products")
    public ApiResponse listProducts() {
        Map<String, Object> params = new HashMap<>();
        return sellerService.listProducts(params);
    }

    // 冻结商品
    @PutMapping("/product/freeze")
    public ApiResponse freezeProduct(@RequestBody ProductIdRequest request) {
        return sellerService.freezeProduct(request.getProductId(), "");
    }

    // 恢复商品上线
    @PutMapping("/product/unfreeze")
    public ApiResponse unfreezeProduct(@RequestBody ProductIdRequest request) {
        return sellerService.unfreezeProduct(request.getProductId(), "");
    }

    // 标记商品为已售出
    @PutMapping("/product/mark-sold")
    public ApiResponse markProductSold(@RequestBody ProductIdRequest request) {
        return sellerService.markProductSold(request.getProductId(), 1, "");
    }

    // 查看所有客户信息
    @GetMapping("/customers")
    public ApiResponse listAllCustomers(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        // 调用customerService来获取所有客户
        List<Customer> customers = customerService.getAllCustomersWithPagination(page, size);
        int total = customerService.getCustomerCount();

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("items", customers);

        return new ApiResponse(200, "查询成功", result);
    }

    // 查看特定客户的购买历史
    @GetMapping("/customers/{customer_id}/purchase-history")
    public ApiResponse getCustomerPurchaseHistory(@PathVariable("customer_id") Integer customerId) {
        // 获取客户信息
        Customer customer = customerService.findById(customerId);

        // 获取客户的购买意向
        List<PurchaseIntent> purchaseIntents = purchaseIntentService.getPurchaseIntentsByCustomerId(customerId);

        Map<String, Object> result = new HashMap<>();
        result.put("customer_info", customer);
        result.put("purchase_history", purchaseIntents);

        return new ApiResponse(200, "查询成功", result);
    }
}
