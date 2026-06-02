# P0-4 — Search Technologies — Why Elasticsearch Over PostgreSQL Full-Text Search

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> Search is deceptively hard. Every founder says "we'll just add a LIKE query." Three months later they're debugging why "iphone" doesn't return "iPhone 15 Pro Max", why "pne" doesn't return "pune", and why search takes 4 seconds for 500K products. This chapter explains the engineering behind search — and when to level up from PostgreSQL to Elasticsearch.

---

## 1. Why Search Is a Separate Concern From Storage

Your database's job is to **store data consistently and serve structured queries**. Its B-tree indexes are optimized for:
- Exact equality lookups: `WHERE id = 42`
- Range scans: `WHERE price BETWEEN 100 AND 500`
- Sorted access: `ORDER BY created_at DESC`

Search's job is fundamentally different:
- **Relevance ranking**: "iphone 15" should rank iPhone 15 Pro Max above a generic phone case that mentions "iphone" in passing
- **Tokenization**: Break "Spring Boot in Action" into tokens `[spring, boot, action]`
- **Stemming**: "running" and "runs" and "ran" should match "run"
- **Fuzzy matching**: "elsticsearch" should match "elasticsearch" (typo tolerance)
- **Phonetic matching**: "Bengaluru" and "Bangalore" should return same results
- **Faceted filters**: Return all products matching "shoes" AND show count per brand, size, color
- **Autocomplete**: Return suggestions as user types "app..."
- **Geospatial**: "restaurants near me" within 5km radius

PostgreSQL can do some of this. But it becomes increasingly painful as data grows and search requirements evolve. Elasticsearch was **purpose-built** for exactly this problem.

---

## 2. PostgreSQL Full-Text Search — When It's Enough

### 2.1 The tsvector / tsquery System

PostgreSQL has a built-in full-text search engine using `tsvector` (preprocessed document) and `tsquery` (search query):

```sql
-- Add tsvector column for efficient search
ALTER TABLE products ADD COLUMN search_vector tsvector;

-- Populate it from multiple columns with different weights
UPDATE products SET search_vector = 
    setweight(to_tsvector('english', coalesce(name, '')), 'A') ||       -- Name: highest weight
    setweight(to_tsvector('english', coalesce(brand, '')), 'B') ||      -- Brand: medium weight
    setweight(to_tsvector('english', coalesce(description, '')), 'C');  -- Description: lowest

-- GIN index on tsvector for fast lookups
CREATE INDEX idx_products_search ON products USING GIN (search_vector);

-- Create a trigger to keep tsvector updated on INSERT/UPDATE
CREATE FUNCTION update_product_search_vector() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.brand, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.description, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER products_search_vector_trigger
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_product_search_vector();

-- Search query
SELECT id, name, brand, price,
       ts_rank(search_vector, query) AS relevance
FROM products,
     to_tsquery('english', 'apple & iphone') query
WHERE search_vector @@ query
ORDER BY relevance DESC
LIMIT 20;
```

### 2.2 When PostgreSQL FTS Is Sufficient

Use PostgreSQL full-text search when:
- **< 1 million rows** in the searched table
- **Search is secondary** to your application (not the core feature)
- **Simple keyword matching** is acceptable — no fuzzy, no autocomplete needed
- **Single language** support is fine
- **You want zero infrastructure overhead** — no separate Elasticsearch cluster to manage

The performance is acceptable: a GIN-indexed tsvector query on 500K rows typically returns in **20-100ms**.

### 2.3 Where PostgreSQL FTS Breaks Down

