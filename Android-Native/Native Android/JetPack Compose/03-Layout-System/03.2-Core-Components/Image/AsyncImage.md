# AsyncImage in Jetpack Compose — Complete Notes

---

## What is AsyncImage?

`AsyncImage` is a Composable from the **Coil** library that loads images from the internet (or local storage) asynchronously without blocking the UI thread.

---

## Basic Usage

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = "My Image"
)
```

That's the simplest form. Just a URL string and a description.

---

## The `model` Parameter

This is where you tell Coil **what to load**. It has two forms:

**Form 1 — Simple String URL** (most common)

```kotlin
model = "https://example.com/image.jpg"
```

**Form 2 — ImageRequest Builder** (when you need more control)

```kotlin
model = ImageRequest.Builder(LocalContext.current)
    .data("https://example.com/image.jpg")  // the URL
    .decoderFactory(SvgDecoder.Factory())   // for SVG images
    .crossfade(true)                         // fade animation
    .placeholder(R.drawable.loading)         // show while loading
    .error(R.drawable.error)                 // show if failed
    .build()
```

---

## The `contentDescription` Parameter

A string describing the image for **accessibility** (screen readers for visually impaired users). Always provide it.

```kotlin
contentDescription = "User profile photo"
// or null if image is purely decorative
contentDescription = null
```

---

## The `modifier` Parameter

Controls **size, shape, padding, border** etc. of the image.

```kotlin
// Size
modifier = Modifier.size(80.dp)           // fixed square
modifier = Modifier.size(width=100.dp, height=50.dp) // rectangle
modifier = Modifier.fillMaxWidth()         // full width
modifier = Modifier.fillMaxSize()          // full screen

// Shape (clip)
modifier = Modifier.clip(CircleShape)              // circle
modifier = Modifier.clip(RoundedCornerShape(12.dp)) // rounded corners
modifier = Modifier.clip(RectangleShape)            // sharp corners

// Border
modifier = Modifier
    .size(80.dp)
    .clip(CircleShape)
    .border(2.dp, Color.Gray, CircleShape)  // border after clip

// Combined example
modifier = Modifier
    .size(80.dp)
    .padding(8.dp)
    .clip(CircleShape)
    .border(2.dp, Color.Blue, CircleShape)
```

---

## The `contentScale` Parameter

Controls **how the image fills its bounds** — similar to CSS `object-fit`.

```kotlin
contentScale = ContentScale.Crop    // fills bounds, crops overflow ✅ most used
contentScale = ContentScale.Fit     // fits inside bounds, may show empty space
contentScale = ContentScale.FillBounds // stretches to fill, may distort
contentScale = ContentScale.Inside  // like Fit but never scales up
contentScale = ContentScale.None    // original size, no scaling
contentScale = ContentScale.FillWidth  // fills width, height adjusts
contentScale = ContentScale.FillHeight // fills height, width adjusts
```

Visual comparison:

```
Original image: wide rectangle 🖼️

Crop       → [####] fills box, sides cut off
Fit        → [_##_] fits inside, empty sides
FillBounds → [####] stretches/distorts to fill
```

---

## Loading States (placeholder, error, fallback)

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(user.profileImage)
        .placeholder(R.drawable.ic_loading)   // shown WHILE loading
        .error(R.drawable.ic_error)           // shown IF load fails
        .fallback(R.drawable.ic_default)      // shown IF data is null
        .crossfade(true)                      // smooth fade when loaded
        .crossfade(500)                       // fade duration in ms
        .build(),
    contentDescription = "Profile Image"
)
```

Or using Compose Painters instead of drawables:

```kotlin
AsyncImage(
    model = user.profileImage,
    contentDescription = "Profile Image",
    placeholder = painterResource(R.drawable.ic_loading),
    error = painterResource(R.drawable.ic_error),
    fallback = painterResource(R.drawable.ic_default)
)
```

---

## SubcomposeAsyncImage — Full Control Over States

When you want to show a **custom Composable** for each state:

```kotlin
SubcomposeAsyncImage(
    model = user.profileImage,
    contentDescription = "Profile Image",
    modifier = Modifier.size(80.dp).clip(CircleShape)
) {
    when (painter.state) {
        is AsyncImagePainter.State.Loading -> {
            CircularProgressIndicator()  // custom loading UI
        }
        is AsyncImagePainter.State.Error -> {
            Icon(Icons.Default.Person, contentDescription = null) // custom error UI
        }
        else -> {
            SubcomposeAsyncImageContent()  // actual image
        }
    }
}
```

---

## Decoder Types (for special image formats)

```kotlin
// SVG images
.decoderFactory(SvgDecoder.Factory())

// GIF images
.decoderFactory(GifDecoder.Factory())

// Video frames
.decoderFactory(VideoFrameDecoder.Factory())
```

Dependencies needed:

```kotlin
implementation("io.coil-kt:coil-compose:2.6.0") // base
implementation("io.coil-kt:coil-svg:2.6.0")     // SVG
implementation("io.coil-kt:coil-gif:2.6.0")     // GIF
implementation("io.coil-kt:coil-video:2.6.0")   // Video
```

---

## Real World Profile Image Example

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(user.profileImage)
        .decoderFactory(SvgDecoder.Factory())
        .crossfade(true)
        .build(),
    contentDescription = "${user.username}'s profile photo",
    modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .border(2.dp, Color.Gray, CircleShape),
    contentScale = ContentScale.Crop,
    placeholder = painterResource(R.drawable.ic_default_avatar),
    error = painterResource(R.drawable.ic_default_avatar)
)
```

---

## Quick Reference Table

|Parameter|Purpose|
|---|---|
|`model`|What to load (URL or ImageRequest)|
|`contentDescription`|Accessibility text|
|`modifier`|Size, shape, padding|
|`contentScale`|How image fills its space|
|`placeholder`|Shown while loading|
|`error`|Shown if loading fails|
|`fallback`|Shown if data is null|
|`colorFilter`|Tint or color effects|
|`alpha`|Transparency (0f–1f)|