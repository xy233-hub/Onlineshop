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

}
