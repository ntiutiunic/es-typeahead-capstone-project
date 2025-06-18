package com.griddynamics.esgraduationproject.productsearch.service;

import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchRequest;
import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchResponse;
import com.griddynamics.esgraduationproject.productsearch.repository.ProductSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Override
    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        return productSearchRepository.searchProducts(request);
    }
} 