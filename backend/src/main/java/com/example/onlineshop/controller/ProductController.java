// backend/src/main/java/com/example/onlineshop/controller/ProductController.java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.PurchaseRequest;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    // 获取当前在售商品
    @GetMapping("")
    public Object getOnlineProducts() {
        try {
            List<Product> products = productService.getOnlineProducts();
            if (products.isEmpty()) {
                return ResponseUtil.custom(404, "商品不存在", null);
            }
            ProductInfoResponse resp = new ProductInfoResponse(products.get(0));
            return ResponseUtil.success("获取成功", resp);
        } catch (Exception e) {
            return ResponseUtil.error("获取商品失败");
        }
    }


    // 提交购买意向
    @PostMapping("/purchase")
    public Object submitPurchaseIntent(@RequestBody PurchaseRequest request) {
        try {
            Product product = productService.getOnlineProducts().stream().findFirst().orElse(null);
            if (product == null) {
                return ResponseUtil.error("暂无在售商品");
            }
            if (!"online".equals(product.getProductStatus())) {
                return ResponseUtil.error("商品当前不可购买");
            }
            PurchaseIntent purchaseIntent = new PurchaseIntent();
            purchaseIntent.setProduct_id(product.getProductId());
            purchaseIntent.setCustomer_name(request.getCustomer_name());
            purchaseIntent.setCustomer_phone(request.getCustomer_phone());
            purchaseIntent.setCustomer_address(request.getCustomer_address());
            purchaseIntent.setPurchase_status("pending");
            if (purchaseIntentService.createPurchaseIntent(purchaseIntent)) {
                return ResponseUtil.success("提交成功", purchaseIntent);
            } else {
                return ResponseUtil.error("提交失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error("提交失败: " + e.getMessage());
        }
    }
}

