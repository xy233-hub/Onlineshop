// mapper/SellerMapper.java
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Seller;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SellerMapper {

    Seller findByUsername(@Param("username") String username);

    Seller findById(@Param("sellerId") Integer sellerId);

    int updatePassword(@Param("sellerId") Integer sellerId, @Param("newPassword") String newPassword);
}
