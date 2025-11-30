package com.selimhorri.app.interceptor;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign Correlation ID Interceptor
 * 
 * Propagates correlation ID from MDC to downstream service calls
 * made via Feign clients.
 */
@Component
@Slf4j
public class FeignCorrelationIdInterceptor implements RequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        
        if (correlationId != null) {
            template.header(CORRELATION_ID_HEADER, correlationId);
            log.debug("Propagating correlation ID to downstream service: {}", correlationId);
        }
    }
}
