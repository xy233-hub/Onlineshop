// backend/src/main/java/com/example/onlineshop/controller/ProductController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.PurchaseIntentRequest;
import com.example.onlineshop.dto.request.PurchaseRequest;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    /**
     * 搜索/分页/排序获取商品列表
     * 支持参数：q, category_id, status, page, size, sort_by, order
     * 若未传 status，默认只返回 online 商品
     */
    @GetMapping("")
    public Object listProducts(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category_id", required = false) Integer categoryId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort_by", required = false) String sortBy,
            @RequestParam(value = "order", defaultValue = "desc") String order
    ) {
        try {
            if (page == null || page < 1) page = 1;
            if (size == null || size < 1) size = 10;
            int offset = (page - 1) * size;

            // 如果调用方没有显式传 status，默认只查询在线商品
            if (status == null || status.trim().isEmpty()) {
                status = "online";
            }

            List<Product> products = productService.searchProducts(q, categoryId, status, offset, size, sortBy, order);
            int total = productService.countProducts(q, categoryId, status);

            List<ProductInfoResponse> items = products.stream()
                    .map(ProductInfoResponse::new)
                    .collect(Collectors.toList());

            HashMap<String, Object> data = new HashMap<>();
            data.put("page", page);
            data.put("size", size);
            data.put("total", total);
            data.put("items", items);

            return ResponseUtil.success("查询成功", data);
        } catch (IllegalArgumentException iae) {
            return ResponseUtil.custom(400, iae.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }


    @GetMapping("/{product_id}")
    public Object getProductDetail(@PathVariable("product_id") Integer productId) {
        try {
            if (productId == null) {
                return ResponseUtil.custom(400, "product_id 必填", null);
            }
            com.example.onlineshop.dto.response.ProductDetailResponse detail =
                    productService.getProductDetail(productId);
            if (detail == null) {
                return ResponseUtil.custom(404, "商品不存在", null);
            }
            return ResponseUtil.success("查询成功", detail);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // 提交购买意向
    @PostMapping("/purchase-intents")
    public Object createPurchaseIntent(@RequestBody PurchaseIntentRequest req) {
        try {
            if (req.getProductId() == null) {
                return ResponseUtil.custom(400, "product_id 必填", null);
            }
            Integer qty = req.getQuantity() == null ? 1 : req.getQuantity();
            if (qty <= 0) {
                return ResponseUtil.custom(400, "quantity 必须大于 0", null);
            }
            if (req.getCustomerId() == null) {
                return ResponseUtil.custom(400, "customer_id 必填", null);
            }

            PurchaseIntent created = purchaseIntentService.createPurchaseIntent(req);
            if (created == null) {
                return ResponseUtil.error("提交失败");
            }
            return ResponseUtil.success("提交成功", created);
        } catch (IllegalArgumentException iae) {
            return ResponseUtil.custom(400, iae.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("提交失败: " + e.getMessage());
        }
    }
}

