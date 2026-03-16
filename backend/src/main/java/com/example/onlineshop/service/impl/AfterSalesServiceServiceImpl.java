package com.example.onlineshop.service.impl;

import com.example.onlineshop.entity.AfterSalesService;
import com.example.onlineshop.mapper.AfterSalesServiceMapper;
import com.example.onlineshop.service.AfterSalesServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AfterSalesServiceServiceImpl implements AfterSalesServiceService {

    @Autowired
    private AfterSalesServiceMapper afterSalesServiceMapper;

    @Override
    @Transactional
    public AfterSalesService createAfterSalesService(AfterSalesService afterSalesService) {
        afterSalesService.setServiceStatus("PENDING");
        afterSalesService.setCreatedAt(LocalDateTime.now());
        afterSalesService.setUpdatedAt(LocalDateTime.now());
        afterSalesServiceMapper.insert(afterSalesService);
        return afterSalesService;
    }

    @Override
    @Transactional
    public AfterSalesService updateAfterSalesService(AfterSalesService afterSalesService) {
        afterSalesService.setUpdatedAt(LocalDateTime.now());
        afterSalesServiceMapper.update(afterSalesService);
        return afterSalesServiceMapper.selectByServiceId(afterSalesService.getServiceId());
    }

    @Override
    public AfterSalesService getAfterSalesServiceById(Integer serviceId) {
        return afterSalesServiceMapper.selectByServiceId(serviceId);
    }

    @Override
    public List<AfterSalesService> getAfterSalesServicesByCustomerId(Integer customerId, String serviceStatus, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return afterSalesServiceMapper.selectByCustomerId(customerId, serviceStatus, offset, size);
    }

    @Override
    public int countAfterSalesServicesByCustomerId(Integer customerId, String serviceStatus) {
        return afterSalesServiceMapper.countByCustomerId(customerId, serviceStatus);
    }

    @Override
    public List<AfterSalesService> getAfterSalesServicesBySellerId(Integer sellerId, String serviceStatus, String serviceType, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return afterSalesServiceMapper.selectBySellerId(sellerId, serviceStatus, serviceType, offset, size);
    }

    @Override
    public int countAfterSalesServicesBySellerId(Integer sellerId, String serviceStatus, String serviceType) {
        return afterSalesServiceMapper.countBySellerId(sellerId, serviceStatus, serviceType);
    }

    @Override
    @Transactional
    public AfterSalesService cancelAfterSalesService(Integer serviceId, Integer customerId, String cancelReason) {
        AfterSalesService service = afterSalesServiceMapper.selectByServiceId(serviceId);
        if (service == null) {
            throw new RuntimeException("售后服务单不存在");
        }
        if (!service.getCustomerId().equals(customerId)) {
            throw new RuntimeException("无权操作该售后服务单");
        }
        if (!"PENDING".equals(service.getServiceStatus()) && !"NEGOTIATING".equals(service.getServiceStatus())) {
            throw new RuntimeException("仅能取消状态为 PENDING 或 NEGOTIATING 的售后申请");
        }
        
        service.setServiceStatus("CANCELLED");
        service.setCancelReason(cancelReason);
        service.setCancelledAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        afterSalesServiceMapper.update(service);
        
        return service;
    }

    @Override
    @Transactional
    public AfterSalesService returnShipAfterSalesService(Integer serviceId, Integer customerId, String trackingNo, String logisticsProvider) {
        AfterSalesService service = afterSalesServiceMapper.selectByServiceId(serviceId);
        if (service == null) {
            throw new RuntimeException("售后服务单不存在");
        }
        if (!service.getCustomerId().equals(customerId)) {
            throw new RuntimeException("无权操作该售后服务单");
        }
        
        service.setReturnTrackingNo(trackingNo);
        service.setReturnLogisticsProvider(logisticsProvider);
        service.setReturnShippedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        afterSalesServiceMapper.update(service);
        
        return service;
    }

    @Override
    @Transactional
    public AfterSalesService handleAfterSalesService(Integer serviceId, String sellerDecision, String sellerResponse) {
        AfterSalesService service = afterSalesServiceMapper.selectByServiceId(serviceId);
        if (service == null) {
            throw new RuntimeException("售后服务单不存在");
        }
        
        String newStatus;
        switch (sellerDecision) {
            case "AGREE_REFUND":
            case "AGREE_RETURN_REFUND":
            case "AGREE_EXCHANGE":
                newStatus = "AGREED";
                break;
            case "REJECT":
                newStatus = "REJECTED";
                break;
            default:
                throw new RuntimeException("无效的卖家决策");
        }
        
        service.setSellerDecision(sellerDecision);
        service.setSellerResponse(sellerResponse);
        service.setServiceStatus(newStatus);
        service.setSellerDecisionAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        afterSalesServiceMapper.update(service);
        
        return service;
    }

    @Override
    @Transactional
    public AfterSalesService confirmReturnAfterSalesService(Integer serviceId, String remark) {
        AfterSalesService service = afterSalesServiceMapper.selectByServiceId(serviceId);
        if (service == null) {
            throw new RuntimeException("售后服务单不存在");
        }
        
        service.setServiceStatus("COMPLETED");
        service.setReturnReceivedAt(LocalDateTime.now());
        service.setCompletedAt(LocalDateTime.now());
        if (remark != null && !remark.isEmpty()) {
            service.setSellerResponse(service.getSellerResponse() + " [收货备注：" + remark + "]");
        }
        service.setUpdatedAt(LocalDateTime.now());
        afterSalesServiceMapper.update(service);
        
        return service;
    }

    @Override
    public AfterSalesService getByPurchaseIdAndCustomerId(Integer purchaseId, Integer customerId) {
        return afterSalesServiceMapper.selectByPurchaseIdAndCustomerId(purchaseId, customerId);
    }
}
