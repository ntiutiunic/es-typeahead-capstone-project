package com.griddynamics.esgraduationproject.productsearch.repository;

import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchRequest;
import com.griddynamics.esgraduationproject.productsearch.model.ProductSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    @Autowired
    private RestHighLevelClient esClient;

    @Value("${com.griddynamics.es.graduation.project.product.index:product_index}")
    private String indexName;

    @Override
    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        try {
            // Build search request
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            
            // Build query
            QueryBuilder query = buildQuery(request);
            searchSourceBuilder.query(query);
            
            // Set pagination
            int from = request.getPage() * request.getSize();
            searchSourceBuilder.from(from);
            searchSourceBuilder.size(request.getSize());
            
            // Set sorting
            searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
            
            // Add aggregations for facets
            addAggregations(searchSourceBuilder);
            
            // Execute search
            SearchRequest searchRequest = new SearchRequest(indexName).source(searchSourceBuilder);
            SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
            
            // Build response
            return buildResponse(searchResponse);
            
        } catch (IOException e) {
            log.error("Error searching products", e);
            return new ProductSearchResponse();
        }
    }

    private QueryBuilder buildQuery(ProductSearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Text query with shingles
        if (request.getQueryText() != null && !request.getQueryText().trim().isEmpty()) {
            BoolQueryBuilder textQuery = QueryBuilders.boolQuery();
            
            // Main text search
            textQuery.should(QueryBuilders.multiMatchQuery(request.getQueryText())
                .field("name", 2.0f)
                .field("name.shingles", 1.5f)
                .field("brand", 1.5f)
                .field("brand.shingles", 1.0f)
                .field("description", 1.0f)
                .type(org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.BEST_FIELDS));
            
            boolQuery.must(textQuery);
        }
        
        // Filters
        if (request.getColor() != null && !request.getColor().trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("color", request.getColor()));
        }
        
        if (request.getProductSize() != null && !request.getProductSize().trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("size", request.getProductSize()));
        }
        
        if (request.getBrand() != null && !request.getBrand().trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("brand", request.getBrand()));
        }
        
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("category", request.getCategory()));
        }
        
        // Price range
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            var rangeQuery = QueryBuilders.rangeQuery("price");
            if (request.getMinPrice() != null) {
                rangeQuery.gte(request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                rangeQuery.lte(request.getMaxPrice());
            }
            boolQuery.filter(rangeQuery);
        }
        
        return boolQuery;
    }

    private void addAggregations(SearchSourceBuilder searchSourceBuilder) {
        // Color aggregation
        TermsAggregationBuilder colorAgg = AggregationBuilders
            .terms("colors")
            .field("color")
            .size(20);
        searchSourceBuilder.aggregation(colorAgg);
        
        // Size aggregation
        TermsAggregationBuilder sizeAgg = AggregationBuilders
            .terms("sizes")
            .field("size")
            .size(20);
        searchSourceBuilder.aggregation(sizeAgg);
        
        // Category aggregation
        TermsAggregationBuilder categoryAgg = AggregationBuilders
            .terms("categories")
            .field("category")
            .size(20);
        searchSourceBuilder.aggregation(categoryAgg);
        
        // Price range aggregation
        searchSourceBuilder.aggregation(AggregationBuilders
            .range("price_ranges")
            .field("price")
            .addRange("0-50", 0, 50)
            .addRange("50-100", 50, 100)
            .addRange("100-200", 100, 200)
            .addRange("200+", 200, Double.POSITIVE_INFINITY));
    }

    private ProductSearchResponse buildResponse(SearchResponse searchResponse) {
        ProductSearchResponse response = new ProductSearchResponse();
        
        // Set total hits
        response.setTotalHits(searchResponse.getHits().getTotalHits().value);
        
        // Set products
        List<Map<String, Object>> products = Arrays.stream(searchResponse.getHits().getHits())
            .map(SearchHit::getSourceAsMap)
            .collect(Collectors.toList());
        response.setProducts(products);
        
        // Set facets
        Map<String, Map<String, Number>> facets = new LinkedHashMap<>();
        
        if (searchResponse.getAggregations() != null) {
            // Process color facets
            if (searchResponse.getAggregations().get("colors") != null) {
                Map<String, Number> colorFacets = new LinkedHashMap<>();
                Terms colorTerms = searchResponse.getAggregations().get("colors");
                colorTerms.getBuckets().forEach(bucket -> 
                    colorFacets.put(bucket.getKeyAsString(), bucket.getDocCount()));
                facets.put("colors", colorFacets);
            }
            
            // Process size facets
            if (searchResponse.getAggregations().get("sizes") != null) {
                Map<String, Number> sizeFacets = new LinkedHashMap<>();
                Terms sizeTerms = searchResponse.getAggregations().get("sizes");
                sizeTerms.getBuckets().forEach(bucket -> 
                    sizeFacets.put(bucket.getKeyAsString(), bucket.getDocCount()));
                facets.put("sizes", sizeFacets);
            }
            
            // Process brand facets
            if (searchResponse.getAggregations().get("brands") != null) {
                Map<String, Number> brandFacets = new LinkedHashMap<>();
                Terms brandTerms = searchResponse.getAggregations().get("brands");
                brandTerms.getBuckets().forEach(bucket -> 
                    brandFacets.put(bucket.getKeyAsString(), bucket.getDocCount()));
                facets.put("brands", brandFacets);
            }
            
            // Process category facets
            if (searchResponse.getAggregations().get("categories") != null) {
                Map<String, Number> categoryFacets = new LinkedHashMap<>();
                Terms categoryTerms = searchResponse.getAggregations().get("categories");
                categoryTerms.getBuckets().forEach(bucket -> 
                    categoryFacets.put(bucket.getKeyAsString(), bucket.getDocCount()));
                facets.put("categories", categoryFacets);
            }
            
            // Process price range facets
            if (searchResponse.getAggregations().get("price_ranges") != null) {
                Map<String, Number> priceFacets = new LinkedHashMap<>();
                Range priceRange = searchResponse.getAggregations().get("price_ranges");
                priceRange.getBuckets().forEach(bucket -> 
                    priceFacets.put(bucket.getKeyAsString(), bucket.getDocCount()));
                facets.put("price_ranges", priceFacets);
            }
        }
        
        response.setFacets(facets);
        
        return response;
    }
} 