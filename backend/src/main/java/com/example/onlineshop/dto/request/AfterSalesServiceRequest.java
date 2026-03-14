package com.example.onlineshop.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AfterSalesServiceRequest {
    private Integer purchaseId;
    private Integer productId;
    private String serviceType;
    private String serviceTitle;
    private String problemDescription;
    private List<String> evidenceImages;
    private Double refundAmount;
}