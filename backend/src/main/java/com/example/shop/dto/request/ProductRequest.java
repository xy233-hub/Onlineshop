package com.example.shop.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * 商品发布请求参数
 */
@Data
public class ProductRequest {

    @JsonProperty("product_name")
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @JsonProperty("product_desc")
    @NotBlank(message = "商品描述不能为空")
    private String productDesc;

    @JsonProperty("image_url")
    @NotBlank(message = "商品图片链接不能为空")
    private String imageUrl;

    @JsonProperty("price")
    @NotNull(message = "商品价格不能为空")
    @PositiveOrZero(message = "商品价格不能为负数")
    private BigDecimal price;
}

