package com.example.onlineshop.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 客户注册请求参数
 */
@Data
public class CustomerRegisterRequest {

    @JsonProperty("username")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @JsonProperty("password")
    @NotBlank(message = "密码不能为空")
    private String password;

    @JsonProperty("phone")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @JsonProperty("default_address")
    private String default_address;
}
