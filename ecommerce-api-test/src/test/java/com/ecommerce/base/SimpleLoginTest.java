package com.ecommerce.base;

import com.ecommerce.config.ApiConfig;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.hamcrest.Matchers.*;

public class SimpleLoginTest {
    
    @BeforeClass
    public void setup() {
        RestAssured.baseURI = ApiConfig.BASE_URL;
        System.out.println("BASE_URL: " + ApiConfig.BASE_URL);
    }
    
    @Test
    public void testCustomerLogin() {
        String loginBody = "{\"username\":\"user2\",\"password\":\"password123\"}";
        
        System.out.println("发送登录请求到: " + ApiConfig.Endpoints.CUSTOMER_LOGIN);
        
        Response response = RestAssured.given()
            .contentType("application/json")
            .body(loginBody)
            .post(ApiConfig.Endpoints.CUSTOMER_LOGIN);
        
        System.out.println("响应状态码: " + response.getStatusCode());
        System.out.println("响应内容: " + response.getBody().asString());
        
        response.then()
            .statusCode(200)
            .body("code", equalTo(200))
            .body("data.token", notNullValue());
        
        String token = response.jsonPath().getString("data.token");
        System.out.println("登录成功！Token: " + token);
    }
}