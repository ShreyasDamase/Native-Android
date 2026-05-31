# Chapter 15 — Elasticsearch Search and Analytics

### _Full-text search, autocomplete, filters, geo search and read-optimized indexes_

---

## 15.1 Why Elasticsearch Exists

PostgreSQL can search, but a product search page usually needs more:

- Fuzzy text matching: "biryni" should find "biryani".
- Ranking: better matches first.
- Autocomplete.
- Filtering by tags, price, cuisine, rating, availability.
- Geo distance sorting.
- Analytics aggregations.
- Denormalized read models.

Elasticsearch is not the source of truth. Treat it as a read index rebuilt from PostgreSQL/events.

---

## 15.2 Common App Uses

| App | Elasticsearch use |
|---|---|
| Delivery | restaurant search, dish search, cuisine filters, nearby open restaurants |
| Uber-like | driver/admin search, trip history search, support tooling |
| Booking | hotel/event search, availability read model, location filters |
| Marketplace | product catalog search, autocomplete, facets |
| AI/NLP | lexical search combined with vector/semantic search |
| Ops | log search and dashboards |

---

## 15.3 Dependencies

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
}
```

Spring Data Elasticsearch provides repositories, object mapping, Elasticsearch operations, and reactive variants. Current docs list Spring Data Elasticsearch 6.x with support for the modern Elasticsearch Java client.

---

## 15.4 Document Model

Design Elasticsearch documents for the screen/query, not for normalized database purity.

```kotlin
@Document(indexName = "restaurants")
data class RestaurantSearchDocument(
    @Id
    val id: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val name: String,

    @Field(type = FieldType.Keyword)
    val cityId: String,

    @Field(type = FieldType.Keyword)
    val cuisines: List<String>,

    @Field(type = FieldType.Double)
    val rating: Double,

    @Field(type = FieldType.Boolean)
    val openNow: Boolean,

    @GeoPointField
    val location: GeoPoint,

    @Field(type = FieldType.Date)
    val updatedAt: Instant
)
```

Annotation explanation:

- `@Document`: maps a Kotlin class to an Elasticsearch index.
- `@Id`: document id.
- `@Field(type = Text)`: analyzed text, good for full-text search.
- `@Field(type = Keyword)`: exact value, good for filtering/sorting/aggregations.
- `@GeoPointField`: stores latitude/longitude for geo queries.

Good practice: store IDs as strings in Elasticsearch documents. Keep domain IDs as UUID in PostgreSQL.

---

## 15.5 Repository for Simple Queries

```kotlin
interface RestaurantSearchRepository :
    ElasticsearchRepository<RestaurantSearchDocument, String> {

    fun findByCityIdAndOpenNow(cityId: String, openNow: Boolean): List<RestaurantSearchDocument>
}
```

Repository methods are fine for simple lookups. For serious search screens, use `ElasticsearchOperations` because you need bool queries, filters, sorting and pagination.

---

## 15.6 Production Search Query

```kotlin
@Service
class RestaurantSearchService(
    private val operations: ElasticsearchOperations
) {
    fun search(query: RestaurantSearchQuery): SearchPage<RestaurantSearchResult> {
        val criteria = Criteria("cityId").`is`(query.cityId)
            .and("openNow").`is`(true)

        val criteriaQuery = CriteriaQuery(criteria).apply {
            pageable = PageRequest.of(query.page, query.size)
        }

        val hits = operations.search(criteriaQuery, RestaurantSearchDocument::class.java)

        return SearchPage(
            items = hits.searchHits.map { RestaurantSearchResult.from(it.content) },
            total = hits.totalHits
        )
    }
}
```

For advanced ranking, use native queries from the Elasticsearch Java client via Spring Data support. Keep query construction in one search service so controllers stay clean.

---

## 15.7 Syncing PostgreSQL to Elasticsearch

Do not write PostgreSQL and Elasticsearch in the same transaction and pretend they are atomic. They are different systems.

Better pattern:

```text
1. Update PostgreSQL in transaction.
2. Insert outbox event in same PostgreSQL transaction.
3. Background publisher sends event to Kafka/RabbitMQ.
4. Search indexer consumes event.
5. Indexer updates Elasticsearch.
```

Event:

```kotlin
data class RestaurantChangedEvent(
    val restaurantId: UUID,
    val eventId: UUID = UUID.randomUUID(),
    val occurredAt: Instant = Instant.now()
)
```

Indexer:

```kotlin
@Component
class RestaurantSearchIndexer(
    private val restaurantRepository: RestaurantRepository,
    private val searchRepository: RestaurantSearchRepository
) {
    fun reindex(restaurantId: UUID) {
        val restaurant = restaurantRepository.findByIdOrNull(restaurantId)
            ?: run {
                searchRepository.deleteById(restaurantId.toString())
                return
            }

        searchRepository.save(RestaurantSearchDocument.from(restaurant))
    }
}
```

Good practice: build a full reindex job. Any search index can get out of sync.

---

## 15.8 Autocomplete

Autocomplete usually needs:

- Prefix matching.
- Typo tolerance.
- Popularity/ranking.
- Small response payload.

Create a separate suggestions index:

```kotlin
@Document(indexName = "restaurant_suggestions")
data class RestaurantSuggestionDocument(
    @Id val id: String,
    @Field(type = FieldType.Search_As_You_Type) val name: String,
    @Field(type = FieldType.Keyword) val cityId: String,
    @Field(type = FieldType.Integer) val popularity: Int
)
```

Do not return full restaurant cards from autocomplete. Return id, name, type and maybe highlighted text.

---

## 15.9 Search Quality Checklist

- Normalize synonyms: "cab" vs "taxi", "burger" vs "hamburger".
- Handle typos.
- Apply business boosts: sponsored, open now, higher rating, faster delivery.
- Use filters for exact constraints; use scoring for relevance.
- Log zero-result queries.
- A/B test ranking changes.
- Protect search endpoints with rate limits.
- Cache only stable query pages with short TTL.

---

## 15.10 Source Links

- Spring Data Elasticsearch: https://docs.spring.io/spring-data/elasticsearch/reference/index.html
- Spring Boot data overview: https://docs.spring.io/spring-boot/reference/data/index.html

