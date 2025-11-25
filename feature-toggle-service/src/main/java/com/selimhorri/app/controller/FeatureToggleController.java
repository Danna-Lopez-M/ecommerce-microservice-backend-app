package com.selimhorri.app.controller;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.selimhorri.app.domain.FeatureToggle;
import com.selimhorri.app.service.FeatureToggleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feature Toggle REST Controller
 * 
 * Provides API endpoints for managing feature flags
 */
@RestController
@RequestMapping("/api/features")
@Slf4j
@RequiredArgsConstructor
public class FeatureToggleController {

    private final FeatureToggleService service;

    @GetMapping
    public ResponseEntity<List<FeatureToggle>> getAllFeatures() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/environment/{environment}")
    public ResponseEntity<List<FeatureToggle>> getFeaturesByEnvironment(@PathVariable String environment) {
        return ResponseEntity.ok(service.findByEnvironment(environment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeatureToggle> getFeatureById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/check/{name}")
    public ResponseEntity<Map<String, Boolean>> checkFeature(
            @PathVariable String name,
            @RequestParam(defaultValue = "dev") String environment) {
        boolean enabled = service.isFeatureEnabled(name, environment);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @PostMapping
    public ResponseEntity<FeatureToggle> createFeature(@Valid @RequestBody FeatureToggle featureToggle) {
        FeatureToggle created = service.create(featureToggle);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeatureToggle> updateFeature(
            @PathVariable Long id,
            @Valid @RequestBody FeatureToggle featureToggle) {
        return ResponseEntity.ok(service.update(id, featureToggle));
    }

    @PutMapping("/{name}/enable")
    public ResponseEntity<Void> enableFeature(@PathVariable String name) {
        service.enable(name);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{name}/disable")
    public ResponseEntity<Void> disableFeature(@PathVariable String name) {
        service.disable(name);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
