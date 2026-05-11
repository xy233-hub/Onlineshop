package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.PriceAlertSetting;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PriceAlertMapper {

    @Select("SELECT COUNT(1) FROM favorites WHERE customer_id = #{customerId} AND product_id = #{productId}")
    int countFavorite(@Param("customerId") Integer customerId, @Param("productId") Integer productId);

    @Select("SELECT COUNT(1) FROM shopping_cart WHERE customer_id = #{customerId} AND product_id = #{productId}")
    int countCart(@Param("customerId") Integer customerId, @Param("productId") Integer productId);

    @Select("SELECT alert_id, customer_id, product_id, alert_type, is_enabled, threshold_percentage, last_alerted_price, last_alerted_at, created_at, updated_at " +
            "FROM price_alert_settings WHERE customer_id = #{customerId} AND product_id = #{productId} AND alert_type = #{alertType} LIMIT 1")
    PriceAlertSetting findByUnique(@Param("customerId") Integer customerId,
                                   @Param("productId") Integer productId,
                                   @Param("alertType") String alertType);

    @Insert("INSERT INTO price_alert_settings(customer_id, product_id, alert_type, is_enabled, threshold_percentage, last_alerted_price) " +
            "VALUES(#{customerId}, #{productId}, #{alertType}, #{isEnabled}, #{thresholdPercentage}, #{lastAlertedPrice})")
    @Options(useGeneratedKeys = true, keyProperty = "alertId", keyColumn = "alert_id")
    int insert(PriceAlertSetting setting);

    @Update("UPDATE price_alert_settings SET is_enabled = #{isEnabled}, threshold_percentage = #{thresholdPercentage}, updated_at = NOW() WHERE alert_id = #{alertId}")
    int updateBasic(PriceAlertSetting setting);

    @Select("SELECT alert_id, customer_id, product_id, alert_type, is_enabled, threshold_percentage, last_alerted_price, last_alerted_at, created_at, updated_at " +
            "FROM price_alert_settings WHERE alert_id = #{alertId} LIMIT 1")
    PriceAlertSetting findById(@Param("alertId") Integer alertId);

    @Delete("DELETE FROM price_alert_settings WHERE alert_id = #{alertId}")
    int deleteById(@Param("alertId") Integer alertId);

    @Select({
            "<script>",
            "SELECT pas.alert_id, pas.product_id, pas.alert_type, pas.is_enabled, pas.threshold_percentage, pas.last_alerted_price, pas.last_alerted_at, pas.created_at,",
            "p.product_name, p.price AS current_price,",
            "(SELECT pi.image_url FROM product_images pi WHERE pi.product_id = p.product_id ORDER BY pi.image_order ASC, pi.image_id ASC LIMIT 1) AS image_url",
            "FROM price_alert_settings pas",
            "JOIN products p ON p.product_id = pas.product_id",
            "WHERE pas.customer_id = #{customerId}",
            "<if test='isEnabled != null'> AND pas.is_enabled = #{isEnabled} </if>",
            "<if test='alertType != null and alertType != \"\"'> AND pas.alert_type = #{alertType} </if>",
            "ORDER BY pas.created_at DESC",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    List<Map<String, Object>> listAlerts(@Param("customerId") Integer customerId,
                                         @Param("isEnabled") Boolean isEnabled,
                                         @Param("alertType") String alertType,
                                         @Param("size") int size,
                                         @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM price_alert_settings",
            "WHERE customer_id = #{customerId}",
            "<if test='isEnabled != null'> AND is_enabled = #{isEnabled} </if>",
            "<if test='alertType != null and alertType != \"\"'> AND alert_type = #{alertType} </if>",
            "</script>"
    })
    int countAlerts(@Param("customerId") Integer customerId,
                    @Param("isEnabled") Boolean isEnabled,
                    @Param("alertType") String alertType);

    @Select({
            "<script>",
            "SELECT pan.notification_id, pan.product_id, pan.old_price, pan.new_price, pan.change_percentage, pan.change_direction, pan.is_read, pan.created_at,",
            "p.product_name,",
            "(SELECT pi.image_url FROM product_images pi WHERE pi.product_id = p.product_id ORDER BY pi.image_order ASC, pi.image_id ASC LIMIT 1) AS image_url",
            "FROM price_alert_notifications pan",
            "JOIN products p ON p.product_id = pan.product_id",
            "WHERE pan.customer_id = #{customerId}",
            "<if test='isRead != null'> AND pan.is_read = #{isRead} </if>",
            "ORDER BY pan.created_at DESC",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    List<Map<String, Object>> listNotifications(@Param("customerId") Integer customerId,
                                                @Param("isRead") Boolean isRead,
                                                @Param("size") int size,
                                                @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(1)",
            "FROM price_alert_notifications",
            "WHERE customer_id = #{customerId}",
            "<if test='isRead != null'> AND is_read = #{isRead} </if>",
            "</script>"
    })
    int countNotifications(@Param("customerId") Integer customerId, @Param("isRead") Boolean isRead);

    @Select("SELECT COUNT(1) FROM price_alert_notifications WHERE customer_id = #{customerId} AND is_read = FALSE")
    int unreadCount(@Param("customerId") Integer customerId);

    @Update("UPDATE price_alert_notifications SET is_read = TRUE, read_at = NOW() WHERE notification_id = #{notificationId} AND customer_id = #{customerId}")
    int markRead(@Param("notificationId") Integer notificationId, @Param("customerId") Integer customerId);
    
    @Update("<script>UPDATE price_alert_notifications SET is_read = TRUE, read_at = NOW() WHERE customer_id = #{customerId} AND notification_id IN <foreach collection='notificationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int batchMarkAsRead(@Param("customerId") Integer customerId, @Param("notificationIds") List<Integer> notificationIds);

    @Update("UPDATE price_alert_notifications SET is_read = TRUE, read_at = NOW() WHERE customer_id = #{customerId} AND is_read = FALSE")
    int markAllRead(@Param("customerId") Integer customerId);
}

