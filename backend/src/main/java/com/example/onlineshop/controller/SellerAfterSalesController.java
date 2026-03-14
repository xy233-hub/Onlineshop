package com.example.onlineshop.controller;

import com.example.onlineshop.entity.AfterSalesService;
import com.example.onlineshop.entity.Seller;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.service.AfterSalesServiceService;
import com.example.onlineshop.service.CustomerService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller/after-sales")
public class SellerAfterSalesController {

    @Autowired
    private AfterSalesServiceService afterSalesServiceService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    @Autowired
    private ProductService productService;

    /**
     * 55. 卖家查询售后服务列表
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse> getAfterSalesServices(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "service_status", required = false) String serviceStatus,
            @RequestParam(value = "service_type", required = false) String serviceType) {
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            List<AfterSalesService> services = afterSalesServiceService.getAfterSalesServicesBySellerId(
                    sellerId, serviceStatus, serviceType, page, size);
            int total = afterSalesServiceService.countAfterSalesServicesBySellerId(sellerId, serviceStatus, serviceType);

            Map<String, Object> result = new HashMap<>();
            result.put("page", page);
            result.put("size", size);
            result.put("total", total);

            List<Map<String, Object>> items = services.stream().map(service -> {
                Map<String, Object> item = new HashMap<>();
                item.put("service_id", service.getServiceId());
                item.put("purchase_id", service.getPurchaseId());
                
                // 获取并填充客户信息
                Map<String, Object> customerInfo = new HashMap<>();
                Customer customer = customerService.findById(service.getCustomerId());
                if (customer != null) {
                    customerInfo.put("customer_id", customer.getCustomerId());
                    customerInfo.put("username", customer.getUsername());
                    customerInfo.put("phone", customer.getPhone());
                }
                item.put("customer_info", customerInfo);
                
                // 获取并填充商品信息
                Map<String, Object> productInfo = new HashMap<>();
                System.out.println("Service ID: " + service.getServiceId());
                System.out.println("Purchase ID: " + service.getPurchaseId());
                List<com.example.onlineshop.entity.PurchaseIntentItem> purchaseItems = purchaseIntentService.getItemsByPurchaseIntentId(service.getPurchaseId());
                System.out.println("Purchase Intent Items: " + purchaseItems);
                if (purchaseItems != null && !purchaseItems.isEmpty()) {
                    // 取第一个商品项的信息（假设每个售后申请对应一个商品）
                    com.example.onlineshop.entity.PurchaseIntentItem purchaseItem = purchaseItems.get(0);
                    System.out.println("Product ID from purchase intent item: " + purchaseItem.getProductId());
                    Product product = productService.getProductById(purchaseItem.getProductId());
                    System.out.println("Product: " + product);
                    if (product != null) {
                        System.out.println("Product Name: " + product.getProductName());
                        System.out.println("Product Price: " + product.getPrice());
                        productInfo.put("product_id", product.getProductId());
                        productInfo.put("product_name", product.getProductName());
                        productInfo.put("price", product.getPrice());
                    } else {
                        System.out.println("Product not found for product ID: " + purchaseItem.getProductId());
                    }
                } else {
                    System.out.println("Purchase intent items not found for purchase ID: " + service.getPurchaseId());
                }
                item.put("product_info", productInfo);
                
                item.put("service_type", service.getServiceType());
                item.put("service_title", service.getServiceTitle());
                item.put("refund_amount", service.getRefundAmount());
                item.put("service_status", service.getServiceStatus());
                item.put("created_at", service.getCreatedAt());
                
                return item;
            }).collect(Collectors.toList());

            result.put("items", items);

            return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * 56. 卖家查询售后服务详情
     */
    @GetMapping("/{service_id}")
    public ResponseEntity<ApiResponse> getAfterSalesServiceDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable("service_id") Integer serviceId) {
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            AfterSalesService service = afterSalesServiceService.getAfterSalesServiceById(serviceId);
            if (service == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "售后服务单不存在", null));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("service_id", service.getServiceId());
            result.put("purchase_id", service.getPurchaseId());

             // 获取并填充客户信息
            Customer customer = customerService.findById(service.getCustomerId());
            if (customer != null) {
                Map<String, Object> customerInfo = new HashMap<>();
                customerInfo.put("customer_id", customer.getCustomerId());
                customerInfo.put("username", customer.getUsername());
                customerInfo.put("phone", customer.getPhone());
                result.put("customer_info", customerInfo);
            } else {
                result.put("customer_info", null);
            }
            
            // 获取并填充订单信息
            PurchaseIntent purchaseIntent = purchaseIntentService.getById(service.getPurchaseId());
            if (purchaseIntent != null) {
                Map<String, Object> orderInfo = new HashMap<>();
                // 通过 purchase_intent_items 获取商品信息
                List<com.example.onlineshop.entity.PurchaseIntentItem> purchaseItems = purchaseIntentService.getItemsByPurchaseIntentId(service.getPurchaseId());
                if (purchaseItems != null && !purchaseItems.isEmpty()) {
                    // 取第一个商品项的信息
                    com.example.onlineshop.entity.PurchaseIntentItem purchaseItem = purchaseItems.get(0);
                    Product product = productService.getProductById(purchaseItem.getProductId());
                    if (product != null) {
                        orderInfo.put("product_name", product.getProductName());
                        orderInfo.put("product_image", product.getCoverImage());
                    }
                }
                orderInfo.put("order_amount", purchaseIntent.getTotalAmount());
                orderInfo.put("order_status", purchaseIntent.getPurchaseStatus());
                orderInfo.put("quantity", purchaseIntent.getQuantity());
                orderInfo.put("unit_price", purchaseIntent.getTotalAmount() != null && purchaseIntent.getQuantity() != null && purchaseIntent.getQuantity() > 0 
                    ? purchaseIntent.getTotalAmount().divide(java.math.BigDecimal.valueOf(purchaseIntent.getQuantity())) 
                    : null);
                result.put("order_info", orderInfo);
            } else {
                result.put("order_info", null);
            }
            
            result.put("service_type", service.getServiceType());
            result.put("service_title", service.getServiceTitle());
            result.put("problem_description", service.getProblemDescription());
            
            if (service.getEvidenceImages() != null && !service.getEvidenceImages().isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<String> evidenceImages = mapper.readValue(service.getEvidenceImages(), List.class);
                result.put("evidence_images", evidenceImages);
            }
            
            result.put("refund_amount", service.getRefundAmount());
            result.put("service_status", service.getServiceStatus());
            result.put("seller_response", service.getSellerResponse());
            result.put("seller_decision", service.getSellerDecision());
            result.put("created_at", service.getCreatedAt());
            result.put("updated_at", service.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));         
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * 57. 卖家处理售后服务申请
     */
    @PostMapping("/{service_id}/handle")
    public ResponseEntity<ApiResponse> handleAfterSalesService(
            @RequestHeader("Authorization") String token,
            @PathVariable("service_id") Integer serviceId,
            @RequestBody Map<String, String> request) {
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            String sellerDecision = request.get("seller_decision");
            String sellerResponse = request.get("seller_response");
            
            AfterSalesService service = afterSalesServiceService.handleAfterSalesService(
                    serviceId, sellerDecision, sellerResponse);

            Map<String, Object> result = new HashMap<>();
            result.put("service_id", service.getServiceId());
            result.put("service_status", service.getServiceStatus());
            result.put("seller_decision", service.getSellerDecision());
            result.put("seller_response", service.getSellerResponse());
            result.put("seller_decision_at", service.getSellerDecisionAt());
            result.put("updated_at", service.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "售后处理成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "处理失败：" + e.getMessage(), null));
        }
    }

    /**
     * 58. 卖家确认收到退货
     */
    @PostMapping("/{service_id}/confirm-return")
    public ResponseEntity<ApiResponse> confirmReturnAfterSalesService(
            @RequestHeader("Authorization") String token,
            @PathVariable("service_id") Integer serviceId,
            @RequestBody Map<String, String> request) {
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            String remark = request.get("remark");
            
            AfterSalesService service = afterSalesServiceService.confirmReturnAfterSalesService(serviceId, remark);

            Map<String, Object> result = new HashMap<>();
            result.put("service_id", service.getServiceId());
            result.put("service_status", service.getServiceStatus());
            result.put("return_received_at", service.getReturnReceivedAt());
            result.put("completed_at", service.getCompletedAt());
            result.put("refund_executed", true);
            result.put("refund_amount", service.getRefundAmount());
            result.put("updated_at", service.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "退货确认成功，退款已执行", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "确认失败：" + e.getMessage(), null));
        }
    }
}