| Requirement | PostgreSQL FTS | Elasticsearch |
|---|---|---|
| Fuzzy matching ("adidas" → "addidas") | ❌ Limited | ✅ Native fuzzy queries |
| Autocomplete / prefix suggestions | ❌ Slow, hacky | ✅ Edge n-gram tokenizer |
| Relevance scoring with fine-tuning | ❌ ts_rank is crude | ✅ BM25, field boosts, custom scoring |
| Multilingual (Hindi, Tamil, Arabic) | ❌ Limited language support | ✅ Per-index language analyzers |
| Faceted filters with counts | ❌ Requires GROUP BY — slow | ✅ Aggregations — O(1) |
| Synonym expansion ("phone" → "mobile", "smartphone") | ❌ Manual, brittle | ✅ Synonym token filter |
| Search as you type (debounced autocomplete) | ❌ Very slow on large tables | ✅ Dedicated suggest API |
| Log analytics / time-series search | ❌ Wrong tool entirely | ✅ Core use case |
| > 10M rows with < 50ms p95 latency | ❌ Impractical | ✅ Distributed shards |
| Highlighting matched terms in results | ❌ Not built in | ✅ Native highlighter |

---

## 3. Elasticsearch Architecture — How It Actually Works

### 3.1 The Inverted Index

The core data structure in Elasticsearch (and all full-text search engines) is the **inverted index** — a mapping from **terms to documents**.

Traditional index (like PostgreSQL B-tree):
```
Document 1 → ["apple", "iphone", "smartphone", "camera"]
Document 2 → ["samsung", "galaxy", "smartphone", "android"]
```

Inverted index (Elasticsearch):
```
"apple"       → [Document 1]
"iphone"      → [Document 1]
"smartphone"  → [Document 1, Document 2]
"samsung"     → [Document 2]
"galaxy"      → [Document 2]
"android"     → [Document 2]
"camera"      → [Document 1]
```

When you search "smartphone camera", Elasticsearch:
1. Tokenizes "smartphone camera" → `["smartphone", "camera"]`
2. Looks up both terms in the inverted index
3. Intersects the results: `[Document 1, Document 2] ∩ [Document 1]` = `[Document 1]`
4. Scores Document 1 using BM25
5. Returns ranked results

This is why Elasticsearch is fast — term lookups are O(log n) in the inverted index, not full table scans.

### 3.2 Cluster Architecture

```
                    ┌─────────────────────────────┐
                    │       Elasticsearch Cluster  │
                    │                             │
          ┌─────────┤   Master Node               │
          │         │   - Cluster state           │
          │         │   - Index management        │
          │         │   - Node monitoring         │
          │         └─────────────────────────────┘
          │
    ┌─────┴──────────────────────────────────────┐
    │                                             │
┌───▼───┐       ┌───────┐       ┌───────┐        │
│Data   │       │Data   │       │Data   │        │
│Node 1 │       │Node 2 │       │Node 3 │        │
│Shard 0│       │Shard 1│       │Shard 2│        │
│(Primary)      │(Primary)      │(Primary)       │
│Shard 1│       │Shard 2│       │Shard 0│        │
│(Replica)      │(Replica)      │(Replica)       │
└───────┘       └───────┘       └───────┘        │
└─────────────────────────────────────────────────┘
```

**Shards**: An index is split into N primary shards. Each shard is an independent Lucene index. More shards = more parallelism.

**Replicas**: Each primary shard has R replica copies. Replicas serve read requests and provide failover.

**Rule of thumb for sizing**:
- Shard size: **10-50 GB per shard** for search workloads
- For 100 GB index: 3-5 primary shards, 1 replica each = 6-10 shards total
- Always have **at least 1 replica** in production — a node failure without replicas means data loss

> [!CAUTION]
> **Over-sharding kills performance.** If you create 100 shards for a 1 GB index, every query must fan out to 100 shards and merge results. This is MORE overhead than a single shard. Start small and increase shards as data grows. You cannot change primary shard count after index creation — plan ahead or reindex.

### 3.3 TF-IDF vs BM25 — Relevance Scoring

**TF-IDF** (older, Elasticsearch < 5.0):
- TF (Term Frequency): how often does "iphone" appear in this document?
- IDF (Inverse Document Frequency): how rare is "iphone" across all documents? Rarer = more relevant
- Problem: longer documents score higher just because they have more words

