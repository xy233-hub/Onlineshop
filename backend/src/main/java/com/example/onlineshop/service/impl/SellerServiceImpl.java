package com.example.onlineshop.service.impl;

import com.example.onlineshop.dto.request.ProductRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.dto.response.TokenResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.ProductImage;
import com.example.onlineshop.entity.MediaResource;
import com.example.onlineshop.entity.Seller;
import com.example.onlineshop.mapper.ProductImageMapper;
import com.example.onlineshop.mapper.MediaResourceMapper;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.mapper.SellerMapper;
import com.example.onlineshop.service.SellerService;
import com.example.onlineshop.util.JwtUtil;
import com.example.onlineshop.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SellerServiceImpl implements SellerService {

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper;

    @Autowired
    private MediaResourceMapper mediaResourceMapper;

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

    // ... existing code ...
    /**
     * 发布新商品
     */
    @Override
    @Transactional
    public ApiResponse publishProduct(ProductRequest request) {
        Integer sellerId = 1; // 从token中获取
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setCategoryId(request.getCategoryId());
        product.setProductName(request.getProductName());
        product.setProductDesc(request.getProductDesc());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setSearchKeywords(request.getSearchKeywords());

        // 根据库存设置商品状态
        if (product.getStockQuantity() > 0) {
            product.setProductStatus("online");
        } else {
            product.setProductStatus("outOfStock");
        }

        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // 插入商品
        productMapper.insert(product);

        // 插入商品图片
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                ProductImage image = new ProductImage();
                image.setProductId(product.getProductId());
                image.setImageUrl(request.getImages().get(i).getImageUrl());
                image.setImageOrder(request.getImages().get(i).getImageOrder() != null
                        ? request.getImages().get(i).getImageOrder() : i);
                image.setCreatedAt(LocalDateTime.now());
                productImageMapper.insert(image);
            }
        }

        // 插入媒体资源
        if (request.getMediaResources() != null && !request.getMediaResources().isEmpty()) {
            for (ProductRequest.MediaResource media : request.getMediaResources()) {
                MediaResource mediaResource = new MediaResource();
                mediaResource.setProductId(product.getProductId());
                mediaResource.setMediaType(media.getMediaType() != null ? media.getMediaType() : "image");
                mediaResource.setMediaUrl(media.getMediaUrl());
                mediaResource.setFileName(media.getFileName());
                mediaResource.setFileSize(media.getFileSize() != null ? media.getFileSize().intValue() : null);
                mediaResource.setMimeType(media.getMimeType());
                mediaResource.setDisplayOrder(media.getDisplayOrder() != null ? media.getDisplayOrder() : 0);
                mediaResource.setIsEmbedded(media.getIsEmbedded() != null ? media.getIsEmbedded() : false);
                mediaResource.setCreatedAt(LocalDateTime.now());
                mediaResourceMapper.insert(mediaResource);
            }
        }

        return new ApiResponse(200, "发布成功", product);
    }
// ... existing code ...

    /**
     * 批量发布商品
     */
    @Override
    @Transactional
    public ApiResponse publishProducts(List<ProductRequest> requests) {
        // 检查请求是否为空
        if (requests == null || requests.isEmpty()) {
            return new ApiResponse(400, "请求数据不能为空", null);
        }

        // 批量发布商品
        List<Product> products = requests.stream()
                .map(this::createProductFromRequest)
                .collect(Collectors.toList());

        return new ApiResponse(200, "批量发布成功", products);
    }

