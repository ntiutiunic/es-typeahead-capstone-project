package com.griddynamics.esgraduationproject.productsearch.controller;

import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchRequest;
import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchResponse;
import com.griddynamics.esgraduationproject.productsearch.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/product")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ProductSearchResponse searchProducts(@RequestBody ProductSearchRequest request) {
        return productSearchService.searchProducts(request);
    }
} 