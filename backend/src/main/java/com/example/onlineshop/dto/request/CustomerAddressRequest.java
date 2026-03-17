package com.example.onlineshop.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddressRequest {
    private Integer customerId;
    private String recipientName;
    private String recipientPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
}