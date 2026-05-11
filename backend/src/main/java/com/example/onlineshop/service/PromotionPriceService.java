package com.example.onlineshop.service;

import com.example.onlineshop.entity.Promotion;
import com.example.onlineshop.mapper.PromotionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromotionPriceService {

    @Autowired
    private PromotionMapper promotionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Integer, PromotionPriceCache> priceCache = new ConcurrentHashMap<>();
    private final Map<Integer, Long> cacheTimestamp = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000;

    private volatile List<Promotion> cachedActivePromotions = null;
    private volatile long promotionsCacheTime = 0;
    private static final long PROMOTIONS_CACHE_TTL_MS = 30_000;

    public static class PromotionPriceResult {
        public BigDecimal originalPrice;
        public BigDecimal currentPromotionPrice;
        public Boolean hasActivePromotion;
        public List<Integer> activePromotionIds;
        public List<Map<String, Object>> promotions;

        public PromotionPriceResult(BigDecimal originalPrice, BigDecimal currentPromotionPrice,
                                    Boolean hasActivePromotion, List<Integer> activePromotionIds,
                                    List<Map<String, Object>> promotions) {
            this.originalPrice = originalPrice;
            this.currentPromotionPrice = currentPromotionPrice;
            this.hasActivePromotion = hasActivePromotion;
            this.activePromotionIds = activePromotionIds;
            this.promotions = promotions;
        }
    }

    private static class PromotionPriceCache {
        final PromotionPriceResult result;
        final long timestamp;

        PromotionPriceCache(PromotionPriceResult result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }
    }

    public PromotionPriceResult calculateProductPromotionPrice(Integer productId, BigDecimal basePrice, BigDecimal originalPrice, Integer categoryId) {
        if (productId == null || basePrice == null) {
            return new PromotionPriceResult(originalPrice, null, false, Collections.emptyList(), Collections.emptyList());
        }

        long now = System.currentTimeMillis();
        PromotionPriceCache cached = priceCache.get(productId);
        if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
            return cached.result;
        }

        PromotionPriceResult result = doCalculateProductPromotionPrice(productId, basePrice, originalPrice, categoryId);

        priceCache.put(productId, new PromotionPriceCache(result, now));
        cacheTimestamp.put(productId, now);

        return result;
    }

    private PromotionPriceResult doCalculateProductPromotionPrice(Integer productId, BigDecimal basePrice, BigDecimal originalPrice, Integer categoryId) {
        List<Promotion> activePromotions = getActivePromotions();
        if (activePromotions.isEmpty()) {
            return new PromotionPriceResult(originalPrice, null, false, Collections.emptyList(), Collections.emptyList());
        }

        List<Map<String, Object>> applicablePromotions = new ArrayList<>();

        for (Promotion promo : activePromotions) {
            if (isPromotionApplicable(promo, productId, categoryId)) {
                BigDecimal promoPrice = calculatePromotionPrice(promo, basePrice);
                if (promoPrice != null && promoPrice.compareTo(basePrice) < 0) {
                    Map<String, Object> promoInfo = new LinkedHashMap<>();
                    promoInfo.put("promotion_id", promo.getPromotionId());
                    promoInfo.put("promotion_name", promo.getPromotionName());
                    promoInfo.put("promotion_type", promo.getPromotionType());
                    promoInfo.put("discount_value", promo.getDiscountValue());
                    promoInfo.put("final_price", promoPrice);
                    promoInfo.put("discount_amount", basePrice.subtract(promoPrice).setScale(2, RoundingMode.HALF_UP));
                    promoInfo.put("priority", promo.getPriority());
                    promoInfo.put("start_time", promo.getStartTime());
                    promoInfo.put("end_time", promo.getEndTime());
                    applicablePromotions.add(promoInfo);
                }
            }
        }

        if (applicablePromotions.isEmpty()) {
            return new PromotionPriceResult(originalPrice, null, false, Collections.emptyList(), Collections.emptyList());
        }

        applicablePromotions.sort((a, b) -> {
            Integer priorityA = (Integer) a.get("priority");
            Integer priorityB = (Integer) b.get("priority");
            if (priorityA == null) priorityA = 0;
            if (priorityB == null) priorityB = 0;

            int cmp = priorityB.compareTo(priorityA);
            if (cmp != 0) return cmp;

            BigDecimal priceA = (BigDecimal) a.get("final_price");
            BigDecimal priceB = (BigDecimal) b.get("final_price");
            return priceA.compareTo(priceB);
        });

        Map<Integer, List<Map<String, Object>>> byPriority = new TreeMap<>(Comparator.reverseOrder());
        for (Map<String, Object> promo : applicablePromotions) {
            Integer priority = (Integer) promo.get("priority");
            if (priority == null) priority = 0;
            byPriority.computeIfAbsent(priority, k -> new ArrayList<>()).add(promo);
        }

        BigDecimal finalPrice = basePrice;
        List<Integer> appliedPromotionIds = new ArrayList<>();
        List<Map<String, Object>> appliedPromotions = new ArrayList<>();

        for (Map.Entry<Integer, List<Map<String, Object>>> entry : byPriority.entrySet()) {
            List<Map<String, Object>> samePriorityPromos = entry.getValue();
            samePriorityPromos.sort(Comparator.comparing(p -> (BigDecimal) p.get("final_price")));

            Map<String, Object> bestPromo = samePriorityPromos.get(0);
            BigDecimal bestPrice = (BigDecimal) bestPromo.get("final_price");

            if (bestPrice.compareTo(finalPrice) < 0) {
                finalPrice = bestPrice;
                Integer promoId = (Integer) bestPromo.get("promotion_id");
                if (!appliedPromotionIds.contains(promoId)) {
                    appliedPromotionIds.add(promoId);
                    appliedPromotions.add(bestPromo);
                }
            }
        }

        boolean hasActivePromotion = !appliedPromotionIds.isEmpty();
        BigDecimal currentPromotionPrice = hasActivePromotion ? finalPrice : null;

        return new PromotionPriceResult(originalPrice, currentPromotionPrice, hasActivePromotion, appliedPromotionIds, appliedPromotions);
    }

    private List<Promotion> getActivePromotions() {
        long now = System.currentTimeMillis();
        if (cachedActivePromotions != null && (now - promotionsCacheTime) < PROMOTIONS_CACHE_TTL_MS) {
            return cachedActivePromotions;
        }

        synchronized (this) {
            if (cachedActivePromotions != null && (now - promotionsCacheTime) < PROMOTIONS_CACHE_TTL_MS) {
                return cachedActivePromotions;
            }

            cachedActivePromotions = promotionMapper.listActive(null);
            promotionsCacheTime = now;
            return cachedActivePromotions;
        }
    }

    private boolean isPromotionApplicable(Promotion promo, Integer productId, Integer categoryId) {
        if (promo == null || productId == null) return false;

        String scope = promo.getApplicableScope();
        if (scope == null) return false;

        switch (scope.toUpperCase()) {
            case "ALL":
                return true;
            case "PRODUCT":
                List<Integer> targetProductIds = parseTargetIds(promo.getTargetIds());
                return targetProductIds.contains(productId);
            case "CATEGORY":
                if (categoryId == null) return false;
                List<Integer> targetCategoryIds = parseTargetIds(promo.getTargetIds());
                return targetCategoryIds.contains(categoryId);
            case "USER_GROUP":
                return false;
            default:
                return false;
        }
    }

    private BigDecimal calculatePromotionPrice(Promotion promo, BigDecimal basePrice) {
        if (promo == null || basePrice == null) return null;

        String type = promo.getPromotionType();
        if (type == null) return null;

        BigDecimal result;
        if ("DISCOUNT".equalsIgnoreCase(type)) {
            BigDecimal discountValue = promo.getDiscountValue();
            if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(BigDecimal.ONE) > 0) {
                return null;
            }
            result = basePrice.multiply(discountValue);
        } else if ("FULL_REDUCTION".equalsIgnoreCase(type)) {
            BigDecimal minPurchase = promo.getMinPurchaseAmount();
            if (minPurchase != null && basePrice.compareTo(minPurchase) < 0) {
                return null;
            }
            BigDecimal reduction = promo.getDiscountValue();
            if (reduction == null || reduction.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            BigDecimal maxDiscount = promo.getMaxDiscountAmount();
            if (maxDiscount != null && reduction.compareTo(maxDiscount) > 0) {
                reduction = maxDiscount;
            }
            result = basePrice.subtract(reduction);
        } else {
            return null;
        }

        if (result.compareTo(BigDecimal.ZERO) < 0) {
            result = BigDecimal.ZERO;
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Integer> parseTargetIds(String targetIdsJson) {
        if (targetIdsJson == null || targetIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(targetIdsJson, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void invalidateCache() {
        synchronized (this) {
            priceCache.clear();
            cacheTimestamp.clear();
            cachedActivePromotions = null;
            promotionsCacheTime = 0;
        }
    }

    public void invalidateProductCache(Integer productId) {
        if (productId != null) {
            priceCache.remove(productId);
            cacheTimestamp.remove(productId);
        }
    }

    public void invalidatePromotionsCache() {
        synchronized (this) {
            cachedActivePromotions = null;
            promotionsCacheTime = 0;
        }
    }
}
