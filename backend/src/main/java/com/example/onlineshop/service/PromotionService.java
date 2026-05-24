package com.example.onlineshop.service;

import com.example.onlineshop.entity.Promotion;
import com.example.onlineshop.entity.PromotionRule;
import com.example.onlineshop.mapper.PromotionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class PromotionService {

    @Autowired
    private PromotionMapper promotionMapper;

    @Autowired
    private PriceHistoryService priceHistoryService;

    @Autowired
    private PriceAlertService priceAlertService;

    @Autowired
    private PromotionPriceService promotionPriceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> createPromotion(Map<String, Object> req, Integer createdBy) {
        Promotion promotion = parsePromotion(req, createdBy);
        promotion.setStatus("DRAFT");
        promotionMapper.insert(promotion);
        saveRules(promotion.getPromotionId(), req.get("rules"));

        Map<String, Object> data = toSimplePromotion(promotion);
        data.put("affected_products_count", resolveAffectedProductIds(promotion).size());
        return data;
    }

    public Map<String, Object> listPromotions(String status, String type, LocalDate startDate, LocalDate endDate, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Promotion> rows = promotionMapper.list(status, type, start, end, safeSize, offset);
        int total = promotionMapper.count(status, type, start, end);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Promotion row : rows) {
            Map<String, Object> item = toSimplePromotion(row);
            item.put("affected_products_count", resolveAffectedProductIds(row).size());
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", total);
        data.put("items", items);
        return data;
    }

    public Map<String, Object> getPromotionDetail(Integer promotionId) {
        Promotion promotion = promotionMapper.findById(promotionId);
        if (promotion == null) {
            return null;
        }

        Map<String, Object> data = toSimplePromotion(promotion);
        data.put("target_ids", parseTargetIds(promotion.getTargetIds()));
        data.put("rules", promotionMapper.listRules(promotionId));

        List<Map<String, Object>> products = promotionMapper.productsByPromotion(promotionId);
        data.put("products", products == null ? Collections.emptyList() : products);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_products", promotionMapper.activeProductCount(promotionId));
        stats.put("total_orders", 0);
        stats.put("total_discount_amount", BigDecimal.ZERO);
        data.put("statistics", stats);
        return data;
    }

    @Transactional
    public Map<String, Object> updatePromotion(Integer promotionId, Map<String, Object> req, Integer createdBy) {
        Promotion exists = promotionMapper.findById(promotionId);
        if (exists == null) {
            throw new IllegalArgumentException("促销活动不存在");
        }
        if (!"DRAFT".equalsIgnoreCase(exists.getStatus())) {
            throw new IllegalArgumentException("仅草稿状态可编辑");
        }

        Promotion updated = parsePromotion(req, createdBy == null ? exists.getCreatedBy() : createdBy);
        updated.setPromotionId(promotionId);
        promotionMapper.update(updated);
        promotionMapper.deleteRules(promotionId);
        saveRules(promotionId, req.get("rules"));

        Map<String, Object> data = toSimplePromotion(updated);
        data.put("affected_products_count", resolveAffectedProductIds(updated).size());
        return data;
    }

    @Transactional
    public Map<String, Object> activatePromotion(Integer promotionId, Integer operatorId) {
        Promotion promotion = mustPromotion(promotionId);
        if (!("DRAFT".equalsIgnoreCase(promotion.getStatus()) || "ENDED".equalsIgnoreCase(promotion.getStatus()))) {
            throw new IllegalArgumentException("当前状态不可激活");
        }

        List<Integer> productIds = resolveAffectedProductIds(promotion);
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;

        for (Integer productId : productIds) {
            Map<String, Object> product = promotionMapper.productById(productId);
            if (product == null) continue;

            BigDecimal current = toDecimal(product.get("price"));
            BigDecimal original = toDecimal(product.get("original_price"));
            if (original == null) original = current;
            if (current == null || original == null) continue;

            BigDecimal finalPrice = calculatePrice(promotion, original);
            if (finalPrice == null || finalPrice.compareTo(BigDecimal.ZERO) < 0) continue;
            BigDecimal discountAmount = original.subtract(finalPrice).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            promotionMapper.upsertProductPromotion(productId, promotionId, finalPrice, discountAmount, now);
            changed++;
        }

        promotionMapper.updateStatus(promotionId, "ACTIVE");

        promotionPriceService.invalidatePromotionsCache();

        Integer priority = promotion.getPriority() == null ? 0 : promotion.getPriority();
        for (Integer productId : productIds) {
            if (productId == null) continue;
            promotionMapper.enforceSingleActivePerPriority(productId, priority, now);
        }

        refreshProductsPriceByPromotion(promotionId, "PROMOTION_START", "促销活动激活", operatorId == null ? 0 : operatorId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("promotion_id", promotionId);
        data.put("status", "ACTIVE");
        data.put("activated_at", LocalDateTime.now());
        data.put("affected_products_count", changed);
        data.put("estimated_discount_total", BigDecimal.ZERO);
        return data;
    }

    @Transactional
    public Map<String, Object> endPromotion(Integer promotionId, Integer operatorId) {
        Promotion promotion = mustPromotion(promotionId);
        if (!"ACTIVE".equalsIgnoreCase(promotion.getStatus())) {
            throw new IllegalArgumentException("仅进行中的促销可结束");
        }

        promotionMapper.updateStatus(promotionId, "ENDED");
        promotionMapper.deactivateByPromotion(promotionId);
        promotionPriceService.invalidatePromotionsCache();
        refreshProductsPriceByPromotion(promotionId, "PROMOTION_END", "促销活动结束", operatorId == null ? 0 : operatorId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("promotion_id", promotionId);
        data.put("status", "ENDED");
        data.put("ended_at", LocalDateTime.now());
        data.put("actual_duration_days", Duration.between(promotion.getStartTime(), LocalDateTime.now()).toDays());
        data.put("total_orders", 0);
        data.put("total_discount_amount", BigDecimal.ZERO);
        return data;
    }

    @Transactional
    public Map<String, Object> cancelPromotion(Integer promotionId, Integer operatorId) {
        Promotion promotion = mustPromotion(promotionId);
        if (!"DRAFT".equalsIgnoreCase(promotion.getStatus()) && !"ACTIVE".equalsIgnoreCase(promotion.getStatus())) {
            throw new IllegalArgumentException("仅草稿或进行中的活动可取消");
        }

        promotionMapper.updateStatus(promotionId, "CANCELLED");
        promotionMapper.deactivateByPromotion(promotionId);
        promotionPriceService.invalidatePromotionsCache();
        refreshProductsPriceByPromotion(promotionId, "PROMOTION_END", "促销活动取消", operatorId == null ? 0 : operatorId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("promotion_id", promotionId);
        data.put("status", "CANCELLED");
        data.put("cancelled_at", LocalDateTime.now());
        return data;
    }

    public List<Map<String, Object>> listActivePromotions(String promotionType, Integer productId) {
        if (productId != null) {
            return promotionMapper.promotionsByProduct(productId, true);
        }

        List<Promotion> rows = promotionMapper.listActive(promotionType);
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Promotion row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("promotion_id", row.getPromotionId());
            item.put("promotion_name", row.getPromotionName());
            item.put("promotion_type", row.getPromotionType());
            item.put("discount_value", row.getDiscountValue());
            item.put("start_time", row.getStartTime());
            item.put("end_time", row.getEndTime());
            item.put("remaining_hours", Math.max(0, Duration.between(now, row.getEndTime()).toHours()));
            item.put("applicable_scope", row.getApplicableScope());
            list.add(item);
        }
        return list;
    }

    public Map<String, Object> productPromotions(Integer productId) {
        Map<String, Object> product = promotionMapper.productById(productId);
        if (product == null) return null;

        List<Map<String, Object>> rows = promotionMapper.promotionsByProduct(productId, true);
        BigDecimal currentPromotionPrice = toDecimal(product.get("current_promotion_price"));
        BigDecimal basePrice = toDecimal(product.get("price"));
        BigDecimal effectiveCurrentPrice = currentPromotionPrice != null ? currentPromotionPrice : basePrice;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("product_id", productId);
        data.put("product_name", product.get("product_name"));
        data.put("original_price", toDecimal(product.get("original_price")) == null ? toDecimal(product.get("price")) : toDecimal(product.get("original_price")));
        data.put("current_price", effectiveCurrentPrice);
        data.put("current_promotion_price", currentPromotionPrice);
        data.put("active_promotion_ids", parsePromotionIds(product.get("active_promotion_ids")));
        data.put("has_active_promotion", rows != null && !rows.isEmpty());
        data.put("promotions", rows == null ? Collections.emptyList() : rows);

        if (rows != null && !rows.isEmpty()) {
            Map<String, Object> best = rows.get(0);
            Map<String, Object> bestOut = new LinkedHashMap<>();
            bestOut.put("promotion_id", best.get("promotion_id"));
            bestOut.put("final_price", best.get("final_price"));
            bestOut.put("save_amount", best.get("discount_amount"));
            data.put("best_promotion", bestOut);
        }
        return data;
    }

    @Transactional
    public void refreshProductsPriceByPromotion(Integer promotionId, String changeType, String reason, Integer operatorId) {
        List<Integer> productIds = promotionMapper.productIdsByPromotion(promotionId);
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Integer productId : productIds) {
            Map<String, Object> product = promotionMapper.productById(productId);
            if (product == null) continue;
            BigDecimal oldPrice = toDecimal(product.get("price"));
            BigDecimal originalPrice = toDecimal(product.get("original_price"));
            if (originalPrice == null) originalPrice = oldPrice;

            List<Map<String, Object>> candidates = promotionMapper.activePromotionCandidatesForProduct(productId);
            if (candidates != null && !candidates.isEmpty()) {
                BigDecimal combinedPrice = originalPrice;
                List<Integer> appliedPromotionIds = new ArrayList<>();

                Map<Integer, List<Map<String, Object>>> byPriority = new TreeMap<>(Comparator.reverseOrder());
                for (Map<String, Object> row : candidates) {
                    Integer priority = intValue(row.get("priority"), 0);
                    byPriority.computeIfAbsent(priority, k -> new ArrayList<>()).add(row);
                }

                for (Map.Entry<Integer, List<Map<String, Object>>> entry : byPriority.entrySet()) {
                    List<Map<String, Object>> samePriorityRows = entry.getValue();
                    Map<String, Object> winner = null;
                    BigDecimal winnerPrice = null;

                    for (Map<String, Object> row : samePriorityRows) {
                        Integer candidatePromotionId = intValue(row.get("promotion_id"), -1);
                        if (candidatePromotionId == null || candidatePromotionId <= 0) {
                            continue;
                        }

                        BigDecimal candidatePrice = calculatePriceByRule(row, combinedPrice);
                        if (candidatePrice == null) {
                            candidatePrice = combinedPrice;
                        }
                        BigDecimal candidateDiscount = combinedPrice.subtract(candidatePrice).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                        promotionMapper.updatePromotionComputedPrice(productId, candidatePromotionId, candidatePrice, candidateDiscount, now);

                        if (winner == null || candidatePrice.compareTo(winnerPrice) < 0 ||
                                (candidatePrice.compareTo(winnerPrice) == 0 && candidatePromotionId < intValue(winner.get("promotion_id"), Integer.MAX_VALUE))) {
                            winner = row;
                            winnerPrice = candidatePrice;
                        }
                    }

                    Integer winnerPromotionId = winner == null ? null : intValue(winner.get("promotion_id"), -1);
                    if (winnerPrice != null && winnerPrice.compareTo(combinedPrice) < 0 && winnerPromotionId != null && winnerPromotionId > 0) {
                        combinedPrice = winnerPrice;
                        appliedPromotionIds.add(winnerPromotionId);
                    }
                }

                boolean hasApplied = !appliedPromotionIds.isEmpty();
                BigDecimal effectivePrice = hasApplied ? combinedPrice : originalPrice;
                String activePromotionIdsJson = toJsonArray(appliedPromotionIds);

                promotionMapper.updateProductEffectivePrice(
                        productId,
                        effectivePrice,
                        originalPrice,
                        hasApplied ? effectivePrice : null,
                        hasApplied,
                        activePromotionIdsJson,
                        now
                );

                if (oldPrice != null && effectivePrice != null && oldPrice.compareTo(effectivePrice) != 0) {
                    priceHistoryService.recordPriceChange(productId, oldPrice, effectivePrice, changeType, reason, operatorId);
                    priceAlertService.handlePriceChange(productId, oldPrice, effectivePrice);
                }
            } else {
                if (originalPrice == null) continue;
                promotionMapper.updateProductEffectivePrice(productId, originalPrice, originalPrice, null, false, "[]", now);
                if (oldPrice != null && oldPrice.compareTo(originalPrice) != 0) {
                    priceHistoryService.recordPriceChange(productId, oldPrice, originalPrice, changeType, reason, operatorId);
                    priceAlertService.handlePriceChange(productId, oldPrice, originalPrice);
                }
            }
        }
    }

    private Promotion mustPromotion(Integer promotionId) {
        Promotion promotion = promotionMapper.findById(promotionId);
        if (promotion == null) throw new IllegalArgumentException("促销活动不存在");
        return promotion;
    }

    private Promotion parsePromotion(Map<String, Object> req, Integer createdBy) {
        String name = stringValue(req.get("promotion_name"));
        String type = stringValue(req.get("promotion_type"));
        String scope = stringValue(req.get("applicable_scope"));
        LocalDateTime start = parseDateTime(req.get("start_time"));
        LocalDateTime end = parseDateTime(req.get("end_time"));
        if (name == null || type == null || scope == null || start == null || end == null) {
            throw new IllegalArgumentException("促销活动参数不完整");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start_time 必须早于 end_time");
        }

        BigDecimal discountValue = toDecimal(req.get("discount_value"));
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("discount_value 必须大于0");
        }

        Promotion promotion = Promotion.builder()
                .promotionName(name)
                .promotionType(type)
                .description(stringValue(req.get("description")))
                .startTime(start)
                .endTime(end)
                .discountValue(discountValue)
                .minPurchaseAmount(toDecimal(req.get("min_purchase_amount")))
                .maxDiscountAmount(toDecimal(req.get("max_discount_amount")))
                .applicableScope(scope)
                .targetIds(toJsonArray(req.get("target_ids")))
                .priority(intValue(req.get("priority"), 0))
                .createdBy(createdBy == null ? 0 : createdBy)
                .build();

        if ("DISCOUNT".equalsIgnoreCase(type) && (discountValue.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("DISCOUNT 类型 discount_value 需在 0~1 之间");
        }
        return promotion;
    }

    private void saveRules(Integer promotionId, Object rulesRaw) {
        if (rulesRaw == null) {
            return;
        }
        List<Map<String, Object>> rules = objectMapper.convertValue(rulesRaw, new TypeReference<List<Map<String, Object>>>() {});
        for (Map<String, Object> rule : rules) {
            String type = stringValue(rule.get("rule_type"));
            if (type == null) continue;
            PromotionRule row = PromotionRule.builder()
                    .promotionId(promotionId)
                    .ruleType(type)
                    .ruleConfig(toJsonObject(rule.get("rule_config")))
                    .build();
            promotionMapper.insertRule(row);
        }
    }

    private List<Integer> resolveAffectedProductIds(Promotion promotion) {
        if (promotion == null) return Collections.emptyList();
        String scope = promotion.getApplicableScope() == null ? "" : promotion.getApplicableScope().toUpperCase();
        if ("ALL".equals(scope)) {
            return promotionMapper.allProductIds();
        }

        List<Integer> targetIds = parseTargetIds(promotion.getTargetIds());
        if (targetIds.isEmpty()) {
            return Collections.emptyList();
        }
        if ("PRODUCT".equals(scope)) {
            return targetIds;
        }
        if ("CATEGORY".equals(scope)) {
            return promotionMapper.productIdsByCategory(targetIds);
        }
        return Collections.emptyList();
    }

    private BigDecimal calculatePrice(Promotion promotion, BigDecimal basePrice) {
        if (promotion == null || basePrice == null) return null;

        String type = promotion.getPromotionType() == null ? "" : promotion.getPromotionType().toUpperCase();
        BigDecimal result;
        if ("DISCOUNT".equals(type)) {
            result = basePrice.multiply(promotion.getDiscountValue());
        } else if ("FULL_REDUCTION".equals(type)) {
            BigDecimal min = promotion.getMinPurchaseAmount() == null ? BigDecimal.ZERO : promotion.getMinPurchaseAmount();
            if (basePrice.compareTo(min) < 0) {
                return basePrice;
            }
            BigDecimal reduction = promotion.getDiscountValue();
            if (promotion.getMaxDiscountAmount() != null && reduction.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                reduction = promotion.getMaxDiscountAmount();
            }
            result = basePrice.subtract(reduction);
        } else {
            return basePrice;
        }

        if (result.compareTo(BigDecimal.ZERO) < 0) result = BigDecimal.ZERO;
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePriceByRule(Map<String, Object> row, BigDecimal basePrice) {
        if (row == null || basePrice == null) return basePrice;
        String type = stringValue(row.get("promotion_type"));
        BigDecimal discountValue = toDecimal(row.get("discount_value"));
        if (type == null || discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return basePrice;
        }

        BigDecimal result = basePrice;
        if ("DISCOUNT".equalsIgnoreCase(type)) {
            result = basePrice.multiply(discountValue);
        } else if ("FULL_REDUCTION".equalsIgnoreCase(type)) {
            BigDecimal min = toDecimal(row.get("min_purchase_amount"));
            if (min == null) min = BigDecimal.ZERO;
            if (basePrice.compareTo(min) < 0) {
                return basePrice;
            }
            BigDecimal reduction = discountValue;
            BigDecimal maxDiscount = toDecimal(row.get("max_discount_amount"));
            if (maxDiscount != null && reduction.compareTo(maxDiscount) > 0) {
                reduction = maxDiscount;
            }
            result = basePrice.subtract(reduction);
        }

        if (result.compareTo(BigDecimal.ZERO) < 0) {
            result = BigDecimal.ZERO;
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toSimplePromotion(Promotion promotion) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("promotion_id", promotion.getPromotionId());
        data.put("promotion_name", promotion.getPromotionName());
        data.put("promotion_type", promotion.getPromotionType());
        data.put("description", promotion.getDescription());
        data.put("start_time", promotion.getStartTime());
        data.put("end_time", promotion.getEndTime());
        data.put("status", promotion.getStatus());
        data.put("discount_value", promotion.getDiscountValue());
        data.put("min_purchase_amount", promotion.getMinPurchaseAmount());
        data.put("max_discount_amount", promotion.getMaxDiscountAmount());
        data.put("applicable_scope", promotion.getApplicableScope());
        data.put("target_ids", parseTargetIds(promotion.getTargetIds()));
        data.put("priority", promotion.getPriority());
        data.put("created_by", promotion.getCreatedBy());
        data.put("created_at", promotion.getCreatedAt());
        data.put("updated_at", promotion.getUpdatedAt());
        return data;
    }

    private String stringValue(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private Integer intValue(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue()).setScale(2, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String toJsonArray(Object value) {
        try {
            if (value == null) return "[]";
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Integer> parsePromotionIds(Object value) {
        if (value == null) return Collections.emptyList();
        try {
            String json = String.valueOf(value);
            if (json.isBlank()) return Collections.emptyList();
            List<Object> raw = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
            List<Integer> ids = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Number) ids.add(((Number) item).intValue());
                else ids.add(Integer.parseInt(String.valueOf(item)));
            }
            return ids;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJsonObject(Object value) {
        try {
            if (value == null) return "{}";
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    public List<Map<String, Object>> getRuleDefinitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();

        Map<String, Object> discountRule = new LinkedHashMap<>();
        discountRule.put("rule_type", "DISCOUNT");
        discountRule.put("name", "折扣优惠");
        discountRule.put("fields", List.of(
            Map.of("key", "discount_value", "label", "折扣比例", "type", "number", "required", true)
        ));
        definitions.add(discountRule);

        Map<String, Object> fullReductionRule = new LinkedHashMap<>();
        fullReductionRule.put("rule_type", "FULL_REDUCTION");
        fullReductionRule.put("name", "满减优惠");
        fullReductionRule.put("fields", List.of(
            Map.of("key", "min_purchase_amount", "label", "满减门槛", "type", "number", "required", true),
            Map.of("key", "discount_value", "label", "减免金额", "type", "number", "required", true),
            Map.of("key", "max_discount_amount", "label", "最高减免", "type", "number", "required", false)
        ));
        definitions.add(fullReductionRule);

        Map<String, Object> customRule = new LinkedHashMap<>();
        customRule.put("rule_type", "CUSTOM");
        customRule.put("name", "自定义规则");
        customRule.put("fields", List.of(
            Map.of("key", "rule_expression", "label", "规则表达式", "type", "string", "required", true),
            Map.of("key", "description", "label", "规则说明", "type", "string", "required", false)
        ));
        definitions.add(customRule);

        return definitions;
    }

    private List<Integer> parseTargetIds(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Object> raw = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
            List<Integer> ids = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Number) {
                    ids.add(((Number) item).intValue());
                } else {
                    ids.add(Integer.parseInt(String.valueOf(item)));
                }
            }
            return ids;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scheduleAutoActivateAndEndPromotions() {
        LocalDateTime now = LocalDateTime.now();
        
        List<Promotion> toActivate = promotionMapper.findDraftPromotionsToActivate(now);
        for (Promotion promotion : toActivate) {
            try {
                log.info("定时任务：自动激活促销活动 [{}] {}", promotion.getPromotionId(), promotion.getPromotionName());
                activatePromotion(promotion.getPromotionId(), 0);
            } catch (Exception e) {
                log.error("自动激活促销活动 [{}] 失败: {}", promotion.getPromotionId(), e.getMessage());
            }
        }
        
        List<Promotion> toEnd = promotionMapper.findActivePromotionsToEnd(now);
        for (Promotion promotion : toEnd) {
            try {
                log.info("定时任务：自动结束促销活动 [{}] {}", promotion.getPromotionId(), promotion.getPromotionName());
                endPromotion(promotion.getPromotionId(), 0);
            } catch (Exception e) {
                log.error("自动结束促销活动 [{}] 失败: {}", promotion.getPromotionId(), e.getMessage());
            }
        }
    }
}

