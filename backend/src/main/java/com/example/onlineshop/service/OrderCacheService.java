package com.example.onlineshop.service;

import com.example.onlineshop.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 下单功能缓存服务
 * 用于缓存商品信息和促销规则，减少数据库查询
 */
@Service
public class OrderCacheService {

    private static final Logger log = LoggerFactory.getLogger(OrderCacheService.class);

    // 缓存 Key 前缀
    private static final String PRODUCT_CACHE_PREFIX = "order:product:";
    private static final String PRODUCT_STOCK_PREFIX = "order:stock:";
    private static final String PROMOTION_CACHE_PREFIX = "order:promotion:";
    
    // 缓存过期时间
    private static final long PRODUCT_CACHE_TTL = 10; // 商品信息缓存10分钟
    private static final long STOCK_CACHE_TTL = 5;    // 库存缓存5分钟
    private static final long PROMOTION_CACHE_TTL = 5; // 促销规则缓存5分钟

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private PromotionService promotionService;

    /**
     * 获取商品信息（优先从缓存获取）
     */
    public Product getProduct(Integer productId) {
        if (productId == null) return null;
        
        String key = PRODUCT_CACHE_PREFIX + productId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null && cached instanceof Product) {
                log.debug("从缓存获取商品信息: productId={}", productId);
                return (Product) cached;
            }
        } catch (Exception e) {
            log.warn("Redis获取商品缓存失败: productId={}, error={}", productId, e.getMessage());
        }
        
        // 缓存不存在，从数据库查询
        Product product = productService.getProductById(productId);
        if (product != null) {
            try {
                redisTemplate.opsForValue().set(key, product, PRODUCT_CACHE_TTL, TimeUnit.MINUTES);
                log.debug("商品信息已缓存: productId={}", productId);
            } catch (Exception e) {
                log.warn("Redis缓存商品失败: productId={}, error={}", productId, e.getMessage());
            }
        }
        
        return product;
    }

    /**
     * 批量获取商品信息（优先从缓存获取）
     */
    public Map<Integer, Product> batchGetProducts(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Integer, Product> result = new HashMap<>();
        List<Integer> missedIds = new ArrayList<>();
        
        // 先从缓存批量获取
        List<String> keys = productIds.stream()
                .map(id -> PRODUCT_CACHE_PREFIX + id)
                .collect(Collectors.toList());
        
        try {
            List<Object> cachedList = redisTemplate.opsForValue().multiGet(keys);
            if (cachedList != null) {
                for (int i = 0; i < cachedList.size(); i++) {
                    Object cached = cachedList.get(i);
                    if (cached != null && cached instanceof Product) {
                        result.put(productIds.get(i), (Product) cached);
                    } else {
                        missedIds.add(productIds.get(i));
                    }
                }
            }
            log.debug("批量获取商品缓存: 总数={}, 缓存命中={}, 缓存未命中={}", 
                    productIds.size(), result.size(), missedIds.size());
        } catch (Exception e) {
            log.warn("Redis批量获取商品缓存失败: error={}", e.getMessage());
            missedIds = productIds; // 全部从数据库查询
        }
        
        // 缓存未命中的从数据库查询
        if (!missedIds.isEmpty()) {
            for (Integer productId : missedIds) {
                Product product = productService.getProductById(productId);
                if (product != null) {
                    result.put(productId, product);
                    
                    // 写入缓存
                    try {
                        redisTemplate.opsForValue().set(
                                PRODUCT_CACHE_PREFIX + productId, 
                                product, 
                                PRODUCT_CACHE_TTL, 
                                TimeUnit.MINUTES);
                    } catch (Exception ex) {
                        log.warn("Redis缓存商品失败: productId={}", productId);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 获取商品库存（优先从缓存获取）
     */
    public Integer getProductStock(Integer productId) {
        if (productId == null) return 0;
        
        String key = PRODUCT_STOCK_PREFIX + productId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null && cached instanceof Integer) {
                log.debug("从缓存获取商品库存: productId={}", productId);
                return (Integer) cached;
            }
        } catch (Exception e) {
            log.warn("Redis获取库存缓存失败: productId={}", productId);
        }
        
        // 从数据库查询
        Integer stock = productService.getProductStock(productId);
        
        try {
            redisTemplate.opsForValue().set(key, stock, STOCK_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis缓存库存失败: productId={}", productId);
        }
        
        return stock;
    }

    /**
     * 获取商品促销信息（优先从缓存获取）
     */
    public Map<String, Object> getProductPromotion(Integer productId) {
        if (productId == null) return null;
        
        String key = PROMOTION_CACHE_PREFIX + productId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null && cached instanceof Map) {
                log.debug("从缓存获取促销信息: productId={}", productId);
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis获取促销缓存失败: productId={}", productId);
        }
        
        // 从数据库查询
        Map<String, Object> promotion = promotionService.productPromotions(productId);
        
        if (promotion != null) {
            try {
                redisTemplate.opsForValue().set(key, promotion, PROMOTION_CACHE_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Redis缓存促销失败: productId={}", productId);
            }
        }
        
        return promotion;
    }

    /**
     * 批量获取商品促销信息（优先从缓存获取）
     */
    public Map<Integer, Map<String, Object>> batchGetPromotions(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Integer, Map<String, Object>> result = new HashMap<>();
        List<Integer> missedIds = new ArrayList<>();
        
        // 先从缓存批量获取
        List<String> keys = productIds.stream()
                .map(id -> PROMOTION_CACHE_PREFIX + id)
                .collect(Collectors.toList());
        
        try {
            List<Object> cachedList = redisTemplate.opsForValue().multiGet(keys);
            if (cachedList != null) {
                for (int i = 0; i < cachedList.size(); i++) {
                    Object cached = cachedList.get(i);
                    if (cached != null && cached instanceof Map) {
                        result.put(productIds.get(i), (Map<String, Object>) cached);
                    } else {
                        missedIds.add(productIds.get(i));
                    }
                }
            }
            log.debug("批量获取促销缓存: 总数={}, 缓存命中={}, 缓存未命中={}", 
                    productIds.size(), result.size(), missedIds.size());
        } catch (Exception e) {
            log.warn("Redis批量获取促销缓存失败: error={}", e.getMessage());
            missedIds = productIds;
        }
        
        // 缓存未命中的从数据库查询
        if (!missedIds.isEmpty()) {
            for (Integer productId : missedIds) {
                Map<String, Object> promotion = promotionService.productPromotions(productId);
                if (promotion != null) {
                    result.put(productId, promotion);
                    
                    try {
                        redisTemplate.opsForValue().set(
                                PROMOTION_CACHE_PREFIX + productId, 
                                promotion, 
                                PROMOTION_CACHE_TTL, 
                                TimeUnit.MINUTES);
                    } catch (Exception ex) {
                        log.warn("Redis缓存促销失败: productId={}", productId);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 清除商品缓存（商品信息变更时调用）
     */
    public void evictProductCache(Integer productId) {
        if (productId == null) return;
        
        try {
            redisTemplate.delete(PRODUCT_CACHE_PREFIX + productId);
            redisTemplate.delete(PRODUCT_STOCK_PREFIX + productId);
            redisTemplate.delete(PROMOTION_CACHE_PREFIX + productId);
            log.debug("清除商品缓存: productId={}", productId);
        } catch (Exception e) {
            log.warn("清除商品缓存失败: productId={}", productId);
        }
    }

    /**
     * 清除所有商品缓存
     */
    public void evictAllProductCache() {
        try {
            Set<String> keys = redisTemplate.keys(PRODUCT_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            keys = redisTemplate.keys(PRODUCT_STOCK_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            keys = redisTemplate.keys(PROMOTION_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            log.debug("清除所有商品缓存");
        } catch (Exception e) {
            log.warn("清除所有商品缓存失败: error={}", e.getMessage());
        }
    }
}