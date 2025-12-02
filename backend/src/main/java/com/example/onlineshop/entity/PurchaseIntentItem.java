// File: src/main/java/com/example/onlineshop/entity/PurchaseIntentItem.java
package com.example.onlineshop.entity;

import java.math.BigDecimal;

public class PurchaseIntentItem {
    private Integer itemId;
    private Integer purchaseId;
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public Integer getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Integer purchaseId) { this.purchaseId = purchaseId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getProductPrice() { return productPrice; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
