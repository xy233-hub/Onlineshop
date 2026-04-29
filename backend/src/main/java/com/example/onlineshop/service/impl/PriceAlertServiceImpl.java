package com.example.onlineshop.service.impl;

import com.example.onlineshop.dto.request.PriceAlertRequest;
import com.example.onlineshop.dto.response.PriceAlertNotificationResponse;
import com.example.onlineshop.dto.response.PriceAlertResponse;
import com.example.onlineshop.entity.PriceAlertNotification;
import com.example.onlineshop.entity.PriceAlertSetting;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.mapper.PriceAlertNotificationMapper;
import com.example.onlineshop.mapper.PriceAlertSettingMapper;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.service.PriceAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PriceAlertServiceImpl implements PriceAlertService {

    @Autowired
    private PriceAlertSettingMapper priceAlertSettingMapper;

    @Autowired
    private PriceAlertNotificationMapper priceAlertNotificationMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public PriceAlertResponse createPriceAlert(Integer customerId, PriceAlertRequest request) {
        if (request.getProductId() == null || request.getAlertType() == null) {
            throw new IllegalArgumentException("商品ID和提醒类型必填");
        }

        if (!"FAVORITE".equals(request.getAlertType()) && !"CART".equals(request.getAlertType())) {
            throw new IllegalArgumentException("提醒类型必须是FAVORITE或CART");
        }

        PriceAlertSetting existing = priceAlertSettingMapper.findByCustomerProductAndType(
                customerId, request.getProductId(), request.getAlertType());

        if (existing != null) {
            throw new IllegalArgumentException("该商品的价格提醒已存在");
        }

        Product product = productMapper.findById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        PriceAlertSetting setting = PriceAlertSetting.builder()
                .customerId(customerId)
                .productId(request.getProductId())
                .alertType(request.getAlertType())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .thresholdPercentage(request.getThresholdPercentage() != null ? 
                        request.getThresholdPercentage() : BigDecimal.valueOf(5.0))
                .build();

        priceAlertSettingMapper.insert(setting);

        PriceAlertResponse response = new PriceAlertResponse();
        response.setAlertId(setting.getAlertId());
        response.setProductId(setting.getProductId());
        response.setProductName(product.getProductName());
        response.setAlertType(setting.getAlertType());
        response.setThresholdPercentage(setting.getThresholdPercentage());
        response.setIsEnabled(setting.getIsEnabled());
        response.setCurrentPrice(product.getPrice());
        response.setCreatedAt(setting.getCreatedAt());

        return response;
    }

    @Override
    public Map<String, Object> getPriceAlerts(Integer customerId, Boolean isEnabled, String alertType, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        int offset = (page - 1) * size;

        List<PriceAlertSetting> settings = priceAlertSettingMapper.findByCustomerId(
                customerId, isEnabled, alertType, offset, size);
        int total = priceAlertSettingMapper.countByCustomerId(customerId, isEnabled, alertType);

        List<PriceAlertResponse> items = settings.stream().map(setting -> {
            Product product = productMapper.findById(setting.getProductId());
            PriceAlertResponse response = new PriceAlertResponse();
            response.setAlertId(setting.getAlertId());
            response.setProductId(setting.getProductId());
            response.setProductName(product != null ? product.getProductName() : "未知商品");
            response.setAlertType(setting.getAlertType());
            response.setThresholdPercentage(setting.getThresholdPercentage());
            response.setIsEnabled(setting.getIsEnabled());
            response.setCurrentPrice(product != null ? product.getPrice() : BigDecimal.ZERO);
            response.setCreatedAt(setting.getCreatedAt());
            return response;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("items", items);

        return result;
    }

    @Override
    @Transactional
    public PriceAlertResponse updatePriceAlert(Integer customerId, Integer alertId, PriceAlertRequest request) {
        PriceAlertSetting setting = priceAlertSettingMapper.findById(alertId);
        if (setting == null) {
            throw new IllegalArgumentException("价格提醒配置不存在");
        }

        if (!setting.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("无权修改此价格提醒");
        }

        if (request.getIsEnabled() != null) {
            setting.setIsEnabled(request.getIsEnabled());
        }
        if (request.getThresholdPercentage() != null) {
            setting.setThresholdPercentage(request.getThresholdPercentage());
        }

        priceAlertSettingMapper.update(setting);

        Product product = productMapper.findById(setting.getProductId());
        PriceAlertResponse response = new PriceAlertResponse();
        response.setAlertId(setting.getAlertId());
        response.setProductId(setting.getProductId());
        response.setProductName(product != null ? product.getProductName() : "未知商品");
        response.setAlertType(setting.getAlertType());
        response.setThresholdPercentage(setting.getThresholdPercentage());
        response.setIsEnabled(setting.getIsEnabled());
        response.setCurrentPrice(product != null ? product.getPrice() : BigDecimal.ZERO);
        response.setCreatedAt(setting.getCreatedAt());

        return response;
    }

    @Override
    @Transactional
    public void deletePriceAlert(Integer customerId, Integer alertId) {
        int deleted = priceAlertSettingMapper.delete(alertId, customerId);
        if (deleted == 0) {
            throw new IllegalArgumentException("价格提醒配置不存在或无权删除");
        }
    }

    @Override
    public Map<String, Object> getNotifications(Integer customerId, Boolean isRead, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        int offset = (page - 1) * size;

        List<PriceAlertNotification> notifications = priceAlertNotificationMapper.findByCustomerId(
                customerId, isRead, offset, size);
        int total = priceAlertNotificationMapper.countByCustomerId(customerId, isRead);

        List<PriceAlertNotificationResponse> items = notifications.stream().map(notification -> {
            Product product = productMapper.findById(notification.getProductId());
            PriceAlertNotificationResponse response = new PriceAlertNotificationResponse();
            response.setNotificationId(notification.getNotificationId());
            response.setProductId(notification.getProductId());
            response.setProductName(product != null ? product.getProductName() : "未知商品");
            response.setOldPrice(notification.getOldPrice());
            response.setNewPrice(notification.getNewPrice());
            response.setChangePercentage(notification.getChangePercentage());
            response.setChangeDirection(notification.getChangeDirection());
            response.setIsRead(notification.getIsRead());
            response.setCreatedAt(notification.getCreatedAt());
            response.setReadAt(notification.getReadAt());
            return response;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("items", items);

        return result;
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Integer customerId, Integer notificationId) {
        int updated = priceAlertNotificationMapper.markAsRead(notificationId, customerId);
        if (updated == 0) {
            throw new IllegalArgumentException("通知不存在或无权操作");
        }
    }

    @Override
    @Transactional
    public void batchMarkNotificationsAsRead(Integer customerId, List<Integer> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            throw new IllegalArgumentException("通知ID列表不能为空");
        }
        priceAlertNotificationMapper.batchMarkAsRead(customerId, notificationIds);
    }

    @Override
    @Transactional
    public int markAllNotificationsAsRead(Integer customerId) {
        return priceAlertNotificationMapper.markAllAsRead(customerId);
    }

    @Override
    public int getUnreadCount(Integer customerId) {
        return priceAlertNotificationMapper.countUnreadByCustomerId(customerId);
    }

    @Override
    @Transactional
    public int checkAndNotifyPriceChange(Integer productId, BigDecimal oldPrice, BigDecimal newPrice) {
        List<PriceAlertSetting> settings = priceAlertSettingMapper.findByProductId(productId);
        
        if (settings.isEmpty()) {
            return 0;
        }

        BigDecimal changePercent = BigDecimal.ZERO;
        String direction;

        if (oldPrice.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = newPrice.subtract(oldPrice)
                    .divide(oldPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        if (changePercent.compareTo(BigDecimal.ZERO) < 0) {
            direction = "DECREASE";
        } else if (changePercent.compareTo(BigDecimal.ZERO) > 0) {
            direction = "INCREASE";
        } else {
            return 0;
        }

        int triggeredCount = 0;
        for (PriceAlertSetting setting : settings) {
            if (!setting.getIsEnabled()) {
                continue;
            }

            BigDecimal threshold = setting.getThresholdPercentage() != null ? 
                    setting.getThresholdPercentage() : BigDecimal.valueOf(5.0);

            if (changePercent.abs().compareTo(threshold) >= 0) {
                PriceAlertNotification notification = PriceAlertNotification.builder()
                        .alertId(setting.getAlertId())
                        .customerId(setting.getCustomerId())
                        .productId(productId)
                        .oldPrice(oldPrice)
                        .newPrice(newPrice)
                        .changePercentage(changePercent.abs())
                        .changeDirection(direction)
                        .isRead(false)
                        .build();

                priceAlertNotificationMapper.insert(notification);

                setting.setLastAlertedPrice(newPrice);
                setting.setLastAlertedAt(LocalDateTime.now());
                priceAlertSettingMapper.update(setting);
                
                triggeredCount++;
            }
        }
        
        return triggeredCount;
    }
}