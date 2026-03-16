
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ShoppingCartItem;
import com.example.onlineshop.dto.response.CartItemResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShoppingCartMapper {

    @Select("SELECT cart_item_id, customer_id, product_id, quantity, created_at, updated_at " +
            "FROM shopping_cart WHERE customer_id = #{customerId} AND product_id = #{productId} LIMIT 1")
    ShoppingCartItem findByCustomerAndProduct(@Param("customerId") Integer customerId, @Param("productId") Integer productId);

    @Insert("INSERT INTO shopping_cart(customer_id, product_id, quantity) VALUES(#{customerId}, #{productId}, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "cartItemId", keyColumn = "cart_item_id")
    int insert(ShoppingCartItem item);

    @Update("UPDATE shopping_cart SET quantity = #{quantity}, updated_at = CURRENT_TIMESTAMP WHERE cart_item_id = #{cartItemId}")
    int updateQuantity(@Param("cartItemId") Integer cartItemId, @Param("quantity") Integer quantity);

    @Select({
            "<script>",
            "SELECT sc.cart_item_id, sc.product_id, sc.quantity, sc.created_at, p.product_name, p.price, p.product_status, p.stock_quantity",
            "FROM shopping_cart sc",
            "LEFT JOIN products p ON sc.product_id = p.product_id",
            "WHERE sc.customer_id = #{customerId}",
            "<if test='filterStock != null and filterStock'>",
            "AND p.stock_quantity &gt;= sc.quantity",
            "</if>",
            "ORDER BY ${sortBy} ${order}",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    @Results({
            @Result(column = "cart_item_id", property = "cartItemId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "product_name", property = "productName"),
            @Result(column = "quantity", property = "quantity"),
            @Result(column = "price", property = "unitPrice"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "product_status", property = "productStatus"),
            @Result(column = "stock_quantity", property = "stockQuantity")
    })
    List<CartItemResponse> listByCustomer(@Param("customerId") Integer customerId,
                                          @Param("offset") int offset,
                                          @Param("size") int size,
                                          @Param("filterStock") Boolean filterStock,
                                          @Param("sortBy") String sortBy,
                                          @Param("order") String order);

    @Select("SELECT COUNT(1) FROM shopping_cart WHERE customer_id = #{customerId}")
    int countByCustomer(@Param("customerId") Integer customerId);

    @Select("SELECT cart_item_id FROM shopping_cart WHERE cart_item_id = #{cartItemId} AND customer_id = #{customerId} LIMIT 1")
    Integer findIdByIdAndCustomer(@Param("cartItemId") Integer cartItemId, @Param("customerId") Integer customerId);

    // 新增：按 cart_item_id + customer 查询完整购物车项
    @Select("SELECT cart_item_id, customer_id, product_id, quantity, created_at, updated_at " +
            "FROM shopping_cart WHERE cart_item_id = #{cartItemId} AND customer_id = #{customerId} LIMIT 1")
    ShoppingCartItem findByIdAndCustomer(@Param("cartItemId") Integer cartItemId, @Param("customerId") Integer customerId);

    @Delete("DELETE FROM shopping_cart WHERE cart_item_id = #{cartItemId}")
    int deleteById(@Param("cartItemId") Integer cartItemId);

    @Delete({
            "<script>",
            "DELETE FROM shopping_cart WHERE customer_id = #{customerId} AND cart_item_id IN",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int deleteByIds(@Param("customerId") Integer customerId, @Param("ids") List<Integer> ids);


    // 新增：按 cart_item_id 列表查询并关联产品信息
    @Select({
            "<script>",
            "SELECT sc.cart_item_id, sc.product_id, sc.quantity, sc.created_at, p.product_name, p.price, p.product_status, p.stock_quantity",
            "FROM shopping_cart sc",
            "LEFT JOIN products p ON sc.product_id = p.product_id",
            "WHERE sc.customer_id = #{customerId} AND sc.cart_item_id IN",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    @Results({
            @Result(column = "cart_item_id", property = "cartItemId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "product_name", property = "productName"),
            @Result(column = "quantity", property = "quantity"),
            @Result(column = "price", property = "unitPrice"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "product_status", property = "productStatus"),
            @Result(column = "stock_quantity", property = "stockQuantity")
    })
    List<CartItemResponse> listByCustomerAndIds(@Param("customerId") Integer customerId, @Param("ids") List<Integer> ids);
}

