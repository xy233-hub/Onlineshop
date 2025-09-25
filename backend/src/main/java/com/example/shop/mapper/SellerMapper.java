package com.example.shop.mapper;

import com.example.shop.entity.Seller;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SellerMapper {

    /**
     * 按用户名查询卖家
     */
    Seller selectByUsername(String username);

    /**
     * 按ID查询卖家
     */
    Seller selectById(Integer sellerId);

    /**
     * 更新卖家密码
     * @param sellerId 卖家ID
     * @param newPassword 加密后的新密码
     * @return 影响行数（1=成功）
     */
    int updatePassword(@Param("sellerId") Integer sellerId, @Param("newPassword") String newPassword);
}