**BM25** (Elasticsearch default since 5.0):
- Improves on TF-IDF with a saturation function — after N occurrences, extra mentions contribute diminishing returns
- Length normalization is configurable (k1 and b parameters)
- Better for product descriptions and typical e-commerce search

```json
// Custom BM25 similarity per field
PUT /products
{
  "settings": {
    "similarity": {
      "product_similarity": {
        "type": "BM25",
        "k1": 1.5,   // Term frequency saturation (default 1.2)
        "b": 0.75    // Length normalization (0=disabled, 1=full, default 0.75)
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "similarity": "product_similarity"
      }
    }
  }
}
```

---

## 4. The Sync Problem — Elasticsearch Is NOT Your Database

> [!IMPORTANT]
> This is the most critical architectural concept in this chapter. **NEVER write to Elasticsearch as your primary store.** Elasticsearch has no transactions, no foreign keys, no guaranteed durability for writes. If you write directly to Elasticsearch, you will lose data.

The correct architecture:
```
Write Request → Application → PostgreSQL (source of truth) → [sync mechanism] → Elasticsearch (search index)
Read (search) → Application → Elasticsearch
Read (detail) → Application → PostgreSQL OR Redis cache
```

### 4.1 Strategy 1: Synchronous Dual Write (Simple, Acceptable for Small Scale)

Write to PostgreSQL first. If that succeeds, write to Elasticsearch. If Elasticsearch write fails — log it and use a background retry job.

```kotlin
@Service
@Transactional
class ProductService(
    private val productRepository: ProductRepository,
    private val elasticsearchProductRepository: ElasticsearchProductRepository,
    private val syncQueue: ProductSyncQueue  // Fallback retry queue
) {

    fun createProduct(request: CreateProductRequest): Product {
        // 1. Write to PostgreSQL (source of truth)
        val product = productRepository.save(
            Product(
                name = request.name,
                price = request.price,
                categoryId = request.categoryId,
                description = request.description
            )
        )
        
        // 2. Sync to Elasticsearch (best-effort, non-transactional)
        try {
            elasticsearchProductRepository.save(product.toEsDocument())
        } catch (e: Exception) {
            // Don't fail the request — queue for retry
            log.error("ES sync failed for product ${product.id}, queuing retry", e)
            syncQueue.enqueue(product.id)
        }
        
        return product
    }
}
```

> [!WARNING]
> Dual-write has a race condition: if your app crashes AFTER writing to PostgreSQL but BEFORE writing to Elasticsearch, the search index is stale. For low-traffic apps this is acceptable. For production with high write volume, use CDC (Change Data Capture) instead.

### 4.2 Strategy 2: Outbox Pattern (Reliable, Transactional)

```kotlin
// Write the event to an outbox table in the SAME transaction as the entity
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val outboxRepository: OutboxEventRepository
) {

    @Transactional
    fun createProduct(request: CreateProductRequest): Product {
        val product = productRepository.save(/* ... */)
        
        // Write outbox event in SAME DB transaction — atomically
        outboxRepository.save(
            OutboxEvent(
                aggregateType = "PRODUCT",
                aggregateId = product.id.toString(),
                eventType = "PRODUCT_CREATED",
                payload = objectMapper.writeValueAsString(product.toEsDocument()),
                status = OutboxStatus.PENDING
            )
        )
        
        return product
        // If transaction commits: both product AND outbox event are written atomically
        // If transaction fails: neither is written
    }
}

// Separate outbox processor — runs on a schedule or listens to Debezium
@Component
class OutboxProcessor(
    private val outboxRepository: OutboxEventRepository,
    private val elasticsearchProductRepository: ElasticsearchProductRepository
) {

    @Scheduled(fixedDelay = 5000)  // Run every 5 seconds
    fun processOutbox() {
        val pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.PENDING, 
            PageRequest.of(0, 100)
        )
        
        pendingEvents.forEach { event ->
            try {
                when (event.eventType) {
                    "PRODUCT_CREATED", "PRODUCT_UPDATED" -> {
                        val document = objectMapper.readValue<ProductEsDocument>(event.payload)
                        elasticsearchProductRepository.save(document)
                    }
                    "PRODUCT_DELETED" -> {
                        elasticsearchProductRepository.deleteById(event.aggregateId)
                    }
                }
                event.status = OutboxStatus.PROCESSED
                outboxRepository.save(event)
            } catch (e: Exception) {
                event.retryCount++
                if (event.retryCount >= 5) event.status = OutboxStatus.FAILED
                outboxRepository.save(event)
                log.error("Failed to process outbox event ${event.id}", e)
            }
        }
    }
}
```

