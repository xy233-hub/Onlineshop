// service/PurchaseIntentService.java
package com.example.onlineshop.service;

import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.mapper.PurchaseIntentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    public PurchaseIntent getPurchaseIntentById(Integer purchaseId) {
        return purchaseIntentMapper.findById(purchaseId);
    }

    public boolean updatePurchaseStatus(Integer purchaseId, String newStatus) {
        PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);
        if (intent == null) return false;
        if ("success".equals(newStatus)) {
            // 先将同商品下其他pending意向设为failed
            purchaseIntentMapper.markOtherIntentsFailed(intent.getProduct_id(), purchaseId);
        }
        intent.setPurchase_status(newStatus);
        intent.setUpdated_at(LocalDateTime.now());
        return purchaseIntentMapper.updateStatus(purchaseId, newStatus) > 0;
    }

    public List<PurchaseIntent> getAllPurchaseIntents() {
        return purchaseIntentMapper.findAll();
    }

    public void markOtherIntentsFailed(Integer productId, Integer excludePurchaseId) {
        purchaseIntentMapper.markOtherIntentsFailed(productId, excludePurchaseId);
    }

}
