// service/SellerService.java
package com.example.onlineshop.service;

import com.example.onlineshop.entity.Seller;
import com.example.onlineshop.mapper.SellerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SellerService {

    @Autowired
    private SellerMapper sellerMapper;

    public Seller findByUsername(String username) {
        return sellerMapper.findByUsername(username);
    }

    public Seller findById(Integer sellerId) {
        return sellerMapper.findById(sellerId);
    }

    public boolean updatePassword(Integer sellerId, String newPassword) {
        return sellerMapper.updatePassword(sellerId, newPassword) > 0;
    }
}
