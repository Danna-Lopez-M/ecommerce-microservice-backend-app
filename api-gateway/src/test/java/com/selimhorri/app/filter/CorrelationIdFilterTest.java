package com.selimhorri.app.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Unit tests for Correlation ID Filter (Spring Cloud Gateway version)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;
    
    @Mock
    private ServerHttpRequest.Builder requestBuilder;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private GatewayFilterChain chain;
    
    @Mock
    private ServerWebExchange.Builder exchangeBuilder;

    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        requestHeaders = new HttpHeaders();
        responseHeaders = new HttpHeaders();
        
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(requestHeaders);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request((ServerHttpRequest) any())).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void testFilter_WithExistingCorrelationId() {
        // Given
        String existingCorrelationId = "existing-correlation-id-123";
        requestHeaders.add(CORRELATION_ID_HEADER, existingCorrelationId);

        // When
        filter.filter(exchange, chain).block();

        // Then
        assertTrue(responseHeaders.containsKey(CORRELATION_ID_HEADER));
        assertEquals(existingCorrelationId, responseHeaders.getFirst(CORRELATION_ID_HEADER));
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void testFilter_WithoutCorrelationId_ShouldGenerate() {
        // Given - no correlation ID in request

        // When
        filter.filter(exchange, chain).block();

        // Then
        assertTrue(responseHeaders.containsKey(CORRELATION_ID_HEADER));
        String generatedId = responseHeaders.getFirst(CORRELATION_ID_HEADER);
        assertNotNull(generatedId);
        assertFalse(generatedId.isEmpty());
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void testFilter_WithEmptyCorrelationId_ShouldGenerate() {
        // Given
        requestHeaders.add(CORRELATION_ID_HEADER, "");

        // When
        filter.filter(exchange, chain).block();

        // Then
        assertTrue(responseHeaders.containsKey(CORRELATION_ID_HEADER));
        String generatedId = responseHeaders.getFirst(CORRELATION_ID_HEADER);
        assertNotNull(generatedId);
        assertFalse(generatedId.isEmpty());
    }

    @Test
    void testFilter_AddsHeaderToDownstreamRequest() {
        // Given
        String correlationId = "test-correlation-id";
        requestHeaders.add(CORRELATION_ID_HEADER, correlationId);

        // When
        filter.filter(exchange, chain).block();

        // Then
        verify(requestBuilder).header(CORRELATION_ID_HEADER, correlationId);
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void testGetOrder_ReturnsHighestPrecedence() {
        // When
        int order = filter.getOrder();

        // Then - Ordered.HIGHEST_PRECEDENCE is Integer.MIN_VALUE
        assertEquals(Ordered.HIGHEST_PRECEDENCE, order);
    }
}
