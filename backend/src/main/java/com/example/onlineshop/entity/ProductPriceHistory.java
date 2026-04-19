package com.example.onlineshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPriceHistory {
    private Integer historyId;
    private Integer productId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String changeType;
    private String changeReason;
    private Integer changedBy;
    private LocalDateTime createdAt;
}

