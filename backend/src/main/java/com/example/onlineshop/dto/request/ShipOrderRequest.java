package com.example.onlineshop.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipOrderRequest {
    private Integer logistics_provider_id;
    private String tracking_no;
}