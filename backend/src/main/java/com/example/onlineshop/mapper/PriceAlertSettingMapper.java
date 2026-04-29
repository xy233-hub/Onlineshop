package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.PriceAlertSetting;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PriceAlertSettingMapper {

    @Insert("INSERT INTO price_alert_settings (customer_id, product_id, alert_type, is_enabled, threshold_percentage) " +
            "VALUES (#{customerId}, #{productId}, #{alertType}, #{isEnabled}, #{thresholdPercentage})")
    @Options(useGeneratedKeys = true, keyProperty = "alertId")
    int insert(PriceAlertSetting setting);

    @Select("SELECT * FROM price_alert_settings WHERE alert_id = #{alertId}")
    PriceAlertSetting findById(Integer alertId);

    @Select("SELECT * FROM price_alert_settings WHERE customer_id = #{customerId} AND product_id = #{productId} AND alert_type = #{alertType}")
    PriceAlertSetting findByCustomerProductAndType(@Param("customerId") Integer customerId, 
                                                     @Param("productId") Integer productId, 
                                                     @Param("alertType") String alertType);

    @Select("<script>" +
            "SELECT * FROM price_alert_settings WHERE customer_id = #{customerId}" +
            "<if test='isEnabled != null'> AND is_enabled = #{isEnabled}</if>" +
            "<if test='alertType != null'> AND alert_type = #{alertType}</if>" +
            " ORDER BY created_at DESC" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<PriceAlertSetting> findByCustomerId(@Param("customerId") Integer customerId,
                                              @Param("isEnabled") Boolean isEnabled,
                                              @Param("alertType") String alertType,
                                              @Param("offset") Integer offset,
                                              @Param("size") Integer size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM price_alert_settings WHERE customer_id = #{customerId}" +
            "<if test='isEnabled != null'> AND is_enabled = #{isEnabled}</if>" +
            "<if test='alertType != null'> AND alert_type = #{alertType}</if>" +
            "</script>")
    int countByCustomerId(@Param("customerId") Integer customerId,
                          @Param("isEnabled") Boolean isEnabled,
                          @Param("alertType") String alertType);

    @Update("UPDATE price_alert_settings SET is_enabled = #{isEnabled}, threshold_percentage = #{thresholdPercentage}, updated_at = NOW() WHERE alert_id = #{alertId}")
    int update(PriceAlertSetting setting);

    @Delete("DELETE FROM price_alert_settings WHERE alert_id = #{alertId} AND customer_id = #{customerId}")
    int delete(@Param("alertId") Integer alertId, @Param("customerId") Integer customerId);

    @Select("SELECT * FROM price_alert_settings WHERE product_id = #{productId} AND is_enabled = TRUE")
    List<PriceAlertSetting> findByProductId(Integer productId);
}