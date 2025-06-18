package com.griddynamics.esgraduationproject.productsearch.model;

import lombok.Data;

@Data
public class ProductSearchRequest {
    private String queryText;
    private Integer size = 10;
    private Integer page = 0;
    private String color;
    private String productSize;
    private String brand;
    private String category;
    private Float minPrice;
    private Float maxPrice;
} 