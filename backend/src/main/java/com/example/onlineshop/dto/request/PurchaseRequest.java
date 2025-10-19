// dto/PurchaseRequestDTO.java
package com.example.onlineshop.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_phone")
    private String customerPhone;

    @JsonProperty("customer_address")
    private String customerAddress;
}

