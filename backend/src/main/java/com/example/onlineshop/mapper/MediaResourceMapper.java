package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.MediaResource;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MediaResourceMapper {
    int insert(MediaResource mediaResource);

    MediaResource selectById(Integer mediaId);

    List<MediaResource> selectByProductId(Integer productId);

    int update(MediaResource mediaResource);

    int deleteById(Integer mediaId);

    int deleteByProductId(Integer productId);
}
