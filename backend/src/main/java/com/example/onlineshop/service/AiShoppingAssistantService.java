// src/main/java/com/example/onlineshop/service/AiShoppingAssistantService.java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.ai.AiProductQuery;
import com.example.onlineshop.dto.response.AiAssistantProductResponse;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiShoppingAssistantService {

    private final ProductService productService;
    private final ExternalAiClient externalAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiShoppingAssistantService(ProductService productService, ExternalAiClient externalAiClient) {
        this.productService = productService;
        this.externalAiClient = externalAiClient;
    }

    public AiAssistantProductResponse recommend(String userText, Integer page, Integer size) {
        if (userText == null || userText.trim().isEmpty()) {
            throw new IllegalArgumentException("text 必填");
        }

        AiProductQuery q = externalAiClient.extractQuery(userText);
        if (q == null) q = new AiProductQuery();

        // 判定：是否提取到了任何有效过滤条件（避免空条件查全表）
        boolean hasAnyFilter =
                (q.getQ() != null && !q.getQ().isBlank())
                        || q.getCategoryId() != null
                        || q.getMinPrice() != null
                        || q.getMaxPrice() != null
                        || (q.getStatus() != null && !q.getStatus().isBlank());

        // 若 AI 什么都没提取到，就不做“查全部商品”
        if (!hasAnyFilter) {
            int p = (page != null && page > 0) ? page : 1;
            int s = (size != null && size > 0) ? size : 10;
            q.setPage(p);
            q.setSize(s);
            q.setStatus("online");
            q.setSortBy("created_at");
            q.setOrder("desc");
            return new AiAssistantProductResponse(q, "未能从输入中提取到有效筛选条件", p, s, 0, java.util.Collections.emptyList());
        }

        if (q.getStatus() == null || q.getStatus().isBlank()) {
            q.setStatus("online");
        }
        if (q.getPage() == null || q.getPage() < 1) {
            q.setPage(page != null && page > 0 ? page : 1);
        }
        if (q.getSize() == null || q.getSize() < 1) {
            q.setSize(size != null && size > 0 ? size : 10);
        }

        String order = q.getOrder();
        if (order == null || (!order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc"))) {
            order = "desc";
        }
        q.setOrder(order.toLowerCase(java.util.Locale.ROOT));

        String sortBy = q.getSortBy();
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "created_at";
        }
        if (!"price".equalsIgnoreCase(sortBy) && !"created_at".equalsIgnoreCase(sortBy) && !"stock_quantity".equalsIgnoreCase(sortBy)) {
            sortBy = "created_at";
        }
        q.setSortBy(sortBy.toLowerCase(java.util.Locale.ROOT));

        int offset = (q.getPage() - 1) * q.getSize();

        java.util.List<com.example.onlineshop.entity.Product> products = productService.searchProducts(
                q.getQ(),
                q.getCategoryId(),
                q.getStatus(),
                q.getMinPrice(),
                q.getMaxPrice(),
                offset,
                q.getSize(),
                q.getSortBy(),
                q.getOrder()
        );
        int total = productService.countProducts(q.getQ(), q.getCategoryId(), q.getStatus(), q.getMinPrice(), q.getMaxPrice());

        java.util.List<com.example.onlineshop.dto.response.ProductInfoResponse> items = products == null ? java.util.Collections.emptyList()
                : products.stream().filter(java.util.Objects::nonNull).map(com.example.onlineshop.dto.response.ProductInfoResponse::new).collect(java.util.stream.Collectors.toList());

        String summaryJson = buildProductsSummaryJson(products);
        String aiDesc = externalAiClient.generateDescription(userText, summaryJson);

        return new com.example.onlineshop.dto.response.AiAssistantProductResponse(q, aiDesc, q.getPage(), q.getSize(), total, items);
    }

    private String buildProductsSummaryJson(List<Product> products) {
        try {
            List<Map<String, Object>> summary = products == null ? Collections.emptyList() : products.stream()
                    .filter(Objects::nonNull)
                    .limit(10)
                    .map(p -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("productId", p.getProductId());
                        m.put("productName", p.getProductName());
                        m.put("price", p.getPrice());
                        m.put("stockQuantity", p.getStockQuantity());
                        m.put("productStatus", p.getProductStatus());
                        m.put("coverImage", p.getCoverImage());
                        return m;
                    })
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "[]";
        }
    }
}
