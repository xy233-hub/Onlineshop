package com.example.shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 商品ID操作请求参数（如冻结、解冻、标记售出）
 */
@Data
public class ProductIdRequest {

    @JsonProperty("product_id")
    @NotNull(message = "商品ID不能为空")
    @Positive(message = "商品ID必须为正整数")
    private Long productId;
}

