// src/test/java/com/ecommerce/utils/TestDataUtils.java
package com.ecommerce.utils;

import com.github.javafaker.Faker;
import java.util.HashMap;
import java.util.Map;

public class TestDataUtils {
    private static final Faker faker = new Faker();
    
    public static Map<String, Object> getTestAddress() {
        Map<String, Object> address = new HashMap<>();
        address.put("recipient_name", "张三");
        address.put("recipient_phone", "13800138000");
        address.put("province", "北京市");
        address.put("city", "北京市");
        address.put("district", "朝阳区");
        address.put("detail_address", "");
        address.put("is_default", false);
        return address;
    }
    
    public static Map<String, Object> getTestPayment() {
        Map<String, Object> payment = new HashMap<>();
        payment.put("paymentMethod", "alipay");
        payment.put("amount", 99.99);
        return payment;
    }
    
    public static Map<String, Object> getTestAfterSale() {
        Map<String, Object> afterSale = new HashMap<>();
        afterSale.put("serviceTitle", "质量问题");
        afterSale.put("problemDescription", "屏幕裂了");
        afterSale.put("serviceType", "REFUND_ONLY");
        afterSale.put("refundAmount", 99.99);
        return afterSale;
    }
    
   public static Map<String, Object> getTestOrder() {
    Map<String, Object> order = new HashMap<>();
    order.put("product_id", 1);    
    order.put("quantity", 1);
    order.put("customer_id", 4);    
    return order;
 }
}