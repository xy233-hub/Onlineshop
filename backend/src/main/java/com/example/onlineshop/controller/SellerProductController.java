// java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.MediaResourceRequest;
import com.example.onlineshop.dto.request.ProductRequest;
import com.example.onlineshop.dto.request.ProductIdRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.LogisticsProvider;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.PurchaseIntentItem;
import com.example.onlineshop.entity.LogisticsProvider;
import com.example.onlineshop.entity.LogisticsTrack;
import com.example.onlineshop.service.*;
import com.example.onlineshop.mapper.PurchaseIntentItemMapper;
import com.example.onlineshop.util.JwtUtil;
import com.example.onlineshop.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/seller")
public class SellerProductController {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    @Autowired
    private LogisticsProviderService logisticsProviderService;

    @Autowired
    private LogisticsTrackService logisticsTrackService;

    @Autowired
    private PurchaseIntentItemMapper purchaseIntentItemMapper;

    @Autowired
    private  ExternalAiClient externalAiClient;

    @Autowired
    private com.example.onlineshop.mapper.PricingMapper pricingMapper;

    @Autowired
    private PriceHistoryService priceHistoryService;

    @Autowired
    private PriceAlertService priceAlertService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private void validateMediaResources(List<MediaResourceRequest> medias) {
        if (medias == null) return;
        for (MediaResourceRequest m : medias) {
            if (m == null) continue;
            String tempKey = m.getTempKey();
            String mediaUrl = m.getMediaUrl();
            String fileName = m.getFileName();
            Integer fileSize = m.getFileSize();
            String mimeType = m.getMimeType();

            // 若整项为空（既无 temp_key 也无任何元数据），视为可忽略（通常前端提交空对象）
            boolean allEmpty = ( (tempKey == null || tempKey.isBlank())
                    && (mediaUrl == null || mediaUrl.isBlank())
                    && (fileName == null || fileName.isBlank())
                    && fileSize == null
                    && (mimeType == null || mimeType.isBlank()) );
            if (allEmpty) continue;

            // 若有 temp_key，则允许其它字段为空（后续通过 associateTemporaryToProduct 关联）
            if (tempKey != null && !tempKey.isBlank()) continue;

            // 没有 temp_key 的情况下必须提供完整元数据
            if (mediaUrl == null || mediaUrl.isBlank()
                    || fileName == null || fileName.isBlank()
                    || fileSize == null
                    || mimeType == null || mimeType.isBlank()) {
                throw new IllegalArgumentException("media_resources 中缺少必填元数据（media_url/file_name/file_size/mime_type）");
            }
        }
    }