### 4.3 Strategy 3: Debezium CDC (Production Grade for High Scale)

Debezium is a CDC (Change Data Capture) tool that reads PostgreSQL's WAL (Write-Ahead Log) and streams changes to Kafka. A consumer reads from Kafka and updates Elasticsearch.

```
PostgreSQL WAL → Debezium Connector → Kafka Topic → Elasticsearch Sink Connector
```

This is how Uber, Swiggy, and other large-scale companies sync their primary databases to search indexes. It's eventually consistent (typically < 1 second lag) and requires zero application code changes.

```yaml
# Debezium PostgreSQL Connector config (deployed to Kafka Connect)
connector.class=io.debezium.connector.postgresql.PostgresConnector
database.hostname=postgres-host
database.port=5432
database.user=debezium_user
database.password=${DEBEZIUM_PASSWORD}
database.dbname=sentinel_db
table.include.list=public.products,public.categories
plugin.name=pgoutput
topic.prefix=sentinel
transforms=unwrap
transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState
transforms.unwrap.drop.tombstones=false
```

---

## 5. Spring Boot + Elasticsearch Integration

### 5.1 Dependencies and Configuration

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    // Uses Elasticsearch Java Client (co.elastic.clients) under the hood
}
```

```yaml
# application.yml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: ${ES_USERNAME:elastic}
    password: ${ES_PASSWORD:changeme}
    connection-timeout: 5s
    socket-timeout: 30s
```

### 5.2 Product Document Mapping

```kotlin
@Document(indexName = "products", createIndex = false)  // Manage index creation explicitly
@Setting(settingPath = "elasticsearch/product-settings.json")
data class ProductEsDocument(
    
    @Id
    val id: String,
    
    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "standard"),
        otherFields = [
            InnerField(suffix = "keyword", type = FieldType.Keyword),  // For exact match/sorting
            InnerField(suffix = "suggest", type = FieldType.Search_As_You_Type)  // For autocomplete
        ]
    )
    val name: String,
    
    @Field(type = FieldType.Text, analyzer = "standard")
    val description: String,
    
    @Field(type = FieldType.Keyword)
    val brand: String,
    
    @Field(type = FieldType.Keyword)
    val categoryId: String,
    
    @Field(type = FieldType.Keyword)
    val categoryName: String,
    
    @Field(type = FieldType.Double)
    val price: Double,
    
    @Field(type = FieldType.Boolean)
    val inStock: Boolean,
    
    @Field(type = FieldType.Integer)
    val stockQuantity: Int,
    
    // Geospatial: store dark store location for "products near me"
    @GeoPointField
    val location: GeoPoint?,
    
    @Field(type = FieldType.Keyword)
    val tags: List<String> = emptyList(),
    
    @Field(type = FieldType.Date, format = [DateFormat.epoch_millis])
    val createdAt: Instant = Instant.now(),
    
    // Nested for faceted filters on attributes
    @Field(type = FieldType.Object)
    val attributes: Map<String, String> = emptyMap()
)
```

```json
// resources/elasticsearch/product-settings.json
{
  "number_of_shards": 3,
  "number_of_replicas": 1,
  "analysis": {
    "filter": {
      "autocomplete_filter": {
        "type": "edge_ngram",
        "min_gram": 2,
        "max_gram": 20
      },
      "synonym_filter": {
        "type": "synonym",
        "synonyms": [
          "phone, mobile, smartphone",
          "laptop, notebook",
          "tv, television"
        ]
      }
    },
    "analyzer": {
      "autocomplete_analyzer": {
        "type": "custom",
        "tokenizer": "standard",
        "filter": ["lowercase", "autocomplete_filter"]
      },
      "search_analyzer": {
        "type": "custom",
        "tokenizer": "standard",
        "filter": ["lowercase", "synonym_filter"]
      }
    }
  }
}
```

### 5.3 Repository with Custom Queries

```kotlin
interface ProductEsRepository : ElasticsearchRepository<ProductEsDocument, String>

