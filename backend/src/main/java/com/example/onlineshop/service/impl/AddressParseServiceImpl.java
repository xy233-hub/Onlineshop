package com.example.onlineshop.service.impl;

import com.example.onlineshop.config.BaiduMapConfig;
import com.example.onlineshop.dto.response.AddressParseResponse;
import com.example.onlineshop.service.AddressParseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AddressParseServiceImpl implements AddressParseService {

    private static final Logger log = LoggerFactory.getLogger(AddressParseServiceImpl.class);

    @Autowired
    private BaiduMapConfig baiduMapConfig;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    public AddressParseServiceImpl() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

     @Override
    public AddressParseResponse parseAddress(String addressText) {
        if (addressText == null || addressText.trim().isEmpty()) {
            return AddressParseResponse.builder().build();
        }

        log.info("[百度智能云地址识别] 开始解析: {}", addressText);

        try {
            if (!isConfigValid()) {
                log.warn("[百度智能云地址识别] 配置无效，使用基础解析");
                String phone = extractPhone(addressText);
                String name = extractName(addressText);
                return buildPartialResult(name, phone, addressText);
            }

            String accessToken = getAccessToken();
            if (accessToken == null) {
                log.error("[百度智能云地址识别] 获取access_token失败");
                String phone = extractPhone(addressText);
                String name = extractName(addressText);
                return buildPartialResult(name, phone, addressText);
            }

            log.debug("[百度智能云地址识别] 调用地址解析API");
            JsonNode parseResult = callAddressParseApi(addressText, accessToken);
            
            if (parseResult == null) {
                log.error("[百度智能云地址识别] API返回null");
                String phone = extractPhone(addressText);
                String name = extractName(addressText);
                return buildPartialResult(name, phone, addressText);
            }
            
            if (parseResult.has("error_code")) {
                int errorCode = parseResult.get("error_code").asInt(-1);
                if (errorCode != 0) {
                    String errorMsg = parseResult.path("error_msg").asText("unknown");
                    log.error("[百度智能云地址识别] API返回错误 - 错误码: {}, 消息: {}", errorCode, errorMsg);
                    String phone = extractPhone(addressText);
                    String name = extractName(addressText);
                    return buildPartialResult(name, phone, addressText);
                }
            }
            
            String person = parseResult.path("person").asText("");
            String phonenum = parseResult.path("phonenum").asText("");
            String province = parseResult.path("province").asText("");
            String city = parseResult.path("city").asText("");
            String county = parseResult.path("county").asText("");
            String detail = parseResult.path("detail").asText("");
            
            String name = !person.isEmpty() ? person : extractName(addressText);
            String phone = !phonenum.isEmpty() ? phonenum : extractPhone(addressText);

            log.info("[百度智能云地址识别] 成功 - 姓名:{}, 电话:{}, 省:{}, 市:{}, 区:{}, 详细:{}", 
                    name, phone, province, city, county, detail);

            return AddressParseResponse.builder()
                    .recipientName(name)
                    .recipientPhone(phone)
                    .province(province)
                    .city(city)
                    .district(county)
                    .detailAddress(detail)
                    .build();
                    
        } catch (Exception e) {
            log.error("[百度智能云地址识别] 异常: {}", e.getMessage(), e);
            String phone = extractPhone(addressText);
            String name = extractName(addressText);
            return buildPartialResult(name, phone, addressText);
        }
    }

    private String getAccessToken() {
        String apiKey = baiduMapConfig.getApiKey();
        String secretKey = baiduMapConfig.getSecretKey();
        
        String cacheKey = apiKey + ":" + secretKey;
        TokenInfo cached = tokenCache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            log.debug("[百度智能云] 使用缓存的access_token");
            return cached.getAccessToken();
        }
        
        try {
            log.debug("[百度智能云] 获取新的access_token");
            String response = webClient.post()
                    .uri("https://aip.baidubce.com/oauth/2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> 
                        Mono.error(new RuntimeException("获取token失败: " + clientResponse.statusCode())))
                    .bodyToMono(String.class)
                    .block();

            JsonNode tokenResult = objectMapper.readTree(response);
            String accessToken = tokenResult.path("access_token").asText(null);
            int expiresIn = tokenResult.path("expires_in").asInt(2592000);
            
            if (accessToken != null && !accessToken.isEmpty()) {
                TokenInfo tokenInfo = new TokenInfo(accessToken, System.currentTimeMillis() + (expiresIn - 300) * 1000L);
                tokenCache.put(cacheKey, tokenInfo);
                log.debug("[百度智能云] access_token获取成功，有效期: {}秒", expiresIn);
                return accessToken;
            } else {
                String errorDesc = tokenResult.path("error_description").asText("unknown");
                log.error("[百度智能云] access_token为空, 错误信息: {}", errorDesc);
                return null;
            }
        } catch (Exception e) {
            log.error("[百度智能云] 获取access_token异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private JsonNode callAddressParseApi(String addressText, String accessToken) throws Exception {
        log.info("[百度智能云地址识别] 请求地址: {}", addressText);
        
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("text", addressText);
        
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        log.debug("[百度智能云地址识别] 请求JSON: {}", jsonBody);
        
        String response = webClient.post()
                .uri("https://aip.baidubce.com/rpc/2.0/nlp/v1/address?access_token=" + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> 
                    Mono.error(new RuntimeException("地址解析API调用失败: " + clientResponse.statusCode())))
                .bodyToMono(String.class)
                .block();

        log.info("[百度智能云地址识别] API响应: {}", response);
        return objectMapper.readTree(response);
    }

    private boolean isConfigValid() {
        String apiKey = baiduMapConfig.getApiKey();
        String secretKey = baiduMapConfig.getSecretKey();
        return apiKey != null && !apiKey.isEmpty() && 
               secretKey != null && !secretKey.isEmpty();
    }

    private AddressParseResponse buildPartialResult(String name, String phone, String originalAddress) {
        return AddressParseResponse.builder()
                .recipientName(name != null ? name : "")
                .recipientPhone(phone != null ? phone : "")
                .province("")
                .city("")
                .district("")
                .detailAddress(originalAddress != null ? originalAddress : "")
                .build();
    }

    private String extractPhone(String text) {
        Pattern pattern = Pattern.compile("(1[3-9]\\d{9})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractName(String text) {
        Pattern pattern = Pattern.compile("^(.{1,4}?)[\\s,，]*(?:1[3-9]\\d{9})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            if (name.length() >= 2 && name.length() <= 4) {
                return name;
            }
        }
        
        pattern = Pattern.compile("(?:收件人|收货人)[:：\\s]*([\\u4e00-\\u9fa5]{2,4})");
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    private static class TokenInfo {
        private final String accessToken;
        private final long expireTime;

        public TokenInfo(String accessToken, long expireTime) {
            this.accessToken = accessToken;
            this.expireTime = expireTime;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireTime;
        }
    }
}