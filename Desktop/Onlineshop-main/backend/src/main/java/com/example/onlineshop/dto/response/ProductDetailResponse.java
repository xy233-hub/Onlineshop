// java
// 文件：`backend/src/main/java/com/example/onlineshop/dto/response/ProductDetailResponse.java`
package com.example.onlineshop.dto.response;

import com.example.onlineshop.entity.Category;
import com.example.onlineshop.entity.MediaResource;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.ProductImage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ProductDetailResponse {
    public Integer product_id;
    public Integer seller_id;
    public Integer category_id;
    public String product_name;
    public String product_desc;
    public List<ProductImageItem> images;
    public List<MediaItem> media_resources;
    public Double price;
    public Integer stock_quantity;
    public String product_status;
    public String search_keywords;
    public CategoryItem category;
    public String created_at;
    public String updated_at;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProductDetailResponse(Product p,
                                 List<ProductImage> imgs,
                                 List<MediaResource> medias,
                                 Category cat) {
        if (p == null) return;
        this.product_id = p.getProductId();
        this.seller_id = p.getSellerId();
        this.category_id = p.getCategoryId();
        this.product_name = p.getProductName();
        this.product_desc = p.getProductDesc();
        this.price = p.getPrice() == null ? null : p.getPrice().doubleValue();
        this.stock_quantity = p.getStockQuantity();
        this.product_status = p.getProductStatus();
        this.search_keywords = p.getSearchKeywords();
        this.created_at = p.getCreatedAt() == null ? null : p.getCreatedAt().format(FMT);
        this.updated_at = p.getUpdatedAt() == null ? null : p.getUpdatedAt().format(FMT);

        this.images = imgs == null ? null :
                imgs.stream().map(ProductImageItem::new).collect(Collectors.toList());
        this.media_resources = medias == null ? null :
                medias.stream().map(MediaItem::new).collect(Collectors.toList());
        this.category = cat == null ? null : new CategoryItem(cat);
    }

    public static class ProductImageItem {
        public Integer image_id;
        public String image_url;
        public Integer image_order;
        public ProductImageItem(ProductImage img) {
            this.image_id = img.getImageId();
            this.image_url = img.getImageUrl();
            this.image_order = img.getImageOrder();
        }
    }

    public static class MediaItem {
        public Integer media_id;
        public String media_type;
        public String media_url;
        public String file_name;
        public Long file_size;
        public String mime_type;
        public Integer display_order;
        public Boolean is_embedded;
        public MediaItem(MediaResource m) {
            this.media_id = m.getMediaId();
            this.media_type = m.getMediaType();
            this.media_url = m.getMediaUrl();
            this.file_name = m.getFileName();
            this.file_size = m.getFileSize() == null ? null : m.getFileSize().longValue();
            this.mime_type = m.getMimeType();
            this.display_order = m.getDisplayOrder();
            this.is_embedded = m.getIsEmbedded();
        }
    }

    public static class CategoryItem {
        public Integer category_id;
        public Integer parent_id;
        public String category_name;
        public CategoryItem(Category c) {
            this.category_id = c.getCategoryId();
            this.parent_id = c.getParentId();
            this.category_name = c.getCategoryName();
        }
    }
}
