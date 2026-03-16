package com.example.onlineshop.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 客户登录请求参数
 */
@Data
public class CustomerLoginRequest {

    @JsonProperty("username")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @JsonProperty("password")
    @NotBlank(message = "密码不能为空")
    private String password;
}
