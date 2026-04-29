package com.example.onlineshop.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceAlertRequest {
    private Integer productId;
    private String alertType;
    private BigDecimal thresholdPercentage;
    private Boolean isEnabled;
}