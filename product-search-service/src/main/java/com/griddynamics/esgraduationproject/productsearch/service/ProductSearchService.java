package com.griddynamics.esgraduationproject.productsearch.service;

import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchRequest;
import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchResponse;

public interface ProductSearchService {
    ProductSearchResponse searchProducts(ProductSearchRequest request);
} 