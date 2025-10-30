// language: java
package com.example.onlineshop.dto.request;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
    @JsonProperty("product_name")
    @NotBlank(message = "product_name 必填")
    private String productName;

    @JsonProperty("product_desc")
    @NotBlank(message = "product_desc 必填")
    private String productDesc;

    @JsonProperty("category_id")
    @NotNull(message = "category_id 必填")
    private Integer categoryId;

    @JsonProperty("price")
    @NotNull(message = "price 必填")
    @DecimalMin(value = "0.0", inclusive = true, message = "price 必须 >= 0")
    private BigDecimal price;

    @JsonProperty("stock_quantity")
    private Integer stockQuantity;

    @JsonProperty("search_keywords")
    private String searchKeywords;

    @JsonProperty("images")
    @Valid
    private List<ImageRequest> images; // 现在接收 {image_url,image_order} 对象列表

    @JsonProperty("media_resources")
    @Valid
    private List<MediaResourceRequest> mediaResources;
}



