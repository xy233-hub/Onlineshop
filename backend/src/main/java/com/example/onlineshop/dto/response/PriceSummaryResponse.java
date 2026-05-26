package com.example.onlineshop.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceSummaryResponse {
    private Integer productId;
    private BigDecimal currentPrice;
    private BigDecimal originalPrice;
    private BigDecimal lowestPrice30d;
    private BigDecimal highestPrice30d;
    private BigDecimal averagePrice30d;
    private BigDecimal priceChangePercentage;
    private Boolean hasActivePromotion;
    private String promotionEndTime;
}