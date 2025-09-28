// controller/PurchaseIntentController.java
package com.example.onlineshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.onlineshop.dto.PurchaseRequestDTO;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.ResponseUtil;

@RestController
@RequestMapping("/api/product")
public class PurchaseIntentController {

    @Autowired
    private PurchaseIntentService purchaseIntentService;

    @Autowired
    private ProductService productService;

    // 提交购买意向
    @PostMapping("/purchase")
    public Object submitPurchaseIntent(@RequestBody PurchaseRequestDTO request) {
        try {
            // 检查是否有在售商品
            Product product = productService.getOnlineProducts().stream().findFirst().orElse(null);
            if (product == null) {
                return ResponseUtil.error("暂无在售商品");
            }

            // 检查商品状态
            if (!"online".equals(product.getProduct_status())) {
                return ResponseUtil.error("商品当前不可购买");
            }

            PurchaseIntent purchaseIntent = new PurchaseIntent();
            purchaseIntent.setProduct_id(product.getProduct_id());
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
