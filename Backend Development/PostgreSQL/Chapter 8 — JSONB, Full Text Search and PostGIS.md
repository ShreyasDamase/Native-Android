# Chapter 8 — JSONB, Full Text Search and PostGIS

### _PostgreSQL features beyond normal tables_

---

## 8.1 JSONB

JSONB stores structured JSON efficiently.

Good for:

- provider response snapshot,
- flexible metadata,
- settings,
- audit payload.

Bad for:

- core relational facts,
- money,
- relationships,
- values requiring foreign keys.

```sql
ALTER TABLE payment_attempts
ADD COLUMN provider_response JSONB NOT NULL DEFAULT '{}';
```

Query:

```sql
SELECT *
FROM payment_attempts
WHERE provider_response ->> 'status' = 'succeeded';
```

---

## 8.2 Full Text Search

PostgreSQL can do basic full-text search.

```sql
ALTER TABLE restaurants
ADD COLUMN search_vector tsvector;

CREATE INDEX idx_restaurants_search
ON restaurants USING GIN(search_vector);
```

Query:

```sql
SELECT *
FROM restaurants
WHERE search_vector @@ plainto_tsquery('pizza');
```

Use PostgreSQL full-text search for small/medium search needs. Use Elasticsearch for advanced ranking, typo tolerance, autocomplete and large search products.

---

## 8.3 PostGIS

PostGIS adds geospatial capabilities.

```sql
CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE restaurants
ADD COLUMN location GEOGRAPHY(POINT, 4326);

CREATE INDEX idx_restaurants_location
ON restaurants USING GIST(location);
```

Nearby query:

```sql
SELECT *
FROM restaurants
WHERE ST_DWithin(
    location,
    ST_MakePoint(72.8777, 19.0760)::geography,
    3000
);
```

Use PostGIS for durable location queries. Use Redis GEO for fast temporary live matching.

