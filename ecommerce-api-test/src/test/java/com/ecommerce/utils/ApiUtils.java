// src/test/java/com/ecommerce/utils/ApiUtils.java
package com.ecommerce.utils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import com.ecommerce.config.ApiConfig;

public class ApiUtils {
    private static String authToken;
    
    static {
        RestAssured.baseURI = ApiConfig.BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    public static RequestSpecification getRequestSpec() {
        RequestSpecification spec = RestAssured.given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON);
        
        if (authToken != null && !authToken.isEmpty()) {
            spec.header("Authorization", "Bearer " + authToken);
        }
        
        return spec;
    }
    
    public static void setAuthToken(String token) {
        authToken = token;
    }
    
    public static void clearAuthToken() {
        authToken = null;
    }
    
    public static Response login(String username, String password) {
        String loginBody = String.format(
            "{\"username\":\"%s\",\"password\":\"%s\"}",
            username, password
        );
        
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginBody)
            .post(ApiConfig.Endpoints.CUSTOMER_LOGIN);
    }
    
    public static void assertSuccess(Response response) {
        response.then().statusCode(ApiConfig.StatusCode.OK);
    }
    
    public static void assertCreated(Response response) {
        response.then().statusCode(ApiConfig.StatusCode.CREATED);
    }
}