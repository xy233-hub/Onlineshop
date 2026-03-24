// java
// 文件：`backend/src/main/java/com/example/onlineshop/service/ProductService.java`
package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.ProductDetailResponse;
import com.example.onlineshop.entity.MediaResource;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.ProductImage;
import com.example.onlineshop.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;


    public Product getProductById(Integer productId) {
        return productMapper.findById(productId);
    }

    public boolean createProduct(Product product) {
        return productMapper.insert(product) > 0;
    }

    public boolean updateProductStatus(Integer productId, String status) {
        return productMapper.updateStatus(productId, status) > 0;
    }

    // 修改：返回历史商品时同时填充 images 列表（和必要时回填 coverImage）
    public List<Product> getHistoryProducts(Integer sellerId) {
        List<Product> products = productMapper.findAllBySellerId(sellerId);
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        for (Product p : products) {
            if (p == null) continue;
            p.setImages(Collections.emptyList());
        }
        return products;
    }

    /**
     * 查询所有商品（管理员视角）- 带图片信息
     */
    public List<Product> getAllProducts() {
        // 使用 selectAll 查询所有商品，但不包含图片
        List<Product> products = productMapper.selectAll();
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        // 为每个商品加载第一张图片作为 coverImage
        for (Product p : products) {
            if (p == null) continue;
            
            // 尝试从数据库加载第一张图片
            List<ProductImage> images = productMapper.selectImagesByProductId(p.getProductId());
            if (images != null && !images.isEmpty()) {
                // 设置第一张图片为封面图
                p.setCoverImage(images.get(0).getImageUrl());
                // 设置所有图片 URL 列表
                List<String> imageUrls = images.stream()
                        .map(ProductImage::getImageUrl)
                        .collect(Collectors.toList());
                p.setImages(imageUrls);
            } else {
                p.setImages(Collections.emptyList());
            }
        }
        
        return products;
    }

    // 兼容前端分页/搜索接口（可直接被之前的 Controller 调用）
    public List<Product> searchProducts(String q, Integer categoryId, String status, int offset, int size, String sortBy, String order) {
        if (order == null || (!order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc"))) {
            order = "desc";
        }
        String sort = null;
        if ("price".equalsIgnoreCase(sortBy) || "created_at".equalsIgnoreCase(sortBy) || "stock_quantity".equalsIgnoreCase(sortBy)) {
            sort = sortBy;
        }
        return productMapper.selectProducts(q, categoryId, status, offset, size, sort, order);
    }

    public int countProducts(String q, Integer categoryId, String status) {
        return productMapper.countProducts(q, categoryId, status);
    }

    // 仅返回在线商品的详情；非 online 返回 null（Controller 会转换为 404）
    public ProductDetailResponse getProductDetail(Integer productId) {
        if (productId == null) return null;
        Product p = productMapper.selectProductById(productId);
        if (p == null) return null;
        if (p.getProductStatus() == null || !"online".equalsIgnoreCase(p.getProductStatus())) {
            return null;
        }

        List<ProductImage> imgs = productMapper.selectImagesByProductId(productId);
        List<MediaResource> medias = productMapper.selectMediaByProductId(productId);
        com.example.onlineshop.entity.Category cat = productMapper.selectCategoryById(p.getCategoryId());
        return new ProductDetailResponse(p, imgs, medias, cat);
    }

    public boolean isProductOnline(Integer productId) {
        if (productId == null) return false;
        Product p = productMapper.selectProductById(productId);
        if (p == null) return false;
        String status = p.getProductStatus();
        return status != null && "online".equalsIgnoreCase(status.trim());
    }

    public Integer getProductStock(Integer productId) {
        if (productId == null) return 0;
        Product p = productMapper.selectProductById(productId);
        if (p == null) return 0;
        Integer qty = p.getStockQuantity();
        return qty != null ? qty : 0;
    }
    /**
     * 根据卖家 ID 查询商品（支持搜索、分类、状态过滤）
     */
    public List<Product> getProductsBySellerId(Integer sellerId, String q, Integer categoryId, String status, 
                                               int offset, int size, String sortBy, String order) {
        if (order == null || (!order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc"))) {
            order = "desc";
        }
        String sort = null;
        if ("price".equalsIgnoreCase(sortBy) || "created_at".equalsIgnoreCase(sortBy) || "stock_quantity".equalsIgnoreCase(sortBy)) {
            sort = sortBy;
        }
        return productMapper.selectProductsBySellerId(sellerId, q, categoryId, status, offset, size, sort, order);
    }

    /**
     * 统计指定卖家的商品数量（支持搜索、分类、状态过滤）
     */
    public int countProductsBySellerId(Integer sellerId, String q, Integer categoryId, String status) {
        return productMapper.countProductsBySellerId(sellerId, q, categoryId, status);
    }
}
