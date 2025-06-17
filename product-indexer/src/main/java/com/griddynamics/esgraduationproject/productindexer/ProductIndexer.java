package com.griddynamics.esgraduationproject.productindexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

@Slf4j
public class ProductIndexer {
    private static final String INDEX_NAME = "product_index";
    private static final String INDEX_ALIAS = "product_index_alias";
    private static final String SETTINGS_FILE = "elastic/products/settings.json";
    private static final String MAPPINGS_FILE = "elastic/products/mappings.json";
    private static final String DATA_FILE = "elastic/products/products.json";
    private static final String ELASTICSEARCH_HOST = "localhost";
    private static final int ELASTICSEARCH_PORT = 9200;

    private final RestHighLevelClient esClient;
    private final ObjectMapper objectMapper;

    public ProductIndexer() {
        this.esClient = ElasticsearchClientFactory.createClient(ELASTICSEARCH_HOST, ELASTICSEARCH_PORT);
        this.objectMapper = new ObjectMapper();
    }

    public void recreateIndex() throws IOException {
        if (indexExists(INDEX_NAME)) {
            deleteIndex(INDEX_NAME);
        }

        String settings = getStrFromResource(SETTINGS_FILE);
        String mappings = getStrFromResource(MAPPINGS_FILE);
        createIndex(INDEX_NAME, settings, mappings);

        processBulkInsertData(DATA_FILE);

        // Force refresh the index to make documents searchable immediately
        esClient.indices().refresh(new RefreshRequest(INDEX_NAME), RequestOptions.DEFAULT);
    }

    private boolean indexExists(String indexName) throws IOException {
        return esClient.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    private void deleteIndex(String indexName) throws IOException {
        esClient.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        log.info("Index {} has been deleted.", indexName);
    }

    private void createIndex(String indexName, String settings, String mappings) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder().loadFromSource(settings, XContentType.JSON));
        request.mapping(mappings, XContentType.JSON);
        esClient.indices().create(request, RequestOptions.DEFAULT);
        log.info("Index {} has been created.", indexName);

        // Create alias
        IndicesAliasesRequest aliasRequest = new IndicesAliasesRequest();
        aliasRequest.addAliasAction(IndicesAliasesRequest.AliasActions.add()
                .index(indexName)
                .alias(INDEX_ALIAS));
        esClient.indices().updateAliases(aliasRequest, RequestOptions.DEFAULT);
        log.info("Alias {} has been created for index {}.", INDEX_ALIAS, indexName);
    }

    private void processBulkInsertData(String dataFile) throws IOException {
        String data = getStrFromResource(dataFile);
        JsonNode rootNode = objectMapper.readTree(data);
        BulkRequest bulkRequest = new BulkRequest();

        for (Iterator<JsonNode> it = rootNode.elements(); it.hasNext(); ) {
            JsonNode node = it.next();
            IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                    .source(node.toString(), XContentType.JSON);
            bulkRequest.add(indexRequest);
        }

        esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        log.info("{} requests have been processed in a bulk request.", rootNode.size());
    }

    private String getStrFromResource(String resourceName) throws IOException {
        URL url = Resources.getResource(resourceName);
        return Resources.toString(url, Charsets.UTF_8);
    }

    public static void main(String[] args) {
        try {
            ProductIndexer indexer = new ProductIndexer();
            indexer.recreateIndex();
        } catch (Exception e) {
            log.error("Failed to recreate index", e);
            System.exit(1);
        }
    }
} 