package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    // 新增商品
    int insert(Product product);

    // 查询所有商品（按创建时间倒序）
    List<Product> selectAll();

    // 根据商品ID查询商品
    Product findById(@Param("productId") Integer productId);

    // 更新商品信息
    int update(Product product);

    // 根据状态查询商品
    List<Product> selectByStatus(@Param("productStatus") String productStatus);

    // 查询所有在售商品
    List<Product> findOnlineProducts();

    // 更新商品状态
    int updateStatus(@Param("productId") Integer productId, @Param("status") String status);

    // 查询某卖家的所有商品
    List<Product> findAllBySellerId(@Param("sellerId") Integer sellerId);
}
