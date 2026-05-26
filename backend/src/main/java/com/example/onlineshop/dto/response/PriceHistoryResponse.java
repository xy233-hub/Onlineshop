package com.example.onlineshop.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PriceHistoryResponse {
    private Integer productId;
    private String productName;
    private BigDecimal currentPrice;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
    private BigDecimal averagePrice;
    private List<PriceTrendItem> priceTrend;
    private PriceStatistics statistics;
    private Integer page;
    private Integer size;
    private Integer total;

    @Data
    public static class PriceTrendItem {
        private String date;
        private BigDecimal price;
        private String changeType;
    }

    @Data
    public static class PriceStatistics {
        private Integer totalChanges;
        private Integer decreaseCount;
        private Integer increaseCount;
        private BigDecimal maxDecreasePercentage;
        private BigDecimal maxIncreasePercentage;
    }
}