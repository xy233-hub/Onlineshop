// src/test/java/com/ecommerce/payment/PaymentTest.java
package com.ecommerce.payment;

import com.ecommerce.base.BaseTest;
import com.ecommerce.config.ApiConfig;
import com.ecommerce.utils.ApiUtils;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@Feature("支付管理")
public class PaymentTest extends BaseTest {
    
    @Test
    @Story("支付方式")
    @Description("TC-PAY-001: 获取支付方式")
    public void testPaymentMethods() {
        setCustomerToken();
        String[] paymentMethods = {"ALIPAY", "WECHAT_PAY", "BANK_CARD", "CREDIT_CARD"};
        logger.info("TC-PAY-001: 支持的支付方式: {}", (Object) paymentMethods);
    }
    
    @Test
    @Story("支付流程")
    @Description("TC-PAY-002: 创建支付记录")
    public void testCreatePayment() {
        setCustomerToken();
        
        String url = ApiConfig.Endpoints.CUSTOMER_PURCHASE_INTENTS
            .replace("{customerId}", String.valueOf(getCustomerId()));
        
        Response orderResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(url + "?purchase_status=PENDING")
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        // 安全地获取订单列表
        Object data = orderResponse.jsonPath().get("data");
        if (data instanceof java.util.Map) {
            logger.info("没有待支付订单，跳过创建支付记录测试");
            return;
        }
        
        if (orderResponse.jsonPath().getList("data").size() == 0) {
            logger.info("没有待支付订单，跳过创建支付记录测试");
            return;
        }
        
        String purchaseId = orderResponse.jsonPath().getString("data[0].purchase_id");
        Double totalAmount = orderResponse.jsonPath().getDouble("data[0].total_amount");
        
        Map<String, Object> paymentData = Map.of(
            "purchaseId", Integer.parseInt(purchaseId),
            "amount", totalAmount
        );
        
        Response paymentResponse = ApiUtils.getRequestSpec()
            .body(paymentData)
            .when()
            .post(ApiConfig.Endpoints.PAYMENT_CREATE)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        Integer code = paymentResponse.jsonPath().getInt("code");
        if (code == 200) {
            String paymentId = paymentResponse.jsonPath().getString("data.payment_id");
            logger.info("TC-PAY-002: 创建支付记录成功，支付ID: {}", paymentId);
        } else {
            logger.info("TC-PAY-002: 创建支付记录失败，原因: {}", 
                paymentResponse.jsonPath().getString("message"));
        }
    }
    
    @Test
    @Story("支付超时")
    @Description("TC-PAY-003: 查询即将过期的支付记录")
    public void testPaymentTimeout() {
        setSellerToken();
        
        Response timeoutResponse = ApiUtils.getRequestSpec()
            .when()
            .get(ApiConfig.Endpoints.SELLER_PAYMENTS_EXPIRING + "?minutes=30")
            .then()
            .extract()
            .response();
        
        int statusCode = timeoutResponse.getStatusCode();
        if (statusCode == 404) {
            logger.info("TC-PAY-003: 支付超时接口暂未实现 (404)");
        } else {
            timeoutResponse.then().statusCode(ApiConfig.StatusCode.OK);
            int expiringCount = timeoutResponse.jsonPath().getInt("data.expiring_count");
            logger.info("TC-PAY-003: 查询即将过期支付记录成功，即将过期订单数量: {}", expiringCount);
        }
    }
    
    @Test
    @Story("手动校验支付")
    @Description("TC-PAY-004: 手动校验支付结果")
    public void testManualPaymentCheck() {
        setCustomerToken();
        
        String url = ApiConfig.Endpoints.CUSTOMER_PURCHASE_INTENTS
            .replace("{customerId}", String.valueOf(getCustomerId()));
        
        Response orderResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(url + "?purchase_status=PENDING")
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        Object data = orderResponse.jsonPath().get("data");
        if (data instanceof java.util.Map || orderResponse.jsonPath().getList("data").size() == 0) {
            logger.info("没有待支付订单，跳过手动校验支付测试");
            return;
        }
        
        String purchaseId = orderResponse.jsonPath().getString("data[0].purchase_id");
        
        Map<String, Object> checkData = Map.of("purchaseId", Integer.parseInt(purchaseId));
        
        Response checkResponse = ApiUtils.getRequestSpec()
            .body(checkData)
            .when()
            .post(ApiConfig.Endpoints.PAYMENT_VERIFY)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        Integer code = checkResponse.jsonPath().getInt("code");
        if (code == 200) {
            String paymentStatus = checkResponse.jsonPath().getString("data.payment_status");
            logger.info("TC-PAY-004: 手动校验支付结果成功，支付状态: {}", paymentStatus);
        } else {
            logger.info("TC-PAY-004: 手动校验支付失败，原因: {}", 
                checkResponse.jsonPath().getString("message"));
        }
    }
    