@Repository
class ProductSearchRepository(
    private val elasticsearchOperations: ElasticsearchOperations
) {

    fun searchProducts(
        query: String,
        categoryId: String? = null,
        brand: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        inStockOnly: Boolean = false,
        lat: Double? = null,
        lon: Double? = null,
        radiusKm: Double? = null,
        page: Int = 0,
        size: Int = 20
    ): SearchHits<ProductEsDocument> {
        
        val queryBuilder = BoolQuery.Builder()
        
        // Full-text search with field boosting
        if (query.isNotBlank()) {
            queryBuilder.must(
                MultiMatchQuery.of { m ->
                    m.query(query)
                     .fields(
                         "name^3",           // Name 3x boost
                         "brand^2",          // Brand 2x boost  
                         "description^1",    // Description baseline
                         "tags^1.5"
                     )
                     .type(TextQueryType.BestFields)
                     .fuzziness("AUTO")      // Typo tolerance
                     .minimumShouldMatch("75%")
                }._toQuery()
            )
        }
        
        // Filters (don't affect relevance score, use filter context for caching)
        categoryId?.let {
            queryBuilder.filter(TermQuery.of { t -> t.field("categoryId").value(it) }._toQuery())
        }
        
        brand?.let {
            queryBuilder.filter(TermQuery.of { t -> t.field("brand").value(it) }._toQuery())
        }
        
        if (minPrice != null || maxPrice != null) {
            queryBuilder.filter(
                RangeQuery.of { r ->
                    r.field("price").apply {
                        minPrice?.let { gte(JsonData.of(it)) }
                        maxPrice?.let { lte(JsonData.of(it)) }
                    }
                }._toQuery()
            )
        }
        
        if (inStockOnly) {
            queryBuilder.filter(TermQuery.of { t -> t.field("inStock").value(true) }._toQuery())
        }
        
        // Geospatial filter
        if (lat != null && lon != null && radiusKm != null) {
            queryBuilder.filter(
                GeoDistanceQuery.of { g ->
                    g.field("location")
                     .distance("${radiusKm}km")
                     .location(GeoLocation.of { l -> l.latlon(LatLonGeoLocation.of { ll -> ll.lat(lat).lon(lon) }) })
                }._toQuery()
            )
        }
        
        val nativeQuery = NativeQuery.builder()
            .withQuery(queryBuilder.build()._toQuery())
            .withPageable(PageRequest.of(page, size))
            .withAggregation("brands", TermsAggregation.of { t -> t.field("brand").size(20) }._toAggregation())
            .withAggregation("price_ranges", RangeAggregation.of { r ->
                r.field("price").ranges(
                    AggregationRange.of { ar -> ar.to(100.0) },
                    AggregationRange.of { ar -> ar.from(100.0).to(500.0) },
                    AggregationRange.of { ar -> ar.from(500.0).to(2000.0) },
                    AggregationRange.of { ar -> ar.from(2000.0) }
                )
            }._toAggregation())
            .withHighlightQuery(
                HighlightQuery(
                    Highlight.builder()
                        .fields("name", HighlightField.builder().build())
                        .fields("description", HighlightField.builder().numberOfFragments(2).build())
                        .build(),
                    ProductEsDocument::class.java
                )
            )
            .build()
        
        return elasticsearchOperations.search(nativeQuery, ProductEsDocument::class.java)
    }
}
```

### 5.4 Autocomplete Endpoint

```kotlin
@RestController
@RequestMapping("/api/search")
class SearchController(
    private val productSearchRepository: ProductSearchRepository,
    private val elasticsearchOperations: ElasticsearchOperations
) {

    @GetMapping("/suggest")
    fun suggest(@RequestParam q: String): List<String> {
        if (q.length < 2) return emptyList()
        
        val query = NativeQuery.builder()
            .withQuery(
                MultiMatchQuery.of { m ->
                    m.query(q)
                     .fields("name.suggest", "brand.keyword")
                     .type(TextQueryType.BoolPrefix)
                }._toQuery()
            )
            .withPageable(PageRequest.of(0, 5))
            .withSourceFilter(FetchSourceFilter(arrayOf("name"), emptyArray()))
            .build()
        
        return elasticsearchOperations.search(query, ProductEsDocument::class.java)
            .searchHits
            .map { it.content.name }
            .distinct()
    }

    @GetMapping("/products")
    fun searchProducts(
        @RequestParam(required = false) q: String = "",
        @RequestParam(required = false) category: String? = null,
        @RequestParam(required = false) brand: String? = null,
        @RequestParam(required = false) minPrice: Double? = null,
        @RequestParam(required = false) maxPrice: Double? = null,
        @RequestParam(required = false, defaultValue = "false") inStock: Boolean,
        @RequestParam(required = false) lat: Double? = null,
        @RequestParam(required = false) lon: Double? = null,
        @RequestParam(required = false) radius: Double? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): SearchResultDto {
        val hits = productSearchRepository.searchProducts(
            query = q, categoryId = category, brand = brand,
            minPrice = minPrice, maxPrice = maxPrice, inStockOnly = inStock,
            lat = lat, lon = lon, radiusKm = radius,
            page = page, size = size
        )
        
        return SearchResultDto(
            total = hits.totalHits,
            products = hits.searchHits.map { hit ->
                ProductSearchResultDto(
                    id = hit.content.id,
                    name = hit.content.name,
                    brand = hit.content.brand,
                    price = hit.content.price,
                    inStock = hit.content.inStock,
                    score = hit.score,
                    highlights = hit.highlightFields
                )
            },
            facets = FacetsDto(
                brands = hits.aggregations?.get("brands")?.let { /* parse terms agg */ emptyList() } ?: emptyList()
            )
        )
    }
}
```

---

## 6. Geospatial Search — Find Nearest Dark Stores (Blinkit / Zepto Model)

Blinkit and Zepto operate hundreds of dark stores across Indian cities. When a user opens the app, the system must:
1. Find all dark stores within N km of the user
2. Check which dark stores have the searched product in stock
3. Sort by delivery time (distance proxy)

```kotlin
// Index dark store locations
@Document(indexName = "dark_stores")
data class DarkStoreEsDocument(
    @Id val id: String,
    val name: String,
    val city: String,
    
    @GeoPointField
    val location: GeoPoint,
    
    val isActive: Boolean,
    val deliveryRadiusKm: Double
)

