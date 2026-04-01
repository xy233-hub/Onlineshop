package com.example.onlineshop.service;

import com.example.onlineshop.dto.ai.AiProductQuery;
import com.example.onlineshop.entity.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiVectorRetrieverService {

    private final ProductService productService;
    private final ExternalAiClient externalAiClient;

    @Value("${rag.vector.index-ttl-ms:300000}")
    private long indexTtlMs;

    @Value("${rag.vector.min-score:0.15}")
    private double minScore;

    private volatile List<IndexedProduct> cachedIndex = Collections.emptyList();
    private volatile long lastBuiltAtMs = 0L;

    public AiVectorRetrieverService(ProductService productService, ExternalAiClient externalAiClient) {
        this.productService = productService;
        this.externalAiClient = externalAiClient;
    }

    public RetrievalResult retrieve(AiProductQuery query, String userText, int page, int size) {
        List<IndexedProduct> index = loadIndex();
        if (index.isEmpty()) return new RetrievalResult(0, Collections.emptyList());

        String semanticText = buildSemanticText(query, userText);
        List<Double> queryVector = externalAiClient.generateEmbedding(semanticText);

        List<ProductScore> scores = new ArrayList<>();
        for (IndexedProduct ip : index) {
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

        List<Product> items = scores.subList(from, to).stream().map(ProductScore::product).collect(Collectors.toList());
        return new RetrievalResult(total, items);
    }

    private List<IndexedProduct> loadIndex() {
        long now = System.currentTimeMillis();
        if (!cachedIndex.isEmpty() && (now - lastBuiltAtMs) < Math.max(indexTtlMs, 60_000L)) {
            return cachedIndex;
        }

        synchronized (this) {
            now = System.currentTimeMillis();
            if (!cachedIndex.isEmpty() && (now - lastBuiltAtMs) < Math.max(indexTtlMs, 60_000L)) {
                return cachedIndex;
            }

            List<Product> products = productService.getAllProducts();
            if (products == null || products.isEmpty()) {
                cachedIndex = Collections.emptyList();
                lastBuiltAtMs = now;
                return cachedIndex;
            }

            List<IndexedProduct> rebuilt = new ArrayList<>();
            for (Product p : products) {
                if (p == null) continue;
                String status = p.getProductStatus();
                if (status == null || !"online".equalsIgnoreCase(status.trim())) continue;

                String text = buildProductEmbeddingText(p);
                if (text.isBlank()) continue;
                List<Double> embedding = externalAiClient.generateEmbedding(text);
                rebuilt.add(new IndexedProduct(p, text, embedding));
            }
            cachedIndex = rebuilt;
            lastBuiltAtMs = now;
            return cachedIndex;
        }
    }

    private String buildProductEmbeddingText(Product p) {
        return String.join(" ",
                norm(p.getProductName()),
                norm(p.getShortDesc()),
                norm(p.getProductDesc()),
                norm(p.getSearchKeywords())
        ).trim();
    }

    private String buildSemanticText(AiProductQuery query, String userText) {
        List<String> parts = new ArrayList<>();
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

    private String norm(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private record IndexedProduct(Product product, String searchText, List<Double> vector) {}

    private record ProductScore(Product product, double score) {}

    public record RetrievalResult(int total, List<Product> items) {}
}

