// src/test/java/com/ecommerce/base/BaseTest.java
package com.ecommerce.base;

import com.ecommerce.utils.ApiUtils;
import com.ecommerce.config.ApiConfig;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTest {
    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected static String customerToken;
    protected static String sellerToken;
    protected static final int CUSTOMER_ID = 4;  // user2 的 customer_id
    
    @BeforeSuite
    public void setupSuite() {
        RestAssured.baseURI = ApiConfig.BASE_URL;
        logger.info("开始执行API测试套件，BASE_URL: {}", ApiConfig.BASE_URL);
    }
    
    @BeforeMethod
    public void setupMethod() {
        logger.info("开始执行测试用例: {}", getClass().getSimpleName());
        if (customerToken == null) {
            customerToken = loginAsCustomer();
        }
        ApiUtils.setAuthToken(customerToken);
    }
    
    @AfterMethod
    public void teardownMethod() {
        logger.info("测试用例执行完成");
        ApiUtils.clearAuthToken();
    }
    
    @AfterSuite
    public void teardownSuite() {
        logger.info("API测试套件执行完成");
        ApiUtils.clearAuthToken();
    }
    
    protected String loginAsCustomer() {
        Response response = ApiUtils.login("user2", "password123");
        response.then().statusCode(ApiConfig.StatusCode.OK);
        String token = response.jsonPath().getString("data.token");
        logger.info("买家登录成功，customer_id: {}", CUSTOMER_ID);
        return token;
    }
    
    protected String loginAsSeller() {
        Response response = ApiUtils.login("admin", "Admin123456");
        response.then().statusCode(ApiConfig.StatusCode.OK);
        String token = response.jsonPath().getString("data.token");
        logger.info("卖家登录成功");
        return token;
    }
    
    protected void setCustomerToken() {
        ApiUtils.setAuthToken(customerToken);
    }
    
    protected void setSellerToken() {
        ApiUtils.setAuthToken(sellerToken);
    }
    
    protected int getCustomerId() {
        return 4;
    }
    
}