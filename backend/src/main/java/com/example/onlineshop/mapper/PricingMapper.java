package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ProductPriceHistory;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PricingMapper {

    @Select("SELECT product_id, seller_id, category_id, product_name, price, original_price, current_promotion_price, has_active_promotion " +
            "FROM products WHERE product_id = #{productId} LIMIT 1")
    Map<String, Object> selectProductPricingInfo(@Param("productId") Integer productId);

    @Update("UPDATE products SET price = #{newPrice}, original_price = #{originalPrice}, current_promotion_price = #{promotionPrice}, " +
            "has_active_promotion = #{hasActivePromotion}, updated_at = #{now} WHERE product_id = #{productId}")
    int updateProductPrice(@Param("productId") Integer productId,
                           @Param("newPrice") BigDecimal newPrice,
                           @Param("originalPrice") BigDecimal originalPrice,
                           @Param("promotionPrice") BigDecimal promotionPrice,
                           @Param("hasActivePromotion") Boolean hasActivePromotion,
                           @Param("now") LocalDateTime now);

    @Insert("INSERT INTO product_price_history(product_id, old_price, new_price, change_type, change_reason, changed_by, created_at) " +
            "VALUES(#{productId}, #{oldPrice}, #{newPrice}, #{changeType}, #{changeReason}, #{changedBy}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "historyId", keyColumn = "history_id")
    int insertPriceHistory(ProductPriceHistory history);

    @Select({
            "<script>",
            "SELECT history_id, product_id, old_price, new_price, change_type, change_reason, changed_by, created_at",
            "FROM product_price_history",
            "WHERE product_id = #{productId}",
            "<if test='startTime != null'> AND created_at <![CDATA[>=]]> #{startTime} </if>",
            "<if test='endTime != null'> AND created_at <![CDATA[<=]]> #{endTime} </if>",
            "ORDER BY created_at DESC",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    List<ProductPriceHistory> listPriceHistory(@Param("productId") Integer productId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("size") int size,
                                               @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM product_price_history",
            "WHERE product_id = #{productId}",
            "<if test='startTime != null'> AND created_at <![CDATA[>=]]> #{startTime} </if>",
            "<if test='endTime != null'> AND created_at <![CDATA[<=]]> #{endTime} </if>",
            "</script>"
    })
    int countPriceHistory(@Param("productId") Integer productId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    @Select({
            "<script>",
            "SELECT",
            "MIN(new_price) AS lowest_price,",
            "MAX(new_price) AS highest_price,",
            "AVG(new_price) AS average_price,",
            "COUNT(1) AS total_changes,",
            "SUM(CASE WHEN new_price &lt; old_price THEN 1 ELSE 0 END) AS decrease_count,",
            "SUM(CASE WHEN new_price &gt; old_price THEN 1 ELSE 0 END) AS increase_count,",
            "MAX(CASE WHEN old_price &gt; 0 AND new_price &lt; old_price THEN ROUND((old_price - new_price) / old_price * 100, 2) ELSE 0 END) AS max_decrease_percentage,",
            "MAX(CASE WHEN old_price &gt; 0 AND new_price &gt; old_price THEN ROUND((new_price - old_price) / old_price * 100, 2) ELSE 0 END) AS max_increase_percentage",
            "FROM product_price_history",
            "WHERE product_id = #{productId}",
            "<if test='startTime != null'> AND created_at <![CDATA[>=]]> #{startTime} </if>",
            "<if test='endTime != null'> AND created_at <![CDATA[<=]]> #{endTime} </if>",
            "</script>"
    })
    Map<String, Object> priceStats(@Param("productId") Integer productId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    @Select("SELECT end_time FROM promotions p JOIN product_promotions pp ON p.promotion_id = pp.promotion_id " +
            "WHERE pp.product_id = #{productId} AND pp.is_active = TRUE AND p.status = 'ACTIVE' AND NOW() BETWEEN p.start_time AND p.end_time " +
            "ORDER BY p.end_time ASC LIMIT 1")
    LocalDateTime nearestPromotionEndTime(@Param("productId") Integer productId);

    @Select("SELECT customer_id, product_id, alert_type, is_enabled, threshold_percentage, last_alerted_price, last_alerted_at, alert_id, created_at, updated_at " +
            "FROM price_alert_settings WHERE product_id = #{productId} AND is_enabled = TRUE")
    List<Map<String, Object>> enabledAlertsByProduct(@Param("productId") Integer productId);

    @Insert("INSERT INTO price_alert_notifications(alert_id, customer_id, product_id, old_price, new_price, change_percentage, change_direction, is_read, created_at) " +
            "VALUES(#{alertId}, #{customerId}, #{productId}, #{oldPrice}, #{newPrice}, #{changePercentage}, #{changeDirection}, FALSE, #{createdAt})")
    int insertNotification(@Param("alertId") Integer alertId,
                           @Param("customerId") Integer customerId,
                           @Param("productId") Integer productId,
                           @Param("oldPrice") BigDecimal oldPrice,
                           @Param("newPrice") BigDecimal newPrice,
                           @Param("changePercentage") BigDecimal changePercentage,
                           @Param("changeDirection") String changeDirection,
                           @Param("createdAt") LocalDateTime createdAt);

    @Update("UPDATE price_alert_settings SET last_alerted_price = #{price}, last_alerted_at = #{at}, updated_at = #{at} WHERE alert_id = #{alertId}")
    int updateAlertCheckpoint(@Param("alertId") Integer alertId,
                              @Param("price") BigDecimal price,
                              @Param("at") LocalDateTime at);

    @Update("UPDATE product_promotions SET is_active = FALSE, updated_at = #{now} WHERE product_id = #{productId} AND is_active = TRUE")
    int deactivateProductPromotions(@Param("productId") Integer productId,
                                    @Param("now") LocalDateTime now);
}

