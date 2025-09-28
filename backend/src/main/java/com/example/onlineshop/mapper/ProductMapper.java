// mapper/ProductMapper.java
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<Product> findOnlineProducts();

    Product findById(@Param("productId") Integer productId);

    int insert(Product product);

    int updateStatus(@Param("productId") Integer productId, @Param("status") String status);

    List<Product> findAllBySellerId(@Param("sellerId") Integer sellerId);
}
