package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer paymentId;
    private Integer purchaseId;
    private Integer customerId;
    private String paymentMethod;
    private BigDecimal paymentAmount;
    private String paymentStatus;
    private String transactionId;
    private LocalDateTime paymentTime;
    private LocalDateTime refundTime;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime paymentExpiry;
    private String paymentNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}