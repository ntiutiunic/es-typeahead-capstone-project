package com.griddynamics.esgraduationproject.productsearch;

import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchRequest;
import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "elasticsearch.host=localhost",
    "elasticsearch.port=9200",
    "elasticsearch.scheme=http",
    "com.griddynamics.es.graduation.project.product.index=product_index"
})
public class ProductSearchIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        
        // Populate index through product-indexer before each test
        populateIndex();
    }

    private void populateIndex() {
        try {
            // Run product-indexer to populate the index
            ProcessBuilder pb = new ProcessBuilder(
                "java", "-cp", 
                "../product-indexer/target/classes:" +
                "../product-indexer/target/dependency/*",
                "com.griddynamics.esgraduationproject.productindexer.ProductIndexer"
            );
            pb.directory(new java.io.File("."));
            Process process = pb.start();

            // Print stdout and stderr for diagnostics
            java.io.InputStream is = process.getInputStream();
            java.io.InputStream es = process.getErrorStream();
            new Thread(() -> {
                try (java.util.Scanner s = new java.util.Scanner(is)) {
                    while (s.hasNextLine()) System.out.println("[product-indexer] " + s.nextLine());
                }
            }).start();
            new Thread(() -> {
                try (java.util.Scanner s = new java.util.Scanner(es)) {
                    while (s.hasNextLine()) System.err.println("[product-indexer-err] " + s.nextLine());
                }
            }).start();

            // Wait for completion
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Failed to populate index, exit code: " + exitCode);
            }
            
            // Wait a bit for index stabilization
            Thread.sleep(2000);
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to populate index", e);
        }
    }

    @Test
    void testEmptyRequest() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setSize(10);
        request.setPage(0);

        ProductSearchResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // Check that all products are returned (5 items)
        assertEquals(5L, response.getTotalHits());
        assertEquals(5, response.getProducts().size());
        
        // Check facets
        assertNotNull(response.getFacets());
        assertTrue(response.getFacets().containsKey("colors"));
        assertTrue(response.getFacets().containsKey("sizes"));
        assertTrue(response.getFacets().containsKey("categories"));
        assertTrue(response.getFacets().containsKey("price_ranges"));
    }

    @Test
    void testHappyPathSearch() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQueryText("nike");
        request.setSize(10);
        request.setPage(0);

        ProductSearchResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // Check that 2 Nike products are found
        assertEquals(2L, response.getTotalHits());
        assertEquals(2, response.getProducts().size());
        
        // Check that all found products contain "Nike"
        response.getProducts().forEach(product -> {
            String name = (String) product.get("name");
            String brand = (String) product.get("brand");
            assertTrue(name.toLowerCase().contains("nike") || brand.toLowerCase().contains("nike"));
        });
    }

    @Test
    void testFacets() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQueryText("shoes");
        request.setSize(10);
        request.setPage(0);

        ProductSearchResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // Check facets
        Map<String, Map<String, Number>> facets = response.getFacets();
        
        // Check colors
        Map<String, Number> colors = facets.get("colors");
        assertNotNull(colors);
        assertTrue(colors.containsKey("black"));
        assertTrue(colors.containsKey("white"));
        
        // Check sizes
        Map<String, Number> sizes = facets.get("sizes");
        assertNotNull(sizes);
        assertTrue(sizes.containsKey("42"));
        assertTrue(sizes.containsKey("44"));
        
        // Check categories
        Map<String, Number> categories = facets.get("categories");
        assertNotNull(categories);
        assertTrue(categories.containsKey("shoes"));
        
        // Check price ranges
        Map<String, Number> priceRanges = facets.get("price_ranges");
        assertNotNull(priceRanges);
        assertTrue(priceRanges.containsKey("50-100"));
        assertTrue(priceRanges.containsKey("100-200"));
    }

    @Test
    void testSorting() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQueryText("air");
        request.setSize(10);
        request.setPage(0);

        ProductSearchResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // Check that results are sorted by relevance
        assertTrue(response.getTotalHits() > 0);
        assertFalse(response.getProducts().isEmpty());
        
        // Check that all found products contain "air"
        response.getProducts().forEach(product -> {
            String name = (String) product.get("name");
            String description = (String) product.get("description");
            assertTrue(name.toLowerCase().contains("air") || description.toLowerCase().contains("air"));
        });
    }

    @Test
    void testPagination() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setSize(2);
        request.setPage(0);

        ProductSearchResponse response1 = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // First page should contain 2 products
        assertEquals(2, response1.getProducts().size());
        assertEquals(5L, response1.getTotalHits());

        // Second page
        request.setPage(1);
        ProductSearchResponse response2 = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // Second page should contain remaining products
        assertEquals(3, response2.getProducts().size());
        assertEquals(5L, response2.getTotalHits());

        // Check that products on different pages are different
        String firstProductId1 = (String) response1.getProducts().get(0).get("id");
        String firstProductId2 = (String) response2.getProducts().get(0).get("id");
        assertNotEquals(firstProductId1, firstProductId2);
    }

    @Test
    void testFilters() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setColor("black");
        request.setProductSize("42");
        request.setCategory("shoes");
        request.setMinPrice(100.0f);
        request.setMaxPrice(150.0f);
        request.setSize(10);
        request.setPage(0);

        ProductSearchResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // Check that all found products match the filters
        response.getProducts().forEach(product -> {
            assertEquals("black", product.get("color"));
            assertEquals("42", product.get("size"));
            assertEquals("shoes", product.get("category"));
            
            Float price = ((Number) product.get("price")).floatValue();
            assertTrue(price >= 100.0f && price <= 150.0f);
        });
    }

    @Test
    void testShinglesSearch() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQueryText("nike air max");
        request.setSize(10);
        request.setPage(0);

        ProductSearchResponse response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/v1/product")
            .then()
            .statusCode(200)
            .extract()
            .as(ProductSearchResponse.class);

        // Check that products with "Nike Air Max" are found
        assertTrue(response.getTotalHits() > 0);
        
        boolean foundNikeAirMax = response.getProducts().stream()
            .anyMatch(product -> {
                String name = (String) product.get("name");
                return name.toLowerCase().contains("nike air max");
            });
        
        assertTrue(foundNikeAirMax, "Should find Nike Air Max products");
    }
} 