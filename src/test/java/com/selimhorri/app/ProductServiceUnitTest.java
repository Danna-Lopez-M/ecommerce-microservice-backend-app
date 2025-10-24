package com.selimhorri.app.service;

import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testProduct = new Product();
        testProduct.setProductId(1);
        testProduct.setProductTitle("Test Product");
        testProduct.setSku("TEST-001");
        testProduct.setPriceUnit(99.99);
        testProduct.setQuantity(10);
        
        testProductDto = new ProductDto();
        testProductDto.setProductTitle("Test Product");
        testProductDto.setSku("TEST-001");
        testProductDto.setPriceUnit(99.99);
        testProductDto.setQuantity(10);
    }

    @Test
    @DisplayName("Unit Test 1: Should create product successfully")
    void testCreateProduct() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        ProductDto result = productService.save(testProductDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test Product", result.getProductTitle());
        assertEquals("TEST-001", result.getSku());
        assertEquals(99.99, result.getPriceUnit());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Unit Test 2: Should find product by ID")
    void testFindProductById() {
        // Arrange
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // Act
        ProductDto result = productService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getProductId());
        assertEquals("Test Product", result.getProductTitle());
        verify(productRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Unit Test 3: Should throw exception when product not found")
    void testFindProductByIdNotFound() {
        // Arrange
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            productService.findById(999);
        });
        verify(productRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Unit Test 4: Should update product successfully")
    void testUpdateProduct() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setProductId(1);
        updatedProduct.setProductTitle("Updated Product");
        updatedProduct.setSku("UPDATED-001");
        updatedProduct.setPriceUnit(149.99);
        
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        ProductDto updateDto = new ProductDto();
        updateDto.setProductTitle("Updated Product");
        updateDto.setSku("UPDATED-001");
        updateDto.setPriceUnit(149.99);

        // Act
        ProductDto result = productService.update(1, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Product", result.getProductTitle());
        assertEquals("UPDATED-001", result.getSku());
        verify(productRepository, times(1)).findById(1);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Unit Test 5: Should validate product price correctly")
    void testProductPriceValidation() {
        // Arrange
        ProductDto validProduct = new ProductDto();
        validProduct.setPriceUnit(50.0);
        
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setPriceUnit(-10.0);

        // Act
        boolean validResult = productService.isValidPrice(validProduct.getPriceUnit());
        boolean invalidResult = productService.isValidPrice(invalidProduct.getPriceUnit());

        // Assert
        assertTrue(validResult, "Valid price should pass validation");
        assertFalse(invalidResult, "Invalid price should fail validation");
    }

    @Test
    @DisplayName("Unit Test 6: Should list all products")
    void testFindAllProducts() {
        // Arrange
        List<Product> products = Arrays.asList(testProduct, new Product());
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = productService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should handle product deletion")
    void testDeleteProduct() {
        // Arrange
        when(productRepository.existsById(1)).thenReturn(true);

        // Act
        productService.deleteById(1);

        // Assert
        verify(productRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Unit Test 8: Should validate SKU uniqueness")
    void testSkuUniquenessValidation() {
        // Arrange
        when(productRepository.existsBySku("UNIQUE-SKU")).thenReturn(false);
        when(productRepository.existsBySku("EXISTING-SKU")).thenReturn(true);

        // Act
        boolean uniqueResult = productService.isSkuUnique("UNIQUE-SKU");
        boolean existingResult = productService.isSkuUnique("EXISTING-SKU");

        // Assert
        assertTrue(uniqueResult, "Unique SKU should be valid");
        assertFalse(existingResult, "Existing SKU should be invalid");
    }
}
