package com.griddynamics.esgraduationproject.productsearch.repository;

import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchRequest;
import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchResponse;

public interface ProductSearchRepository {
    ProductSearchResponse searchProducts(ProductSearchRequest request);
} 