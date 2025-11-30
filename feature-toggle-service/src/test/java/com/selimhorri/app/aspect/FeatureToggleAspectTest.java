package com.selimhorri.app.aspect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.annotation.FeatureToggle;
import com.selimhorri.app.service.FeatureToggleService;

import java.lang.reflect.Method;

/**
 * Unit tests for Feature Toggle Aspect
 */
@ExtendWith(MockitoExtension.class)
class FeatureToggleAspectTest {

    @Mock
    private FeatureToggleService featureToggleService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @InjectMocks
    private FeatureToggleAspect aspect;

    private Method testMethod;
    private FeatureToggle featureToggleAnnotation;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        testMethod = TestClass.class.getMethod("testMethod");
        featureToggleAnnotation = testMethod.getAnnotation(FeatureToggle.class);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(testMethod);
    }

    @Test
    void testCheckFeatureToggle_WhenEnabled_ShouldProceed() throws Throwable {
        // Given
        when(featureToggleService.isFeatureEnabled("test-feature", "dev")).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // When
        Object result = aspect.checkFeatureToggle(joinPoint);

        // Then
        assertEquals("success", result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void testCheckFeatureToggle_WhenDisabled_ShouldThrowException() {
        // Given
        when(featureToggleService.isFeatureEnabled("test-feature", "dev")).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aspect.checkFeatureToggle(joinPoint);
        });
        
        assertTrue(exception.getMessage().contains("Feature 'test-feature' is not enabled"));
    }

    @Test
    void testCheckFeatureToggle_WhenDisabledWithFallback_ShouldCallFallback() throws Throwable {
        // Given
        Method methodWithFallback = TestClass.class.getMethod("testMethodWithFallback");
        when(signature.getMethod()).thenReturn(methodWithFallback);
        when(featureToggleService.isFeatureEnabled("test-feature-fallback", "dev")).thenReturn(false);
        
        TestClass testInstance = new TestClass();
        when(joinPoint.getTarget()).thenReturn(testInstance);
        when(signature.getParameterTypes()).thenReturn(new Class<?>[0]);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        // When
        Object result = aspect.checkFeatureToggle(joinPoint);

        // Then
        assertEquals("fallback", result);
    }

    // Test class with annotated methods
    public static class TestClass {
        
        @FeatureToggle(name = "test-feature", environment = "dev")
        public String testMethod() {
            return "original";
        }

        @FeatureToggle(name = "test-feature-fallback", environment = "dev", fallbackMethod = "fallbackMethod")
        public String testMethodWithFallback() {
            return "original";
        }

        public String fallbackMethod() {
            return "fallback";
        }
    }
}
