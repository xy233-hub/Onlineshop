package com.example.onlineshop.dto.request;

import lombok.Data;

@Data
public class AfterSalesReturnShipRequest {
    private String returnTrackingNo;
    private String returnLogisticsProvider;
}