# ES Graduation Project

## Overview
This project implements a full-text product search using Elasticsearch. It consists of two main modules:
- **product-indexer**: Indexes product data into Elasticsearch.
- **product-search-service**: Provides a REST API for searching products with filtering, sorting, and facets.

## Requirements
- Java 11 or higher
- Maven 3.6+
- Elasticsearch 7.x running locally on port 9200

## Quick Start

### 1. Clone the repository
```sh
git clone <your-repo-url>
cd es-graduation-project
```

### 2. Start Elasticsearch
Make sure Elasticsearch is running locally:
```sh
curl -XGET localhost:9200
```
You should get a standard JSON response from Elasticsearch.

### 3. Build the project
```sh
mvn clean package
```

### 4. Index product data
This will recreate the index and load product data:
```sh
cd product-indexer
mvn exec:java -Dexec.mainClass=com.griddynamics.esgraduationproject.productindexer.ProductIndexer
```

### 5. Run the search service
```sh
cd ../product-search-service
mvn spring-boot:run
```
The service will start on port 8081 by default.

### 6. Test the REST API
Example search request:
```sh
curl -XPOST localhost:8081/v1/product -H "Content-Type:application/json" -d '{"queryText":"nike"}'
```

## Integration Tests
To run integration tests for the search service:
```sh
mvn test -pl product-search-service
```
This will automatically re-index data before each test.

## Commit History and Tasks
Each major task is a separate commit. You can follow the project progress and requirements by reviewing the commit messages:

- **Initial commit: imported project** — Imported the original capstone project
- **Task 1–7** — Each task is implemented and committed separately
- **Task 8.1: implemented product-indexer** — New module for indexing
- **Task 8.2: implemented product-search-service** — New module for search API
- **Task 8.3: integration tests for product-search-service** — Full integration test coverage

## Project Structure
```
.
├── product-indexer/           # Index creation and data loading
├── product-search-service/    # REST API for product search
├── README.md                  # This file
└── ...                        # Other Maven and config files
```

## Checklist for Reviewers
- [x] Repository is private and professor has access
- [x] Each task is a separate commit
- [x] All tests are green
- [x] Both services start without errors
- [x] API returns correct results for all example queries
- [x] Integration tests cover all required cases (empty, happy path, facets, sort, pagination, filters, shingles)
