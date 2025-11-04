package com.selimhorri.app.integration;

import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/product-service/api/products";
    }

    @Test
    @DisplayName("Integration Test 1: Should create and retrieve product")
    void testCreateAndRetrieveProduct() {
        ProductDto productDto = createTestProduct("INT-PROD-001");
        
        ResponseEntity<ProductDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), productDto, ProductDto.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Integer productId = createResponse.getBody().getProductId();
        
        ResponseEntity<ProductDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + productId, ProductDto.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("INT-PROD-001", getResponse.getBody().getSku());
    }

    @Test
    @DisplayName("Integration Test 2: Should update product price and quantity")
    void testUpdateProduct() {
        ProductDto productDto = createTestProduct("INT-PROD-002");
        ResponseEntity<ProductDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), productDto, ProductDto.class
        );
        
        ProductDto createdProduct = createResponse.getBody();
        createdProduct.setPriceUnit(149.99);
        createdProduct.setQuantity(50);
        
        restTemplate.put(getBaseUrl(), createdProduct);
        
        ResponseEntity<ProductDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + createdProduct.getProductId(), ProductDto.class
        );
        
        assertEquals(149.99, getResponse.getBody().getPriceUnit(), 0.01);
        assertEquals(50, getResponse.getBody().getQuantity());
    }

    @Test
    @DisplayName("Integration Test 3: Should delete product")
    void testDeleteProduct() {
        ProductDto productDto = createTestProduct("INT-PROD-003");
        ResponseEntity<ProductDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), productDto, ProductDto.class
        );
        
        Integer productId = createResponse.getBody().getProductId();
        
        restTemplate.delete(getBaseUrl() + "/" + productId);
        
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + productId, String.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("Integration Test 4: Should list all products")
    void testListAllProducts() {
        ResponseEntity<DtoCollectionResponse<ProductDto>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<ProductDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCollection());
    }

    private ProductDto createTestProduct(String sku) {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setCategoryId(1);
        categoryDto.setCategoryTitle("Test Category");
        
        ProductDto productDto = new ProductDto();
        productDto.setProductTitle("Integration Test Product");
        productDto.setSku(sku);
        productDto.setPriceUnit(99.99);
        productDto.setQuantity(100);
        productDto.setCategoryDto(categoryDto);
        
        return productDto;
    }
}