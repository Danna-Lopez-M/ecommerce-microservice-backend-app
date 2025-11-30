package com.selimhorri.app.interceptor;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Correlation ID Interceptor for User Service
 * 
 * Generates or extracts correlation ID from incoming requests and adds it to MDC
 * for logging purposes. Also adds it to the response headers.
 */
@Component
@Order(1)
@Slf4j
public class CorrelationIdInterceptor extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            
            // Generate correlation ID if not provided
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Correlation ID extracted: {}", correlationId);
            }
            
            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add to response headers only if not already set
            // This prevents duplication when called through API Gateway
            if (!response.containsHeader(CORRELATION_ID_HEADER)) {
                response.setHeader(CORRELATION_ID_HEADER, correlationId);
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
    
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
