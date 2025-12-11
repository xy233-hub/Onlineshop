// file: backend/src/main/java/com/example/onlineshop/entity/PurchaseIntent.java
package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购买意向记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseIntent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer purchaseId;
    private Integer productId;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String purchaseStatus;
    private String sellerNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


