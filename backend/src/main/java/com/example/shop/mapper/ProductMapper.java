
package com.example.shop.mapper;

import com.example.shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ProductMapper {

    /**
     * 新增商品
     * @param product 商品实体
     * @return 插入行数
     */
    int insert(Product product);

    /**
     * 查询所有商品（按创建时间倒序）
     * @return 商品列表
     */
    List<Product> selectAll();

    /**
     * 根据商品ID查询商品
     * @param productId 商品ID
     * @return 商品实体
     */
    Product selectById(Integer productId);

    /**
     * 更新商品信息
     * @param product 商品实体
     * @return 更新行数
     */
    int update(Product product);
}



