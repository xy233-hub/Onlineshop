package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Promotion;
import com.example.onlineshop.entity.PromotionRule;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PromotionMapper {

    @Insert("INSERT INTO promotions(promotion_name, promotion_type, description, start_time, end_time, status, discount_value, min_purchase_amount, max_discount_amount, applicable_scope, target_ids, priority, created_by) " +
            "VALUES(#{promotionName}, #{promotionType}, #{description}, #{startTime}, #{endTime}, #{status}, #{discountValue}, #{minPurchaseAmount}, #{maxDiscountAmount}, #{applicableScope}, #{targetIds}, #{priority}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "promotionId", keyColumn = "promotion_id")
    int insert(Promotion promotion);

    @Update("UPDATE promotions SET promotion_name = #{promotionName}, promotion_type = #{promotionType}, description = #{description}, start_time = #{startTime}, end_time = #{endTime}, discount_value = #{discountValue}, min_purchase_amount = #{minPurchaseAmount}, max_discount_amount = #{maxDiscountAmount}, applicable_scope = #{applicableScope}, target_ids = #{targetIds}, priority = #{priority}, updated_at = NOW() WHERE promotion_id = #{promotionId}")
    int update(Promotion promotion);

    @Select("SELECT promotion_id, promotion_name, promotion_type, description, start_time, end_time, status, discount_value, min_purchase_amount, max_discount_amount, applicable_scope, target_ids, priority, created_by, created_at, updated_at FROM promotions WHERE promotion_id = #{promotionId} LIMIT 1")
    Promotion findById(@Param("promotionId") Integer promotionId);

    @Update("UPDATE promotions SET status = #{status}, updated_at = NOW() WHERE promotion_id = #{promotionId}")
    int updateStatus(@Param("promotionId") Integer promotionId, @Param("status") String status);

    @Select({
            "<script>",
            "SELECT promotion_id, promotion_name, promotion_type, description, start_time, end_time, status, discount_value, min_purchase_amount, max_discount_amount, applicable_scope, target_ids, priority, created_by, created_at, updated_at",
            "FROM promotions",
            "WHERE 1=1",
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>",
            "<if test='promotionType != null and promotionType != \"\"'> AND promotion_type = #{promotionType} </if>",
            "<if test='startDate != null'> AND end_time <![CDATA[>=]]> #{startDate} </if>",
            "<if test='endDate != null'> AND start_time <![CDATA[<=]]> #{endDate} </if>",
            "ORDER BY created_at DESC",
            "LIMIT #{size} OFFSET #{offset}",
            "</script>"
    })
    List<Promotion> list(@Param("status") String status,
                         @Param("promotionType") String promotionType,
                         @Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate,
                         @Param("size") int size,
                         @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM promotions",
            "WHERE 1=1",
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>",
            "<if test='promotionType != null and promotionType != \"\"'> AND promotion_type = #{promotionType} </if>",
            "<if test='startDate != null'> AND end_time <![CDATA[>=]]> #{startDate} </if>",
            "<if test='endDate != null'> AND start_time <![CDATA[<=]]> #{endDate} </if>",
            "</script>"
    })
    int count(@Param("status") String status,
              @Param("promotionType") String promotionType,
              @Param("startDate") LocalDateTime startDate,
              @Param("endDate") LocalDateTime endDate);

    @Insert("INSERT INTO promotion_rules(promotion_id, rule_type, rule_config) VALUES(#{promotionId}, #{ruleType}, #{ruleConfig})")
    int insertRule(PromotionRule rule);

    @Delete("DELETE FROM promotion_rules WHERE promotion_id = #{promotionId}")
    int deleteRules(@Param("promotionId") Integer promotionId);

    @Select("SELECT rule_id, promotion_id, rule_type, rule_config, created_at FROM promotion_rules WHERE promotion_id = #{promotionId} ORDER BY rule_id ASC")
    List<PromotionRule> listRules(@Param("promotionId") Integer promotionId);

    @Select("SELECT product_id FROM products")
    List<Integer> allProductIds();

    @Select({
            "<script>",
            "SELECT product_id FROM products WHERE category_id IN",
            "<foreach item='id' collection='categoryIds' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    List<Integer> productIdsByCategory(@Param("categoryIds") List<Integer> categoryIds);

    @Select("SELECT product_id, product_name, price, original_price, has_active_promotion FROM products WHERE product_id = #{productId} LIMIT 1")
    Map<String, Object> productById(@Param("productId") Integer productId);

    @Insert("INSERT INTO product_promotions(product_id, promotion_id, final_price, discount_amount, is_active, created_at, updated_at) " +
            "VALUES(#{productId}, #{promotionId}, #{finalPrice}, #{discountAmount}, TRUE, #{now}, #{now}) " +
            "ON DUPLICATE KEY UPDATE final_price = VALUES(final_price), discount_amount = VALUES(discount_amount), is_active = TRUE, updated_at = VALUES(updated_at)")
    int upsertProductPromotion(@Param("productId") Integer productId,
                               @Param("promotionId") Integer promotionId,
                               @Param("finalPrice") BigDecimal finalPrice,
                               @Param("discountAmount") BigDecimal discountAmount,
                               @Param("now") LocalDateTime now);

    @Update("UPDATE product_promotions SET is_active = FALSE, updated_at = NOW() WHERE promotion_id = #{promotionId}")
    int deactivateByPromotion(@Param("promotionId") Integer promotionId);

    @Select("SELECT DISTINCT product_id FROM product_promotions WHERE promotion_id = #{promotionId}")
    List<Integer> productIdsByPromotion(@Param("promotionId") Integer promotionId);

    @Select("SELECT pp.promotion_id, pp.product_id, pp.final_price, pp.discount_amount, p.priority, p.promotion_name, p.promotion_type, p.start_time, p.end_time, p.discount_value " +
            "FROM product_promotions pp JOIN promotions p ON p.promotion_id = pp.promotion_id " +
            "WHERE pp.product_id = #{productId} AND pp.is_active = TRUE AND p.status = 'ACTIVE' AND NOW() BETWEEN p.start_time AND p.end_time " +
            "ORDER BY p.priority DESC, pp.final_price ASC")
    List<Map<String, Object>> activePromotionsForProduct(@Param("productId") Integer productId);

    @Update("UPDATE products SET price = #{price}, original_price = #{originalPrice}, current_promotion_price = #{currentPromotionPrice}, has_active_promotion = #{hasActivePromotion}, updated_at = #{now} WHERE product_id = #{productId}")
    int updateProductEffectivePrice(@Param("productId") Integer productId,
                                    @Param("price") BigDecimal price,
                                    @Param("originalPrice") BigDecimal originalPrice,
                                    @Param("currentPromotionPrice") BigDecimal currentPromotionPrice,
                                    @Param("hasActivePromotion") Boolean hasActivePromotion,
                                    @Param("now") LocalDateTime now);

    @Select({
            "<script>",
            "SELECT promotion_id, promotion_name, promotion_type, description, start_time, end_time, status, discount_value, min_purchase_amount, max_discount_amount, applicable_scope, target_ids, priority, created_by, created_at, updated_at",
            "FROM promotions",
            "WHERE status = 'ACTIVE' AND NOW() BETWEEN start_time AND end_time",
            "<if test='promotionType != null and promotionType != \"\"'> AND promotion_type = #{promotionType} </if>",
            "ORDER BY priority DESC, created_at DESC",
            "</script>"
    })
    List<Promotion> listActive(@Param("promotionType") String promotionType);

    @Select({
            "<script>",
            "SELECT p.promotion_id, p.promotion_name, p.promotion_type, p.start_time, p.end_time, p.discount_value, p.applicable_scope, p.priority, pp.final_price, pp.discount_amount",
            "FROM promotions p JOIN product_promotions pp ON p.promotion_id = pp.promotion_id",
            "WHERE pp.product_id = #{productId}",
            "<if test='onlyActive'> AND pp.is_active = TRUE AND p.status = 'ACTIVE' AND NOW() BETWEEN p.start_time AND p.end_time </if>",
            "ORDER BY p.priority DESC, pp.final_price ASC",
            "</script>"
    })
    List<Map<String, Object>> promotionsByProduct(@Param("productId") Integer productId, @Param("onlyActive") boolean onlyActive);

    @Select("SELECT pp.product_id, pr.product_name, pr.original_price, pp.final_price AS promotion_price, pp.discount_amount " +
            "FROM product_promotions pp JOIN products pr ON pr.product_id = pp.product_id " +
            "WHERE pp.promotion_id = #{promotionId} ORDER BY pp.product_id ASC")
    List<Map<String, Object>> productsByPromotion(@Param("promotionId") Integer promotionId);

    @Select("SELECT COUNT(1) FROM product_promotions WHERE promotion_id = #{promotionId} AND is_active = TRUE")
    int activeProductCount(@Param("promotionId") Integer promotionId);
}

