package com.griddynamics.esgraduationproject.productindexer;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticsearchClientFactory {
    public static RestHighLevelClient createClient(String host, int port) {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, "http")));
    }
} 