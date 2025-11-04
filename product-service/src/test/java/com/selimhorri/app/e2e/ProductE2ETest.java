package com.selimhorri.app.e2e;

import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static Integer productId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/product-service/api/products";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete product creation flow")
    void testCompleteProductCreation() {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setCategoryId(1);
        categoryDto.setCategoryTitle("Electronics");
        
        ProductDto productDto = new ProductDto();
        productDto.setProductTitle("E2E Test Laptop");
        productDto.setSku("E2E-LAP-001");
        productDto.setPriceUnit(999.99);
        productDto.setQuantity(50);
        productDto.setCategoryDto(categoryDto);
        
        ResponseEntity<ProductDto> response = restTemplate.postForEntity(
            baseUrl, productDto, ProductDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        productId = response.getBody().getProductId();
        assertEquals("E2E-LAP-001", response.getBody().getSku());
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: Product inventory update flow")
    void testProductInventoryUpdate() {
        assertNotNull(productId, "Product must be created first");
        
        ResponseEntity<ProductDto> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + productId, ProductDto.class
        );
        
        ProductDto product = getResponse.getBody();
        product.setQuantity(product.getQuantity() - 5);
        
        restTemplate.put(baseUrl, product);
        
        ResponseEntity<ProductDto> updatedResponse = restTemplate.getForEntity(
            baseUrl + "/" + productId, ProductDto.class
        );
        
        assertEquals(45, updatedResponse.getBody().getQuantity());
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Product price update flow")
    void testProductPriceUpdate() {
        ResponseEntity<ProductDto> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + productId, ProductDto.class
        );
        
        ProductDto product = getResponse.getBody();
        product.setPriceUnit(899.99);
        
        restTemplate.put(baseUrl, product);
        
        ResponseEntity<ProductDto> updatedResponse = restTemplate.getForEntity(
            baseUrl + "/" + productId, ProductDto.class
        );
        
        assertEquals(899.99, updatedResponse.getBody().getPriceUnit(), 0.01);
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Product catalog browsing flow")
    void testProductCatalogBrowsing() {
        ResponseEntity<DtoCollectionResponse<ProductDto>> allProductsResponse = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<ProductDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, allProductsResponse.getStatusCode());
        assertNotNull(allProductsResponse.getBody());
        assertNotNull(allProductsResponse.getBody().getCollection());
        assertTrue(allProductsResponse.getBody().getCollection().size() > 0);
        
        ResponseEntity<ProductDto> specificProductResponse = restTemplate.getForEntity(
            baseUrl + "/" + productId, ProductDto.class
        );
        
        assertEquals(HttpStatus.OK, specificProductResponse.getStatusCode());
        assertEquals("E2E Test Laptop", specificProductResponse.getBody().getProductTitle());
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Product removal flow")
    void testProductRemoval() {
        restTemplate.delete(baseUrl + "/" + productId);
        
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/" + productId, String.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}