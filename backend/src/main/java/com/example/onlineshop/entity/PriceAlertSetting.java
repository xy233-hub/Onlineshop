package com.example.onlineshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlertSetting {
    private Integer alertId;
    private Integer customerId;
    private Integer productId;
    private String alertType;
    private Boolean isEnabled;
    private BigDecimal thresholdPercentage;
    private BigDecimal lastAlertedPrice;
    private LocalDateTime lastAlertedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}