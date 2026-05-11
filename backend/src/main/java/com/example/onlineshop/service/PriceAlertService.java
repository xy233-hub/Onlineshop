package com.example.onlineshop.service;

import com.example.onlineshop.entity.PriceAlertSetting;
import com.example.onlineshop.mapper.PriceAlertMapper;
import com.example.onlineshop.mapper.PricingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PriceAlertService {

    @Autowired
    private PriceAlertMapper priceAlertMapper;

    @Autowired
    private PricingMapper pricingMapper;

    public boolean canConfigure(Integer customerId, Integer productId, String alertType) {
        if ("FAVORITE".equalsIgnoreCase(alertType)) {
            return priceAlertMapper.countFavorite(customerId, productId) > 0;
        }
        if ("CART".equalsIgnoreCase(alertType)) {
            return priceAlertMapper.countCart(customerId, productId) > 0;
        }
        return false;
    }

    @Transactional
    public Map<String, Object> createAlert(Integer customerId,
                                           Integer productId,
                                           String alertType,
                                           BigDecimal thresholdPercentage,
                                           Boolean isEnabled) {
        Map<String, Object> product = pricingMapper.selectProductPricingInfo(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!"FAVORITE".equalsIgnoreCase(alertType) && !"CART".equalsIgnoreCase(alertType)) {
            throw new IllegalArgumentException("alert_type 仅支持 FAVORITE 或 CART");
        }
        if (!canConfigure(customerId, productId, alertType)) {
            throw new IllegalArgumentException("仅允许为收藏或购物车中的商品配置对应提醒");
        }

        BigDecimal threshold = thresholdPercentage == null ? BigDecimal.valueOf(5) : thresholdPercentage;
        if (threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("threshold_percentage 不能小于0");
        }

        PriceAlertSetting existing = priceAlertMapper.findByUnique(customerId, productId, alertType.toUpperCase());
        BigDecimal currentPrice = toDecimal(product.get("price"));
        if (existing == null) {
            PriceAlertSetting setting = PriceAlertSetting.builder()
                    .customerId(customerId)
                    .productId(productId)
                    .alertType(alertType.toUpperCase())
                    .isEnabled(isEnabled == null || isEnabled)
                    .thresholdPercentage(threshold)
                    .lastAlertedPrice(currentPrice)
                    .build();
            priceAlertMapper.insert(setting);
            return buildAlertResult(setting, product);
        }

        existing.setIsEnabled(isEnabled == null ? existing.getIsEnabled() : isEnabled);
        existing.setThresholdPercentage(threshold);
        priceAlertMapper.updateBasic(existing);
        return buildAlertResult(existing, product);
    }

    public Map<String, Object> listAlerts(Integer customerId, Boolean isEnabled, String alertType, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        List<Map<String, Object>> rows = priceAlertMapper.listAlerts(customerId, isEnabled, alertType, safeSize, offset);
        int total = priceAlertMapper.countAlerts(customerId, isEnabled, alertType);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("alert_id", row.get("alert_id"));
            item.put("product_id", row.get("product_id"));
            item.put("alert_type", row.get("alert_type"));
            item.put("threshold_percentage", row.get("threshold_percentage"));
            item.put("is_enabled", row.get("is_enabled"));
            item.put("last_alerted_price", row.get("last_alerted_price"));
            item.put("last_alerted_at", row.get("last_alerted_at"));
            item.put("created_at", row.get("created_at"));

            Map<String, Object> productInfo = new LinkedHashMap<>();
            productInfo.put("product_name", row.get("product_name"));
            productInfo.put("image_url", row.get("image_url"));
            productInfo.put("current_price", row.get("current_price"));
            item.put("product_info", productInfo);
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", total);
        data.put("items", items);
        return data;
    }

    @Transactional
    public Map<String, Object> updateAlert(Integer customerId, Integer alertId, Boolean isEnabled, BigDecimal thresholdPercentage) {
        PriceAlertSetting setting = priceAlertMapper.findById(alertId);
        if (setting == null) {
            throw new IllegalArgumentException("提醒配置不存在");
        }
        if (!Objects.equals(setting.getCustomerId(), customerId)) {
            throw new IllegalArgumentException("无权修改该提醒配置");
        }

        if (isEnabled != null) {
            setting.setIsEnabled(isEnabled);
        }
        if (thresholdPercentage != null) {
            if (thresholdPercentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("threshold_percentage 不能小于0");
            }
            setting.setThresholdPercentage(thresholdPercentage);
        }
        priceAlertMapper.updateBasic(setting);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alert_id", alertId);
        data.put("is_enabled", setting.getIsEnabled());
        data.put("threshold_percentage", setting.getThresholdPercentage());
        data.put("updated_at", LocalDateTime.now());
        return data;
    }

    @Transactional
    public Map<String, Object> deleteAlert(Integer customerId, Integer alertId) {
        PriceAlertSetting setting = priceAlertMapper.findById(alertId);
        if (setting == null) {
            throw new IllegalArgumentException("提醒配置不存在");
        }
        if (!Objects.equals(setting.getCustomerId(), customerId)) {
            throw new IllegalArgumentException("无权删除该提醒配置");
        }

        priceAlertMapper.deleteById(alertId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alert_id", alertId);
        data.put("deleted_at", LocalDateTime.now());
        return data;
    }

    public Map<String, Object> listNotifications(Integer customerId, Boolean isRead, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        List<Map<String, Object>> rows = priceAlertMapper.listNotifications(customerId, isRead, safeSize, offset);
        int total = priceAlertMapper.countNotifications(customerId, isRead);
        int unreadCount = priceAlertMapper.unreadCount(customerId);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("notification_id", row.get("notification_id"));
            item.put("product_id", row.get("product_id"));
            item.put("old_price", row.get("old_price"));
            item.put("new_price", row.get("new_price"));
            item.put("change_percentage", row.get("change_percentage"));
            item.put("change_direction", row.get("change_direction"));
            item.put("is_read", row.get("is_read"));
            item.put("created_at", row.get("created_at"));

            Map<String, Object> productInfo = new LinkedHashMap<>();
            productInfo.put("product_name", row.get("product_name"));
            productInfo.put("image_url", row.get("image_url"));
            item.put("product_info", productInfo);
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", total);
        data.put("unread_count", unreadCount);
        data.put("items", items);
        return data;
    }

    @Transactional
    public Map<String, Object> markRead(Integer customerId, Integer notificationId) {
        int updated = priceAlertMapper.markRead(notificationId, customerId);
        if (updated <= 0) {
            throw new IllegalArgumentException("通知不存在或无权操作");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("notification_id", notificationId);
        data.put("is_read", true);
        data.put("read_at", LocalDateTime.now());
        return data;
    }

    @Transactional
    public Map<String, Object> markAllRead(Integer customerId) {
        int count = priceAlertMapper.markAllRead(customerId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("marked_count", count);
        data.put("marked_at", LocalDateTime.now());
        return data;
    }

    @Transactional
    public int handlePriceChange(Integer productId, BigDecimal oldPrice, BigDecimal newPrice) {
        if (productId == null || oldPrice == null || newPrice == null || oldPrice.compareTo(newPrice) == 0) {
            return 0;
        }

        List<Map<String, Object>> alerts = pricingMapper.enabledAlertsByProduct(productId);
        if (alerts == null || alerts.isEmpty()) {
            return 0;
        }

        int notified = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> alert : alerts) {
            Integer alertId = ((Number) alert.get("alert_id")).intValue();
            Integer customerId = ((Number) alert.get("customer_id")).intValue();

            BigDecimal basePrice = toDecimal(alert.get("last_alerted_price"));
            if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                basePrice = oldPrice;
            }
            if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal pct = newPrice.subtract(basePrice)
                    .divide(basePrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal threshold = toDecimal(alert.get("threshold_percentage"));
            if (threshold == null) threshold = BigDecimal.valueOf(5);

            if (pct.abs().compareTo(threshold) < 0) {
                continue;
            }

            String direction = pct.compareTo(BigDecimal.ZERO) <= 0 ? "DECREASE" : "INCREASE";
            pricingMapper.insertNotification(alertId, customerId, productId, oldPrice, newPrice, pct, direction, now);
            pricingMapper.updateAlertCheckpoint(alertId, newPrice, now);
            notified++;
        }
        return notified;
    }

    @Transactional
    public int checkAndNotifyPriceChange(Integer productId, BigDecimal oldPrice, BigDecimal newPrice) {
        return handlePriceChange(productId, oldPrice, newPrice);
    }

    public int getUnreadCount(Integer customerId) {
        return priceAlertMapper.unreadCount(customerId);
    }

    @Transactional
    public void batchMarkNotificationsAsRead(Integer customerId, List<Integer> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new IllegalArgumentException("通知ID列表不能为空");
        }
        priceAlertMapper.batchMarkAsRead(customerId, notificationIds);
    }

    private Map<String, Object> buildAlertResult(PriceAlertSetting setting, Map<String, Object> product) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alert_id", setting.getAlertId());
        data.put("product_id", setting.getProductId());
        data.put("product_name", product.get("product_name"));
        data.put("alert_type", setting.getAlertType());
        data.put("threshold_percentage", setting.getThresholdPercentage());
        data.put("is_enabled", setting.getIsEnabled());
        data.put("current_price", product.get("price"));
        data.put("created_at", LocalDateTime.now());
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
}

