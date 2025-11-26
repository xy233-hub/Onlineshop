package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.FavoriteResponse;
import com.example.onlineshop.entity.Favorite;
import com.example.onlineshop.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ProductService productService; // 假设存在，用于校验商品状态/读取价格等

    @Transactional
    public FavoriteResponse addFavorite(Integer customerId, Integer productId) {
        Favorite existing = favoriteMapper.findByCustomerAndProduct(customerId, productId);
        if (existing != null) {
            // 查询并返回带商品信息的响应（复用 listByCustomer 查询第1条）
            List<FavoriteResponse> list = favoriteMapper.listByCustomer(customerId, 1, 0);
            for (FavoriteResponse fr : list) {
                if (fr.getProductId().equals(productId)) return fr;
            }
            // 如果未通过上面查到（理论不会），构造简要响应
            FavoriteResponse r = new FavoriteResponse();
            r.setFavoriteId(existing.getFavoriteId());
            r.setCustomerId(existing.getCustomerId());
            r.setProductId(existing.getProductId());
            r.setFavoritedAt(existing.getCreatedAt());
            return r;
        }

        // 校验商品存在且状态为 online
        if (!productService.getProductDetail(productId).getStatus().equals("online")) {
            throw new IllegalStateException("商品不存在或已下架");
        }

        Favorite favorite = new Favorite();
        favorite.setCustomerId(customerId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);

        // 查询并返回带商品信息的记录
        List<FavoriteResponse> inserted = favoriteMapper.listByCustomer(customerId, 1, 0);
        for (FavoriteResponse fr : inserted) {
            if (fr.getProductId().equals(productId)) return fr;
        }
        // fallback
        FavoriteResponse r = new FavoriteResponse();
        r.setFavoriteId(favorite.getFavoriteId());
        r.setCustomerId(customerId);
        r.setProductId(productId);
        r.setFavoritedAt(LocalDateTime.now());
        return r;
    }

    public List<FavoriteResponse> listFavorites(Integer customerId, int page, int size) {
        int limit = size;
        int offset = (page - 1) * size;
        return favoriteMapper.listByCustomer(customerId, limit, offset);
    }

    @Transactional
    public boolean deleteFavorite(Integer customerId, Integer favoriteId) {
        Integer found = favoriteMapper.findIdByIdAndCustomer(favoriteId, customerId);
        if (found == null) return false;
        int rows = favoriteMapper.deleteById(favoriteId);
        return rows > 0;
    }
}
