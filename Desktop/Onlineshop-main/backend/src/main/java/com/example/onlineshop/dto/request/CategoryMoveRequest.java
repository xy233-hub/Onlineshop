// java
package com.example.onlineshop.dto.request;

public class CategoryMoveRequest {
    private Integer newParentId;
    private Integer newOrder;

    public Integer getNewParentId() {
        return newParentId;
    }
    public void setNewParentId(Integer newParentId) {
        this.newParentId = newParentId;
    }
    public Integer getNewOrder() {
        return newOrder;
    }
    public void setNewOrder(Integer newOrder) {
        this.newOrder = newOrder;
    }
}
