package com.selimhorri.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be controlled by feature toggles
 * 
 * Usage:
 * @FeatureToggle(name = "new-search-feature")
 * public List<Product> advancedSearch(String query) {
 *     // implementation
 * }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureToggle {
    
    /**
     * Name of the feature toggle
     */
    String name();
    
    /**
     * Environment to check (defaults to "stage")
     */
    String environment() default "stage";
    
    /**
     * Fallback method name to call if feature is disabled
     */
    String fallbackMethod() default "";
}
