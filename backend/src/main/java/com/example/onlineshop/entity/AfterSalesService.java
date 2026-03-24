package com.example.onlineshop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AfterSalesService {
    private Integer serviceId;
    private Integer purchaseId;
    private Integer productId;
    private Integer customerId;
    private String serviceType;
    private String serviceTitle;
    private String problemDescription;
    private String evidenceImages;
    private BigDecimal refundAmount;
    private String serviceStatus;
    private String sellerResponse;
    private String sellerDecision;
    private LocalDateTime sellerDecisionAt;
    private String returnTrackingNo;
    private String returnLogisticsProvider;
    private LocalDateTime returnShippedAt;
    private LocalDateTime returnReceivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AfterSalesService() {}

    public Integer getServiceId() { return serviceId; }
    public void setServiceId(Integer serviceId) { this.serviceId = serviceId; }
    public Integer getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Integer purchaseId) { this.purchaseId = purchaseId; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getServiceTitle() { return serviceTitle; }
    public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }
    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }
    public String getEvidenceImages() { return evidenceImages; }
    public void setEvidenceImages(String evidenceImages) { this.evidenceImages = evidenceImages; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(String serviceStatus) { this.serviceStatus = serviceStatus; }
    public String getSellerResponse() { return sellerResponse; }
    public void setSellerResponse(String sellerResponse) { this.sellerResponse = sellerResponse; }
    public String getSellerDecision() { return sellerDecision; }
    public void setSellerDecision(String sellerDecision) { this.sellerDecision = sellerDecision; }
    public LocalDateTime getSellerDecisionAt() { return sellerDecisionAt; }
    public void setSellerDecisionAt(LocalDateTime sellerDecisionAt) { this.sellerDecisionAt = sellerDecisionAt; }
    public String getReturnTrackingNo() { return returnTrackingNo; }
    public void setReturnTrackingNo(String returnTrackingNo) { this.returnTrackingNo = returnTrackingNo; }
    public String getReturnLogisticsProvider() { return returnLogisticsProvider; }
    public void setReturnLogisticsProvider(String returnLogisticsProvider) { this.returnLogisticsProvider = returnLogisticsProvider; }
    public LocalDateTime getReturnShippedAt() { return returnShippedAt; }
    public void setReturnShippedAt(LocalDateTime returnShippedAt) { this.returnShippedAt = returnShippedAt; }
    public LocalDateTime getReturnReceivedAt() { return returnReceivedAt; }
    public void setReturnReceivedAt(LocalDateTime returnReceivedAt) { this.returnReceivedAt = returnReceivedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}