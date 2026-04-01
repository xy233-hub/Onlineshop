package com.example.onlineshop.service;

import com.example.onlineshop.dto.ai.AiProductQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExternalAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ThreadLocal<String> lastFailureReason = new ThreadLocal<>();

    @Value("${ai.api.base-url:}")
    private String baseUrl;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.model:qwen3-max-2026-01-23}")
    private String model;

    // 只打印摘要（默认 true 也可以，但不会刷屏）
    @Value("${ai.api.debug:false}")
    private boolean debug;

    // 只有 verbose=true 才打印 Raw/完整 content 等大段文字
    @Value("${ai.api.debug-verbose:false}")
    private boolean debugVerbose;

    @Value("${ai.api.connect-timeout-ms:10000}")
    private long connectTimeoutMs;

    @Value("${ai.api.read-timeout-ms:80000}")
    private long readTimeoutMs;

    @Value("${ai.api.retry-times:1}")
    private int retryTimes;

    @Value("${ai.api.enable-thinking:false}")
    private boolean defaultEnableThinking;

    @Value("${ai.api.embedding-model:text-embedding-v4}")
    private String embeddingModel;

    public ExternalAiClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public AiProductQuery extractQuery(String userText) {
        try {
            if (userText == null || userText.isBlank()) return null;

            JsonNode msg = callDashScopeMessage(buildExtractPrompt(userText), false, "extractQuery");
            if (msg == null) return null;

            String content = msg.path("content").asText("");
            if (content.isBlank()) return null;

            return objectMapper.readValue(content.trim(), AiProductQuery.class);
        } catch (Exception e) {
            if (debug) System.out.println("[AI] extractQuery exception: " + e);
            return null;
        }
    }

    public String generateDescription(String userText, String productsSummaryJson) {
        try {
            if (userText == null || userText.isBlank()) return "";

            JsonNode msg = callDashScopeMessage(
                    buildDescriptionPrompt(userText, productsSummaryJson),
                    defaultEnableThinking,
                    "generateDescription"
            );
            if (msg == null) return "";

            String content = msg.path("content").asText("");
            return content == null ? "" : content.trim();
        } catch (Exception e) {
            if (debug) System.out.println("[AI] generateDescription exception: " + e);
            return "";
        }
    }

    public String generateChatReply(String userText, String historyContext) {
        try {
            if (userText == null || userText.isBlank()) return "";

            JsonNode msg = callDashScopeMessage(
                    buildChatPrompt(userText, historyContext),
                    defaultEnableThinking,
                    "generateChatReply"
            );
            if (msg == null) return "";

            String content = msg.path("content").asText("");
            return content == null ? "" : content.trim();
        } catch (Exception e) {
            if (debug) System.out.println("[AI] generateChatReply exception: " + e);
            return "";
        }
    }

    // 供卖家发布页按钮调用：生成详情描述（Markdown）
    public String generateProductDescription(String productName, Integer categoryId, String keywords) {
        Map<String, Object> pack = generateProductDescriptionPack(productName, categoryId, keywords, "");
        Object desc = pack.get("description");
        return desc == null ? "" : String.valueOf(desc);
    }

    // 返回 description + source，便于前端判断是否走了 fallback
    public Map<String, Object> generateProductDescriptionPack(String productName, Integer categoryId, String keywords, String productDesc) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (productName == null || productName.isBlank()) {
                result.put("description", "");
                result.put("source", "fallback");
                result.put("fallback_reason", "product_name empty");
                return result;
            }

            JsonNode msg = callDashScopeMessage(
                    buildProductDescriptionPrompt(productName, categoryId, keywords, productDesc),
                    defaultEnableThinking,
                    "generateProductDescription",
                    0.85,
                    0.9
            );
            if (msg == null) {
                result.put("description", fallbackProductDescription(productName, keywords));
                result.put("source", "fallback");
                result.put("fallback_reason", consumeFailureReason("ai response null"));
                return result;
            }

            String content = msg.path("content").asText("");
            if (content == null || content.isBlank()) {
                result.put("description", fallbackProductDescription(productName, keywords));
                result.put("source", "fallback");
                result.put("fallback_reason", "ai content empty");
                return result;
            }

            String cleaned = content.replace("\r", "\n").trim();
            result.put("description", cleaned.isBlank() ? fallbackProductDescription(productName, keywords) : cleaned);
            result.put("source", cleaned.isBlank() ? "fallback" : "ai");
            if (cleaned.isBlank()) result.put("fallback_reason", "ai content blank after trim");
            return result;
        } catch (Exception e) {
            if (debug) System.out.println("[AI] generateProductDescription exception: " + e);
            result.put("description", fallbackProductDescription(productName, keywords));
            result.put("source", "fallback");
            result.put("fallback_reason", e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "unknown" : e.getMessage()));
            return result;
        }
    }

    // 供卖家发布页按钮调用：预估价格区间
    public Map<String, Object> estimateProductPriceRange(String productName, Integer categoryId, String productDesc) {
        Map<String, Object> fallback = fallbackPriceEstimate();
        try {
            if (productName == null || productName.isBlank()) return fallback;

            JsonNode msg = callDashScopeMessage(
                    buildPriceEstimatePrompt(productName, categoryId, productDesc),
                    false,
                    "estimateProductPriceRange"
            );
            if (msg == null) return fallback;

            String content = msg.path("content").asText("").trim();
            if (content.isBlank()) return fallback;

            // 优先 JSON 解析：{"min":100,"max":200}
            try {
                JsonNode n = objectMapper.readTree(content);
                int min = n.path("min").asInt(-1);
                int max = n.path("max").asInt(-1);
                if (min > 0 && max >= min) {
                    Map<String, Object> ok = new HashMap<>();
                    ok.put("min", min);
                    ok.put("max", max);
                    ok.put("source", "ai");
                    return ok;
                }
            } catch (Exception ignore) {
                // 非 JSON 继续走正则兜底
            }

            // 次优：从自由文本提取两个数字
            Matcher matcher = Pattern.compile("(\\d{1,6})").matcher(content);
            List<Integer> nums = new ArrayList<>();
            while (matcher.find()) {
                nums.add(Integer.parseInt(matcher.group(1)));
                if (nums.size() >= 2) break;
            }
            if (nums.size() >= 2) {
                int a = nums.get(0);
                int b = nums.get(1);
                int min = Math.min(a, b);
                int max = Math.max(a, b);
                if (min > 0) {
                    Map<String, Object> ok = new HashMap<>();
                    ok.put("min", min);
                    ok.put("max", Math.max(max, min + 20));
                    ok.put("source", "ai");
                    return ok;
                }
            }

            return fallback;
        } catch (Exception e) {
            if (debug) System.out.println("[AI] estimateProductPriceRange exception: " + e);
            return fallback;
        }
    }

    public List<Double> generateEmbedding(String text) {
        try {
            if (text == null || text.isBlank()) return Collections.emptyList();
            if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) return Collections.emptyList();

            String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String url = normalized + "/services/embeddings/text-embedding/text-embedding";

            Map<String, Object> input = new HashMap<>();
            input.put("texts", Collections.singletonList(text));

            Map<String, Object> body = new HashMap<>();
            body.put("model", embeddingModel);
            body.put("input", input);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            String raw = resp.getBody();
            if (raw == null || raw.isBlank()) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(raw);
            JsonNode embedding = root.path("output").path("embeddings").path(0).path("embedding");
            if (!embedding.isArray() || embedding.isEmpty()) return Collections.emptyList();

            List<Double> vector = new ArrayList<>(embedding.size());
            for (JsonNode node : embedding) {
                vector.add(node.asDouble());
            }
            return vector;
        } catch (Exception e) {
            if (debug) System.out.println("[AI] generateEmbedding exception: " + e);
            return Collections.emptyList();
        }
    }

    private JsonNode callDashScopeMessage(String prompt, boolean enableThinking, String op) throws Exception {
        return callDashScopeMessage(prompt, enableThinking, op, null, null);
    }

    private JsonNode callDashScopeMessage(
            String prompt,
            boolean enableThinking,
            String op,
            Double temperature,
            Double topP
    ) throws Exception {
        clearFailureReason();
        if (baseUrl == null || baseUrl.isBlank()) {
            setFailureReason("ai.api.base-url empty");
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            setFailureReason("ai.api.key empty");
            return null;
        }

        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String url = normalized + "/services/aigc/text-generation/generation";

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", "你是一个电商助手。"));
        messages.add(msg("user", prompt));

        Map<String, Object> input = new HashMap<>();
        input.put("messages", messages);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        parameters.put("enable_thinking", enableThinking);
        if (temperature != null) parameters.put("temperature", temperature);
        if (topP != null) parameters.put("top_p", topP);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", input);
        body.put("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        if (restTemplate.getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory f) {
            f.setConnectTimeout((int) connectTimeoutMs);
            f.setReadTimeout((int) readTimeoutMs);
        }

        long t0 = System.currentTimeMillis();
        ResponseEntity<String> resp;
        String raw = null;

        for (int attempt = 0; attempt <= Math.max(retryTimes, 0); attempt++) {
            try {
                resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
                raw = resp.getBody();

                if (debug) {
                    System.out.println("[AI] op=" + op
                            + " http=" + resp.getStatusCode().value()
                            + " costMs=" + (System.currentTimeMillis() - t0)
                            + " thinking=" + enableThinking
                            + " attempt=" + (attempt + 1));
                }

                if (debug && debugVerbose) {
                    System.out.println("[AI] Raw: " + raw);
                }
                break;
            } catch (ResourceAccessException ex) {
                if (attempt < Math.max(retryTimes, 0)) {
                    if (debug) {
                        System.out.println("[AI] op=" + op + " timeout/retry attempt=" + (attempt + 1)
                                + " msg=" + safeOneLine(ex.getMessage()));
                    }
                    continue;
                }
                setFailureReason("resource access: " + safeOneLine(ex.getMessage()));
                return null;
            } catch (RestClientResponseException ex) {
                if (debug) {
                    System.out.println("[AI] op=" + op
                            + " http=" + ex.getRawStatusCode()
                            + " costMs=" + (System.currentTimeMillis() - t0)
                            + " thinking=" + enableThinking);
                    System.out.println("[AI] errBody=" + safeOneLine(ex.getResponseBodyAsString()));
                }
                setFailureReason("http " + ex.getRawStatusCode() + ": " + safeOneLine(ex.getResponseBodyAsString()));
                return null;
            }
        }

        if (raw == null || raw.isBlank()) {
            setFailureReason("empty response body");
            return null;
        }

        JsonNode root = objectMapper.readTree(raw);

        JsonNode errCode = root.get("code");
        if (errCode != null && !errCode.isNull() && !errCode.asText("").isBlank()) {
            if (debug) {
                System.out.println("[AI] op=" + op
                        + " apiErrorCode=" + errCode.asText("")
                        + " apiErrorMessage=" + safeOneLine(root.path("message").asText("")));
            }
            setFailureReason("provider error " + errCode.asText("") + ": " + safeOneLine(root.path("message").asText("")));
            return null;
        }

        JsonNode messageNode = root.path("output").path("choices").path(0).path("message");
        if (messageNode.isMissingNode() || messageNode.isNull()) {
            setFailureReason("message node missing");
            return null;
        }

        if (debug && debugVerbose) {
            String reasoning = messageNode.path("reasoning_content").asText("");
            String content = messageNode.path("content").asText("");
            if (reasoning != null && !reasoning.isBlank()) {
                System.out.println("[AI] reasoning_content: " + reasoning);
            }
            System.out.println("[AI] content: " + content);
        }

        return messageNode;
    }

    private static Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static String safeOneLine(String s) {
        if (s == null) return "";
        String x = s.replace("\r", " ").replace("\n", " ").trim();
        return x.length() > 300 ? x.substring(0, 300) + "...(truncated)" : x;
    }

    // 新增：生成缩减商品描述（用于 products.short_desc）
    public String generateShortDesc(String productName, String productDesc) {
        try {
            JsonNode msg = callDashScopeMessage(
                    buildShortDescPrompt(productName, productDesc),
                    defaultEnableThinking,
                    "generateShortDesc"
            );
            if (msg == null) return fallbackShortDesc(productDesc, 300);

            String content = msg.path("content").asText("");
            if (content == null || content.isBlank()) {
                return fallbackShortDesc(productDesc, 300);
            }

            String cleaned = content.replace("\r", " ").replace("\n", " ").trim();
            if (cleaned.isBlank()) return fallbackShortDesc(productDesc, 300);
            return cleaned.length() > 300 ? cleaned.substring(0, 300) : cleaned;
        } catch (Exception e) {
            if (debug) System.out.println("[AI] generateShortDesc exception: " + e);
            return fallbackShortDesc(productDesc, 300);
        }
    }

    private String buildShortDescPrompt(String productName, String productDesc) {
        return ""
                + "你是电商商品文案助手。\n"
                + "请根据商品名称和详情，生成一段缩减商品描述。\n"
                + "要求：突出核心卖点，语气自然；不要输出多余解释；总长度不超过25字。\n"
                + "商品名称：\n"
                + (productName == null ? "" : productName) + "\n"
                + "商品详情：\n"
                + (productDesc == null ? "" : productDesc) + "\n";
    }

    private String fallbackShortDesc(String s, int maxLen) {
        if (s == null) return "";
        String x = s.replace("\r", " ").replace("\n", " ").trim();
        if (x.isBlank()) return "";
        return x.length() > maxLen ? x.substring(0, maxLen) : x;
    }

    private String buildExtractPrompt(String userText) {
        return ""
                + "你是电商搜索条件提取器。\n"
                + "从用户输入中提取结构化查询条件，输出严格 JSON，禁止输出多余文本。\n"
                + "允许字段：q, categoryId, status, minPrice, maxPrice, sortBy, order, page, size。\n"
                + "重要规则（必须遵守）：\n"
                + "1) `q` 必须尽量保留用户的关键短语，不要只保留一个名词，用逗号分离。\n"
                + "2) status 默认 online。\n"
                + "3) sortBy 只能是 price / created_at / stock_quantity 之一，不提供则为 created_at。\n"
                + "4) order 只能是 asc / desc，不提供则 desc。\n"
                + "5) page 默认 1，size 默认 10。\n"
                + "6) 价格区间识别：例如 100-250 元 -> minPrice=100,maxPrice=250。\n"
                + "用户输入：\n"
                + userText + "\n";
    }
    private String buildDescriptionPrompt(String userText, String productsSummaryJson) {
        return ""
                + "你是电商导购助手。\n"
                + "基于用户需求与候选商品摘要，生成一段简短推荐文案（50字左右）。\n"
                + "不要编造不存在的参数；如无合适商品，说明原因并给出调整建议。\n"
                + "用户需求：\n"
                + userText + "\n"
                + "候选商品摘要 JSON：\n"
                + (productsSummaryJson == null ? "[]" : productsSummaryJson) + "\n";
    }

    private String buildChatPrompt(String userText, String historyContext) {
        return ""
                + "你是电商对话助手。请用简洁、友好的中文回复。\n"
                + "规则：\n"
                + "1) 优先基于历史上下文理解用户意图；\n"
                + "2) 不确定时明确说明并追问关键信息（预算/用途/品牌偏好）；\n"
                + "3) 不要输出思维链，不要输出多余格式。\n"
                + "历史对话：\n"
                + (historyContext == null || historyContext.isBlank() ? "（无）" : historyContext) + "\n"
                + "当前用户输入：\n"
                + userText + "\n";
    }

    private String buildProductDescriptionPrompt(String productName, Integer categoryId, String keywords, String productDesc) {
        return ""
                + "你是二手电商商品文案助手。\n"
                + "请根据输入信息生成可直接发布的商品详情，输出 Markdown。\n"
                + "要求：\n"
                + "1) 必须结合商品名称、关键词、已有描述中的具体信息；\n"
                + "2) 禁止机械套用固定句式（例如每段都写同一句）；\n"
                + "3) 结构包含：产品特点、核心规格、使用场景、交易说明；\n"
                + "4) 不要输出任何解释性前言。\n"
                + "商品名称：" + productName + "\n"
                + "分类ID：" + (categoryId == null ? "" : categoryId) + "\n"
                + "关键词：" + (keywords == null ? "" : keywords) + "\n"
                + "已有描述：" + (productDesc == null ? "" : productDesc) + "\n";
    }

    private String buildPriceEstimatePrompt(String productName, Integer categoryId, String productDesc) {
        return ""
                + "你是二手商品定价助手。\n"
                + "请根据商品信息给出价格区间，只输出 JSON：{\"min\":数字,\"max\":数字}。\n"
                + "约束：min>0，max>=min。不要输出任何多余文字。\n"
                + "商品名称：" + productName + "\n"
                + "分类ID：" + (categoryId == null ? "" : categoryId) + "\n"
                + "商品描述：" + (productDesc == null ? "" : productDesc) + "\n";
    }

    private String fallbackProductDescription(String productName, String keywords) {
        String kw = (keywords == null || keywords.isBlank()) ? "品质保证" : keywords;
        return "### " + productName + "\n\n"
                + "**产品特点：**\n"
                + "- " + kw + "\n"
                + "- 成色良好，功能正常\n\n"
                + "**核心规格：**\n"
                + "- 具体参数可私信确认\n\n"
                + "**使用场景：**\n"
                + "- 日常使用/学习办公\n\n"
                + "**交易说明：**\n"
                + "- 诚心出售，支持沟通细节";
    }

    private Map<String, Object> fallbackPriceEstimate() {
        Map<String, Object> m = new HashMap<>();
        m.put("min", 100);
        m.put("max", 300);
        m.put("source", "fallback");
        return m;
    }

    private void setFailureReason(String reason) {
        lastFailureReason.set(reason == null || reason.isBlank() ? "unknown" : reason);
    }

    private void clearFailureReason() {
        lastFailureReason.remove();
    }

    private String consumeFailureReason(String fallback) {
        String reason = lastFailureReason.get();
        lastFailureReason.remove();
        if (reason == null || reason.isBlank()) return fallback;
        return reason;
    }
}
