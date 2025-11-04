package com.selimhorri.app.unit;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.impl.ProductServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDto testProductDto;
    private Category testCategory;
    private CategoryDto testCategoryDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testCategory = new Category();
        testCategory.setCategoryId(1);
        testCategory.setCategoryTitle("Electronics");
        testCategory.setImageUrl("http://example.com/electronics.jpg");
        
        testCategoryDto = new CategoryDto();
        testCategoryDto.setCategoryId(1);
        testCategoryDto.setCategoryTitle("Electronics");
        testCategoryDto.setImageUrl("http://example.com/electronics.jpg");
        
        testProduct = new Product();
        testProduct.setProductId(1);
        testProduct.setProductTitle("Test Product");
        testProduct.setSku("TEST-001");
        testProduct.setPriceUnit(99.99);
        testProduct.setQuantity(10);
        testProduct.setCategory(testCategory);
        
        testProductDto = new ProductDto();
        testProductDto.setProductTitle("Test Product");
        testProductDto.setSku("TEST-001");
        testProductDto.setPriceUnit(99.99);
        testProductDto.setQuantity(10);
        testProductDto.setCategoryDto(testCategoryDto);
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
        assertNotNull(result.getCategoryDto());
        assertEquals(1, result.getCategoryDto().getCategoryId());
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
        assertNotNull(result.getCategoryDto());
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
        Category updatedCategory = new Category();
        updatedCategory.setCategoryId(2);
        updatedCategory.setCategoryTitle("Books");
        
        Product updatedProduct = new Product();
        updatedProduct.setProductId(1);
        updatedProduct.setProductTitle("Updated Product");
        updatedProduct.setSku("UPDATED-001");
        updatedProduct.setPriceUnit(149.99);
        updatedProduct.setCategory(updatedCategory);
        
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        CategoryDto updatedCategoryDto = new CategoryDto();
        updatedCategoryDto.setCategoryId(2);
        updatedCategoryDto.setCategoryTitle("Books");

        ProductDto updateDto = new ProductDto();
        updateDto.setProductId(1);
        updateDto.setProductTitle("Updated Product");
        updateDto.setSku("UPDATED-001");
        updateDto.setPriceUnit(149.99);
        updateDto.setCategoryDto(updatedCategoryDto);

        // Act
        ProductDto result = productService.update(updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Product", result.getProductTitle());
        assertEquals("UPDATED-001", result.getSku());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Unit Test 5: Should validate product price correctly")
    void testProductPriceValidation() {
        // Act & Assert
        assertTrue(testProduct.getPriceUnit() > 0);
        assertFalse(testProduct.getPriceUnit() < 0);
        assertEquals(99.99, testProduct.getPriceUnit(), 0.01);
    }

    @Test
    @DisplayName("Unit Test 6: Should list all products")
    void testFindAllProducts() {
        // Arrange
        Category category2 = new Category();
        category2.setCategoryId(2);
        category2.setCategoryTitle("Books");
        
        Product product2 = new Product();
        product2.setProductId(2);
        product2.setProductTitle("Product 2");
        product2.setSku("TEST-002");
        product2.setPriceUnit(49.99);
        product2.setQuantity(20);
        product2.setCategory(category2);
        
        List<Product> products = Arrays.asList(testProduct, product2);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = productService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should delete product successfully")
    void testDeleteProduct() {
        // Arrange
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).delete(any(Product.class)); 

        // Act
        productService.deleteById(1);

        // Assert
        verify(productRepository, times(1)).findById(1);
        verify(productRepository, times(1)).delete(any(Product.class));
    }

    @Test
    @DisplayName("Unit Test 8: Should validate product quantity")
    void testProductQuantityValidation() {
        // Act & Assert
        assertTrue(testProduct.getQuantity() >= 0);
        assertEquals(10, testProduct.getQuantity());
        
        // Test negative quantity
        testProduct.setQuantity(-5);
        assertTrue(testProduct.getQuantity() < 0, "Negative quantity should be detected");
    }

    @Test
    @DisplayName("Unit Test 9: Should validate SKU uniqueness")
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