// Find stores + products near a user
fun findProductNearUser(productId: String, userLat: Double, userLon: Double): List<NearbyProductDto> {
    val query = NativeQuery.builder()
        .withQuery(
            BoolQuery.of { b ->
                b.must(TermQuery.of { t -> t.field("productId").value(productId) }._toQuery())
                 .must(TermQuery.of { t -> t.field("inStock").value(true) }._toQuery())
                 .filter(
                     GeoDistanceQuery.of { g ->
                         g.field("storeLocation")
                          .distance("10km")
                          .location(GeoLocation.of { l ->
                              l.latlon(LatLonGeoLocation.of { ll -> ll.lat(userLat).lon(userLon) })
                          })
                     }._toQuery()
                 )
            }._toQuery()
        )
        .withSort(
            GeoDistanceSort.of { s ->
                s.field("storeLocation")
                 .location(GeoLocation.of { l ->
                     l.latlon(LatLonGeoLocation.of { ll -> ll.lat(userLat).lon(userLon) })
                 })
                 .order(SortOrder.Asc)
                 .unit(DistanceUnit.Kilometers)
            }._toSortOptions()
        )
        .build()
    
    return elasticsearchOperations.search(query, StoreProductEsDocument::class.java)
        .searchHits.map { /* map to DTO */ }
}
```

---

## 7. Production Pitfalls

### 7.1 Mapping Explosion

> [!CAUTION]
> **Mapping explosion** occurs when you dynamically add new field names to an index and Elasticsearch auto-creates mappings for each. At 1000+ unique field names, heap memory explodes and cluster performance degrades catastrophically.

This happens when you index arbitrary JSON (like user-generated content or event properties):
```json
// BAD: each user has different property names
{ "user_42_score": 100 }
{ "user_43_score": 200 }
// → creates 2 new mappings per user → millions of mappings at scale
```

Fix: disable dynamic mapping or limit it:
```json
PUT /events
{
  "mappings": {
    "dynamic": "strict",  // Reject any field not in the explicit mapping
    "properties": {
      "event_type": { "type": "keyword" },
      "timestamp": { "type": "date" },
      "properties": {
        "type": "flattened"  // Store arbitrary key-values without creating new mappings
      }
    }
  }
}
```

### 7.2 No Replicas in Production

> [!WARNING]
> Elasticsearch defaults to `number_of_replicas: 1` for new indexes, but if your index was created with `replicas: 0` (common in development configs that make it into production), a single node failure causes that index to go **red** (data unavailable).

```bash
# Check index health
GET /_cat/indices?v&health=red

