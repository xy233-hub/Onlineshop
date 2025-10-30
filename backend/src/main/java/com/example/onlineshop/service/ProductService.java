// service/ProductService.java
package com.example.onlineshop.service;

import com.example.onlineshop.entity.Product;
import com.example.onlineshop.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    public List<Product> getOnlineProducts() {
        return productMapper.findOnlineProducts();
    }

    public Product getProductById(Integer productId) {
        return productMapper.findById(productId);
    }

    public boolean createProduct(Product product) {
        return productMapper.insert(product) > 0;
    }

    public boolean updateProductStatus(Integer productId, String status) {
        return productMapper.updateStatus(productId, status) > 0;
    }

    public List<Product> getHistoryProducts(Integer sellerId) {
        return productMapper.findAllBySellerId(sellerId);
    }

    public Object getProductsByCondition(Map<String, Object> params) {
        // 处理分页参数
        Integer page = (Integer) params.getOrDefault("page", 1);
        Integer size = (Integer) params.getOrDefault("size", 10);
        Integer offset = (page - 1) * size;

        params.put("offset", offset);
        params.put("limit", size);

        List<Product> products = productMapper.findByCondition(params);
        int total = productMapper.countByCondition(params);

        // 为每个商品加载图片和媒体资源
        for (Product product : products) {
            loadProductImagesAndMedia(product);
        }

        Map<String, Object> result = Map.of(
                "page", page,
                "size", size,
                "total", total,
                "items", products
        );

        return result;
    }

    private void loadProductImagesAndMedia(Product product) {
        // 加载商品图片和媒体资源
        // 这里需要调用对应的Mapper方法，但为了简化，暂时留空
    }
}
