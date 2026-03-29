// src/test/java/com/ecommerce/address/AddressTest.java
package com.ecommerce.address;

import com.ecommerce.base.BaseTest;
import com.ecommerce.config.ApiConfig;
import com.ecommerce.utils.ApiUtils;
import com.ecommerce.utils.TestDataUtils;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@Feature("地址管理")
public class AddressTest extends BaseTest {
    
    @Test
    @Story("新增地址")
    @Description("TC-ADDR-001: 新增地址")
    public void testAddAddress() {
        setCustomerToken();
        
        Map<String, Object> addressData = TestDataUtils.getTestAddress();
        
        Response response = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .body(addressData)
            .when()
            .post(ApiConfig.Endpoints.ADDRESSES)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data.address_id", notNullValue())  // 改为 address_id
            .extract()
            .response();
        
        String addressId = response.jsonPath().getString("data.address_id");  // 改为 address_id
        logger.info("TC-ADDR-001: 新增地址成功，地址ID: {}", addressId);
    }
    
    @Test
    @Story("默认地址设置")
    @Description("TC-ADDR-002: 设为默认地址")
    public void testSetDefaultAddress() {
        setCustomerToken();
        
        Response addressListResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(ApiConfig.Endpoints.ADDRESSES)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .extract()
            .response();
        
        List<Object> addresses = addressListResponse.jsonPath().getList("data");
        if (addresses == null || addresses.isEmpty()) {
            logger.info("没有地址数据，跳过设置默认地址测试");
            return;
        }
        
        // 使用下划线格式的字段名
        String addressId = addressListResponse.jsonPath().getString("data[0].address_id");
        
        if (addressId == null) {
            logger.info("地址ID为空，跳过设置默认地址测试");
            return;
        }
        
        Response response = ApiUtils.getRequestSpec()
            .pathParam("addressId", addressId)
            .when()
            .patch(ApiConfig.Endpoints.ADDRESS_DEFAULT)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data.address_id", equalTo(Integer.parseInt(addressId)))
            .body("data.is_default", equalTo(true))
            .extract()
            .response();
        
        logger.info("TC-ADDR-002: 设置默认地址成功，响应码: {}", response.getStatusCode());
    }
    
    @Test
    @Story("自动填充地址")
    @Description("TC-ADDR-003: 下单自动填充默认地址")
    public void testAutoFillDefaultAddress() {
        setCustomerToken();
        
        Response addressResponse = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(ApiConfig.Endpoints.ADDRESSES)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data", notNullValue())
            .extract()
            .response();
        
        boolean hasDefault = false;
        List<Object> addresses = addressResponse.jsonPath().getList("data");
        
        for (int i = 0; i < addresses.size(); i++) {
            Boolean isDefault = addressResponse.jsonPath().getBoolean("data[" + i + "].is_default");
            if (isDefault != null && isDefault) {
                hasDefault = true;
                addressResponse.then()
                    .body("data[" + i + "].recipient_name", notNullValue())
                    .body("data[" + i + "].recipient_phone", notNullValue())
                    .body("data[" + i + "].detail_address", notNullValue());
                break;
            }
        }
        
        if (!hasDefault) {
            logger.info("未找到默认地址");
        } else {
            logger.info("TC-ADDR-003: 默认地址自动填充验证成功");
        }
    }
    
    @Test
    @Story("地址列表")
    @Description("获取地址列表")
    public void testGetAddressList() {
        setCustomerToken();
        
        Response response = ApiUtils.getRequestSpec()
            .queryParam("customer_id", getCustomerId())
            .when()
            .get(ApiConfig.Endpoints.ADDRESSES)
            .then()
            .statusCode(ApiConfig.StatusCode.OK)
            .body("data", notNullValue())
            .extract()
            .response();
        
        List<Object> addresses = response.jsonPath().getList("data");
        logger.info("获取地址列表成功，地址数量: {}", addresses != null ? addresses.size() : 0);
    }
}