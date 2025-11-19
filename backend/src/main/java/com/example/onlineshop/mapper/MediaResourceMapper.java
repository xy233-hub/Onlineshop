package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.MediaResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MediaResourceMapper {

    // 插入媒体资源
    int insert(MediaResource mediaResource);

    // 根据商品ID查询所有媒体资源
    List<MediaResource> findByProductId(@Param("productId") Integer productId);

    // 根据商品ID删除所有媒体资源
    int deleteByProductId(@Param("productId") Integer productId);
}