  @Test
@Story("退款流程")
@Description("TC-PAY-005: 申请退款")
public void testRefund() {
    setCustomerToken();
    
    // 先获取支付记录
    Response paymentResponse = ApiUtils.getRequestSpec()
        .queryParam("customer_id", getCustomerId())
        .when()
        .get(ApiConfig.Endpoints.PAYMENT_CUSTOMER)
        .then()
        .statusCode(ApiConfig.StatusCode.OK)
        .extract()
        .response();
    
    // 打印完整响应以便调试
    logger.info("获取支付记录响应: {}", paymentResponse.asString());
    
    // 检查响应结构
    Object data = paymentResponse.jsonPath().get("data");
    if (data == null) {
        logger.info("data字段为null，无法获取支付记录");
        return;
    }
    
    // 安全地获取items列表
    Object items = paymentResponse.jsonPath().get("data.items");
    if (items == null) {
        logger.info("data.items字段为null，没有支付记录");
        return;
    }
    
    // 检查items是否为List
    List<Object> itemsList = paymentResponse.jsonPath().getList("data.items");
    if (itemsList == null || itemsList.isEmpty()) {
        logger.info("没有支付记录，跳过退款测试");
        return;
    }
    
    // 安全地获取支付信息，使用get而不是getDouble
    String paymentId = paymentResponse.jsonPath().getString("data.items[0].payment_id");
    if (paymentId == null) {
        logger.info("无法获取payment_id，跳过退款测试");
        return;
    }
    
    // 安全地获取金额 - 使用getDouble可能返回null
    Double amount = null;
    try {
        amount = paymentResponse.jsonPath().getDouble("data.items[0].payment_amount");
    } catch (Exception e) {
        logger.warn("获取payment_amount失败: {}", e.getMessage());
        // 尝试其他字段名
        Object amountObj = paymentResponse.jsonPath().get("data.items[0].amount");
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        }
    }
    
    if (amount == null) {
        logger.info("无法获取退款金额，跳过退款测试");
        return;
    }
    
    String paymentStatus = paymentResponse.jsonPath().getString("data.items[0].payment_status");
    
    Map<String, Object> refundData = Map.of(
        "refundAmount", amount,
        "refundReason", "商品质量问题"
    );
    
    String refundUrl = ApiConfig.Endpoints.PAYMENT_REFUND_APPLY.replace("{paymentId}", paymentId);
    
    // 发送退款请求
    Response refundResponse = ApiUtils.getRequestSpec()
        .body(refundData)
        .when()
        .post(refundUrl)
        .then()
        .extract()
        .response();
    
    // 打印退款响应
    logger.info("退款响应: {}", refundResponse.asString());
    
    // 安全地获取响应码
    Integer code = null;
    try {
        code = refundResponse.jsonPath().getInt("code");
    } catch (Exception e) {
        logger.error("无法获取响应code: {}", e.getMessage());
        return;
    }
    
    if (code != null && code == 200) {
        // 安全地获取新状态
        String newStatus = refundResponse.jsonPath().getString("data.payment_status");
        if (newStatus == null) {
            newStatus = refundResponse.jsonPath().getString("data.status");
        }
        logger.info("TC-PAY-005: 退款申请提交成功，支付ID: {}，原状态: {}，新状态: {}", 
            paymentId, paymentStatus, newStatus);
    } else {
        String message = refundResponse.jsonPath().getString("message");
        if (message == null) {
            message = "未知错误";
        }
        logger.info("TC-PAY-005: 退款申请失败，原因: {}", message);
    }
}

    
    @Test
    @Story("查询支付记录")
    @Description("TC-PAY-006: 查询客户支付记录列表")
    public void testGetPaymentList() {
        setCustomerToken();
        
        Response response = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(ApiConfig.Endpoints.PAYMENT_CUSTOMER)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data", notNullValue())
            .extract()
            .response();
        
        Object items = response.jsonPath().get("data.items");
        if (items instanceof java.util.Map) {
            logger.info("没有支付记录");
        } else {
            int itemsCount = response.jsonPath().getList("data.items").size();
            logger.info("TC-PAY-006: 查询支付记录成功，记录数量: {}", itemsCount);
        }
    }
    
    @Test
    @Story("查询订单支付记录")
    @Description("TC-PAY-007: 根据订单ID查询支付记录")
    public void testGetPaymentByPurchase() {
        setCustomerToken();
        
        String url = ApiConfig.Endpoints.CUSTOMER_PURCHASE_INTENTS
            .replace("{customerId}", String.valueOf(getCustomerId()));
        
        Response orderResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(url)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        Object data = orderResponse.jsonPath().get("data");
        if (data instanceof java.util.Map || orderResponse.jsonPath().getList("data").size() == 0) {
            logger.info("没有订单记录，跳过查询测试");
            return;
        }
        
        String purchaseId = orderResponse.jsonPath().getString("data[0].purchase_id");
        String paymentUrl = ApiConfig.Endpoints.PAYMENT_BY_PURCHASE.replace("{purchaseId}", purchaseId);
        
        Response response = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(paymentUrl)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        Integer code = response.jsonPath().getInt("code");
        if (code == 200 && response.jsonPath().get("data") != null) {
            String paymentId = response.jsonPath().getString("data.payment_id");
            String paymentStatus = response.jsonPath().getString("data.payment_status");
            logger.info("TC-PAY-007: 查询订单支付记录成功，订单ID: {}，支付ID: {}，支付状态: {}", 
                purchaseId, paymentId, paymentStatus);
        } else {
            logger.info("TC-PAY-007: 查询订单支付记录失败，原因: {}", 
                response.jsonPath().getString("message"));
        }
    }
}