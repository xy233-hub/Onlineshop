package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 商品实体（对应product表）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer productId;
    private Integer sellerId;
    private Integer categoryId;
    private String productName;
    private String productDesc;
    private BigDecimal price;
    private Integer stockQuantity;
    private String productStatus;
    private String searchKeywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // 新增：封面图片 URL（来自 product_images 第一张图，非持久化）
    private String coverImage;


    // 新增：由 ProductService 填充，用于响应中的图片列表（仅 URL 列表）
    @Builder.Default
    private List<String> images = Collections.emptyList();

    // 新增：封面图（可与 images.first 一致）

    // 新增：媒体资源 URL 列表（如果需要返回简化结构）
    @Builder.Default
    private List<String> mediaResources = Collections.emptyList();

}
