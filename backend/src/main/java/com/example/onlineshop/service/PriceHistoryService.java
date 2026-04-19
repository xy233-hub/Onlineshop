package com.example.onlineshop.service;

import com.example.onlineshop.entity.ProductPriceHistory;
import com.example.onlineshop.mapper.PricingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class PriceHistoryService {

    @Autowired
    private PricingMapper pricingMapper;

    public void recordPriceChange(Integer productId,
                                  BigDecimal oldPrice,
                                  BigDecimal newPrice,
                                  String changeType,
                                  String changeReason,
                                  Integer changedBy) {
        if (productId == null || oldPrice == null || newPrice == null) {
            return;
        }
        if (oldPrice.compareTo(newPrice) == 0) {
            return;
        }
        ProductPriceHistory history = ProductPriceHistory.builder()
                .productId(productId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .changeType(changeType == null ? "MANUAL" : changeType)
                .changeReason(changeReason)
                .changedBy(changedBy)
                .createdAt(LocalDateTime.now())
                .build();
        pricingMapper.insertPriceHistory(history);
    }

    public Map<String, Object> getPriceHistory(Integer productId,
                                               Integer days,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               Integer page,
                                               Integer size) {
        Map<String, Object> product = pricingMapper.selectProductPricingInfo(productId);
        if (product == null) {
            return null;
        }

        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size < 1) ? 50 : Math.min(size, 200);

        LocalDateTime end = endDate == null ? LocalDateTime.now() : LocalDateTime.of(endDate, LocalTime.MAX);
        LocalDateTime start;
        if (startDate != null) {
            start = LocalDateTime.of(startDate, LocalTime.MIN);
        } else {
            int safeDays = (days == null || days < 1) ? 7 : days;
            start = end.minusDays(safeDays);
        }

        int offset = (safePage - 1) * safeSize;
        List<ProductPriceHistory> rows = pricingMapper.listPriceHistory(productId, start, end, safeSize, offset);
        int total = pricingMapper.countPriceHistory(productId, start, end);
        Map<String, Object> stats = pricingMapper.priceStats(productId, start, end);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (ProductPriceHistory row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", row.getCreatedAt() == null ? null : row.getCreatedAt().toLocalDate().toString());
            item.put("price", row.getNewPrice());
            item.put("change_type", row.getChangeType());
            trend.add(item);
        }

        BigDecimal currentPrice = toDecimal(product.get("price"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("product_id", product.get("product_id"));
        data.put("product_name", product.get("product_name"));
        data.put("current_price", currentPrice);
        data.put("lowest_price", valueOrDefault(stats == null ? null : stats.get("lowest_price"), currentPrice));
        data.put("highest_price", valueOrDefault(stats == null ? null : stats.get("highest_price"), currentPrice));
        data.put("average_price", valueOrDefault(stats == null ? null : stats.get("average_price"), currentPrice));
        data.put("price_trend", trend);

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("total_changes", valueOrZero(stats, "total_changes"));
        statistics.put("decrease_count", valueOrZero(stats, "decrease_count"));
        statistics.put("increase_count", valueOrZero(stats, "increase_count"));
        statistics.put("max_decrease_percentage", valueOrDefault(stats == null ? null : stats.get("max_decrease_percentage"), BigDecimal.ZERO));
        statistics.put("max_increase_percentage", valueOrDefault(stats == null ? null : stats.get("max_increase_percentage"), BigDecimal.ZERO));

        data.put("statistics", statistics);
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", total);
        return data;
    }

    public Map<String, Object> getPriceSummary(Integer productId) {
        Map<String, Object> product = pricingMapper.selectProductPricingInfo(productId);
        if (product == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(30);
        Map<String, Object> stats = pricingMapper.priceStats(productId, start, now);

        BigDecimal currentPrice = toDecimal(product.get("price"));
        BigDecimal originalPrice = toDecimal(product.get("original_price"));
        if (originalPrice == null) {
            originalPrice = currentPrice;
        }

        BigDecimal deltaPct = BigDecimal.ZERO;
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0 && currentPrice != null) {
            deltaPct = currentPrice.subtract(originalPrice)
                    .divide(originalPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("product_id", product.get("product_id"));
        data.put("current_price", currentPrice);
        data.put("original_price", originalPrice);
        data.put("lowest_price_30d", valueOrDefault(stats == null ? null : stats.get("lowest_price"), currentPrice));
        data.put("highest_price_30d", valueOrDefault(stats == null ? null : stats.get("highest_price"), currentPrice));
        data.put("average_price_30d", valueOrDefault(stats == null ? null : stats.get("average_price"), currentPrice));
        data.put("price_change_percentage", deltaPct);
        data.put("has_active_promotion", Boolean.TRUE.equals(product.get("has_active_promotion")));
        data.put("promotion_end_time", pricingMapper.nearestPromotionEndTime(productId));
        return data;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private Object valueOrDefault(Object value, Object fallback) {
        return value == null ? fallback : value;
    }

    private long valueOrZero(Map<String, Object> stats, String key) {
        if (stats == null || stats.get(key) == null) return 0;
        Object raw = stats.get(key);
        if (raw instanceof Number) return ((Number) raw).longValue();
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception e) {
            return 0;
        }
    }
}

