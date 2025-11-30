package com.selimhorri.app.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.FeatureToggle;
import com.selimhorri.app.repository.FeatureToggleRepository;

/**
 * Integration tests for Feature Toggle REST API
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureToggleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeatureToggleRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testCreateFeature_Success() throws Exception {
        // Given
        FeatureToggle feature = FeatureToggle.builder()
                .name("integration-test-feature")
                .enabled(false)
                .description("Integration test feature")
                .environment("dev")
                .build();

        // When & Then
        mockMvc.perform(post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(feature)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("integration-test-feature"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.environment").value("dev"));
    }

    @Test
    void testGetAllFeatures_Success() throws Exception {
        // Given
        repository.save(FeatureToggle.builder().name("feature1").enabled(true).environment("dev").build());
        repository.save(FeatureToggle.builder().name("feature2").enabled(false).environment("dev").build());

        // When & Then
        mockMvc.perform(get("/api/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testCheckFeature_Enabled() throws Exception {
        // Given
        repository.save(FeatureToggle.builder()
                .name("enabled-feature")
                .enabled(true)
                .environment("dev")
                .build());

        // When & Then
        mockMvc.perform(get("/api/features/check/enabled-feature")
                .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void testCheckFeature_Disabled() throws Exception {
        // Given
        repository.save(FeatureToggle.builder()
                .name("disabled-feature")
                .enabled(false)
                .environment("dev")
                .build());

        // When & Then
        mockMvc.perform(get("/api/features/check/disabled-feature")
                .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void testCheckFeature_NotExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/features/check/non-existent")
                .param("environment", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void testEnableFeature_Success() throws Exception {
        // Given
        repository.save(FeatureToggle.builder()
                .name("toggle-feature")
                .enabled(false)
                .environment("dev")
                .build());

        // When & Then
        mockMvc.perform(put("/api/features/toggle-feature/enable"))
                .andExpect(status().isOk());

        // Verify
        mockMvc.perform(get("/api/features/check/toggle-feature")
                .param("environment", "dev"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void testDisableFeature_Success() throws Exception {
        // Given
        repository.save(FeatureToggle.builder()
                .name("toggle-feature")
                .enabled(true)
                .environment("dev")
                .build());

        // When & Then
        mockMvc.perform(put("/api/features/toggle-feature/disable"))
                .andExpect(status().isOk());

        // Verify
        mockMvc.perform(get("/api/features/check/toggle-feature")
                .param("environment", "dev"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void testUpdateFeature_Success() throws Exception {
        // Given
        FeatureToggle saved = repository.save(FeatureToggle.builder()
                .name("update-feature")
                .enabled(false)
                .description("Original")
                .environment("dev")
                .build());

        // Create update object with all required fields
        FeatureToggle update = FeatureToggle.builder()
                .name("update-feature") // Keep the same name
                .enabled(true)
                .description("Updated")
                .environment("dev") // Keep the same environment
                .build();

        // When & Then
        mockMvc.perform(put("/api/features/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void testDeleteFeature_Success() throws Exception {
        // Given
        FeatureToggle saved = repository.save(FeatureToggle.builder()
                .name("delete-feature")
                .enabled(false)
                .environment("dev")
                .build());

        // When & Then - Delete should succeed
        mockMvc.perform(delete("/api/features/" + saved.getId()))
                .andExpect(status().isNoContent());

        // Verify - feature should no longer exist in repository
        assertFalse(repository.findById(saved.getId()).isPresent());
    }

    @Test
    void testGetFeaturesByEnvironment_Success() throws Exception {
        // Given
        repository.save(FeatureToggle.builder().name("dev-feature").enabled(true).environment("dev").build());
        repository.save(FeatureToggle.builder().name("prod-feature").enabled(true).environment("prod").build());

        // When & Then
        mockMvc.perform(get("/api/features/environment/dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("dev-feature"));
    }
}
