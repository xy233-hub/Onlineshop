
package com.example.onlineshop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class CartItemResponse {
    @JsonProperty("cart_item_id")
    public Integer cartItemId;
    @JsonProperty("product_id")
    public Integer productId;
    @JsonProperty("product_name")
    public String productName;
    @JsonProperty("quantity")
    public Integer quantity;
    @JsonProperty("unit_price")
    public Double unitPrice;
    @JsonProperty("note")
    public String note; // 可选字段，若无请保持 null
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("product_status")
    public String productStatus;
    @JsonProperty("stock_quantity")
    public Integer stockQuantity;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CartItemResponse() {}

    public CartItemResponse(Integer cartItemId, Integer productId, String productName,
                            Integer quantity, Double unitPrice, String note,
                            LocalDateTime createdAt, String productStatus, Integer stockQuantity) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.note = note;
        this.createdAt = createdAt != null ? createdAt.format(FMT) : null;
        this.productStatus = productStatus;
        this.stockQuantity = stockQuantity;
    }
}
