// backend/src/main/java/com/example/onlineshop/controller/ProductController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.AiAssistantQueryRequest;
import com.example.onlineshop.dto.request.AiImageSearchRequest;
import com.example.onlineshop.dto.request.PurchaseIntentRequest;
import com.example.onlineshop.dto.response.AiAssistantProductResponse;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.AiShoppingAssistantService;
import com.example.onlineshop.service.AiVectorRetrieverService;
import com.example.onlineshop.service.PriceHistoryService;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.service.ProductVectorService;
import com.example.onlineshop.service.PromotionService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    @Autowired
    private  AiShoppingAssistantService aiShoppingAssistantService;

    @Autowired
    private AiVectorRetrieverService aiVectorRetrieverService;

    @Autowired
    private ProductVectorService productVectorService;

    @Autowired
    private PriceHistoryService priceHistoryService;

    @Autowired
    private PromotionService promotionService;

    /**
     * 搜索/分页/排序获取商品列表
     * 支持参数：q, category_id, status, min_price, max_price, page, size, sort_by, order
     * 若未传 status，默认只返回 online 商品
     */
    @GetMapping("")
    public Object listProducts(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category_id", required = false) Integer categoryId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "min_price", required = false) BigDecimal minPrice,
            @RequestParam(value = "max_price", required = false) BigDecimal maxPrice,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort_by", required = false) String sortBy,
            @RequestParam(value = "order", defaultValue = "desc") String order
    ) {
        try {
            if (page == null || page < 1) page = 1;
            if (size == null || size < 1) size = 10;
            int offset = (page - 1) * size;

            if (status == null || status.trim().isEmpty()) {
                status = "online";
            }

            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                return ResponseUtil.custom(400, "min_price 不能大于 max_price", null);
            }

            List<Product> products = productService.searchProducts(
                    q,
                    categoryId,
                    status,
                    minPrice,
                    maxPrice,
                    offset,
                    size,
                    sortBy,
                    order
            );
            int total = productService.countProducts(q, categoryId, status, minPrice, maxPrice);

            List<ProductInfoResponse> items = products.stream()
                    .map(ProductInfoResponse::new)
                    .collect(Collectors.toList());

            HashMap<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("size", size);
            data.put("total", total);
            data.put("items", items);

            return ResponseUtil.success("查询成功", data);
        } catch (IllegalArgumentException iae) {
            return ResponseUtil.custom(400, iae.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }


    @GetMapping("/{product_id}")
    public Object getProductDetail(@PathVariable("product_id") Integer productId) {
        try {
            if (productId == null) {
                return ResponseUtil.custom(400, "product_id 必填", null);
            }
            com.example.onlineshop.dto.response.ProductDetailResponse detail =
                    productService.getProductDetail(productId);
            if (detail == null) {
                return ResponseUtil.custom(404, "商品不存在", null);
            }
            return ResponseUtil.success("查询成功", detail);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @GetMapping("/{product_id}/price-history")
    public Object getPriceHistory(@PathVariable("product_id") Integer productId,
                                  @RequestParam(value = "days", required = false) Integer days,
                                  @RequestParam(value = "start_date", required = false) String startDate,
                                  @RequestParam(value = "end_date", required = false) String endDate,
                                  @RequestParam(value = "page", defaultValue = "1") Integer page,
                                  @RequestParam(value = "size", defaultValue = "50") Integer size) {
        try {
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);
            if (startDate != null && start == null) {
                return ResponseUtil.custom(400, "start_date 格式应为 YYYY-MM-DD", null);
            }
            if (endDate != null && end == null) {
                return ResponseUtil.custom(400, "end_date 格式应为 YYYY-MM-DD", null);
            }
            if (start != null && end != null && start.isAfter(end)) {
                return ResponseUtil.custom(400, "start_date 不能晚于 end_date", null);
            }

            Object data = priceHistoryService.getPriceHistory(productId, days, start, end, page, size);
            if (data == null) {
                return ResponseUtil.custom(404, "商品不存在", null);
            }
            return ResponseUtil.success("查询成功", data);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @GetMapping("/{product_id}/price-summary")
    public Object getPriceSummary(@PathVariable("product_id") Integer productId) {
        try {
            Object data = priceHistoryService.getPriceSummary(productId);
            if (data == null) {
                return ResponseUtil.custom(404, "商品不存在", null);
            }
            return ResponseUtil.success("查询成功", data);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @GetMapping("/{product_id}/promotions")
    public Object getProductPromotions(@PathVariable("product_id") Integer productId) {
        try {
            Object data = promotionService.productPromotions(productId);
            if (data == null) {
                return ResponseUtil.custom(404, "商品不存在", null);
            }
            return ResponseUtil.success("查询成功", data);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // 提交购买意向
    @PostMapping("/purchase-intents")
    public Object createPurchaseIntent(@RequestBody PurchaseIntentRequest req) {
        try {
            PurchaseIntent created = purchaseIntentService.createPurchaseIntent(req);
            return new ApiResponse(200, "创建购买意向成功", created);
        } catch (IllegalArgumentException e) {
            return new ApiResponse(400, e.getMessage(), null);
        } catch (IllegalStateException e) {
            return new ApiResponse(500, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(500, "服务器错误", null);
        }
    }


    @PostMapping("/ai-recommend")
    public Object aiRecommend(@RequestBody AiAssistantQueryRequest req) {
        try {
            AiAssistantProductResponse resp = aiShoppingAssistantService.recommend(
                    req == null ? null : req.getText(),
                    req == null ? null : req.getPage(),
                    req == null ? null : req.getSize(),
                    req == null ? null : req.getUserId(),
                    req == null ? null : req.getScene(),
                    req == null ? null : req.getAction()
            );
            return ResponseUtil.success("查询成功", resp);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    @PostMapping("/ai-image-search")
    public Object aiImageSearch(@RequestBody AiImageSearchRequest req) {
        try {
            if (req == null || req.getImageUrl() == null || req.getImageUrl().isBlank()) {
                return ResponseUtil.custom(400, "图片URL必填", null);
            }
            int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() : 1;
            int size = req.getSize() != null && req.getSize() > 0 ? req.getSize() : 10;

            AiVectorRetrieverService.RetrievalResult result = aiVectorRetrieverService.searchByImage(req.getImageUrl(), page, size);
            
            List<ProductInfoResponse> items = result.items().stream().map(sp -> {
                ProductInfoResponse item = new ProductInfoResponse(sp.product());
                item.score = sp.score();
                return item;
            }).collect(Collectors.toList());

            HashMap<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("size", size);
            data.put("total", result.total());
            data.put("items", items);

            return ResponseUtil.success("查询成功", data);
        } catch (IllegalStateException e) {
            return ResponseUtil.custom(503, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }
    
    @GetMapping("/ai-image-search/status")
    public Object getAiImageSearchStatus() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("status", aiVectorRetrieverService.getIndexStatus());
        
        ProductVectorService.VectorGenerationProgress progress = productVectorService.getProgress();
        HashMap<String, Object> progressData = new HashMap<>();
        progressData.put("isGenerating", progress.isGenerating());
        progressData.put("totalProducts", progress.getTotalProducts());
        progressData.put("processedProducts", progress.getProcessedProducts());
        progressData.put("totalImages", progress.getTotalImages());
        progressData.put("processedImages", progress.getProcessedImages());
        progressData.put("lastError", progress.getLastError());
        data.put("progress", progressData);
        
        return ResponseUtil.success("获取状态成功", data);
    }
    
    @PostMapping("/ai-image-search/generate-vectors")
    public Object generateVectorsForAllProducts() {
        if (productVectorService.isGenerating()) {
            return ResponseUtil.custom(400, "已有向量生成任务在运行中，请等待完成", null);
        }
        
        productVectorService.generateAllVectorsAsync();
        
        HashMap<String, Object> data = new HashMap<>();
        data.put("message", "向量生成任务已启动，请通过状态接口查询进度");
        return ResponseUtil.success("任务已启动", data);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
