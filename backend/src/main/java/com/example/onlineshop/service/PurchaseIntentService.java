// service/PurchaseIntentService.java
package com.example.onlineshop.service;

import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.mapper.PurchaseIntentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseIntentService {

    @Autowired
    private PurchaseIntentMapper purchaseIntentMapper;

    public boolean createPurchaseIntent(PurchaseIntent purchaseIntent) {
        return purchaseIntentMapper.insert(purchaseIntent) > 0;
    }

    public List<PurchaseIntent> getPurchaseIntentsByProductId(Integer productId) {
        return purchaseIntentMapper.findByProductId(productId);
    }

    public List<PurchaseIntent> getPurchaseIntentsByCustomerId(Integer customerId) {
        return purchaseIntentMapper.findByCustomerId(customerId);
    }

    public PurchaseIntent getPurchaseIntentById(Integer purchaseId) {
        return purchaseIntentMapper.findById(purchaseId);
    }

    public boolean updatePurchaseStatus(Integer purchaseId, String newStatus) {
        PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);
        if (intent == null) {
            return false;
        }
        if ("success".equals(newStatus)) {
            // 先将同商品下其他pending意向设为failed
            purchaseIntentMapper.markOtherIntentsFailed(intent.getProductId(), purchaseId);
        }
        intent.setPurchaseStatus(newStatus);
        intent.setUpdatedAt(LocalDateTime.now());
        return purchaseIntentMapper.updateStatus(purchaseId, newStatus) > 0;
    }

    public List<PurchaseIntent> getAllPurchaseIntents() {
        return purchaseIntentMapper.findAll();
    }

    public void markOtherIntentsFailed(Integer productId, Integer excludePurchaseId) {
        purchaseIntentMapper.markOtherIntentsFailed(productId, excludePurchaseId);
    }

    public List<PurchaseIntent> getPurchaseIntentsByCondition(Map<String, Object> params) {
        return purchaseIntentMapper.findByCondition(params);
    }

    public int countPurchaseIntentsByCondition(Map<String, Object> params) {
        return purchaseIntentMapper.countByCondition(params);
    }
}
