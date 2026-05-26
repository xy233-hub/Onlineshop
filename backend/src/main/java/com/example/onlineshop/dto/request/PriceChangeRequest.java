package com.example.onlineshop.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceChangeRequest {
    private BigDecimal newPrice;
    private String changeReason;
}