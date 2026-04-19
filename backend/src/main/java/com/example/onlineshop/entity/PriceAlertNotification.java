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
public class PriceAlertNotification {
    private Integer notificationId;
    private Integer alertId;
    private Integer customerId;
    private Integer productId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal changePercentage;
    private String changeDirection;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

