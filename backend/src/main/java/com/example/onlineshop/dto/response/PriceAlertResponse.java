 package com.example.onlineshop.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceAlertResponse {
    private Integer alertId;
    private Integer productId;
    private String productName;
    private String alertType;
    private BigDecimal thresholdPercentage;
    private Boolean isEnabled;
    private BigDecimal currentPrice;
    private LocalDateTime createdAt;
}