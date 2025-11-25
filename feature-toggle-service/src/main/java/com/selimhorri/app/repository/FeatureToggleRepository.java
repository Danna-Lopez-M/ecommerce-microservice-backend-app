package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.selimhorri.app.domain.FeatureToggle;

@Repository
public interface FeatureToggleRepository extends JpaRepository<FeatureToggle, Long> {

    Optional<FeatureToggle> findByNameAndEnvironment(String name, String environment);
    
    Optional<FeatureToggle> findByName(String name);
    
    List<FeatureToggle> findByEnvironment(String environment);
    
    List<FeatureToggle> findByEnabled(Boolean enabled);
}
