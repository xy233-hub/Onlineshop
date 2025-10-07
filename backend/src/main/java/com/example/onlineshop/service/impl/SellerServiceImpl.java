package com.example.onlineshop.service.impl;

import com.example.onlineshop.dto.request.ProductRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.dto.response.TokenResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.Seller;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.mapper.SellerMapper;
import com.example.onlineshop.service.SellerService;
import com.example.onlineshop.util.JwtUtil;
import com.example.onlineshop.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SellerServiceImpl implements SellerService {

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private ProductMapper productMapper;
    /**
     * 卖家登录
     */
    @Override
    public TokenResponse login(String username, String password) {
        // 1. 查询卖家信息
        Seller seller = sellerMapper.selectByUsername(username);

        if (seller == null) {
            throw new IllegalArgumentException("用户名不存在");
        }

        // 2. 验证密码（明文与BCrypt密文比对）
        if (!PasswordUtil.matches(password, seller.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        // 3. 生成JWT Token
        String token = JwtUtil.generateToken(seller.getSellerId(), username);

        // 4. 返回Token信息
        return new TokenResponse(
                token,
                seller.getSellerId(),
                username,
                seller.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

    }
    @Override
    @Transactional  // 事务保证：密码更新失败时回滚
    public void updatePassword(Integer sellerId, String oldPassword, String newPassword) {
        // 1. 查询卖家信息（验证ID有效性）
        Seller seller = sellerMapper.selectById(sellerId);
        if (seller == null) {
            throw new IllegalArgumentException("卖家不存在");
        }

        // 2. 验证旧密码
        if (!PasswordUtil.matches(oldPassword, seller.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        // 3. 加密新密码（BCrypt加密）
        String encryptedNewPassword = PasswordUtil.encrypt(newPassword);

        // 4. 更新密码
        int rows = sellerMapper.updatePassword(sellerId, encryptedNewPassword);
        if (rows != 1) {
            throw new RuntimeException("密码更新失败，请重试");
        }
    }

    /**
     * 发布新商品
     */
    @Override
    @Transactional
    public ApiResponse publishProduct(ProductRequest request) {
        // 检查是否已有在售商品
        List<Product> onlineProducts = productMapper.selectByStatus("online");
        if (!onlineProducts.isEmpty()) {
            return new ApiResponse(400, "已有商品在售，不能重复发布", null);
        }
        Integer sellerId = 1;
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setProductName(request.getProductName());
        product.setProductDesc(request.getProductDesc());
        product.setImageUrl(request.getImageUrl());
        product.setPrice(request.getPrice());
        product.setProductStatus("online");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(product);
        return new ApiResponse(200, "发布成功", new ProductInfoResponse(product));
    }


    /**
     * 查看历史商品列表
     */
    @Override
    public ApiResponse listProducts() {
        List<Product> products = productMapper.selectAll();
        List<ProductInfoResponse> data = products.stream()
                .map(ProductInfoResponse::new)
                .toList();
        return new ApiResponse(200, "查询成功", data);
    }

    /**
     * 冻结指定商品
     */
    @Override
    @Transactional
    public ApiResponse freezeProduct(Long productId) {
        Product product = productMapper.findById(productId.intValue());
        if (product == null) {
            return new ApiResponse(404, "商品不存在", null);
        }
        product.setProductStatus("frozen");
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.update(product);
        return new ApiResponse(200, "冻结成功", new ProductInfoResponse(product));
    }

    /**
     * 恢复商品上线
     */
    @Override
    @Transactional
    public ApiResponse unfreezeProduct(Long productId) {
        Product product = productMapper.findById(productId.intValue());
        if (product == null) {
            return new ApiResponse(404, "商品不存在", null);
        }
        product.setProductStatus("online");
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.update(product);
        return new ApiResponse(200, "恢复成功", new ProductInfoResponse(product));
    }

    /**
     * 标记商品为已售出
     */
    @Override
    @Transactional
    public ApiResponse markProductSold(Long productId) {
        Product product = productMapper.findById(productId.intValue());
        if (product == null) {
            return new ApiResponse(404, "商品不存在", null);
        }
        product.setProductStatus("sold");
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.update(product);
        return new ApiResponse(200, "标记售出成功", new ProductInfoResponse(product));
    }


}
