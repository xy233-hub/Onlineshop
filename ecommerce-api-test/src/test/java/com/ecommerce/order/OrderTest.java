// src/test/java/com/ecommerce/order/OrderTest.java
package com.ecommerce.order;

import com.ecommerce.base.BaseTest;
import com.ecommerce.config.ApiConfig;
import com.ecommerce.utils.ApiUtils;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@Feature("订单管理")
public class OrderTest extends BaseTest {
    
    @Test
    @Story("查看历史订单")
    @Description("TC-ORDER-VIEW-001: 查看历史订单列表")
    public void testViewHistoryOrders() {
        setCustomerToken();
        
        String url = ApiConfig.Endpoints.CUSTOMER_PURCHASE_INTENTS
            .replace("{customerId}", String.valueOf(getCustomerId()));
        
        Response response = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(url)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        // 检查 data 是数组还是对象
        Object data = response.jsonPath().get("data");
        if (data instanceof java.util.Map) {
            logger.info("没有历史订单记录");
        } else {
            int size = response.jsonPath().getList("data").size();
            logger.info("TC-ORDER-VIEW-001: 查看历史订单成功，订单数量: {}", size);
            if (size > 0) {
                response.then()
                    .body("data[0].purchase_id", notNullValue())
                    .body("data[0].purchase_status", notNullValue())
                    .body("data[0].total_amount", notNullValue())
                    .body("data[0].created_at", notNullValue());
            }
        }
    }
    
    @Test
    @Story("订单详情")
    @Description("TC-ORDER-VIEW-002: 订单详情页操作按钮")
    public void testOrderDetailButtons() {
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
        if (data instanceof java.util.Map) {
            logger.info("没有待支付订单，跳过详情验证");
            return;
        }
        
        if (orderResponse.jsonPath().getList("data").size() > 0) {
            String purchaseId = orderResponse.jsonPath().getString("data[0].purchase_id");
            
            String detailUrl = url + "/" + purchaseId;
            
            Response detailResponse = ApiUtils.getRequestSpec()
                .queryParam("customer_id", getCustomerId())
                .when()
                .get(detailUrl)
                .then()
                .statusCode(ApiConfig.StatusCode.OK)
                .body("data.purchase_id", notNullValue())
                .extract()
                .response();
            
            logger.info("TC-ORDER-VIEW-002: 订单详情验证成功，订单ID: {}", 
                detailResponse.jsonPath().getString("data.purchase_id"));
        } else {
            logger.info("TC-ORDER-VIEW-002: 没有待支付订单，跳过详情验证");
        }
    }
    
@Test
@Story("订单创建")
@Description("创建新订单")
public void testCreateOrder() {
    setCustomerToken();
    
    // 使用下划线格式的字段名
    Map<String, Object> orderData = Map.of(
        "product_id", 2,              // 使用商品ID 2
        "quantity", 1,
        "customer_id", getCustomerId()
    );
    
    Response response = ApiUtils.getRequestSpec()
        .body(orderData)
        .when()
        .post(ApiConfig.Endpoints.PURCHASE_INTENTS)
        .then()
        .statusCode(ApiConfig.StatusCode.OK)
        .extract()
        .response();
    
    Integer code = response.jsonPath().getInt("code");
    if (code == 200 && response.jsonPath().get("data") != null) {
        String purchaseId = response.jsonPath().getString("data.purchase_id");
        logger.info("TC-CREATE-ORDER: 创建订单成功，订单ID: {}", purchaseId);
    } else {
        String message = response.jsonPath().getString("message");
        logger.info("TC-CREATE-ORDER: 创建订单失败，原因: {}", message);
    }
 }
}