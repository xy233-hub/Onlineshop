package com.example.onlineshop.dto.request;

public class PurchaseIntentStatusRequest {
    private String newStatus; // "success" | "failed"
    private String sellerNotes;
    private Integer soldQuantity; // 可选，覆盖 purchase_intents.quantity

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getSellerNotes() { return sellerNotes; }
    public void setSellerNotes(String sellerNotes) { this.sellerNotes = sellerNotes; }

    public Integer getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }
}
