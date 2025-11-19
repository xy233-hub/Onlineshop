package com.example.onlineshop.service;
import com.example.onlineshop.dto.request.ImageRequest;
import com.example.onlineshop.dto.request.MediaResourceRequest;
import com.example.onlineshop.dto.request.ProductRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.dto.response.TokenResponse;
import com.example.onlineshop.entity.MediaResource;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.ProductImage;
import com.example.onlineshop.entity.Seller;
import com.example.onlineshop.mapper.MediaResourceMapper;
import com.example.onlineshop.mapper.ProductImageMapper;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.mapper.SellerMapper;
import com.example.onlineshop.util.JwtUtil;
import com.example.onlineshop.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SellerService {

    @Autowired
    private SellerMapper sellerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductImageMapper productImageMapper; // 新增：用于插入图片表

    @Autowired
    private MediaResourceMapper mediaResourceMapper; // 新增：用于插入媒体表

    /**
     * 卖家登录
     */
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

    /**
     * 修改密码
     */
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
    @Transactional
    public ApiResponse publishProduct(ProductRequest request) {
        // 基础校验
        if (request.getProductName() == null || request.getProductName().isBlank()) {
            return new ApiResponse(400, "product_name 必填", null);
        }
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            return new ApiResponse(400, "price 不合法", null);
        }
        if (request.getCategoryId() == null) {
            return new ApiResponse(400, "category_id 必填", null);
        }

        Integer sellerId = 1; // TODO: 从上下文获取真实 sellerId

        Integer stock = request.getStockQuantity() == null ? 0 : request.getStockQuantity();
        String status = stock > 0 ? "frozen" : "outOfStock";

        Product product = Product.builder()
                .sellerId(sellerId)
                .categoryId(request.getCategoryId())
                .productName(request.getProductName())
                .productDesc(request.getProductDesc())
                .price(request.getPrice())
                .stockQuantity(stock)
                .productStatus(status)
                .searchKeywords(request.getSearchKeywords())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productMapper.insert(product); // 回填 productId
        Integer productId = product.getProductId();

        // 处理 images（ImageRequest -> 插入 product_images，并收集 URL 列表回写 product）
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            int orderCounter = 0;
            for (ImageRequest imgReq : request.getImages()) {
                if (imgReq == null) continue;
                // 若带 tempKey 则在此跳过，后续由 mediaService.associateTemporaryToProduct 关联
                if (imgReq.getTempKey() != null && !imgReq.getTempKey().isBlank()) {
                    continue;
                }
                String url = imgReq.getImageUrl();
                if (url == null || url.isBlank()) continue;
                int imageOrder = imgReq.getImageOrder() == null ? orderCounter++ : imgReq.getImageOrder();
                ProductImage img = ProductImage.builder()
                        .productId(productId)
                        .imageUrl(url)
                        .imageOrder(imageOrder)
                        .createdAt(LocalDateTime.now())
                        .build();
                productImageMapper.insert(img);
            }
            List<String> urls = request.getImages().stream()
                    .filter(ir -> ir != null && ir.getImageUrl() != null && !ir.getImageUrl().isBlank())
                    .map(ImageRequest::getImageUrl)
                    .collect(Collectors.toList());
            product.setImages(urls);
        }

        // 插入媒体资源表：若媒体项含 temp_key 则跳过（不在此处入库）
        if (request.getMediaResources() != null && !request.getMediaResources().isEmpty()) {
            int displayOrderCounter = 0;
            for (MediaResourceRequest mrReq : request.getMediaResources()) {
                if (mrReq == null) continue;

                // 若整项为空（既无 temp_key 也无任何元数据），跳过（兼容前端传空对象）
                boolean allEmpty = ((mrReq.getTempKey() == null || mrReq.getTempKey().isBlank())
                        && (mrReq.getMediaUrl() == null || mrReq.getMediaUrl().isBlank())
                        && (mrReq.getFileName() == null || mrReq.getFileName().isBlank())
                        && mrReq.getFileSize() == null
                        && (mrReq.getMimeType() == null || mrReq.getMimeType().isBlank()));
                if (allEmpty) continue;

                // 若有 temp_key，则跳过在此入库（临时文件会在 Controller 层关联后由 MediaService 入库）
                if (mrReq.getTempKey() != null && !mrReq.getTempKey().isBlank()) {
                    continue;
                }

                // 没有 temp_key 的情况下必须提供完整元数据
                if (mrReq.getMediaUrl() == null || mrReq.getMediaUrl().isBlank()
                        || mrReq.getFileName() == null || mrReq.getFileName().isBlank()
                        || mrReq.getFileSize() == null
                        || mrReq.getMimeType() == null || mrReq.getMimeType().isBlank()) {
                    return new ApiResponse(400, "media_resources 中缺少必填元数据（media_url/file_name/file_size/mime_type）", null);
                }

                String mediaType = mrReq.getMediaType() == null ? "image" : mrReq.getMediaType();
                Integer displayOrder = mrReq.getDisplayOrder() == null ? displayOrderCounter++ : mrReq.getDisplayOrder();
                Boolean isEmbedded = mrReq.getIsEmbedded() == null ? false : mrReq.getIsEmbedded();

                MediaResource mr = MediaResource.builder()
                        .productId(productId)
                        .mediaType(mediaType)
                        .mediaUrl(mrReq.getMediaUrl())
                        .fileName(mrReq.getFileName())
                        .fileSize(mrReq.getFileSize())
                        .mimeType(mrReq.getMimeType())
                        .displayOrder(displayOrder)
                        .isEmbedded(isEmbedded)
                        .createdAt(LocalDateTime.now())
                        .build();
                mediaResourceMapper.insert(mr);
            }
            product.setMediaResources(request.getMediaResources().stream()
                    .filter(m -> m != null && m.getMediaUrl() != null && !m.getMediaUrl().isBlank())
                    .map(MediaResourceRequest::getMediaUrl)
                    .collect(Collectors.toList()));
        }

        return new ApiResponse(200, "发布成功", new ProductInfoResponse(product));
    }


    /**
     * 冻结指定商品
     */
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
    // java
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
