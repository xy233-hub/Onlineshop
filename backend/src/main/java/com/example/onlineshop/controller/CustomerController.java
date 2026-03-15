// backend/src/main/java/com/example/onlineshop/controller/CustomerController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.CustomerLoginRequest;
import com.example.onlineshop.dto.request.CustomerRegisterRequest;
import com.example.onlineshop.dto.request.CustomerAddressRequest;
import com.example.onlineshop.dto.request.CustomerCancelOrderRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.CustomerAddress;
import com.example.onlineshop.entity.LogisticsProvider;
import com.example.onlineshop.entity.LogisticsTrack;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.service.CustomerAddressService;
import com.example.onlineshop.service.CustomerService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.service.LogisticsProviderService;
import com.example.onlineshop.service.LogisticsTrackService;
import com.example.onlineshop.entity.AfterSalesService;
import com.example.onlineshop.service.AfterSalesServiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;


    @Autowired
    private AfterSalesServiceService afterSalesServiceService;

    @Autowired
    private ProductService productService;

    @Autowired
    private LogisticsProviderService logisticsProviderService;


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
    @Autowired
    private CustomerAddressService customerAddressService;

    /**
     * 41. 客户添加收货地址
     */
    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse> addAddress(
            @RequestBody CustomerAddressRequest request) {
        try {
            CustomerAddress address = CustomerAddress.builder()
                    .customerId(1) // 使用默认值1
                    .recipientName(request.getRecipientName())
                    .recipientPhone(request.getRecipientPhone())
                    .province(request.getProvince())
                    .city(request.getCity())
                    .district(request.getDistrict())
                    .detailAddress(request.getDetailAddress())
                    .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                    .build();

            CustomerAddress saved = customerAddressService.addAddress(address);

            Map<String, Object> result = new HashMap<>();
            result.put("address_id", saved.getAddressId());
            result.put("customer_id", saved.getCustomerId());
            result.put("recipient_name", saved.getRecipientName());
            result.put("recipient_phone", saved.getRecipientPhone());
            result.put("province", saved.getProvince());
            result.put("city", saved.getCity());
            result.put("district", saved.getDistrict());
            result.put("detail_address", saved.getDetailAddress());
            result.put("is_default", saved.getIsDefault());
            result.put("created_at", saved.getCreatedAt());
            result.put("updated_at", saved.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "地址添加成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "地址添加失败：" + e.getMessage(), null));
        }
    }

    /**
     * 42. 客户编辑收货地址
     */
    @PutMapping("/addresses/{address_id}")
    public ResponseEntity<ApiResponse> updateAddress(
            @PathVariable("address_id") Integer addressId,
            @RequestBody CustomerAddressRequest request) {
        try {
            CustomerAddress existing = customerAddressService.getAddressById(addressId);
            if (existing == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "地址不存在", null));
            }

            CustomerAddress address = CustomerAddress.builder()
                    .recipientName(request.getRecipientName())
                    .recipientPhone(request.getRecipientPhone())
                    .province(request.getProvince())
                    .city(request.getCity())
                    .district(request.getDistrict())
                    .detailAddress(request.getDetailAddress())
                    .isDefault(request.getIsDefault())
                    .build();

            CustomerAddress updated = customerAddressService.updateAddress(addressId, address);

            Map<String, Object> result = new HashMap<>();
            result.put("address_id", updated.getAddressId());
            result.put("customer_id", updated.getCustomerId());
            result.put("recipient_name", updated.getRecipientName());
            result.put("recipient_phone", updated.getRecipientPhone());
            result.put("province", updated.getProvince());
            result.put("city", updated.getCity());
            result.put("district", updated.getDistrict());
            result.put("detail_address", updated.getDetailAddress());
            result.put("is_default", updated.getIsDefault());
            result.put("updated_at", updated.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "地址更新成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "地址更新失败：" + e.getMessage(), null));
        }
    }

    /**
     * 43. 客户删除收货地址
     */
    @DeleteMapping("/addresses/{address_id}")
    public ResponseEntity<ApiResponse> deleteAddress(
            @PathVariable("address_id") Integer addressId) {
        try {
            CustomerAddress existing = customerAddressService.getAddressById(addressId);
            if (existing == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "地址不存在", null));
            }

            boolean deleted = customerAddressService.deleteAddress(addressId, 1); // 使用默认值1

            if (deleted) {
                Map<String, Object> result = new HashMap<>();
                result.put("deleted_address_id", addressId);

                List<CustomerAddress> remaining = customerAddressService.getAddressesByCustomerId(1); // 使用默认值1
                if (!remaining.isEmpty()) {
                    CustomerAddress newDefault = remaining.stream()
                            .filter(CustomerAddress::getIsDefault)
                            .findFirst()
                            .orElse(remaining.get(0));
                    result.put("new_default_address_id", newDefault.getAddressId());
                }

                return ResponseEntity.ok(new ApiResponse(200, "地址删除成功", result));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "地址删除失败", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "地址删除失败：" + e.getMessage(), null));
        }
    }

    /**
     * 44. 客户查询收货地址列表
     */
    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse> getAddresses() {
        try {
            List<CustomerAddress> addresses = customerAddressService.getAddressesByCustomerId(1); // 使用默认值1

            List<Map<String, Object>> result = addresses.stream().map(addr -> {
                Map<String, Object> map = new HashMap<>();
                map.put("address_id", addr.getAddressId());
                map.put("customer_id", addr.getCustomerId());
                map.put("recipient_name", addr.getRecipientName());
                map.put("recipient_phone", addr.getRecipientPhone());
                map.put("province", addr.getProvince());
                map.put("city", addr.getCity());
                map.put("district", addr.getDistrict());
                map.put("detail_address", addr.getDetailAddress());
                map.put("is_default", addr.getIsDefault());
                map.put("created_at", addr.getCreatedAt());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * 45. 客户设置默认收货地址
     */
    @PatchMapping("/addresses/{address_id}/default")
    public ResponseEntity<ApiResponse> setDefaultAddress(
            @PathVariable("address_id") Integer addressId) {
        try {
            CustomerAddress existing = customerAddressService.getAddressById(addressId);
            if (existing == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "地址不存在", null));
            }

            CustomerAddress updated = customerAddressService.setDefaultAddress(addressId, 1); // 使用默认值1

            Map<String, Object> result = new HashMap<>();
            result.put("address_id", updated.getAddressId());
            result.put("is_default", updated.getIsDefault());
            result.put("updated_at", updated.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "默认地址设置成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "默认地址设置失败：" + e.getMessage(), null));
        }
    }

    @Autowired
    private LogisticsTrackService logisticsTrackService;

    /**
     * 48. 查询订单物流轨迹
     */
    @GetMapping("/orders/{purchase_id}/logistics")
    public ResponseEntity<ApiResponse> getOrderLogistics(
            @RequestHeader("Authorization") String token,
            @PathVariable("purchase_id") Integer purchaseId) {
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            PurchaseIntent intent = purchaseIntentService.getById(purchaseId);
            if (intent == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "订单不存在", null));
            }

            if (!intent.getCustomerId().equals(customerIdFromToken)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse(403, "无权查询其他客户的订单", null));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("purchase_id", intent.getPurchaseId());
            result.put("purchase_status", intent.getPurchaseStatus());

            if (intent.getLogisticsProviderId() != null) {
                LogisticsProvider provider = logisticsProviderService.getProviderById(intent.getLogisticsProviderId());
                
                Map<String, Object> logisticsInfo = new HashMap<>();
                logisticsInfo.put("provider_id", provider.getProviderId());
                logisticsInfo.put("provider_name", provider.getProviderName());
                logisticsInfo.put("provider_code", provider.getProviderCode());
                logisticsInfo.put("tracking_no", intent.getTrackingNo());
                logisticsInfo.put("shipped_at", intent.getShippedAt());
                
                result.put("logistics_info", logisticsInfo);

                List<LogisticsTrack> tracks = logisticsTrackService.getTracksByPurchaseId(purchaseId);
                
                List<Map<String, Object>> trackList = tracks.stream().map(track -> {
                    Map<String, Object> trackMap = new HashMap<>();
                    trackMap.put("track_id", track.getTrackId());
                    trackMap.put("track_time", track.getTrackTime());
                    trackMap.put("track_content", track.getTrackContent());
                    trackMap.put("track_location", track.getTrackLocation());
                    trackMap.put("track_status", track.getTrackStatus());
                    return trackMap;
                }).collect(Collectors.toList());

                result.put("tracks", trackList);

                if (!tracks.isEmpty()) {
                    LogisticsTrack latestTrack = tracks.get(tracks.size() - 1);
                    result.put("current_status", latestTrack.getTrackStatus());
                }
            }

            return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * 50. 客户发起售后服务申请
     */
    @PostMapping("/after-sales")
    public ResponseEntity<ApiResponse> createAfterSalesService(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            Integer purchaseId = (Integer) request.get("purchase_id");
            Integer productId = (Integer) request.get("product_id");
            String serviceType = (String) request.get("service_type");
            String serviceTitle = (String) request.get("service_title");
            String problemDescription = (String) request.get("problem_description");
            Double refundAmount = ((Number) request.get("refund_amount")).doubleValue();
            
            List<String> evidenceImages = (List<String>) request.get("evidence_images");
            String evidenceImagesJson = null;
            if (evidenceImages != null && !evidenceImages.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                evidenceImagesJson = mapper.writeValueAsString(evidenceImages);
            }

            AfterSalesService service = new AfterSalesService();
            service.setPurchaseId(purchaseId);
            service.setProductId(productId);
            service.setCustomerId(customerIdFromToken);
            service.setServiceType(serviceType);
            service.setServiceTitle(serviceTitle);
            service.setProblemDescription(problemDescription);
            service.setEvidenceImages(evidenceImagesJson);
            service.setRefundAmount(java.math.BigDecimal.valueOf(refundAmount));

            AfterSalesService created = afterSalesServiceService.createAfterSalesService(service);

            Map<String, Object> result = new HashMap<>();
            result.put("service_id", created.getServiceId());
            result.put("purchase_id", created.getPurchaseId());
            result.put("product_id", created.getProductId());
            result.put("customer_id", created.getCustomerId());
            result.put("service_type", created.getServiceType());
            result.put("service_title", created.getServiceTitle());
            result.put("problem_description", created.getProblemDescription());
            result.put("evidence_images", evidenceImages);
            result.put("refund_amount", created.getRefundAmount());
            result.put("service_status", created.getServiceStatus());
            result.put("created_at", created.getCreatedAt());
            result.put("updated_at", created.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "售后申请已提交", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "售后申请提交失败：" + e.getMessage(), null));
        }
    }

    /**
     * 51. 客户查询售后服务列表
     */
    @GetMapping("/after-sales")
    public ResponseEntity<ApiResponse> getAfterSalesServices(
            @RequestHeader("Authorization") String token,
            @RequestParam("customer_id") Integer customerId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "service_status", required = false) String serviceStatus) {
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        if (!customerIdFromToken.equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(403, "无权查询其他客户的售后", null));
        }

        try {
            List<AfterSalesService> services = afterSalesServiceService.getAfterSalesServicesByCustomerId(
                    customerId, serviceStatus, page, size);
            int total = afterSalesServiceService.countAfterSalesServicesByCustomerId(customerId, serviceStatus);

            Map<String, Object> result = new HashMap<>();
            result.put("page", page);
            result.put("size", size);
            result.put("total", total);

            List<Map<String, Object>> items = services.stream().map(service -> {
                Map<String, Object> item = new HashMap<>();
                item.put("service_id", service.getServiceId());
                item.put("purchase_id", service.getPurchaseId());
                
                Map<String, Object> productInfo = new HashMap<>();
                productInfo.put("product_id", service.getProductId());
                item.put("product_info", productInfo);
                
                item.put("service_type", service.getServiceType());
                item.put("service_title", service.getServiceTitle());
                item.put("refund_amount", service.getRefundAmount());
                item.put("service_status", service.getServiceStatus());
                item.put("seller_response", service.getSellerResponse());
                item.put("created_at", service.getCreatedAt());
                item.put("updated_at", service.getUpdatedAt());
                
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
     * 52. 客户查询售后服务详情
     */
    @GetMapping("/after-sales/{service_id}")
    public ResponseEntity<ApiResponse> getAfterSalesServiceDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable("service_id") Integer serviceId) {
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            AfterSalesService service = afterSalesServiceService.getAfterSalesServiceById(serviceId);
            if (service == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "售后服务单不存在", null));
            }

            if (!service.getCustomerId().equals(customerIdFromToken)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse(403, "无权查询该售后服务单", null));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("service_id", service.getServiceId());
            result.put("purchase_id", service.getPurchaseId());
            
            // 获取并填充订单信息
            PurchaseIntent purchaseIntent = purchaseIntentService.getById(service.getPurchaseId());
            if (purchaseIntent != null) {
                Map<String, Object> orderInfo = new HashMap<>();
                // 通过 productId 获取商品信息
                Product product = productService.getProductById(purchaseIntent.getProductId());
                if (product != null) {
                    orderInfo.put("product_name", product.getProductName());
                    orderInfo.put("product_image", product.getCoverImage());
                }
                orderInfo.put("order_amount", purchaseIntent.getTotalAmount());
                orderInfo.put("order_status", purchaseIntent.getPurchaseStatus());
                orderInfo.put("quantity", purchaseIntent.getQuantity());
                result.put("order_info", orderInfo);
            } else {
                result.put("order_info", null);
            }
            
            result.put("service_type", service.getServiceType());
            
            result.put("service_type", service.getServiceType());
            result.put("service_title", service.getServiceTitle());
            result.put("problem_description", service.getProblemDescription());
            
            List<String> evidenceImages = null;
            if (service.getEvidenceImages() != null && !service.getEvidenceImages().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                evidenceImages = mapper.readValue(service.getEvidenceImages(), List.class);
            }
            result.put("evidence_images", evidenceImages);
            
            result.put("refund_amount", service.getRefundAmount());
            result.put("service_status", service.getServiceStatus());
            result.put("seller_response", service.getSellerResponse());
            result.put("seller_decision", service.getSellerDecision());
            result.put("seller_decision_at", service.getSellerDecisionAt());
            result.put("created_at", service.getCreatedAt());
            result.put("updated_at", service.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * 53. 客户取消售后服务申请
     */
    @PostMapping("/after-sales/{service_id}/cancel")
    public ResponseEntity<ApiResponse> cancelAfterSalesService(
            @RequestHeader("Authorization") String token,
            @PathVariable("service_id") Integer serviceId,
            @RequestBody Map<String, String> request) {
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            String cancelReason = request.get("cancel_reason");
            
            AfterSalesService service = afterSalesServiceService.cancelAfterSalesService(
                    serviceId, customerIdFromToken, cancelReason);

            Map<String, Object> result = new HashMap<>();
            result.put("service_id", service.getServiceId());
            result.put("service_status", service.getServiceStatus());
            result.put("cancel_reason", service.getCancelReason());
            result.put("cancelled_at", service.getCancelledAt());
            result.put("updated_at", service.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "售后申请已取消", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "取消失败：" + e.getMessage(), null));
        }
    }

    /**
     * 54. 客户退货发货
     */
    @PostMapping("/after-sales/{service_id}/return-ship")
    public ResponseEntity<ApiResponse> returnShipAfterSalesService(
            @RequestHeader("Authorization") String token,
            @PathVariable("service_id") Integer serviceId,
            @RequestBody Map<String, String> request) {
        Integer customerIdFromToken = JwtUtil.getCustomerIdFromToken(token);
        if (customerIdFromToken == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }

        try {
            String trackingNo = request.get("return_tracking_no");
            String logisticsProvider = request.get("return_logistics_provider");
            
            AfterSalesService service = afterSalesServiceService.returnShipAfterSalesService(
                    serviceId, customerIdFromToken, trackingNo, logisticsProvider);

            Map<String, Object> result = new HashMap<>();
            result.put("service_id", service.getServiceId());
            result.put("service_status", service.getServiceStatus());
            result.put("return_tracking_no", service.getReturnTrackingNo());
            result.put("return_logistics_provider", service.getReturnLogisticsProvider());
            result.put("return_shipped_at", service.getReturnShippedAt());
            result.put("updated_at", service.getUpdatedAt());

            return ResponseEntity.ok(new ApiResponse(200, "退货发货信息已提交", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "提交失败：" + e.getMessage(), null));
        }
    }
}

