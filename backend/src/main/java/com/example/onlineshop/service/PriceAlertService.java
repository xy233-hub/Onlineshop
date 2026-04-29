package com.example.onlineshop.service;

import com.example.onlineshop.dto.request.PriceAlertRequest;
import com.example.onlineshop.dto.response.PriceAlertNotificationResponse;
import com.example.onlineshop.dto.response.PriceAlertResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PriceAlertService {
    
    PriceAlertResponse createPriceAlert(Integer customerId, PriceAlertRequest request);
    
    Map<String, Object> getPriceAlerts(Integer customerId, Boolean isEnabled, String alertType, Integer page, Integer size);
    
    PriceAlertResponse updatePriceAlert(Integer customerId, Integer alertId, PriceAlertRequest request);
    
    void deletePriceAlert(Integer customerId, Integer alertId);
    
    Map<String, Object> getNotifications(Integer customerId, Boolean isRead, Integer page, Integer size);
    
    void markNotificationAsRead(Integer customerId, Integer notificationId);
    
    void batchMarkNotificationsAsRead(Integer customerId, List<Integer> notificationIds);

    int markAllNotificationsAsRead(Integer customerId);
       
    int getUnreadCount(Integer customerId);
    
    int checkAndNotifyPriceChange(Integer productId, BigDecimal oldPrice, BigDecimal newPrice);
}