package com.example.shop.entity;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 卖家实体（对应seller表）
 */
@Data
public class Seller {
    private Integer seller_id;      // 主键ID
    private String username;       // 登录用户名
    private String password;       // BCrypt加密后的密码
    private LocalDateTime create_time;  // 账号创建时间
}
