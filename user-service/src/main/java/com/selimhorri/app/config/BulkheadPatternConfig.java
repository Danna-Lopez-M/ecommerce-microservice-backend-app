package com.selimhorri.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class for Resilience4j Bulkhead Pattern
 * 
 * This configuration provides resource isolation by limiting concurrent calls
 * to different categories of operations:
 * - Critical operations (save, update, delete): Higher concurrency limit
 * - Non-critical operations (read-only): Lower concurrency limit
 * 
 * Benefits:
 * - Prevents resource exhaustion from non-critical operations
 * - Ensures critical operations always have resources available
 * - Improves system stability under high load
 */
@Configuration
@Slf4j
public class BulkheadPatternConfig {

    /**
     * Creates a BulkheadRegistry with event listeners for monitoring
     * 
     * @return configured BulkheadRegistry
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
        
        // Add event listeners for monitoring
        registry.bulkhead("userServiceCritical").getEventPublisher()
            .onCallPermitted(event -> log.debug("Bulkhead [userServiceCritical] - Call permitted"))
            .onCallRejected(event -> log.warn("Bulkhead [userServiceCritical] - Call REJECTED - Max concurrent calls reached"))
            .onCallFinished(event -> log.debug("Bulkhead [userServiceCritical] - Call finished"));
        
        registry.bulkhead("userServiceNonCritical").getEventPublisher()
            .onCallPermitted(event -> log.debug("Bulkhead [userServiceNonCritical] - Call permitted"))
            .onCallRejected(event -> log.warn("Bulkhead [userServiceNonCritical] - Call REJECTED - Max concurrent calls reached"))
            .onCallFinished(event -> log.debug("Bulkhead [userServiceNonCritical] - Call finished"));
        
        log.info("Bulkhead Pattern configured successfully with critical and non-critical instances");
        
        return registry;
    }
    
    /**
     * Programmatic configuration for critical operations bulkhead
     * This complements the YAML configuration
     * 
     * @return BulkheadConfig for critical operations
     */
    @Bean
    public BulkheadConfig criticalBulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(10)
            .maxWaitDuration(java.time.Duration.ofMillis(100))
            .build();
    }
    
    /**
     * Programmatic configuration for non-critical operations bulkhead
     * This complements the YAML configuration
     * 
     * @return BulkheadConfig for non-critical operations
     */
    @Bean
    public BulkheadConfig nonCriticalBulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(5)
            .maxWaitDuration(java.time.Duration.ofMillis(50))
            .build();
    }
}
