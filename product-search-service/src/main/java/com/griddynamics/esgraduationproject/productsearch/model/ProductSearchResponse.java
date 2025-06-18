package com.griddynamics.esgraduationproject.productsearch.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ProductSearchResponse {
    private Long totalHits;
    private List<Map<String, Object>> products;
    private Map<String, Map<String, Number>> facets;
} 