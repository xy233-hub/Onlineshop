package com.example.onlineshop.service;

import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.ProductVector;
import com.example.onlineshop.mapper.ProductVectorMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ProductVectorService {

    @Autowired
    private ProductVectorMapper productVectorMapper;

    @Autowired
    private ExternalAiClient externalAiClient;

    @Autowired
    private ProductService productService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean isGenerating = new AtomicBoolean(false);
    private final AtomicInteger totalProducts = new AtomicInteger(0);
    private final AtomicInteger processedProducts = new AtomicInteger(0);
    private final AtomicInteger totalImages = new AtomicInteger(0);
    private final AtomicInteger processedImages = new AtomicInteger(0);
    private volatile String lastError = "";

    public boolean isGenerating() {
        return isGenerating.get();
    }

    public VectorGenerationProgress getProgress() {
        return new VectorGenerationProgress(
                isGenerating.get(),
                totalProducts.get(),
                processedProducts.get(),
                totalImages.get(),
                processedImages.get(),
                lastError
        );
    }

    @Async
    public void generateAllVectorsAsync() {
        if (!isGenerating.compareAndSet(false, true)) {
            System.out.println("[Vector] 已有生成任务在运行中");
            return;
        }

        try {
            totalProducts.set(0);
            processedProducts.set(0);
            totalImages.set(0);
            processedImages.set(0);
            lastError = "";

            List<Product> products = productService.getAllProducts();
            if (products == null || products.isEmpty()) {
                System.out.println("[Vector] 没有商品数据");
                return;
            }

            List<Product> onlineProducts = products.stream()
                    .filter(p -> p != null && p.getProductId() != null)
                    .filter(p -> "online".equalsIgnoreCase(p.getProductStatus() != null ? p.getProductStatus().trim() : ""))
                    .collect(Collectors.toList());

            totalProducts.set(onlineProducts.size());
            System.out.println("[Vector] 开始生成向量，共 " + onlineProducts.size() + " 个在线商品");

            for (Product p : onlineProducts) {
                try {
                    generateAndStoreProductVector(p);
                    processedProducts.incrementAndGet();

                    if (p.getImages() != null && !p.getImages().isEmpty()) {
                        productVectorMapper.deleteImageVectorsByProductId(p.getProductId());
                        for (String imageUrl : p.getImages()) {
                            if (imageUrl != null && !imageUrl.isBlank()) {
                                totalImages.incrementAndGet();
                                try {
                                    generateAndStoreImageVector(p, imageUrl);
                                    processedImages.incrementAndGet();
                                } catch (Exception imgEx) {
                                    System.err.println("[Vector] 图片向量生成失败: " + imageUrl + ", error=" + imgEx.getMessage());
                                }
                            }
                        }
                    }

                    System.out.println("[Vector] 进度: 商品 " + processedProducts.get() + "/" + totalProducts.get() + 
                            ", 图片 " + processedImages.get() + "/" + totalImages.get());

                } catch (Exception e) {
                    lastError = "商品 " + p.getProductId() + " 处理失败: " + e.getMessage();
                    System.err.println("[Vector] " + lastError);
                }
            }

            System.out.println("[Vector] 向量生成完成，商品: " + processedProducts.get() + "/" + totalProducts.get() + 
                    ", 图片: " + processedImages.get() + "/" + totalImages.get());

        } catch (Exception e) {
            lastError = "生成失败: " + e.getMessage();
            System.err.println("[Vector] 批量生成失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isGenerating.set(false);
        }
    }

    public void generateAndStoreProductVector(Product product) {
        if (product == null || product.getProductId() == null) return;
        
        try {
            String text = buildProductText(product);
            if (text == null || text.isBlank()) return;
            
            List<Double> vector = externalAiClient.generateEmbedding(text);
            if (vector == null || vector.isEmpty()) return;
            
            String vectorJson = objectMapper.writeValueAsString(vector);
            
            ProductVector existing = productVectorMapper.selectProductVector(product.getProductId());
            if (existing != null) {
                ProductVector pv = ProductVector.builder()
                        .productId(product.getProductId())
                        .vectorType("product")
                        .vectorText(text)
                        .vectorData(vectorJson)
                        .build();
                productVectorMapper.updateByProductAndType(pv);
            } else {
                ProductVector pv = ProductVector.builder()
                        .productId(product.getProductId())
                        .vectorType("product")
                        .vectorText(text)
                        .vectorData(vectorJson)
                        .build();
                productVectorMapper.insert(pv);
            }
            System.out.println("[Vector] 已存储商品向量: productId=" + product.getProductId());
        } catch (Exception e) {
            System.err.println("[Vector] 存储商品向量失败: productId=" + product.getProductId() + ", error=" + e.getMessage());
        }
    }

    public void generateAndStoreImageVector(Product product, String imageUrl) {
        if (product == null || product.getProductId() == null || imageUrl == null || imageUrl.isBlank()) return;
        
        try {
            String productText = buildProductText(product);
            String imageDesc = externalAiClient.generateImageDescription(imageUrl, productText);
            if (imageDesc == null || imageDesc.isBlank()) return;
            
            List<Double> vector = externalAiClient.generateEmbedding(imageDesc);
            if (vector == null || vector.isEmpty()) return;
            
            String vectorJson = objectMapper.writeValueAsString(vector);
            
            ProductVector pv = ProductVector.builder()
                    .productId(product.getProductId())
                    .vectorType("image")
                    .imageUrl(imageUrl)
                    .vectorText(imageDesc)
                    .vectorData(vectorJson)
                    .build();
            
            productVectorMapper.insert(pv);
            System.out.println("[Vector] 已存储图片向量: productId=" + product.getProductId() + ", imageUrl=" + imageUrl);
        } catch (Exception e) {
            System.err.println("[Vector] 存储图片向量失败: imageUrl=" + imageUrl + ", error=" + e.getMessage());
        }
    }

    @Async
    public void generateAndStoreAllVectorsAsync(Product product) {
        generateAndStoreAllVectors(product);
    }

    public void generateAndStoreAllVectors(Product product) {
        if (product == null || product.getProductId() == null) return;
        
        generateAndStoreProductVector(product);
        
        if (product.getImages() != null) {
            productVectorMapper.deleteImageVectorsByProductId(product.getProductId());
            for (String imageUrl : product.getImages()) {
                if (imageUrl != null && !imageUrl.isBlank()) {
                    generateAndStoreImageVector(product, imageUrl);
                }
            }
        }
    }

    public void deleteProductVectors(Integer productId) {
        if (productId == null) return;
        productVectorMapper.deleteByProductId(productId);
        System.out.println("[Vector] 已删除商品向量: productId=" + productId);
    }

    public List<VectorWithProduct> loadAllImageVectors() {
        List<ProductVector> vectors = productVectorMapper.selectOnlineImageVectors();
        return vectors.stream()
                .map(this::toVectorWithProduct)
                .filter(v -> v != null && v.getVector() != null && !v.getVector().isEmpty())
                .collect(Collectors.toList());
    }

    public List<VectorWithProduct> loadAllProductVectors() {
        List<ProductVector> vectors = productVectorMapper.selectOnlineProductVectors();
        return vectors.stream()
                .map(this::toVectorWithProduct)
                .filter(v -> v != null && v.getVector() != null && !v.getVector().isEmpty())
                .collect(Collectors.toList());
    }

    private VectorWithProduct toVectorWithProduct(ProductVector pv) {
        try {
            List<Double> vector = objectMapper.readValue(pv.getVectorData(), new TypeReference<List<Double>>() {});
            return new VectorWithProduct(pv.getProductId(), pv.getImageUrl(), pv.getVectorText(), vector);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildProductText(Product p) {
        return String.join(" ",
                norm(p.getProductName()),
                norm(p.getShortDesc()),
                norm(p.getProductDesc()),
                norm(p.getSearchKeywords())
        ).trim();
    }

    private String norm(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').trim();
    }

    public static class VectorWithProduct {
        private final Integer productId;
        private final String imageUrl;
        private final String text;
        private final List<Double> vector;

        public VectorWithProduct(Integer productId, String imageUrl, String text, List<Double> vector) {
            this.productId = productId;
            this.imageUrl = imageUrl;
            this.text = text;
            this.vector = vector;
        }

        public Integer getProductId() { return productId; }
        public String getImageUrl() { return imageUrl; }
        public String getText() { return text; }
        public List<Double> getVector() { return vector; }
    }

    public static class VectorGenerationProgress {
        private final boolean isGenerating;
        private final int totalProducts;
        private final int processedProducts;
        private final int totalImages;
        private final int processedImages;
        private final String lastError;

        public VectorGenerationProgress(boolean isGenerating, int totalProducts, int processedProducts, 
                                        int totalImages, int processedImages, String lastError) {
            this.isGenerating = isGenerating;
            this.totalProducts = totalProducts;
            this.processedProducts = processedProducts;
            this.totalImages = totalImages;
            this.processedImages = processedImages;
            this.lastError = lastError;
        }

        public boolean isGenerating() { return isGenerating; }
        public int getTotalProducts() { return totalProducts; }
        public int getProcessedProducts() { return processedProducts; }
        public int getTotalImages() { return totalImages; }
        public int getProcessedImages() { return processedImages; }
        public String getLastError() { return lastError; }
    }
}
