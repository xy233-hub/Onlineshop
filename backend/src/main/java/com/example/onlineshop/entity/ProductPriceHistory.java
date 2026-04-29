package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPriceHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer historyId;
    private Integer productId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String changeType;
    private String changeReason;
    private Integer changedBy;
    private LocalDateTime createdAt;
}