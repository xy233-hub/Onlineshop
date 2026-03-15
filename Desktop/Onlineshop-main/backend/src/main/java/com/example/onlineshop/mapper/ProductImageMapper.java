package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@Repository
public interface ProductImageMapper {
    int insert(ProductImage productImage);

    ProductImage selectById(Integer imageId);

    List<ProductImage> selectByProductId(Integer productId);

    int update(ProductImage productImage);

    int deleteById(Integer imageId);

    int deleteByProductId(Integer productId);
}
