package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.PriceAlertNotification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PriceAlertNotificationMapper {

    @Insert("INSERT INTO price_alert_notifications (alert_id, customer_id, product_id, old_price, new_price, change_percentage, change_direction) " +
            "VALUES (#{alertId}, #{customerId}, #{productId}, #{oldPrice}, #{newPrice}, #{changePercentage}, #{changeDirection})")
    @Options(useGeneratedKeys = true, keyProperty = "notificationId")
    int insert(PriceAlertNotification notification);

    @Select("<script>" +
            "SELECT * FROM price_alert_notifications WHERE customer_id = #{customerId}" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            " ORDER BY created_at DESC" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<PriceAlertNotification> findByCustomerId(@Param("customerId") Integer customerId,
                                                    @Param("isRead") Boolean isRead,
                                                    @Param("offset") Integer offset,
                                                    @Param("size") Integer size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM price_alert_notifications WHERE customer_id = #{customerId}" +
            "<if test='isRead != null'> AND is_read = #{isRead}</if>" +
            "</script>")
    int countByCustomerId(@Param("customerId") Integer customerId,
                          @Param("isRead") Boolean isRead);

    @Update("UPDATE price_alert_notifications SET is_read = TRUE, read_at = NOW() WHERE notification_id = #{notificationId} AND customer_id = #{customerId}")
    int markAsRead(@Param("notificationId") Integer notificationId, @Param("customerId") Integer customerId);

    @Update("UPDATE price_alert_notifications SET is_read = TRUE, read_at = NOW() WHERE customer_id = #{customerId} AND notification_id IN <foreach collection='notificationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>")
    int batchMarkAsRead(@Param("customerId") Integer customerId, @Param("notificationIds") List<Integer> notificationIds);

    @Select("SELECT COUNT(*) FROM price_alert_notifications WHERE customer_id = #{customerId} AND is_read = FALSE")
    int countUnreadByCustomerId(Integer customerId);

    @Update("UPDATE price_alert_notifications SET is_read = TRUE, read_at = NOW() WHERE customer_id = #{customerId} AND is_read = FALSE")
    int markAllAsRead(@Param("customerId") Integer customerId);
}