package com.selimhorri.app.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.selimhorri.app.annotation.FeatureToggle;
import com.selimhorri.app.service.FeatureToggleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for intercepting methods annotated with @FeatureToggle
 * 
 * Checks if the feature is enabled before executing the method.
 * If disabled, either calls fallback method or throws exception.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class FeatureToggleAspect {

    private final FeatureToggleService featureToggleService;

    @Around("@annotation(com.selimhorri.app.annotation.FeatureToggle)")
    public Object checkFeatureToggle(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        FeatureToggle featureToggle = method.getAnnotation(FeatureToggle.class);

        String featureName = featureToggle.name();
        String environment = featureToggle.environment();

        log.debug("Checking feature toggle: {} in environment: {}", featureName, environment);

        boolean isEnabled = featureToggleService.isFeatureEnabled(featureName, environment);

        if (isEnabled) {
            log.debug("Feature '{}' is enabled, proceeding with method execution", featureName);
            return joinPoint.proceed();
        } else {
            log.warn("Feature '{}' is disabled in environment '{}'", featureName, environment);
            
            String fallbackMethod = featureToggle.fallbackMethod();
            if (!fallbackMethod.isEmpty()) {
                return invokeFallbackMethod(joinPoint, fallbackMethod);
            }
            
            throw new RuntimeException(String.format("Feature '%s' is not enabled", featureName));
        }
    }

    private Object invokeFallbackMethod(ProceedingJoinPoint joinPoint, String fallbackMethodName) throws Throwable {
        try {
            Method fallbackMethod = joinPoint.getTarget().getClass()
                    .getMethod(fallbackMethodName, ((MethodSignature) joinPoint.getSignature()).getParameterTypes());
            return fallbackMethod.invoke(joinPoint.getTarget(), joinPoint.getArgs());
        } catch (NoSuchMethodException e) {
            log.error("Fallback method '{}' not found", fallbackMethodName, e);
            throw new RuntimeException("Fallback method not found: " + fallbackMethodName);
        }
    }
}
