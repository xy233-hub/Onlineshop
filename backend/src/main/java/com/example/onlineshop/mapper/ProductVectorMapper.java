package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ProductVector;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductVectorMapper {
    
    @Insert("INSERT INTO product_vectors (product_id, vector_type, image_url, vector_text, vector_data) " +
            "VALUES (#{productId}, #{vectorType}, #{imageUrl}, #{vectorText}, #{vectorData}) " +
            "ON DUPLICATE KEY UPDATE vector_text = #{vectorText}, vector_data = #{vectorData}, updated_at = NOW()")
    int upsert(ProductVector vector);
    
    @Insert("INSERT INTO product_vectors (product_id, vector_type, image_url, vector_text, vector_data) " +
            "VALUES (#{productId}, #{vectorType}, #{imageUrl}, #{vectorText}, #{vectorData})")
    int insert(ProductVector vector);
    
    @Update("UPDATE product_vectors SET vector_text = #{vectorText}, vector_data = #{vectorData}, updated_at = NOW() " +
            "WHERE product_id = #{productId} AND vector_type = #{vectorType} AND (image_url = #{imageUrl} OR (image_url IS NULL AND #{imageUrl} IS NULL))")
    int updateByProductAndType(ProductVector vector);
    
    @Delete("DELETE FROM product_vectors WHERE product_id = #{productId}")
    int deleteByProductId(@Param("productId") Integer productId);
    
    @Delete("DELETE FROM product_vectors WHERE product_id = #{productId} AND vector_type = 'image'")
    int deleteImageVectorsByProductId(@Param("productId") Integer productId);
    
    @Select("SELECT * FROM product_vectors WHERE product_id = #{productId} AND vector_type = 'product'")
    ProductVector selectProductVector(@Param("productId") Integer productId);
    
    @Select("SELECT * FROM product_vectors WHERE vector_type = 'image' AND product_id = #{productId} AND image_url = #{imageUrl}")
    ProductVector selectImageVector(@Param("productId") Integer productId, @Param("imageUrl") String imageUrl);
    
    @Select("SELECT * FROM product_vectors WHERE vector_type = 'image'")
    List<ProductVector> selectAllImageVectors();
    
    @Select("SELECT * FROM product_vectors WHERE vector_type = 'product'")
    List<ProductVector> selectAllProductVectors();
    
    @Select("SELECT * FROM product_vectors WHERE vector_type = 'image' AND product_id IN " +
            "(SELECT product_id FROM products WHERE product_status = 'online')")
    List<ProductVector> selectOnlineImageVectors();
    
    @Select("SELECT * FROM product_vectors WHERE vector_type = 'product' AND product_id IN " +
            "(SELECT product_id FROM products WHERE product_status = 'online')")
    List<ProductVector> selectOnlineProductVectors();
}
