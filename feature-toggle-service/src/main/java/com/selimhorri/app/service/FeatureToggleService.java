package com.selimhorri.app.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.selimhorri.app.domain.FeatureToggle;
import com.selimhorri.app.repository.FeatureToggleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feature Toggle Service
 * 
 * Manages feature flags with caching for performance.
 * Cache is invalidated when features are created, updated, or deleted.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FeatureToggleService {

    private final FeatureToggleRepository repository;
    private static final String DEFAULT_ENVIRONMENT = "dev";

    @Cacheable(value = "features", key = "#name + '-' + #environment")
    public boolean isFeatureEnabled(String name, String environment) {
        log.debug("Checking if feature '{}' is enabled in environment '{}'", name, environment);
        return repository.findByNameAndEnvironment(name, environment)
                .map(FeatureToggle::getEnabled)
                .orElse(false);
    }

    @Cacheable(value = "features", key = "#name + '-dev'")
    public boolean isFeatureEnabled(String name) {
        return isFeatureEnabled(name, DEFAULT_ENVIRONMENT);
    }

    public List<FeatureToggle> findAll() {
        log.info("Fetching all feature toggles");
        return repository.findAll();
    }

    public List<FeatureToggle> findByEnvironment(String environment) {
        log.info("Fetching feature toggles for environment: {}", environment);
        return repository.findByEnvironment(environment);
    }

    public FeatureToggle findById(Long id) {
        log.info("Fetching feature toggle by id: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feature toggle not found with id: " + id));
    }

    public FeatureToggle findByName(String name) {
        log.info("Fetching feature toggle by name: {}", name);
        return repository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Feature toggle not found with name: " + name));
    }

    @CacheEvict(value = "features", allEntries = true)
    public FeatureToggle create(FeatureToggle featureToggle) {
        log.info("Creating new feature toggle: {}", featureToggle.getName());
        return repository.save(featureToggle);
    }

    @CacheEvict(value = "features", allEntries = true)
    public FeatureToggle update(Long id, FeatureToggle featureToggle) {
        log.info("Updating feature toggle with id: {}", id);
        FeatureToggle existing = findById(id);
        
        if (featureToggle.getName() != null) {
            existing.setName(featureToggle.getName());
        }
        if (featureToggle.getEnabled() != null) {
            existing.setEnabled(featureToggle.getEnabled());
        }
        if (featureToggle.getDescription() != null) {
            existing.setDescription(featureToggle.getDescription());
        }
        if (featureToggle.getEnvironment() != null) {
            existing.setEnvironment(featureToggle.getEnvironment());
        }
        
        return repository.save(existing);
    }

    @CacheEvict(value = "features", allEntries = true)
    public void enable(String name) {
        log.info("Enabling feature: {}", name);
        FeatureToggle feature = findByName(name);
        feature.setEnabled(true);
        repository.save(feature);
    }

    @CacheEvict(value = "features", allEntries = true)
    public void disable(String name) {
        log.info("Disabling feature: {}", name);
        FeatureToggle feature = findByName(name);
        feature.setEnabled(false);
        repository.save(feature);
    }

    @CacheEvict(value = "features", allEntries = true)
    public void delete(Long id) {
        log.info("Deleting feature toggle with id: {}", id);
        repository.deleteById(id);
    }
}
