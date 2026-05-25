package com.example.onlineshop.service;

import com.example.onlineshop.dto.ai.AiProductQuery;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.service.ProductVectorService.VectorWithProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiVectorRetrieverService {

    @Autowired
    private ProductService productService;

    @Autowired
    private ExternalAiClient externalAiClient;

    @Autowired
    private ProductVectorService productVectorService;

    @Value("${rag.vector.index-ttl-ms:300000}")
    private long indexTtlMs;

    @Value("${rag.vector.min-score:0.40}")
    private double minScore;
    
    @Value("${rag.vector.warmup-on-start:true}")
    private boolean warmupOnStart;

    private volatile List<IndexedProduct> cachedIndex = Collections.emptyList();
    private volatile List<IndexedImage> cachedImageIndex = Collections.emptyList();
    private volatile long lastBuiltAtMs = 0L;
    private final AtomicBoolean isBuildingIndex = new AtomicBoolean(false);
    private volatile String indexBuildStatus = "";

    @PostConstruct
    public void init() {
        if (warmupOnStart) {
            CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("[RAG] 开始从数据库加载索引...");
                    warmupIndex();
                    System.out.println("[RAG] 索引加载完成");
                } catch (Exception e) {
                    System.err.println("[RAG] 索引加载失败: " + e.getMessage());
                }
            });
        }
    }
    
    public void warmupIndex() {
        loadIndexFromDatabase();
    }
    
    public String getIndexStatus() {
        if (isBuildingIndex.get()) {
            return "索引加载中... " + indexBuildStatus;
        }
        if (cachedImageIndex.isEmpty()) {
            return "图片索引为空，请发布商品后等待向量生成";
        }
        return "图片索引已就绪，共 " + cachedImageIndex.size() + " 张图片";
    }

    public RetrievalResult retrieve(AiProductQuery query, String userText, String historyContext, int page, int size, Set<Integer> candidateIds) {
        List<IndexedProduct> index = loadIndexFromDatabase();
        if (index.isEmpty()) return new RetrievalResult(0, Collections.emptyList());

        String semanticText = buildSemanticText(query, userText, historyContext);
        List<Double> queryVector = externalAiClient.generateEmbedding(semanticText);

        List<ProductScore> scores = new ArrayList<>();
        for (IndexedProduct ip : index) {
            if (candidateIds != null && !candidateIds.isEmpty() && !candidateIds.contains(ip.product().getProductId())) {
                continue;
            }
            if (!matchesFilter(ip.product(), query)) continue;
            double score = queryVector.isEmpty() ? lexicalScore(semanticText, ip.searchText()) : cosine(queryVector, ip.vector());
            if (score >= minScore) {
                scores.add(new ProductScore(ip.product(), score));
            }
        }

        scores.sort(Comparator.comparingDouble(ProductScore::score).reversed()
                .thenComparing(ps -> ps.product().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())));

        int total = scores.size();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);

        List<ScoredProduct> items = new ArrayList<>();
        for (ProductScore ps : scores.subList(from, to)) {
            items.add(new ScoredProduct(ps.product(), ps.score()));
        }
        return new RetrievalResult(total, items);
    }

    public RetrievalResult searchByImage(String imageUrl, int page, int size) {
        if (isBuildingIndex.get()) {
            System.out.println("[RAG] 图片搜索: 索引正在加载中，请稍后重试");
            throw new IllegalStateException("索引正在加载中，请稍后重试。" + indexBuildStatus);
        }
        
        List<IndexedImage> index = loadImageIndexFromDatabase();
        System.out.println("[RAG] 图片搜索: 图片索引数量 = " + index.size());
        if (index.isEmpty()) {
            System.out.println("[RAG] 图片搜索: 图片索引为空，请先执行向量生成");
            return new RetrievalResult(0, Collections.emptyList());
        }

        System.out.println("[RAG] 图片搜索: 开始处理用户图片 " + imageUrl);
        String imageDesc = externalAiClient.generateImageDescription(imageUrl, "");
        if (imageDesc == null || imageDesc.isBlank()) {
            System.out.println("[RAG] 图片搜索: 无法生成图片描述");
            return new RetrievalResult(0, Collections.emptyList());
        }
        System.out.println("[RAG] 图片搜索: 图片描述 - " + imageDesc.substring(0, Math.min(200, imageDesc.length())));
        
        List<Double> queryVector = externalAiClient.generateEmbedding(imageDesc);
        System.out.println("[RAG] 图片搜索: 查询向量维度 = " + queryVector.size());
        if (queryVector.isEmpty()) {
            System.out.println("[RAG] 图片搜索: 无法生成向量");
            return new RetrievalResult(0, Collections.emptyList());
        }

        List<ProductScore> scores = new ArrayList<>();
        double maxScore = 0;
        for (IndexedImage ii : index) {
            double score = cosine(queryVector, ii.vector());
            if (score > maxScore) maxScore = score;
            if (score >= minScore) {
                scores.add(new ProductScore(ii.product(), score));
            }
        }
        
        System.out.println("[RAG] 图片搜索: 最高相似度 = " + maxScore + ", 阈值 = " + minScore + ", 匹配数 = " + scores.size());

        scores.sort(Comparator.comparingDouble(ProductScore::score).reversed()
                .thenComparing(ps -> ps.product().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())));

        List<ProductScore> uniqueScores = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (ProductScore ps : scores) {
            if (seen.add(ps.product().getProductId())) {
                uniqueScores.add(ps);
            }
        }

        int total = uniqueScores.size();
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);

        List<ScoredProduct> items = new ArrayList<>();
        for (ProductScore ps : uniqueScores.subList(from, to)) {
            items.add(new ScoredProduct(ps.product(), ps.score()));
            System.out.println("[RAG] 图片搜索: 匹配商品 ID=" + ps.product().getProductId() + ", 名称=" + ps.product().getProductName() + ", 相似度=" + ps.score());
        }
        System.out.println("[RAG] 图片搜索: 找到 " + total + " 个匹配商品");
        return new RetrievalResult(total, items);
    }

    private List<IndexedProduct> loadIndexFromDatabase() {
        long now = System.currentTimeMillis();
        if (!cachedIndex.isEmpty() && (now - lastBuiltAtMs) < Math.max(indexTtlMs, 60_000L)) {
            return cachedIndex;
        }

        if (!isBuildingIndex.compareAndSet(false, true)) {
            System.out.println("[RAG] 索引正在加载中，返回当前缓存");
            return cachedIndex;
        }

        try {
            synchronized (this) {
                now = System.currentTimeMillis();
                if (!cachedIndex.isEmpty() && (now - lastBuiltAtMs) < Math.max(indexTtlMs, 60_000L)) {
                    return cachedIndex;
                }

                indexBuildStatus = "从数据库加载向量...";
                
                List<VectorWithProduct> productVectors = productVectorService.loadAllProductVectors();
                List<VectorWithProduct> imageVectors = productVectorService.loadAllImageVectors();
                
                Map<Integer, Product> productMap = new HashMap<>();
                List<Product> products = productService.getAllProducts();
                if (products != null) {
                    for (Product p : products) {
                        if (p != null && p.getProductId() != null) {
                            productMap.put(p.getProductId(), p);
                        }
                    }
                }

                List<IndexedProduct> rebuilt = new ArrayList<>();
                for (VectorWithProduct vwp : productVectors) {
                    Product p = productMap.get(vwp.getProductId());
                    if (p != null) {
                        rebuilt.add(new IndexedProduct(p, vwp.getText(), vwp.getVector()));
                    }
                }

                List<IndexedImage> rebuiltImg = new ArrayList<>();
                for (VectorWithProduct vwp : imageVectors) {
                    Product p = productMap.get(vwp.getProductId());
                    if (p != null) {
                        rebuiltImg.add(new IndexedImage(p, vwp.getImageUrl(), vwp.getVector()));
                    }
                }

                cachedIndex = rebuilt;
                cachedImageIndex = rebuiltImg;
                lastBuiltAtMs = now;
                indexBuildStatus = "";
                System.out.println("[RAG] 索引加载完成，商品: " + rebuilt.size() + ", 图片: " + rebuiltImg.size());
                return cachedIndex;
            }
        } finally {
            isBuildingIndex.set(false);
        }
    }

    private List<IndexedImage> loadImageIndexFromDatabase() {
        loadIndexFromDatabase();
        return cachedImageIndex;
    }

    private String buildSemanticText(AiProductQuery query, String userText, String historyContext) {
        List<String> parts = new ArrayList<>();
        if (historyContext != null && !historyContext.isBlank()) parts.add(historyContext.trim());
        if (userText != null && !userText.isBlank()) parts.add(userText.trim());
        if (query != null && query.getQ() != null && !query.getQ().isBlank()) parts.add(query.getQ().trim());
        return String.join(" ", parts).trim();
    }

    private boolean matchesFilter(Product p, AiProductQuery q) {
        if (q == null) return true;
        if (q.getCategoryId() != null && !Objects.equals(q.getCategoryId(), p.getCategoryId())) return false;
        if (q.getStatus() != null && !q.getStatus().isBlank()) {
            String status = p.getProductStatus() == null ? "" : p.getProductStatus();
            if (!q.getStatus().equalsIgnoreCase(status)) return false;
        }
        if (q.getMinPrice() != null && p.getPrice() != null && p.getPrice().compareTo(q.getMinPrice()) < 0) return false;
        return q.getMaxPrice() == null || p.getPrice() == null || p.getPrice().compareTo(q.getMaxPrice()) <= 0;
    }

    private double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0D;
        int len = Math.min(a.size(), b.size());

        double dot = 0D;
        double na = 0D;
        double nb = 0D;
        for (int i = 0; i < len; i++) {
            double va = a.get(i) == null ? 0D : a.get(i);
            double vb = b.get(i) == null ? 0D : b.get(i);
            dot += va * vb;
            na += va * va;
            nb += vb * vb;
        }
        if (na <= 0D || nb <= 0D) return 0D;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double lexicalScore(String query, String text) {
        if (query == null || query.isBlank() || text == null || text.isBlank()) return 0D;
        String[] tokens = query.toLowerCase(Locale.ROOT).split("[\\s,，]+");
        int hit = 0;
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (!token.isBlank() && lowerText.contains(token)) hit++;
        }
        return tokens.length == 0 ? 0D : ((double) hit) / tokens.length;
    }

    private record IndexedProduct(Product product, String searchText, List<Double> vector) {}

    private record IndexedImage(Product product, String imageUrl, List<Double> vector) {}

    private record ProductScore(Product product, double score) {}

    public record ScoredProduct(Product product, double score) {}

    public record RetrievalResult(int total, List<ScoredProduct> items) {}
}
