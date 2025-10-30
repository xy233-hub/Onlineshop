package com.example.onlineshop.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品发布请求参数
 */
@Data
public class ProductRequest {

    @JsonProperty("category_id")
    @NotNull(message = "分类ID不能为空")
    private Integer categoryId;

    @JsonProperty("product_name")
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @JsonProperty("product_desc")
    @NotBlank(message = "商品描述不能为空")
    private String productDesc;

    @JsonProperty("price")
    @NotNull(message = "商品价格不能为空")
    @PositiveOrZero(message = "商品价格不能为负数")
    private BigDecimal price;

    @JsonProperty("stock_quantity")
    private Integer stockQuantity;

    @JsonProperty("search_keywords")
    private String searchKeywords;

    private List<Image> images;

    @JsonProperty("media_resources")
    private List<MediaResource> mediaResources;

    @Data
    public static class Image {

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("image_order")
        private Integer imageOrder;

        public String getImageUrl() {
            return imageUrl;
        }

        public Integer getImageOrder() {
            return imageOrder;
        }
    }

    @Data
    public static class MediaResource {

        @JsonProperty("media_type")
        private String mediaType;

        @JsonProperty("media_url")
        private String mediaUrl;

        @JsonProperty("file_name")
        private String fileName;

        @JsonProperty("file_size")
        private Long fileSize;

        @JsonProperty("mime_type")
        private String mimeType;

        @JsonProperty("display_order")
        private Integer displayOrder;

        @JsonProperty("is_embedded")
        private Boolean isEmbedded;

        public String getMediaType() {
            return mediaType;
        }

        public String getMediaUrl() {
            return mediaUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public Boolean getIsEmbedded() {
            return isEmbedded;
        }
    }

    // 显式添加getter方法，确保编译通过
    public Integer getCategoryId() {
        return categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductDesc() {
        return productDesc;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public List<Image> getImages() {
        return images;
    }

    public List<MediaResource> getMediaResources() {
        return mediaResources;
    }
}
