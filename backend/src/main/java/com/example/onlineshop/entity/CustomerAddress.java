package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户收货地址
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer addressId;
    private Integer customerId;
    private String recipientName;
    private String recipientPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}