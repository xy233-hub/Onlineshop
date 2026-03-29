// src/test/java/com/ecommerce/logistics/LogisticsTest.java
package com.ecommerce.logistics;

import com.ecommerce.base.BaseTest;
import com.ecommerce.config.ApiConfig;
import com.ecommerce.utils.ApiUtils;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import static org.hamcrest.Matchers.*;

@Feature("物流管理")
public class LogisticsTest extends BaseTest {
    
    @Test
    @Story("配送方式")
    @Description("TC-LOG-001: 查看配送方式列表")
    public void testGetDeliveryMethods() {
        setCustomerToken();
        
        Response methodsResponse = ApiUtils.getRequestSpec()
            .when()
            .get(ApiConfig.Endpoints.LOGISTICS_PROVIDERS)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data", notNullValue())
            .body("data.size()", greaterThan(0))
            .extract()
            .response();
        
        logger.info("TC-LOG-001: 获取配送方式列表成功，数量: {}", 
            methodsResponse.jsonPath().getList("data").size());
    }
    
@Test
@Story("物流轨迹")
@Description("TC-LOG-002: 查看物流轨迹")
public void testViewLogisticsTracking() {
    setCustomerToken();
    
    String url = ApiConfig.Endpoints.CUSTOMER_PURCHASE_INTENTS
        .replace("{customerId}", String.valueOf(getCustomerId()));
    
    Response orderResponse = ApiUtils.getRequestSpec()
        .queryParam("customer_id", getCustomerId())
        .when()
        .get(url + "?purchase_status=SHIPPING_STARTED")
        .then()
        .statusCode(ApiConfig.StatusCode.OK)
        .extract()
        .response();
    
    Object data = orderResponse.jsonPath().get("data");
    String purchaseId = null;
    
    
    if (data instanceof java.util.List && orderResponse.jsonPath().getList("data").size() > 0) {
        purchaseId = orderResponse.jsonPath().getString("data[0].purchase_id");
        String trackingNo = orderResponse.jsonPath().getString("data[0].tracking_no");
        logger.info("找到已发货订单 - 订单号: {}, 物流单号: {}", purchaseId, trackingNo);
    }
    
    if (purchaseId == null) {
        logger.info("TC-LOG-002: 没有已发货的订单，跳过物流轨迹测试");
        return;
    }
    
    String trackUrl = ApiConfig.Endpoints.ORDER_LOGISTICS.replace("{purchaseId}", purchaseId);
    
    Response trackResponse = ApiUtils.getRequestSpec()
        .when()
        .get(trackUrl)
        .then()
        .statusCode(ApiConfig.StatusCode.OK)
        .body("data.logistics_info.tracking_no", notNullValue())
        .body("data.tracks", notNullValue())
        .extract()
        .response();
    
    String trackingNo = trackResponse.jsonPath().getString("data.logistics_info.tracking_no");
    logger.info("TC-LOG-002: 查看物流轨迹成功，订单号: {}, 运单号: {}", purchaseId, trackingNo);
  }
}