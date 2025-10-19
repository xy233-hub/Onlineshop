// entity/PurchaseIntent.java
package com.example.onlineshop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PurchaseIntent {

    private Integer purchaseId;
    private Integer productId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String purchaseStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}

