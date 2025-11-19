package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Category;
import com.example.onlineshop.entity.MediaResource;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.ProductImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    // 统计某个类别下的商品数量（用于删除类别前的校验）
    int countByCategoryId(@Param("categoryId") Integer categoryId);

    // 新增：原子扣减库存并在库存为0时设置 product_status='outOfStock'
    int deductStockIfEnough(@Param("productId") Integer productId,
                            @Param("deduct") Integer deduct,
                            @Param("now") LocalDateTime now);
    // 用于分页/搜索
    List<Product> selectProducts(@Param("q") String q,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("status") String status,
                                 @Param("offset") int offset,
                                 @Param("size") int size,
                                 @Param("sortBy") String sortBy,
                                 @Param("order") String order);

    int countProducts(@Param("q") String q,
                      @Param("categoryId") Integer categoryId,
                      @Param("status") String status);

    // --- 新增：用于单个商品详情组装的查询 ---
    Product selectProductById(@Param("productId") Integer productId);

    List<ProductImage> selectImagesByProductId(@Param("productId") Integer productId);

    List<MediaResource> selectMediaByProductId(@Param("productId") Integer productId);

    Category selectCategoryById(@Param("categoryId") Integer categoryId);
}
