# Chapter 10: The Domain Model and RSS XML Mapping

## Code Files To Open

- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/domain/RssFeed.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/datasource/network/FeedLoader.kt`
- `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/core/HttpClient.kt`


## 10.1 Why the domain model matters

The domain model is the language of the app.

If the network is the outside world and the UI is the visible world, the domain model is the internal truth.

In this repo, the domain model lives mainly in:

- `RssFeed.kt`

## 10.2 Main classes

This file contains:

- `RssFeed`
- `Channel`
- `Image`
- `Item`
- `MediaContent`
- `MediaDescription`
- `MediaCredit`

These mirror the shape of RSS XML content.

## 10.3 Serialization annotations

The file uses:

- `@Serializable`
- XML-related annotations such as `@XmlSerialName`, `@XmlElement`, `@XmlValue`

This tells the serializer how to map XML elements and namespaces to Kotlin data classes.

This is a very important detail:

- the app is not manually parsing XML strings
- it is mapping XML structure into Kotlin data classes

## 10.4 RSS and media namespaces

Some fields come from namespaced XML entries, especially media-related ones.

That is why fields like `contentEncoded` and `mediaContent` use XML namespace annotations.

This is not random complexity. It is required because RSS feeds often include:

- standard RSS fields
- content module fields
- media module fields

## 10.5 `Item.getImageUrl()`

This helper function tries to determine an image URL for a post.

It checks:

1. `mediaContent?.url`
2. fallback regex extraction from HTML in `contentEncoded`

This is a very practical detail.

Real feeds are inconsistent, so robust apps often need fallback extraction logic.

## 10.6 Mutable properties in `RssFeed`

Notice that `sourceUrl` and `isDefault` are mutable.

Why?

Because the network response itself may not contain those app-specific values.

After loading from the feed URL, the app enriches the model with:

- where this feed came from
- whether it is one of the default feeds

That is application metadata added on top of feed data.

---

