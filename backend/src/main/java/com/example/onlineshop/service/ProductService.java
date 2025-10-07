// service/ProductService.java
package com.example.onlineshop.service;

import com.example.onlineshop.entity.Product;
import com.example.onlineshop.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductMapper ProductMapper;

    public List<Product> getOnlineProducts() {
        return ProductMapper.findOnlineProducts();
    }

    public Product getProductById(Integer productId) {
        return ProductMapper.findById(productId);
    }

    public boolean createProduct(Product product) {
        return ProductMapper.insert(product) > 0;
    }

    public boolean updateProductStatus(Integer productId, String status) {
        return ProductMapper.updateStatus(productId, status) > 0;
    }

    public List<Product> getHistoryProducts(Integer sellerId) {
        return ProductMapper.findAllBySellerId(sellerId);
    }
}
