package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.FeatureToggle;
import com.selimhorri.app.repository.FeatureToggleRepository;

/**
 * Unit tests for Feature Toggle Service
 */
@SpringBootTest
@ActiveProfiles("test")
class FeatureToggleServiceTest {

    @Autowired
    private FeatureToggleService featureToggleService;

    @Autowired
    private FeatureToggleRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testCreateFeatureToggle() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("test-feature")
                .enabled(false)
                .description("Test feature")
                .environment("dev")
                .build();

        // When
        FeatureToggle created = featureToggleService.create(feature);

        // Then
        assertNotNull(created.getId());
        assertEquals("test-feature", created.getName());
        assertFalse(created.getEnabled());
        assertEquals("dev", created.getEnvironment());
    }

    @Test
    void testIsFeatureEnabled_WhenEnabled() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("enabled-feature")
                .enabled(true)
                .environment("dev")
                .build();
        repository.save(feature);

        // When
        boolean result = featureToggleService.isFeatureEnabled("enabled-feature", "dev");

        // Then
        assertTrue(result);
    }

    @Test
    void testIsFeatureEnabled_WhenDisabled() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("disabled-feature")
                .enabled(false)
                .environment("dev")
                .build();
        repository.save(feature);

        // When
        boolean result = featureToggleService.isFeatureEnabled("disabled-feature", "dev");

        // Then
        assertFalse(result);
    }

    @Test
    void testIsFeatureEnabled_WhenNotExists() {
        // When
        boolean result = featureToggleService.isFeatureEnabled("non-existent", "dev");

        // Then
        assertFalse(result);
    }

    @Test
    void testEnableFeature() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("toggle-feature")
                .enabled(false)
                .environment("dev")
                .build();
        repository.save(feature);

        // When
        featureToggleService.enable("toggle-feature");

        // Then
        assertTrue(featureToggleService.isFeatureEnabled("toggle-feature", "dev"));
    }

    @Test
    void testDisableFeature() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("toggle-feature")
                .enabled(true)
                .environment("dev")
                .build();
        repository.save(feature);

        // When
        featureToggleService.disable("toggle-feature");

        // Then
        assertFalse(featureToggleService.isFeatureEnabled("toggle-feature", "dev"));
    }

    @Test
    void testFindAll() {
        // Given
        repository.save(FeatureToggle.builder().name("feature1").enabled(true).environment("dev").build());
        repository.save(FeatureToggle.builder().name("feature2").enabled(false).environment("dev").build());

        // When
        List<FeatureToggle> features = featureToggleService.findAll();

        // Then
        assertEquals(2, features.size());
    }

    @Test
    void testFindByEnvironment() {
        // Given
        repository.save(FeatureToggle.builder().name("dev-feature").enabled(true).environment("dev").build());
        repository.save(FeatureToggle.builder().name("prod-feature").enabled(true).environment("prod").build());

        // When
        List<FeatureToggle> devFeatures = featureToggleService.findByEnvironment("dev");

        // Then
        assertEquals(1, devFeatures.size());
        assertEquals("dev-feature", devFeatures.get(0).getName());
    }

    @Test
    void testUpdateFeature() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("update-feature")
                .enabled(false)
                .description("Original description")
                .environment("dev")
                .build();
        FeatureToggle saved = repository.save(feature);

        // When
        FeatureToggle update = FeatureToggle.builder()
                .enabled(true)
                .description("Updated description")
                .build();
        FeatureToggle updated = featureToggleService.update(saved.getId(), update);

        // Then
        assertTrue(updated.getEnabled());
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    void testDeleteFeature() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("delete-feature")
                .enabled(false)
                .environment("dev")
                .build();
        FeatureToggle saved = repository.save(feature);

        // When
        featureToggleService.delete(saved.getId());

        // Then
        assertFalse(repository.findById(saved.getId()).isPresent());
    }

    @Test
    void testCaching_IsFeatureEnabled() {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("cached-feature")
                .enabled(true)
                .environment("dev")
                .build();
        repository.save(feature);

        // When - First call should hit database
        boolean result1 = featureToggleService.isFeatureEnabled("cached-feature", "dev");
        
        // Second call should use cache
        boolean result2 = featureToggleService.isFeatureEnabled("cached-feature", "dev");

        // Then
        assertTrue(result1);
        assertTrue(result2);
    }
}
