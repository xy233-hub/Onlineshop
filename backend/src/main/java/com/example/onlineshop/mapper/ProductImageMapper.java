package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductImageMapper {

    // 插入商品图片
    int insert(ProductImage productImage);

    // 根据商品ID查询所有图片
    List<ProductImage> findByProductId(@Param("productId") Integer productId);

    // 根据商品ID删除所有图片
    int deleteByProductId(@Param("productId") Integer productId);
}
