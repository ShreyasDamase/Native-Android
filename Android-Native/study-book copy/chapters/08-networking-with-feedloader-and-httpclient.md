# Chapter 8: Networking with `FeedLoader` and `HttpClient`

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/core/HttpClient.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/datasource/network/FeedLoader.kt`
- `shared/build.gradle.kts`
- `gradle/libs.versions.toml`


## 8.1 `FeedLoader`

`FeedLoader` is the network data source.

It is intentionally very small.

Its main job:

- request an RSS URL
- decode the response body into `RssFeed`
- attach metadata like `isDefault` and `sourceUrl`

This is a clean single-responsibility design.

## 8.2 Why `FeedLoader` is in `commonMain`

Because the logic is platform-independent at this level.

It uses Ktor’s multiplatform client API, so the same code can live in shared common code.

The actual engine backend differs by platform, but the call site does not need to care.

## 8.3 `HttpClient.kt`

This file creates the Ktor client.

It installs:

- logging plugin
- content negotiation plugin
- XML serialization support

Important point:

- this app consumes RSS XML, not JSON APIs

That is why XML configuration matters here.

## 8.4 Logging

If logging is enabled:

- Ktor request logs are sent through Napier

This is mostly useful on Android debug builds in this repo.

## 8.5 XML handling

The app configures XML content negotiation for:

- `ContentType.Application.Rss`

and uses XML serializer settings that ignore unknown child elements.

This is important because real RSS feeds are often messy and inconsistent.

This is a real-world detail, not just tutorial decoration.

## 8.6 Platform HTTP engines

The `shared` build file provides platform-specific Ktor engines:

- Android uses OkHttp
- iOS uses Darwin
- JVM desktop uses OkHttp

This is a classic KMP pattern:

- common API
- platform-specific engine implementation

That is one of KMP’s strongest practical benefits.

---

