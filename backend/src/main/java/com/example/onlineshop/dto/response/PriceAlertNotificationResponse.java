package com.example.onlineshop.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceAlertNotificationResponse {
    private Integer notificationId;
    private Integer productId;
    private String productName;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal changePercentage;
    private String changeDirection;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}