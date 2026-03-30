// src/main/java/com/ecommerce/model/Order.java
package com.ecommerce.model;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class Order {
    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private Double totalAmount;
    private String status;
    private Date createTime;
    private Date payTime;
    private String addressId;
    private String deliveryMethod;
    private String trackingNumber;
    
    @Data
    public static class OrderItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private Double price;
    }
}