# Fix: add replicas to existing index
PUT /products/_settings
{
  "number_of_replicas": 1
}
```

### 7.3 Split-Brain

In older versions (before Elasticsearch 7.0), if the cluster splits into two partitions due to network partition, both halves could elect a master and accept writes — creating **split-brain** and data inconsistency.

Elasticsearch 7.0+ uses a **Raft-based consensus** algorithm. Still, configure `discovery.zen.minimum_master_nodes` correctly for pre-7 clusters, or ensure your cluster has an **odd number of master-eligible nodes** (3 or 5).

> [!IMPORTANT]
> In production, always run at least **3 dedicated master-eligible nodes** and **3 data nodes** for an Elasticsearch cluster. Single-node or two-node setups cannot handle failure gracefully.

### 7.4 Deep Pagination Performance

```json
// BAD: from/size pagination becomes exponentially slow after from=10000
GET /products/_search
{ "from": 10000, "size": 20 }
// Elasticsearch must fetch and sort 10,020 documents to return 20
```

Use `search_after` for deep pagination:
```json
// GOOD: cursor-based pagination using sort values from last result
GET /products/_search
{
  "size": 20,
  "sort": [{ "created_at": "desc" }, { "_id": "asc" }],
  "search_after": ["2024-01-15T10:30:00Z", "product_abc123"]
}
```

---

## 8. Summary — When to Choose What

```
Rows < 1M AND search is basic (keywords, no fuzzy) → PostgreSQL tsvector
Rows > 1M OR need autocomplete, fuzzy, facets, multilingual, geo → Elasticsearch

Elasticsearch write pattern:
  Low write volume (<100/min) → Synchronous dual-write with retry queue
  High write volume (>1000/min) → Outbox pattern or Debezium CDC
  
Always:
  - PostgreSQL is source of truth
  - Elasticsearch is search view
  - If ES is down, searches fail gracefully (return empty / fallback to PG)
  - Index health monitor → alert on yellow/red status
```

> [!NOTE]
> When Elasticsearch is down, your product detail pages should still work — they read from PostgreSQL or Redis cache. ONLY search is degraded. Design your search feature to degrade gracefully: catch `ElasticsearchException`, return a fallback message or a DB-based result with a "limited results" warning.
