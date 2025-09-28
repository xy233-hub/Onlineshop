// service/PurchaseIntentService.java
package com.example.onlineshop.service;

import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.mapper.PurchaseIntentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public boolean updatePurchaseStatus(Integer purchaseId, String status) {
        return purchaseIntentMapper.updateStatus(purchaseId, status) > 0;
    }

    public List<PurchaseIntent> getAllPurchaseIntents() {
        return purchaseIntentMapper.findAll();
    }
}
