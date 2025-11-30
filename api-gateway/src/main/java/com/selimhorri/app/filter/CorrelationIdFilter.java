package com.selimhorri.app.filter;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Correlation ID Filter for API Gateway (Spring Cloud Gateway - Reactive)
 * 
 * Generates or extracts correlation ID from incoming requests
 * and adds it to the response headers.
 * 
 * This ensures all requests can be traced across the entire system.
 */
@Component
@Slf4j
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Get or generate correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }
        
        // Add correlation ID to request headers for downstream services
        final String finalCorrelationId = correlationId;
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        
        // Process request through filter chain
        // Note: DedupeResponseHeader in application.yml will automatically deduplicate
        // X-Correlation-ID headers if both API Gateway and downstream service add them
        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            // Ensure correlation ID is in response headers
            // If downstream service already added it, DedupeResponseHeader will keep the first one
            // If not present, we add it here
            if (!mutatedExchange.getResponse().getHeaders().containsKey(CORRELATION_ID_HEADER)) {
                mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
            }
        }));
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
