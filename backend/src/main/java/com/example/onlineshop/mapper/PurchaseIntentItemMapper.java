// File: src/main/java/com/example/onlineshop/mapper/PurchaseIntentItemMapper.java
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.PurchaseIntentItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PurchaseIntentItemMapper {

    @Insert("INSERT INTO purchase_intent_items(purchase_id, product_id, product_name, product_price, quantity, subtotal) " +
            "VALUES(#{purchaseId}, #{productId}, #{productName}, #{productPrice}, #{quantity}, #{subtotal})")
    @Options(useGeneratedKeys = true, keyProperty = "itemId", keyColumn = "item_id")
    int insert(PurchaseIntentItem item);

    /**
     * 性能优化：批量插入订单商品项（减少多次INSERT开销）
     */
    @Insert("<script>" +
            "INSERT INTO purchase_intent_items(purchase_id, product_id, product_name, product_price, quantity, subtotal) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.purchaseId}, #{item.productId}, #{item.productName}, #{item.productPrice}, #{item.quantity}, #{item.subtotal})" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<PurchaseIntentItem> items);

    @Select("SELECT item_id, purchase_id, product_id, product_name, product_price, quantity, subtotal " +
            "FROM purchase_intent_items WHERE purchase_id = #{purchaseId}")
    List<PurchaseIntentItem> findByPurchaseId(Integer purchaseId);
}
