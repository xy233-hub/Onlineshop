package com.example.onlineshop.service;

import com.example.onlineshop.entity.AfterSalesService;
import java.util.List;

public interface AfterSalesServiceService {
    AfterSalesService createAfterSalesService(AfterSalesService afterSalesService);
    
    AfterSalesService updateAfterSalesService(AfterSalesService afterSalesService);
    
    AfterSalesService getAfterSalesServiceById(Integer serviceId);
    
    List<AfterSalesService> getAfterSalesServicesByCustomerId(Integer customerId, String serviceStatus, Integer page, Integer size);
    
    int countAfterSalesServicesByCustomerId(Integer customerId, String serviceStatus);
    
    List<AfterSalesService> getAfterSalesServicesBySellerId(Integer sellerId, String serviceStatus, String serviceType, Integer page, Integer size);
    
    int countAfterSalesServicesBySellerId(Integer sellerId, String serviceStatus, String serviceType);
    AfterSalesService cancelAfterSalesService(Integer serviceId, Integer customerId, String cancelReason);
    
    AfterSalesService returnShipAfterSalesService(Integer serviceId, Integer customerId, String trackingNo, String logisticsProvider);
    
    AfterSalesService handleAfterSalesService(Integer serviceId, String sellerDecision, String sellerResponse);
    
    AfterSalesService confirmReturnAfterSalesService(Integer serviceId, String remark);
    
    AfterSalesService getByPurchaseIdAndCustomerId(Integer purchaseId, Integer customerId);
}