    // 提取 productId 的辅助方法，兼容多种返回形式
    private Integer extractProductId(Object data) {
        if (data == null) return null;
        try {
            if (data instanceof Map) {
                Map<?,?> m = (Map<?,?>) data;
                Object pid = m.get("product_id");
                if (pid == null) pid = m.get("productId");
                if (pid == null) pid = m.get("id");
                if (pid instanceof Number) return ((Number) pid).intValue();
                if (pid instanceof String) {
                    String s = (String) pid;
                    if (!s.isBlank()) return Integer.valueOf(s);
                }
            } else if (data instanceof Number) {
                return ((Number) data).intValue();
            } else if (data instanceof String) {
                String s = (String) data;
                if (!s.isBlank()) return Integer.valueOf(s);
            } else if (data instanceof Product) {
                return ((Product) data).getProductId();
            }
            // 反射尝试（保守）：尝试读取 productId 或 product_id 字段
            try {
                java.lang.reflect.Field f = data.getClass().getDeclaredField("productId");
                f.setAccessible(true);
                Object v = f.get(data);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (NoSuchFieldException ignored) {}
            try {
                java.lang.reflect.Field f2 = data.getClass().getDeclaredField("product_id");
                f2.setAccessible(true);
                Object v2 = f2.get(data);
                if (v2 instanceof Number) return ((Number) v2).intValue();
            } catch (NoSuchFieldException ignored) {}
        } catch (Exception e) {
            // 忽略解析异常，返回 null 由调用方处理
            System.err.println("extractProductId failed: " + e.getMessage());
        }
        return null;
    }


    @PostMapping("/products/ai/description")
    public ApiResponse generateProductDescription(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            Integer sellerIdFromToken = JwtUtil.getSellerIdFromToken(token);
            if (customerId == null && sellerIdFromToken == null) {
                return ApiResponse.error(401, "未授权");
            }

            String productName = body.get("product_name") == null ? "" : String.valueOf(body.get("product_name"));
            Integer categoryId = null;
            Object cid = body.get("category_id");
            if (cid instanceof Number) {
                categoryId = ((Number) cid).intValue();
            } else if (cid instanceof String s && !s.isBlank()) {
                categoryId = Integer.valueOf(s);
            }
            String keywords = body.get("search_keywords") == null ? "" : String.valueOf(body.get("search_keywords"));
            String productDesc = body.get("product_desc") == null ? "" : String.valueOf(body.get("product_desc"));

            if (productName.isBlank()) {
                return ApiResponse.error(400, "product_name 必填");
            }

            Map<String, Object> data = externalAiClient.generateProductDescriptionPack(productName, categoryId, keywords, productDesc);
            return new ApiResponse(200, "生成成功", data);
        } catch (Exception e) {
            return ApiResponse.error(500, "生成失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @PostMapping("/products/ai/price-estimate")
    public ApiResponse estimateProductPrice(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            Integer sellerIdFromToken = JwtUtil.getSellerIdFromToken(token);
            if (customerId == null && sellerIdFromToken == null) {
                return ApiResponse.error(401, "未授权");
            }

            String productName = body.get("product_name") == null ? "" : String.valueOf(body.get("product_name"));
            Integer categoryId = null;
            Object cid = body.get("category_id");
            if (cid instanceof Number) {
                categoryId = ((Number) cid).intValue();
            } else if (cid instanceof String s && !s.isBlank()) {
                categoryId = Integer.valueOf(s);
            }
            String productDesc = body.get("product_desc") == null ? "" : String.valueOf(body.get("product_desc"));

            if (productName.isBlank()) {
                return ApiResponse.error(400, "product_name 必填");
            }

            Map<String, Object> estimate = externalAiClient.estimateProductPriceRange(productName, categoryId, productDesc);
            return new ApiResponse(200, "预估成功", estimate);
        } catch (Exception e) {
            return ApiResponse.error(500, "预估失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @PostMapping("/products")
    public ApiResponse publishProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            Integer sellerIdFromToken = JwtUtil.getSellerIdFromToken(token);
            Integer publisherId = customerId != null ? customerId : sellerIdFromToken;
            if (publisherId == null) {
                return ApiResponse.error(401, "未授权");
            }

            ProductRequest request = objectMapper.convertValue(body, ProductRequest.class);
            request.setSellerId(publisherId);

            // 新增：若前端未传 shortDesc，则调用 AI 自动生成并写入 request，后续入库
            if (request.getShortDesc() == null || request.getShortDesc().isBlank()) {
                String aiShort = externalAiClient.generateShortDesc(
                        request.getProductName(),
                        request.getProductDesc()
                );
                request.setShortDesc(aiShort == null ? "" : aiShort);
            }

            validateMediaResources(request.getMediaResources());
            ApiResponse resp = sellerService.publishProduct(request);

            if (resp != null && resp.getCode() == 200) {
                Integer productId = extractProductId(resp.getData());
                if (productId != null) {
                    Set<String> tempKeys = new LinkedHashSet<>();
                    if (request.getImages() != null) {
                        for (com.example.onlineshop.dto.request.ImageRequest img : request.getImages()) {
                            if (img == null) continue;
                            String key = img.getTempKey();
                            if (key != null && !key.isBlank()) tempKeys.add(key.trim());
                        }
                    }
                    if (request.getMediaResources() != null) {
                        for (MediaResourceRequest media : request.getMediaResources()) {
                            if (media == null) continue;
                            String key = media.getTempKey();
                            if (key != null && !key.isBlank()) tempKeys.add(key.trim());
                        }
                    }
                    tempKeys.addAll(extractTempKeysFromDesc(request.getProductDesc()));

                    String rewrittenDesc = request.getProductDesc();
                    for (String tempKey : tempKeys) {
                        try {
                            MediaService.AssociationResult result = mediaService.associateTemporaryToProduct(tempKey, productId);
                            rewrittenDesc = replaceTempUrlByKey(rewrittenDesc, tempKey, result.getMediaUrl());
                        } catch (Exception ex) {
                            System.err.println("associateTemporaryToProduct failed: " + ex.getMessage());
                        }
                    }

                    if (rewrittenDesc != null && !rewrittenDesc.equals(request.getProductDesc())) {
                        sellerService.updateProductDesc(productId, rewrittenDesc);
                    }

                    Product latest = sellerService.loadProductWithMedia(productId);
                    if (latest != null) {
                        return new ApiResponse(200, "发布成功", new ProductInfoResponse(latest));
                    }
                }
            }

            return resp;
        } catch (Exception e) {
            return ApiResponse.error(500, "查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @GetMapping("/products")
    public Object listProducts(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "seller_id", required = false) Integer sellerId) {
        try {
            // 从 token 中解析用户 ID
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            Integer sellerIdFromToken = JwtUtil.getSellerIdFromToken(token);
            
            // 如果是买家访问，只能查看自己的商品
            if (customerId != null) {
                sellerId = customerId;
            } else if (sellerIdFromToken != null) {
                // 如果是卖家访问：
                // - 如果传入了 seller_id 参数，则查询该卖家的商品
                // - 如果没有传入 seller_id 参数，则查询所有商品（管理员视角）
                if (sellerId == null) {
                    // 卖家没有传 seller_id 参数，查询所有商品
                    List<Product> allProducts = productService.getAllProducts();
                    List<ProductInfoResponse> items = allProducts.stream()
                            .map(ProductInfoResponse::new)
                            .collect(Collectors.toList());

                    HashMap<String, Object> data = new HashMap<>();
                    data.put("items", items);
                    data.put("total", items.size());

                    return ResponseUtil.success("查询成功", data);
                } else {
                    // 卖家传了 seller_id 参数，查询指定卖家的商品
                    sellerId = sellerId;
                }
            } else {
                return ApiResponse.error(401, "未授权");
            }

            List<Product> products = productService.getHistoryProducts(sellerId);
            List<ProductInfoResponse> items = products.stream()
                    .map(ProductInfoResponse::new)
                    .collect(Collectors.toList());

            HashMap<String, Object> data = new HashMap<>();
            data.put("items", items);
            data.put("total", items.size());

            return ResponseUtil.success("查询成功", data);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败：" + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @PutMapping("/products/{product_id}/price")
    public ApiResponse updateProductPrice(@RequestHeader("Authorization") String token,
                                          @PathVariable("product_id") Integer productId,
                                          @RequestBody Map<String, Object> body) {
        try {
            Integer sellerId = JwtUtil.getSellerIdFromToken(token);
            if (sellerId == null) {
                return ApiResponse.error(401, "未授权");
            }

            Product product = productService.getProductById(productId);
            if (product == null) {
                return ApiResponse.error(404, "商品不存在");
            }
            if (!sellerId.equals(product.getSellerId())) {
                return ApiResponse.error(403, "无权修改该商品价格");
            }

            BigDecimal newPrice = parseDecimal(body == null ? null : body.get("new_price"));
            String reason = body == null || body.get("change_reason") == null ? null : String.valueOf(body.get("change_reason"));
            if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error(400, "new_price 必须大于0");
            }

            java.util.Map<String, Object> pricing = pricingMapper.selectProductPricingInfo(productId);
            if (pricing == null) {
                return ApiResponse.error(404, "商品不存在");
            }

            BigDecimal oldPrice = (BigDecimal) pricing.get("price");
            if (oldPrice.compareTo(newPrice) == 0) {
                return new ApiResponse(200, "价格未发生变化", java.util.Map.of(
                        "product_id", productId,
                        "old_price", oldPrice,
                        "new_price", newPrice,
                        "change_type", "MANUAL",
                        "updated_at", LocalDateTime.now(),
                        "alerts_triggered", 0
                ));
            }

            BigDecimal originalPrice = (BigDecimal) pricing.get("original_price");
            Boolean hasActivePromotion = Boolean.TRUE.equals(pricing.get("has_active_promotion"));
            if (originalPrice == null || !hasActivePromotion) {
                originalPrice = newPrice;
            }

            LocalDateTime now = LocalDateTime.now();
            pricingMapper.deactivateProductPromotions(productId, now);
            pricingMapper.updateProductPrice(productId, newPrice, originalPrice, null, false, now);
            priceHistoryService.recordPriceChange(productId, oldPrice, newPrice, "MANUAL", reason, sellerId);
            int alerts = priceAlertService.handlePriceChange(productId, oldPrice, newPrice);

            Map<String, Object> data = new HashMap<>();
            data.put("product_id", productId);
            data.put("old_price", oldPrice);
            data.put("new_price", newPrice);
            data.put("change_type", "MANUAL");
            data.put("updated_at", now);
            data.put("alerts_triggered", alerts);
            return new ApiResponse(200, "价格修改成功", data);
        } catch (Exception e) {
            return ApiResponse.error(500, "价格修改失败: " + e.getMessage());
        }
    }


    // 冻结商品
    @PutMapping("/products/{product_id}/freeze")
    public ApiResponse freezeProduct(
            @PathVariable("product_id") Long productId,
            @RequestBody(required = false) Map<String, Object> body) {
        String reason = null;
        if (body != null && body.get("reason") != null) {
            reason = String.valueOf(body.get("reason"));
        }
        // 调用 sellerService.freezeProduct(productId, reason) 或兼容旧签名
        try {
            return sellerService.freezeProduct(productId);
        } catch (NoSuchMethodError e) {
            // 如果 service 还未修改，退回到只传 productId 的调用
            return sellerService.freezeProduct(productId);
        }
    }
    // 恢复商品上线
    @PutMapping("/products/{product_id}/unfreeze")
    public ApiResponse unfreezeProduct(
            @PathVariable("product_id") Long productId,
            @RequestBody(required = false) Map<String, Object> body) {
        String remark = null;
        if (body != null && body.get("remark") != null) {
            remark = String.valueOf(body.get("remark"));
        }
        try {
            return sellerService.unfreezeProduct(productId);
        } catch (NoSuchMethodError e) {
            return sellerService.unfreezeProduct(productId);
        }
    }

    // 标记商品为已售出
    @PutMapping("/products/{product_id}/mark-sold")
    public ApiResponse markProductSold(
            @PathVariable("product_id") Long productId,
            @RequestBody(required = false) Map<String, Object> body) {
        Integer soldQuantity = 1;
        String note = null;
        if (body != null) {
            Object sq = body.get("sold_quantity");
            if (sq instanceof Number) soldQuantity = ((Number) sq).intValue();
            else if (sq instanceof String && !((String) sq).isBlank()) soldQuantity = Integer.valueOf((String) sq);
            if (body.get("note") != null) note = String.valueOf(body.get("note"));
        }
        try {
            return sellerService.markProductSold(productId);
        } catch (NoSuchMethodError e) {
            return sellerService.markProductSold(productId);
        }
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
        /**
     * 47. 卖家发货（填写物流信息）
     */
      @PostMapping("/purchase-intents/{purchase_id}/ship")
   public ApiResponse shipOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable("purchase_id") Integer purchaseId,
            @RequestBody Map<String, Object> body) {
        try {
            Integer sellerId = JwtUtil.getSellerIdFromToken(token);
            
            // 如果不是卖家，尝试解析 customerId（买家调用时）
            if (sellerId == null) {
                Integer customerId = JwtUtil.getCustomerIdFromToken(token);
                if (customerId != null) {
                    // 买家调用时，将 customerId 作为 sellerId 使用
                    sellerId = customerId;
                }
            }
            
            if (sellerId == null) {
                return new ApiResponse(401, "未授权", null);
            }

            PurchaseIntent intent = purchaseIntentService.getById(purchaseId);
            if (intent == null) {
                return new ApiResponse(400, "购买意向不存在", null);
            }

            List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(purchaseId);
            if (items == null || items.isEmpty()) {
                return new ApiResponse(404, "关联商品不存在", null);
            }
            
            PurchaseIntentItem firstItem = items.get(0);
            Product product = productService.getProductById(firstItem.getProductId());
            if (product == null || !product.getSellerId().equals(sellerId)) {
                return new ApiResponse(403, "无权操作该订单", null);
            }

            Integer logisticsProviderId = null;
            String trackingNo = null;

            if (body != null) {
                Object lpId = body.get("logistics_provider_id");
                if (lpId instanceof Number) {
                    logisticsProviderId = ((Number) lpId).intValue();
                } else if (lpId instanceof String && !((String) lpId).isBlank()) {
                    logisticsProviderId = Integer.valueOf((String) lpId);
                }

                trackingNo = body.containsKey("tracking_no") ? String.valueOf(body.get("tracking_no")) : null;
            }

            if (logisticsProviderId == null || trackingNo == null) {
                return new ApiResponse(400, "缺少必填参数：logistics_provider_id 或 tracking_no", null);
            }

            LogisticsProvider provider = logisticsProviderService.getProviderById(logisticsProviderId);
            if (provider == null) {
                return new ApiResponse(400, "物流公司不存在", null);
            }

           purchaseIntentService.shipOrder(purchaseId, logisticsProviderId, trackingNo);

            Map<String, Object> result = new HashMap<>();
            result.put("purchase_id", intent.getPurchaseId());
            result.put("purchase_status", "SHIPPING_STARTED");
            result.put("logistics_provider_id", logisticsProviderId);
            result.put("logistics_provider_name", provider.getProviderName());
            result.put("tracking_no", trackingNo);
            result.put("shipped_at", intent.getUpdatedAt());
            result.put("updated_at", intent.getUpdatedAt());

            return new ApiResponse(200, "发货成功", result);
        } catch (Exception e) {
            return new ApiResponse(500, "发货失败：" + e.getMessage(), null);
        }
    }


    /**
     * 49. 卖家手动添加物流轨迹
     */
    @PostMapping("/orders/{purchase_id}/logistics/tracks")
    public ApiResponse addLogisticsTrack(
            @RequestHeader("Authorization") String token,
            @PathVariable("purchase_id") Integer purchaseId,
            @RequestBody Map<String, Object> body) {
        try {
            // 从 token 中解析 sellerId
            Integer sellerId = JwtUtil.getSellerIdFromToken(token);

            // 如果不是卖家，尝试解析 customerId（买家调用时）
            if (sellerId == null) {
                Integer customerId = JwtUtil.getCustomerIdFromToken(token);
                if (customerId != null) {
                    // 买家调用时，将 customerId 作为 sellerId 使用
                    sellerId = customerId;
                }
            }

            if (sellerId == null) {
                return new ApiResponse(401, "未授权", null);
            }

            PurchaseIntent intent = purchaseIntentService.getById(purchaseId);
            if (intent == null) {
                return new ApiResponse(400, "订单不存在", null);
            }

            List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(purchaseId);
            if (items == null || items.isEmpty()) {
                return new ApiResponse(404, "关联商品不存在", null);
            }

            PurchaseIntentItem firstItem = items.get(0);
            Product product = productService.getProductById(firstItem.getProductId());
            if (product == null || !product.getSellerId().equals(sellerId)) {
                return new ApiResponse(403, "无权操作该订单", null);
            }

            String trackContent = null;
            String trackLocation = null;
            String trackStatus = null;

            if (body != null) {
                trackContent = body.containsKey("track_content") ? String.valueOf(body.get("track_content")) : null;
                trackLocation = body.containsKey("track_location") ? String.valueOf(body.get("track_location")) : null;
                trackStatus = body.containsKey("track_status") ? String.valueOf(body.get("track_status")) : null;
            }

            if (trackContent == null || trackContent.isBlank()) {
                return new ApiResponse(400, "track_content 必填", null);
            }

            logisticsTrackService.addManualTrack(purchaseId, trackContent, trackLocation, trackStatus);

            List<LogisticsTrack> allTracks = logisticsTrackService.getTracksByPurchaseId(purchaseId);
            LogisticsTrack latestTrack = allTracks.get(allTracks.size() - 1);

            Map<String, Object> result = new HashMap<>();
            result.put("track_id", latestTrack.getTrackId());
            result.put("purchase_id", purchaseId);
            result.put("track_time", latestTrack.getTrackTime());
            result.put("track_content", latestTrack.getTrackContent());
            result.put("track_location", latestTrack.getTrackLocation());
            result.put("track_status", latestTrack.getTrackStatus());

            return new ApiResponse(200, "物流轨迹添加成功", result);
        } catch (Exception e) {
            return new ApiResponse(500, "添加失败：" + e.getMessage(), null);
        }
    }

    private static final Pattern TEMP_KEY_IN_DESC = Pattern.compile("/temp/([0-9a-fA-F\\-]{36})[^\"'<>\\s]*");

    private Set<String> extractTempKeysFromDesc(String desc) {
        Set<String> keys = new LinkedHashSet<>();
        if (desc == null || desc.isBlank()) return keys;
        Matcher matcher = TEMP_KEY_IN_DESC.matcher(desc);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key != null && !key.isBlank()) keys.add(key);
        }
        return keys;
    }

    private String replaceTempUrlByKey(String desc, String tempKey, String finalUrl) {
        if (desc == null || desc.isBlank() || tempKey == null || tempKey.isBlank() || finalUrl == null || finalUrl.isBlank()) {
            return desc;
        }
        String expr = "(?i)(https?://[^\\\"'\\s<]+)?/media/temp/" + Pattern.quote(tempKey) + "[^\\\"'\\s<]*";
        return desc.replaceAll(expr, Matcher.quoteReplacement(finalUrl));
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
