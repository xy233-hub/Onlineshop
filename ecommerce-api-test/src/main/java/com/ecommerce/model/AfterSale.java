// src/main/java/com/ecommerce/model/AfterSale.java
package com.ecommerce.model;

import lombok.Data;
import java.util.Date;

@Data
public class AfterSale {
    private String saleId;
    private String orderId;
    private String userId;
    private String title;
    private String description;
    private String type;
    private String status;
    private String rejectReason;
    private Date applyTime;
    private Date processTime;
}