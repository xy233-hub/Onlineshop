package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Favorite;
import com.example.onlineshop.dto.response.FavoriteResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FavoriteMapper {

    @Select("SELECT favorite_id, customer_id, product_id, created_at FROM favorites WHERE customer_id = #{customerId} AND product_id = #{productId} LIMIT 1")
    Favorite findByCustomerAndProduct(@Param("customerId") Integer customerId, @Param("productId") Integer productId);

    @Insert("INSERT INTO favorites(customer_id, product_id) VALUES(#{customerId}, #{productId})")
    @Options(useGeneratedKeys = true, keyProperty = "favoriteId", keyColumn = "favorite_id")
    int insert(Favorite favorite);

    @Select({
            "<script>",
            "SELECT f.favorite_id, f.customer_id, f.product_id, f.created_at,",
            " p.product_name, p.product_desc, p.price, MAX(pi.image_url) AS image_url",
            "FROM favorites f",
            "LEFT JOIN products p ON f.product_id = p.product_id",
            "LEFT JOIN product_images pi ON pi.product_id = p.product_id AND pi.image_order = 0",
            "WHERE f.customer_id = #{customerId}",
            "GROUP BY f.favorite_id, f.customer_id, f.product_id, f.created_at, p.product_name, p.product_desc, p.price",
            "ORDER BY f.created_at DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    @Results({
            @Result(column = "favorite_id", property = "favoriteId"),
            @Result(column = "customer_id", property = "customerId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "created_at", property = "favoritedAt"),
            @Result(column = "product_id", property = "productInfo.productId"),
            @Result(column = "product_name", property = "productInfo.productName"),
            @Result(column = "product_desc", property = "productInfo.productDesc"),
            @Result(column = "price", property = "productInfo.price"),
            @Result(column = "image_url", property = "productInfo.imageUrl")
    })
    List<FavoriteResponse> listByCustomer(@Param("customerId") Integer customerId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT favorite_id FROM favorites WHERE favorite_id = #{favoriteId} AND customer_id = #{customerId} LIMIT 1")
    Integer findIdByIdAndCustomer(@Param("favoriteId") Integer favoriteId, @Param("customerId") Integer customerId);

    @Delete("DELETE FROM favorites WHERE favorite_id = #{favoriteId}")
    int deleteById(@Param("favoriteId") Integer favoriteId);
}