// ... existing code ...
    private Product createProductFromRequest(ProductRequest request) {
        Integer sellerId = 1; // 从token中获取
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setCategoryId(request.getCategoryId());
        product.setProductName(request.getProductName());
        product.setProductDesc(request.getProductDesc());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        product.setSearchKeywords(request.getSearchKeywords());

        // 根据库存设置商品状态
        if (product.getStockQuantity() > 0) {
            product.setProductStatus("online");
        } else {
            product.setProductStatus("outOfStock");
        }

        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // 插入商品
        productMapper.insert(product);

        // 插入商品图片
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                ProductImage image = new ProductImage();
                image.setProductId(product.getProductId());
                image.setImageUrl(request.getImages().get(i).getImageUrl());
                image.setImageOrder(request.getImages().get(i).getImageOrder() != null
                        ? request.getImages().get(i).getImageOrder() : i);
                image.setCreatedAt(LocalDateTime.now());
                productImageMapper.insert(image);
            }
        }

        // 插入媒体资源
        if (request.getMediaResources() != null && !request.getMediaResources().isEmpty()) {
            for (ProductRequest.MediaResource media : request.getMediaResources()) {
                MediaResource mediaResource = new MediaResource();
                mediaResource.setProductId(product.getProductId());
                mediaResource.setMediaType(media.getMediaType() != null ? media.getMediaType() : "image");
                mediaResource.setMediaUrl(media.getMediaUrl());
                mediaResource.setFileName(media.getFileName());
                mediaResource.setFileSize(media.getFileSize() != null ? media.getFileSize().intValue() : null);
                mediaResource.setMimeType(media.getMimeType());
                mediaResource.setDisplayOrder(media.getDisplayOrder() != null ? media.getDisplayOrder() : 0);
                mediaResource.setIsEmbedded(media.getIsEmbedded() != null ? media.getIsEmbedded() : false);
                mediaResource.setCreatedAt(LocalDateTime.now());
                mediaResourceMapper.insert(mediaResource);
            }
        }

        return product;
    }
// ... existing code ...

    /**
     * 查看历史商品列表
     */
    @Override
    public ApiResponse listProducts(Map<String, Object> params) {
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

        return new ApiResponse(200, "查询成功", result);
    }

    /**
     * 冻结指定商品
     */
    @Override
    @Transactional
    public ApiResponse freezeProduct(Long productId, String reason) {
        Product product = productMapper.findById(productId.intValue());
        if (product == null) {
            return new ApiResponse(404, "商品不存在", null);
        }
        product.setProductStatus("frozen");
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.update(product);

        // 添加备注到sellerNotes字段（如果有）
        if (reason != null && !reason.isEmpty()) {
            // 这里可以将原因保存到某个地方，比如商品备注字段
        }

        return new ApiResponse(200, "冻结成功", product);
    }

    /**
     * 恢复商品上线
     */
    @Override
    @Transactional
    public ApiResponse unfreezeProduct(Long productId, String remark) {
        Product product = productMapper.findById(productId.intValue());
        if (product == null) {
            return new ApiResponse(404, "商品不存在", null);
        }

        // 根据库存确定状态
        if (product.getStockQuantity() > 0) {
            product.setProductStatus("online");
        } else {
            product.setProductStatus("outOfStock");
        }

        product.setUpdatedAt(LocalDateTime.now());
        productMapper.update(product);

        return new ApiResponse(200, "恢复成功", product);
    }

    /**
     * 标记商品为已售出
     */
    @Override
    @Transactional
    public ApiResponse markProductSold(Long productId, Integer soldQuantity, String note) {
        Product product = productMapper.findById(productId.intValue());
        if (product == null) {
            return new ApiResponse(404, "商品不存在", null);
        }

        // 默认售出数量为1
        int quantity = soldQuantity != null ? soldQuantity : 1;

        // 检查库存是否足够
        if (product.getStockQuantity() < quantity) {
            return new ApiResponse(400, "库存不足", null);
        }

        // 更新库存
        product.setStockQuantity(product.getStockQuantity() - quantity);

        // 根据剩余库存确定状态
        if (product.getStockQuantity() <= 0) {
            product.setProductStatus("sold");
        } else {
            product.setProductStatus("outOfStock");
        }

        product.setUpdatedAt(LocalDateTime.now());
        productMapper.update(product);

        return new ApiResponse(200, "标记售出成功", product);
    }

    /**
     * 为商品加载图片和媒体资源
     */
    private void loadProductImagesAndMedia(Product product) {
        // 加载商品图片
        List<ProductImage> images = productImageMapper.findByProductId(product.getProductId());
        product.setImages(images.stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList()));

        // 加载媒体资源
        List<MediaResource> mediaResources = mediaResourceMapper.findByProductId(product.getProductId());
        product.setMediaResources(mediaResources.stream()
                .map(MediaResource::getMediaUrl)
                .collect(Collectors.toList()));
    }
}
