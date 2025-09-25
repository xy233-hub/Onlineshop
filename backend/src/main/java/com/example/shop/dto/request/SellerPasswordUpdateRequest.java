package com.example.shop.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 卖家修改密码请求参数
 */
@Data
public class SellerPasswordUpdateRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;  // 旧密码（明文）

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,20}$",
            message = "新密码需8-20位，包含字母和数字")
    private String newPassword;  // 新密码（明文，带格式校验）
}
