package com.example.shop.service.impl;

import com.example.shop.dto.response.TokenResponse;
import com.example.shop.entity.Seller;
import com.example.shop.mapper.SellerMapper;
import com.example.shop.service.SellerService;
import com.example.shop.util.JwtUtil;
import com.example.shop.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerServiceImpl implements SellerService {

    @Autowired
    private SellerMapper sellerMapper;

    @Override
    public TokenResponse login(String username, String password) {
        // 1. 查询卖家信息
        Seller seller = sellerMapper.selectByUsername(username);
        if (seller == null) {
            throw new IllegalArgumentException("用户名不存在");
        }

        // 2. 验证密码（明文与BCrypt密文比对）
        if (!PasswordUtil.matches(password, seller.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        // 3. 生成JWT Token
        String token = JwtUtil.generateToken(seller.getSeller_Id(), username);

        // 4. 返回Token信息
        return new TokenResponse(token, seller.getSeller_Id(), username);
    }

    @Override
    @Transactional  // 事务保证：密码更新失败时回滚
    public void updatePassword(Integer sellerId, String oldPassword, String newPassword) {
        // 1. 查询卖家信息（验证ID有效性）
        Seller seller = sellerMapper.selectById(sellerId);
        if (seller == null) {
            throw new IllegalArgumentException("卖家不存在");
        }

        // 2. 验证旧密码
        if (!PasswordUtil.matches(oldPassword, seller.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        // 3. 加密新密码（BCrypt加密）
        String encryptedNewPassword = PasswordUtil.encrypt(newPassword);

        // 4. 更新密码
        int rows = sellerMapper.updatePassword(sellerId, encryptedNewPassword);
        if (rows != 1) {
            throw new RuntimeException("密码更新失败，请重试");
        }
    }
}
