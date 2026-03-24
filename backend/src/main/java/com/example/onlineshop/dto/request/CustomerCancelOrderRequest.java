// java
package com.example.onlineshop.dto.request;

import lombok.Data;

@Data
public class CustomerCancelOrderRequest {

    private String cancelReason;
    private String cancelNotes;
}
