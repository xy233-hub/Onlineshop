// File: src/main/java/com/example/onlineshop/mapper/PurchaseIntentItemMapper.java
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.PurchaseIntentItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface PurchaseIntentItemMapper {

    @Insert("INSERT INTO purchase_intent_items(purchase_id, product_id, product_name, product_price, quantity, subtotal) " +
            "VALUES(#{purchaseId}, #{productId}, #{productName}, #{productPrice}, #{quantity}, #{subtotal})")
    @Options(useGeneratedKeys = true, keyProperty = "itemId", keyColumn = "item_id")
    int insert(PurchaseIntentItem item);
}
