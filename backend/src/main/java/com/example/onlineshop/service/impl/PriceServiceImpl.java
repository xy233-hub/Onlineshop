package com.example.onlineshop.service.impl;

import com.example.onlineshop.dto.request.PriceChangeRequest;
import com.example.onlineshop.dto.response.PriceHistoryResponse;
import com.example.onlineshop.dto.response.PriceSummaryResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.ProductPriceHistory;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.mapper.ProductPriceHistoryMapper;
import com.example.onlineshop.service.PriceAlertService;
import com.example.onlineshop.service.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PriceServiceImpl implements PriceService {

    @Autowired
    private ProductPriceHistoryMapper priceHistoryMapper;

    @Autowired
    private ProductMapper productMapper;

     @Override
    public PriceHistoryResponse getPriceHistory(Integer productId, Integer days,
                                                String startDate, String endDate,
                                                Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 50;
        
        if (days != null && days > 0) {
            LocalDate now = LocalDate.now();
            LocalDate start = now.minusDays(days);
            startDate = start.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            endDate = now.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        int offset = (page - 1) * size;
        List<ProductPriceHistory> histories = priceHistoryMapper.selectByProductId(
                productId, startDate, endDate, offset, size);
        int total = priceHistoryMapper.countByProductId(productId, startDate, endDate);

        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        PriceHistoryResponse response = new PriceHistoryResponse();
        response.setProductId(productId);
        response.setProductName(product.getProductName());
        response.setCurrentPrice(product.getPrice());
        response.setPage(page);
        response.setSize(size);
        response.setTotal(total);

        List<PriceHistoryResponse.PriceTrendItem> trendItems = histories.stream()
                .map(h -> {
                    PriceHistoryResponse.PriceTrendItem item = new PriceHistoryResponse.PriceTrendItem();
                    item.setDate(h.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    item.setPrice(h.getNewPrice());
                    item.setChangeType(h.getChangeType());
                    return item;
                })
                .collect(Collectors.toList());
        response.setPriceTrend(trendItems);

        PriceHistoryResponse.PriceStatistics stats = calculateStatistics(histories);
        response.setStatistics(stats);

        if (!histories.isEmpty()) {
            BigDecimal lowest = histories.stream()
                    .map(ProductPriceHistory::getNewPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getPrice());
            BigDecimal highest = histories.stream()
                    .map(ProductPriceHistory::getNewPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(product.getPrice());
            BigDecimal average = histories.stream()
                    .map(ProductPriceHistory::getNewPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(Math.max(histories.size(), 1)), 2, RoundingMode.HALF_UP);

            response.setLowestPrice(lowest);
            response.setHighestPrice(highest);
            response.setAveragePrice(average);
        } else {
            response.setLowestPrice(product.getPrice());
            response.setHighestPrice(product.getPrice());
            response.setAveragePrice(product.getPrice());
        }

        return response;
    }

     @Override
    public PriceSummaryResponse getPriceSummary(Integer productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        String startDate = thirtyDaysAgo.atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<ProductPriceHistory> histories = priceHistoryMapper.selectByProductId(
                productId, startDate, null, 0, 1000);

        PriceSummaryResponse response = new PriceSummaryResponse();
        response.setProductId(productId);
        response.setCurrentPrice(product.getPrice());
        response.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice() : product.getPrice());
        response.setHasActivePromotion(product.getHasActivePromotion() != null && product.getHasActivePromotion());

        if (!histories.isEmpty()) {
            BigDecimal lowest = histories.stream()
                    .map(ProductPriceHistory::getNewPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(product.getPrice());
            BigDecimal highest = histories.stream()
                    .map(ProductPriceHistory::getNewPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(product.getPrice());
            BigDecimal average = histories.stream()
                    .map(ProductPriceHistory::getNewPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(histories.size()), 2, RoundingMode.HALF_UP);

            response.setLowestPrice30d(lowest);
            response.setHighestPrice30d(highest);
            response.setAveragePrice30d(average);

            BigDecimal firstPrice = histories.get(histories.size() - 1).getNewPrice();
            if (firstPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal changePercent = product.getPrice().subtract(firstPrice)
                        .divide(firstPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                response.setPriceChangePercentage(changePercent);
            } else {
                response.setPriceChangePercentage(BigDecimal.ZERO);
            }
        } else {
            response.setLowestPrice30d(product.getPrice());
            response.setHighestPrice30d(product.getPrice());
            response.setAveragePrice30d(product.getPrice());
            response.setPriceChangePercentage(BigDecimal.ZERO);
        }

        return response;
    }

        @Autowired
    private PriceAlertService priceAlertService;

     @Override
    @Transactional
    public Map<String, Object> updateProductPrice(Integer productId, PriceChangeRequest request, Integer sellerId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        if (request.getNewPrice() == null || request.getNewPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于0");
        }

        BigDecimal oldPrice = product.getPrice();
        BigDecimal newPrice = request.getNewPrice();

        product.setPrice(newPrice);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.update(product);

        ProductPriceHistory history = ProductPriceHistory.builder()
                .productId(productId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .changeType("MANUAL")
                .changeReason(request.getChangeReason())
                .changedBy(sellerId)
                .build();
        priceHistoryMapper.insert(history);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("oldPrice", oldPrice);
        result.put("newPrice", newPrice);
        result.put("changeType", "MANUAL");
        result.put("updatedAt", LocalDateTime.now());

        try {
            int alertsTriggered = priceAlertService.checkAndNotifyPriceChange(productId, oldPrice, newPrice);
            result.put("alertsTriggered", alertsTriggered);
        } catch (Exception e) {
            System.err.println("价格提醒检查失败: " + e.getMessage());
            result.put("alertsTriggered", 0);
        }

        return result;
    }

    private PriceHistoryResponse.PriceStatistics calculateStatistics(List<ProductPriceHistory> histories) {
        PriceHistoryResponse.PriceStatistics stats = new PriceHistoryResponse.PriceStatistics();
        stats.setTotalChanges(histories.size());

        if (histories.size() > 1) {
            int decreaseCount = 0;
            int increaseCount = 0;
            BigDecimal maxDecrease = BigDecimal.ZERO;
            BigDecimal maxIncrease = BigDecimal.ZERO;

            for (int i = 1; i < histories.size(); i++) {
                BigDecimal currentPrice = histories.get(i).getNewPrice();
                BigDecimal previousPrice = histories.get(i - 1).getNewPrice();

                if (previousPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal changePercent = currentPrice.subtract(previousPrice)
                            .divide(previousPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    if (changePercent.compareTo(BigDecimal.ZERO) < 0) {
                        decreaseCount++;
                        if (changePercent.abs().compareTo(maxDecrease) > 0) {
                            maxDecrease = changePercent.abs();
                        }
                    } else if (changePercent.compareTo(BigDecimal.ZERO) > 0) {
                        increaseCount++;
                        if (changePercent.compareTo(maxIncrease) > 0) {
                            maxIncrease = changePercent;
                        }
                    }
                }
            }

            stats.setDecreaseCount(decreaseCount);
            stats.setIncreaseCount(increaseCount);
            stats.setMaxDecreasePercentage(maxDecrease);
            stats.setMaxIncreasePercentage(maxIncrease);
        } else {
            stats.setDecreaseCount(0);
            stats.setIncreaseCount(0);
            stats.setMaxDecreasePercentage(BigDecimal.ZERO);
            stats.setMaxIncreasePercentage(BigDecimal.ZERO);
        }

        return stats;
    }
}