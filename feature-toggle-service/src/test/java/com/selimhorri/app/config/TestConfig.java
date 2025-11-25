package com.selimhorri.app.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration to disable Eureka client in tests
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    EurekaClientAutoConfiguration.class,
    EurekaDiscoveryClientConfiguration.class
})
@Profile("test")
public class TestConfig {
    // This class disables Eureka auto-configuration for tests
}

