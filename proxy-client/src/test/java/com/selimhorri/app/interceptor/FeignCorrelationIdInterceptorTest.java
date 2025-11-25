package com.selimhorri.app.interceptor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import feign.RequestTemplate;

/**
 * Unit tests for Feign Correlation ID Interceptor
 * Uses real RequestTemplate instances instead of mocks since RequestTemplate is final
 */
class FeignCorrelationIdInterceptorTest {

    private FeignCorrelationIdInterceptor interceptor;

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @BeforeEach
    void setUp() {
        interceptor = new FeignCorrelationIdInterceptor();
        MDC.clear();
    }

    @Test
    void testApply_WithCorrelationIdInMDC_ShouldAddHeader() {
        // Given
        String correlationId = "test-correlation-id-123";
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        RequestTemplate template = new RequestTemplate();

        // When
        interceptor.apply(template);

        // Then
        Map<String, Collection<String>> headers = template.headers();
        assertTrue(headers.containsKey(CORRELATION_ID_HEADER));
        Collection<String> headerValues = headers.get(CORRELATION_ID_HEADER);
        assertNotNull(headerValues);
        assertEquals(1, headerValues.size());
        assertTrue(headerValues.contains(correlationId));
    }

    @Test
    void testApply_WithoutCorrelationIdInMDC_ShouldNotAddHeader() {
        // Given - MDC is empty
        RequestTemplate template = new RequestTemplate();

        // When
        interceptor.apply(template);

        // Then
        Map<String, Collection<String>> headers = template.headers();
        assertFalse(headers.containsKey(CORRELATION_ID_HEADER));
    }

    @Test
    void testApply_WithNullCorrelationIdInMDC_ShouldNotAddHeader() {
        // Given
        MDC.put(CORRELATION_ID_MDC_KEY, null);
        RequestTemplate template = new RequestTemplate();

        // When
        interceptor.apply(template);

        // Then
        Map<String, Collection<String>> headers = template.headers();
        assertFalse(headers.containsKey(CORRELATION_ID_HEADER));
    }

    @Test
    void testApply_MultipleCalls_ShouldPropagateDifferentIds() {
        // Given
        String correlationId1 = "correlation-id-1";
        String correlationId2 = "correlation-id-2";

        // When - First call
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId1);
        RequestTemplate template1 = new RequestTemplate();
        interceptor.apply(template1);

        // When - Second call
        MDC.clear();
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId2);
        RequestTemplate template2 = new RequestTemplate();
        interceptor.apply(template2);

        // Then
        Collection<String> headers1 = template1.headers().get(CORRELATION_ID_HEADER);
        Collection<String> headers2 = template2.headers().get(CORRELATION_ID_HEADER);
        
        assertNotNull(headers1);
        assertNotNull(headers2);
        assertTrue(headers1.contains(correlationId1));
        assertTrue(headers2.contains(correlationId2));
    }
}
