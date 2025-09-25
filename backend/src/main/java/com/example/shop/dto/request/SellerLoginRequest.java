package com.example.shop.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;  // 正确包路径

@Data
public class SellerLoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}