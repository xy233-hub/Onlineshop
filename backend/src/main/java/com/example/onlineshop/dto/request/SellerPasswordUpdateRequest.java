package com.example.onlineshop.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 卖家修改密码请求参数
 */
@Data
public class SellerPasswordUpdateRequest {

    @JsonProperty("old_password")
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @JsonProperty("new_password")
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
            message = "新密码需8-20位，包含字母和数字")
    private String newPassword;
}
