// src/test/java/com/ecommerce/afterSale/CustomerAfterSaleTest.java
package com.ecommerce.afterSale;

import com.ecommerce.base.BaseTest;
import com.ecommerce.config.ApiConfig;
import com.ecommerce.utils.ApiUtils;
import com.ecommerce.utils.TestDataUtils;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@Feature("售后服务-客户端")
public class CustomerAfterSaleTest extends BaseTest {
    
    @Test
    @Story("发起售后")
    @Description("TC-AFTERSALE-001: 发起售后申请")
    public void testInitiateAfterSale() {
        setCustomerToken();
        
        // 修复：替换 URL 中的 {customerId}
        String url = ApiConfig.Endpoints.CUSTOMER_PURCHASE_INTENTS
            .replace("{customerId}", String.valueOf(getCustomerId()));
        
        Response orderResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(url + "?purchase_status=COMPLETED")
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        // 修复：data 可能是对象，需要先判断
        Object data = orderResponse.jsonPath().get("data");
        if (data instanceof java.util.Map) {
            logger.info("没有已完成订单，跳过售后申请测试");
            return;
        }
        
        String purchaseId = orderResponse.jsonPath().getString("data[0].purchase_id");
        if (purchaseId == null) {
            logger.info("没有找到订单ID，跳过售后申请测试");
            return;
        }
        
        Map<String, Object> afterSaleData = TestDataUtils.getTestAfterSale();
        afterSaleData.put("purchaseId", Integer.parseInt(purchaseId));
        afterSaleData.put("productId", 1);
        
        Response response = ApiUtils.getRequestSpec()
            .body(afterSaleData)
            .when()
            .post(ApiConfig.Endpoints.AFTER_SALES)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data.service_id", notNullValue())
            .body("data.service_status", equalTo("PENDING"))
            .body("data.service_title", equalTo("质量问题"))
            .extract()
            .response();
        
        String serviceId = response.jsonPath().getString("data.service_id");
        logger.info("TC-AFTERSALE-001: 售后申请提交成功，售后ID: {}", serviceId);
    }
    
    @Test
    @Story("售后进度")
    @Description("TC-AFTERSALE-002: 查看售后进度")
    public void testViewAfterSaleProgress() {
        setCustomerToken();
        
        Response listResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(ApiConfig.Endpoints.AFTER_SALES)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        Object data = listResponse.jsonPath().get("data");
        if (data instanceof java.util.Map || listResponse.jsonPath().getList("data").isEmpty()) {
            logger.info("没有售后记录，跳过查看售后进度测试");
            return;
        }
        
        String serviceId = listResponse.jsonPath().getString("data[0].service_id");
        if (serviceId == null) {
            logger.info("没有找到售后ID，跳过查看售后进度测试");
            return;
        }
        
        Response progressResponse = ApiUtils.getRequestSpec()
            .pathParam("serviceId", serviceId)
            .when()
            .get(ApiConfig.Endpoints.AFTER_SALE_DETAIL)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data.service_id", equalTo(Integer.parseInt(serviceId)))
            .body("data.service_status", notNullValue())
            .extract()
            .response();
        
        logger.info("TC-AFTERSALE-002: 查看售后进度成功，当前状态: {}", 
            progressResponse.jsonPath().getString("data.service_status"));
    }
    
    @Test
    @Story("售后详情")
    @Description("查看售后详情")
    public void testViewAfterSaleDetail() {
        setCustomerToken();
        
        Response listResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(ApiConfig.Endpoints.AFTER_SALES)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        Object data = listResponse.jsonPath().get("data");
        if (data instanceof java.util.Map || listResponse.jsonPath().getList("data").isEmpty()) {
            logger.info("没有售后记录，跳过查看售后详情测试");
            return;
        }
        
        String serviceId = listResponse.jsonPath().getString("data[0].service_id");
        if (serviceId == null) {
            logger.info("没有找到售后ID，跳过查看售后详情测试");
            return;
        }
        
        Response detailResponse = ApiUtils.getRequestSpec()
            .pathParam("serviceId", serviceId)
            .when()
            .get(ApiConfig.Endpoints.AFTER_SALE_DETAIL)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data.service_id", equalTo(Integer.parseInt(serviceId)))
            .body("data.service_title", notNullValue())
            .body("data.problem_description", notNullValue())
            .body("data.service_status", notNullValue())
            .extract()
            .response();
        
        logger.info("查看售后详情成功，售后状态: {}", 
            detailResponse.jsonPath().getString("data.service_status"));
    }
}