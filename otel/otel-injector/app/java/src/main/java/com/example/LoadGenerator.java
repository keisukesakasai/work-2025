package com.example;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class LoadGenerator {
    private static final Logger logger = LoggerFactory.getLogger(LoadGenerator.class);
    
    private final String serverUrl;
    private final int requestInterval;
    private final int concurrentRequests;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Random random;
    
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    private final List<String> endpoints = Arrays.asList(
        "/",
        "/api/users", 
        "/api/orders",
        "/health"
    );
    
    public LoadGenerator() {
        this.serverUrl = System.getenv().getOrDefault("SERVER_URL", "http://nodejs-server:3000");
        this.requestInterval = Integer.parseInt(System.getenv().getOrDefault("REQUEST_INTERVAL", "2000"));
        this.concurrentRequests = Integer.parseInt(System.getenv().getOrDefault("CONCURRENT_REQUESTS", "3"));
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .build();
            
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
        
        logger.info("Java Load Generator starting...");
        logger.info("Target server: {}", serverUrl);
        logger.info("Request interval: {}ms", requestInterval);
        logger.info("Concurrent requests: {}", concurrentRequests);
    }
    
    public void start() {
        logger.info("🚀 Java Load Generator started!");
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(concurrentRequests + 1);
        
        // Wait a bit for the server to be ready
        scheduler.schedule(() -> {
            // Start continuous load generation
            scheduler.scheduleAtFixedRate(
                this::generateLoad,
                0,
                requestInterval,
                TimeUnit.MILLISECONDS
            );
        }, 5, TimeUnit.SECONDS);
        
        // Statistics reporter
        scheduler.scheduleAtFixedRate(
            this::reportStatistics,
            10,
            10,
            TimeUnit.SECONDS
        );
        
        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("📊 Final Statistics:");
            logger.info("Total requests: {}", requestCount.get());
            logger.info("Successful: {}", successCount.get());
            logger.info("Errors: {}", errorCount.get());
            double successRate = requestCount.get() > 0 ? 
                (successCount.get() * 100.0) / requestCount.get() : 0.0;
            logger.info("Success rate: {:.1f}%", successRate);
            logger.info("👋 Java Load Generator shutting down...");
            
            scheduler.shutdown();
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }));
    }
    
    private void generateLoad() {
        for (int i = 0; i < concurrentRequests; i++) {
            String endpoint = endpoints.get(random.nextInt(endpoints.size()));
            makeRequestAsync(endpoint);
        }
    }
    
    private void makeRequestAsync(String endpoint) {
        String url = serverUrl + endpoint;
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", "JavaLoadGenerator/1.0")
            .addHeader("X-Trace-Source", "java-load-generator")
            .build();
            
        Instant startTime = Instant.now();
        long reqNum = requestCount.incrementAndGet();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                long duration = Duration.between(startTime, Instant.now()).toMillis();
                errorCount.incrementAndGet();
                logger.warn("✗ ERROR {} ({}ms) [{}] - {}", 
                    endpoint, duration, reqNum, e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                long duration = Duration.between(startTime, Instant.now()).toMillis();
                
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful()) {
                        successCount.incrementAndGet();
                        
                        // Try to parse JSON response for additional information
                        String serviceInfo = "";
                        if (body != null) {
                            try {
                                String responseBody = body.string();
                                JsonNode jsonNode = objectMapper.readTree(responseBody);
                                if (jsonNode.has("service")) {
                                    serviceInfo = " -> " + jsonNode.get("service").asText();
                                }
                            } catch (Exception e) {
                                // Ignore JSON parsing errors
                            }
                        }
                        
                        logger.info("✓ {} {} ({}ms) [{}]{}", 
                            response.code(), endpoint, duration, reqNum, serviceInfo);
                    } else {
                        errorCount.incrementAndGet();
                        logger.warn("✗ {} {} ({}ms) [{}]", 
                            response.code(), endpoint, duration, reqNum);
                    }
                }
            }
        });
    }
    
    private void reportStatistics() {
        long total = requestCount.get();
        long success = successCount.get();
        long errors = errorCount.get();
        
        if (total > 0) {
            double successRate = (success * 100.0) / total;
            logger.info("📊 Statistics: {} requests, {} success, {} errors ({:.1f}% success rate)",
                total, success, errors, successRate);
        }
    }
    
    public static void main(String[] args) {
        try {
            LoadGenerator loadGenerator = new LoadGenerator();
            loadGenerator.start();
            
            // Keep the application running
            Thread.currentThread().join();
        } catch (Exception e) {
            logger.error("Error starting load generator", e);
            System.exit(1);
        }
    }
} 