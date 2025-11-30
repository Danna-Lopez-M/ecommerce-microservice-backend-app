package com.selimhorri.app.domain;

import java.time.LocalDateTime;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feature Toggle Entity
 * 
 * Represents a feature flag that can be toggled on/off dynamically
 * without requiring application restart.
 */
@Entity
@Table(name = "feature_toggles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureToggle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Feature name is required")
    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String environment = "dev"; // dev, stage, prod

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
