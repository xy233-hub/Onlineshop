package com.example.shop.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（对应product表）
 */
@Data
public class Product {
    private Integer productId;      // 商品ID
    private Integer sellerId;       // 卖家ID
    private String productName;     // 商品名称
    private String productDesc;     // 商品描述
    private String imageUrl;        // 图片链接
    private BigDecimal price;       // 商品价格
    private String productStatus;   // 商品状态
    private LocalDateTime createdAt;// 上架时间
    private LocalDateTime updatedAt;// 最后更新时间
}
