# Firebase Storage + Firestore + Room — File & Image Upload


### Android Jetpack Compose with Hilt

---

## Table of Contents

1. [The Story — What Firebase Storage Is](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#1-the-story--what-firebase-storage-is)
2. [How Firebase Storage Works — The Architecture](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#2-how-firebase-storage-works--the-architecture)
3. [Storage vs Firestore — What Goes Where](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#3-storage-vs-firestore--what-goes-where)
4. [Gradle Setup — All Dependencies](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#4-gradle-setup--all-dependencies)
5. [Permissions & File Picker — Modern API](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#5-permissions--file-picker--modern-api)
6. [The Data Layer — Entity, DTO, Domain Model](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#6-the-data-layer--entity-dto-domain-model)
7. [Storage Reference Paths — How to Organize Files](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#7-storage-reference-paths--how-to-organize-files)
8. [Upload Flow — Worst → Best Evolution](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#8-upload-flow--worst--best-evolution)
9. [Upload Progress Tracking](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#9-upload-progress-tracking)
10. [All File Types — Images, PDFs, Videos, Any File](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#10-all-file-types--images-pdfs-videos-any-file)
11. [Download URL — What It Is and How to Use It](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#11-download-url--what-it-is-and-how-to-use-it)
12. [Delete Files from Storage](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#12-delete-files-from-storage)
13. [Complete Repository — Storage + Firestore + Room](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#13-complete-repository--storage--firestore--room)
14. [Complete ViewModel](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#14-complete-viewmodel)
15. [Complete Compose UI](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#15-complete-compose-ui)
16. [Hilt DI Modules — Full Setup](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#16-hilt-di-modules--full-setup)
17. [Security Rules — Storage + Firestore](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#17-security-rules--storage--firestore)
18. [Upload with WorkManager (Reliable Background Upload)](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#18-upload-with-workmanager-reliable-background-upload)
19. [Common Mistakes](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#19-common-mistakes)
20. [Interview Questions](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#20-interview-questions)
21. [Quick Reference Cheat Sheet](https://claude.ai/chat/5c7ed7d3-70a5-48cb-b36e-519dedf39046#21-quick-reference-cheat-sheet)

---

---

# 1. The Story — What Firebase Storage Is

## The Problem It Solves

When you build an app where users upload profile pictures, post photos, share documents, or record voice notes — where does that file go?

Firestore and Realtime Database store **structured data** (text, numbers, booleans). They are NOT designed for files. Trying to store a 5 MB photo as a Base64 string in Firestore would:

- Hit the 1 MB document size limit immediately
- Be extremely expensive (every byte costs reads/writes)
- Be incredibly slow to load

**Firebase Cloud Storage** was built specifically for this problem — storing user-generated binary files (images, videos, audio, PDFs, anything) at Google scale.

## What Firebase Storage Actually Is

Firebase Storage is built on top of **Google Cloud Storage** — the same infrastructure Google uses for YouTube videos, Google Photos, and internal Google services.

```
Your Android App
      ↓
Firebase Storage SDK
      ↓
Google Cloud Storage bucket
(same as gs://your-project.appspot.com)
      ↓
Files served via CDN (globally distributed)
```

When a user downloads a file, it's served from a Google CDN server nearest to them — not from a single server somewhere. That's why file downloads are fast worldwide.

## Key Features

```
✓ Handles files of any size (up to 5 TB per file)
✓ Auto-pause and resume uploads (if network drops mid-upload)
✓ Google security — files protected by Firebase Security Rules
✓ Works with Firebase Auth — secure by user identity
✓ CDN delivery — fast downloads globally
✓ Direct integration with Firebase SDK
✓ Generate time-limited signed URLs for private files
✓ Progress tracking (bytes transferred / total bytes)
```

## The Pattern: Storage + Firestore

Files are never stored in Firestore. The pattern is always:

```
1. Upload FILE → Firebase Storage
                     ↓
           Get DOWNLOAD URL (a permanent https:// link)
                     ↓
2. Save DOWNLOAD URL → Firestore document
                     ↓
3. Load image in UI using the URL (with Coil or Glide)
```

Room caches the Firestore document (including the URL), so images load instantly from the URL on subsequent opens.

---

---

# 2. How Firebase Storage Works — The Architecture

## The Bucket

Firebase Storage uses a **bucket** — a top-level container for all your files (like a hard drive partition). Your Firebase project automatically gets a default bucket:

```
Bucket URL: gs://your-project-id.appspot.com
             or: gs://your-project-id.firebasestorage.app (newer projects)
```

Everything you upload goes inside this bucket.

## StorageReference — The File Pointer

A `StorageReference` is a pointer to a location inside your bucket. Like a file path on your computer.

```
Bucket:   gs://my-app.appspot.com/
                    │
                    ├── images/
                    │     ├── profile_photos/
                    │     │     ├── user_abc123.jpg    ← StorageReference
                    │     │     └── user_def456.jpg
                    │     └── posts/
                    │           └── post_xyz.jpg
                    ├── documents/
                    │     └── invoice_001.pdf
                    └── videos/
                          └── tutorial_01.mp4
```

In Kotlin:

```kotlin
val storage = Firebase.storage
val storageRef = storage.reference          // root reference

// Navigate to a specific path
val profileRef = storageRef.child("images/profile_photos/user_abc123.jpg")
val docRef = storageRef.child("documents/invoice_001.pdf")

// Properties of a reference:
profileRef.name    // "user_abc123.jpg"  (filename)
profileRef.path    // "images/profile_photos/user_abc123.jpg"  (full path)
profileRef.bucket  // "my-app.appspot.com"  (bucket name)
profileRef.parent  // reference to "images/profile_photos/"
profileRef.root    // reference to root "/"
```

## The Upload → URL → Display Pipeline

```
Step 1: User picks a file (URI from device)
        ↓
Step 2: Upload URI to StorageReference using putFile(uri)
        ↓ (bytes travel over network to Google's servers)
Step 3: Upload completes → get download URL
        val url = storageRef.downloadUrl.await().toString()
        ↓
Step 4: Store URL in Firestore document field "profileImageUrl"
        ↓
Step 5: Store in Room (local cache)
        ↓
Step 6: Load in Compose UI using Coil:
        AsyncImage(model = user.profileImageUrl, ...)
```

---

---

# 3. Storage vs Firestore — What Goes Where

This is the most important concept to internalize before writing any code.

```
FIREBASE STORAGE stores:
✓ Images (.jpg, .png, .webp, .gif)
✓ Videos (.mp4, .mov, .avi)
✓ Audio (.mp3, .wav, .ogg)
✓ Documents (.pdf, .docx, .xlsx)
✓ Any binary file

FIRESTORE stores:
✓ The file's METADATA
  - File name
  - Download URL (https://firebasestorage.googleapis.com/...)
  - Upload timestamp
  - File size
  - MIME type
  - Who uploaded it (userId)
  - Any other structured info about the file
```

## Real-World Example: User Profile

```kotlin
// Firestore document for a user:
data class UserFs(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String = "",   // ← URL to Storage, not the image itself!
    val profileImagePath: String = "",  // ← Storage path (for deletion)
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Firebase Storage holds:
// gs://my-app.appspot.com/images/profiles/user_abc123.jpg  ← the actual .jpg file
```

---

---

# 4. Gradle Setup — All Dependencies

```kotlin
// build.gradle.kts (app module)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")      // Google services plugin
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
    id("androidx.room")
}

android {
    compileSdk = 35
    defaultConfig { minSdk = 23 }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {

    // ── Firebase BOM (manages all versions) ──────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase Storage — for file uploads/downloads
    implementation("com.google.firebase:firebase-storage")

    // Firestore — for storing metadata + URLs
    implementation("com.google.firebase:firebase-firestore")

    // Firebase Auth (needed for storage security rules)
    implementation("com.google.firebase:firebase-auth")

    // ── Coroutines support for Firebase Tasks ────────────────────
    // Adds .await() extension to Firebase Task<T>
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ── Room ─────────────────────────────────────────────────────
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // ── Hilt ─────────────────────────────────────────────────────
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")          // Hilt + WorkManager
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // ── Image loading (to display downloaded images) ──────────────
    implementation("io.coil-kt:coil-compose:2.6.0")          // Coil for Compose
    implementation("io.coil-kt:coil-video:2.6.0")            // video thumbnails

    // ── WorkManager (for reliable background uploads) ────────────
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ── Lifecycle ────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
```

---

---

# 5. Permissions & File Picker — Modern API

## AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Internet permission — ALWAYS needed for Firebase -->
    <uses-permission android:name="android.permission.INTERNET"/>

    <!-- Storage permissions (only needed for API < 33) -->
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32"/>

    <!-- API 33+ uses granular media permissions instead -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>

    <!-- Camera (if you allow camera capture) -->
    <uses-permission android:name="android.permission.CAMERA"/>

    <application ...>
        <!-- File Provider for camera capture (creating temp files) -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths"/>
        </provider>
    </application>
</manifest>
```

## res/xml/file_paths.xml (create this file)

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-cache-path name="camera_photos" path="."/>
    <cache-path name="cache_files" path="."/>
</paths>
```

## Modern File Picker — ActivityResultContracts

```kotlin
// In your Composable or Activity:

// Pick any image from gallery
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
    uri?.let { viewModel.onImageSelected(it) }
}

// Pick any file (any MIME type)
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
    uri?.let { viewModel.onFileSelected(it) }
}

// Pick multiple images at once
val multipleImagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents()
) { uris: List<Uri> ->
    viewModel.onMultipleImagesSelected(uris)
}

// Pick a specific type — "image/*", "video/*", "audio/*", "application/pdf", "*/*"
Button(onClick = { imagePickerLauncher.launch("image/*") }) {
    Text("Pick Image")
}
Button(onClick = { filePickerLauncher.launch("*/*") }) {
    Text("Pick Any File")
}
Button(onClick = { filePickerLauncher.launch("application/pdf") }) {
    Text("Pick PDF")
}
```

## Getting File Info from a URI

When you get a `Uri` from the file picker, you need to extract its name, size, and MIME type to upload correctly:

```kotlin
// FileUtils.kt — utility functions for working with URIs
object FileUtils {

    // Get the MIME type of a URI (e.g., "image/jpeg", "application/pdf")
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    // Get the file name from a URI
    fun getFileName(context: Context, uri: Uri): String {
        // Try ContentResolver first (most reliable)
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex) ?: generateFileName(uri)
                }
            }
        }
        // Fallback: use last segment of the URI path
        return uri.lastPathSegment ?: generateFileName(uri)
    }

    // Get file size in bytes
    fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) return cursor.getLong(sizeIndex)
            }
        }
        return 0L
    }

    // Get human-readable file size
    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }

    private fun generateFileName(uri: Uri): String {
        return "file_${System.currentTimeMillis()}"
    }

    // Get file extension from MIME type
    fun getExtension(mimeType: String?): String = when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "video/mp4" -> "mp4"
        "audio/mpeg" -> "mp3"
        "application/pdf" -> "pdf"
        else -> "bin"
    }
}
```

---

---

# 6. The Data Layer — Entity, DTO, Domain Model

This is the critical section. You need THREE separate model classes when using Room + Firestore + Storage together. Each layer has its own responsibility.

## The Three Layers — Why Each Exists

```
                    STORAGE
                    (raw file)
                        ↓
              downloadUrl: "https://..."
                        ↓
    ┌───────────────────────────────────────┐
    │         FIRESTORE (cloud)             │
    │  UserFs — Firebase DTO                │
    │  @DocumentId, @ServerTimestamp        │
    │  all fields have default values       │
    └───────────────────────────────────────┘
                        ↓ sync
    ┌───────────────────────────────────────┐
    │         ROOM (local SQLite)           │
    │  UserEntity — Room Entity             │
    │  @Entity, @PrimaryKey                 │
    │  has sync status tracking             │
    └───────────────────────────────────────┘
                        ↓ map
    ┌───────────────────────────────────────┐
    │         DOMAIN (pure Kotlin)          │
    │  User — domain model                  │
    │  no Firebase, no Room, no Android     │
    │  ViewModel and UI use this            │
    └───────────────────────────────────────┘
```

## 1. Firestore DTO (data/remote/dto/UserFs.kt)

```kotlin
// Firestore model — what Firebase reads and writes
// All fields MUST have default values (Firestore needs no-arg constructor)
data class UserFs(
    @DocumentId
    val id: String = "",                    // injected by Firestore, not stored in doc

    val name: String = "",
    val email: String = "",
    val bio: String = "",

    // ── File/Image fields ────────────────────────────────────────
    val profileImageUrl: String = "",       // https:// download URL from Storage
    val profileImagePath: String = "",      // gs:// path in Storage (for deletion)
    val profileImageName: String = "",      // original filename
    val profileImageSize: Long = 0L,        // file size in bytes
    val profileImageMimeType: String = "",  // "image/jpeg", "image/png", etc.

    // ── Multiple files example (posts/documents) ─────────────────
    val attachments: List<Map<String, String>> = emptyList(),
    // Each map: {"url": "...", "path": "...", "name": "...", "mimeType": "..."}

    @ServerTimestamp
    val createdAt: Timestamp? = null,

    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
```

## 2. Room Entity (data/local/entity/UserEntity.kt)

```kotlin
// Room entity — stored in SQLite on device
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,                         // same ID as Firestore document

    val name: String,
    val email: String,
    val bio: String = "",

    // ── File/Image fields ────────────────────────────────────────
    val profileImageUrl: String = "",       // cached URL for offline display
    val profileImagePath: String = "",      // for deletion
    val profileImageName: String = "",
    val profileImageSize: Long = 0L,
    val profileImageMimeType: String = "",

    // ── Sync tracking ────────────────────────────────────────────
    val isSynced: Boolean = false,          // false = pending upload to Firestore
    val lastSyncedAt: Long = 0L,            // epoch millis of last successful sync
    val createdAt: Long = System.currentTimeMillis()
)
```

## 3. Domain Model (domain/model/User.kt)

```kotlin
// Pure Kotlin — no Firebase, no Room, no Android imports
// This is what the ViewModel and UI work with
data class User(
    val id: String,
    val name: String,
    val email: String,
    val bio: String = "",
    val profileImageUrl: String = "",
    val profileImageName: String = "",
    val profileImageSize: Long = 0L,
    val hasProfileImage: Boolean = profileImageUrl.isNotEmpty()  // computed
)
```

## 4. File Upload Model (domain/model/FileUpload.kt)

```kotlin
// Represents a file selected by the user, waiting to be uploaded
data class FileUpload(
    val uri: Uri,                           // local URI on the device
    val name: String,                       // file name
    val mimeType: String,                   // "image/jpeg", "application/pdf", etc.
    val sizeBytes: Long,                    // file size
    val localPreviewUri: Uri = uri          // same URI for local preview
)

// Upload result after Storage upload completes
data class UploadResult(
    val downloadUrl: String,                // https:// URL for the file
    val storagePath: String,                // gs:// path for future deletion
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)
```

## 5. Mappers (data/mapper/UserMapper.kt)

```kotlin
object UserMapper {

    // UserEntity → Domain model
    fun UserEntity.toDomain() = User(
        id = id,
        name = name,
        email = email,
        bio = bio,
        profileImageUrl = profileImageUrl,
        profileImageName = profileImageName,
        profileImageSize = profileImageSize
    )

    // Domain model → UserEntity
    fun User.toEntity(isSynced: Boolean = true) = UserEntity(
        id = id,
        name = name,
        email = email,
        bio = bio,
        profileImageUrl = profileImageUrl,
        profileImageName = profileImageName,
        profileImageSize = profileImageSize,
        isSynced = isSynced
    )

    // UserFs (Firestore) → UserEntity (Room)
    fun UserFs.toEntity() = UserEntity(
        id = id,
        name = name,
        email = email,
        bio = bio,
        profileImageUrl = profileImageUrl,
        profileImagePath = profileImagePath,
        profileImageName = profileImageName,
        profileImageSize = profileImageSize,
        profileImageMimeType = profileImageMimeType,
        isSynced = true,
        lastSyncedAt = System.currentTimeMillis()
    )

    // UserEntity (Room) → UserFs (Firestore)
    fun UserEntity.toFirestore() = UserFs(
        id = id,
        name = name,
        email = email,
        bio = bio,
        profileImageUrl = profileImageUrl,
        profileImagePath = profileImagePath,
        profileImageName = profileImageName,
        profileImageSize = profileImageSize,
        profileImageMimeType = profileImageMimeType
    )
}
```

---

---

# 7. Storage Reference Paths — How to Organize Files

## Path Strategy — Plan This Upfront

Good path organization makes security rules simple and files easy to manage.

```
Recommended structure:
gs://my-app.appspot.com/
    ├── users/
    │     └── {userId}/
    │           ├── profile/
    │           │     └── profile.jpg      ← always same name (overwrites old)
    │           └── documents/
    │                 ├── doc_uuid1.pdf
    │                 └── doc_uuid2.pdf
    ├── posts/
    │     └── {postId}/
    │           ├── image_uuid.jpg
    │           └── attachment_uuid.pdf
    └── public/
          └── assets/
                └── placeholder.jpg       ← publicly readable
```

## Path Constants

```kotlin
// data/remote/StoragePaths.kt
object StoragePaths {

    // User profile image — always same name (replaces old one)
    fun userProfile(userId: String): String =
        "users/$userId/profile/profile.jpg"

    // User uploaded document — unique name each time
    fun userDocument(userId: String, fileName: String): String =
        "users/$userId/documents/${UUID.randomUUID()}_$fileName"

    // Post image
    fun postImage(postId: String, fileName: String): String =
        "posts/$postId/images/${UUID.randomUUID()}_$fileName"

    // Any user file with auto-generated unique name
    fun userFile(userId: String, folder: String, mimeType: String): String {
        val ext = FileUtils.getExtension(mimeType)
        return "users/$userId/$folder/${UUID.randomUUID()}.$ext"
    }
}
```

## Why UUID in File Names

```
WITHOUT UUID:
  user uploads "photo.jpg"   → saved as "users/abc/profile/photo.jpg"
  user uploads "photo.jpg"   → OVERWRITES the old photo.jpg
  (sometimes you want this — like for profile photo!)

WITH UUID:
  user uploads "photo.jpg"   → saved as "users/abc/docs/f3a2c1..._photo.jpg"
  user uploads "photo.jpg"   → saved as "users/abc/docs/9b8e7d..._photo.jpg"
  (both exist — for post attachments where you keep history)
```

---

---

# 8. Upload Flow — Worst → Best Evolution

## WORST — Everything in Activity, raw callbacks

```kotlin
// ❌ WORST: callback hell, no separation, no error handling, no progress
class MainActivity : AppCompatActivity() {
    fun uploadImage(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
            .child("images/${UUID.randomUUID()}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        // Nested callback — now save to Firestore
                        Firebase.firestore.collection("users")
                            .document("hardcodedId")
                            .update("imageUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                // More nested callbacks...
                                // UI update on which thread?
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("TAG", e.message!!)
            }
    }
}
```

Problems: 4 levels of nesting, no Room, no architecture, unclear threading, no progress, memory leak risk.

---

## GOOD — Using coroutines + await()

```kotlin
// ✓ GOOD: coroutines flatten the callback pyramid
suspend fun uploadImage(userId: String, uri: Uri): String {
    val storageRef = Firebase.storage.reference
        .child("users/$userId/profile.jpg")

    // Upload the file
    storageRef.putFile(uri).await()

    // Get the download URL
    val downloadUrl = storageRef.downloadUrl.await().toString()

    // Save to Firestore
    Firebase.firestore.collection("users")
        .document(userId)
        .update("profileImageUrl", downloadUrl)
        .await()

    return downloadUrl
}
```

Better: readable, linear flow. But no Room, no progress, no error typing, not injected.

---

## BETTER — Repository with Result wrapping + Room

```kotlin
// ✓✓ BETTER: repository pattern, saves to both Room and Firestore
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore
) {
    suspend fun uploadProfileImage(userId: String, uri: Uri): Result<String> {
        return try {
            // 1. Upload to Storage
            val storagePath = "users/$userId/profile/profile.jpg"
            val storageRef = storage.reference.child(storagePath)
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // 2. Update Firestore
            firestore.collection("users").document(userId)
                .update(
                    "profileImageUrl", downloadUrl,
                    "profileImagePath", storagePath,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()

            // 3. Update Room cache
            userDao.updateProfileImage(userId, downloadUrl, storagePath)

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## BEST — Full repository with metadata, progress flow, all file types

```kotlin
// ✓✓✓ BEST: complete, production-ready upload with progress tracking
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : UserRepository {

    override fun uploadProfileImage(
        userId: String,
        uri: Uri
    ): Flow<UploadState> = flow {

        emit(UploadState.Preparing)

        // Extract file metadata before upload
        val mimeType = FileUtils.getMimeType(context, uri) ?: "image/jpeg"
        val fileName = FileUtils.getFileName(context, uri)
        val fileSize = FileUtils.getFileSize(context, uri)
        val storagePath = StoragePaths.userProfile(userId)

        emit(UploadState.Uploading(progress = 0f, bytesTransferred = 0L, totalBytes = fileSize))

        try {
            // 1. Build storage metadata
            val metadata = storageMetadata {
                contentType = mimeType
                setCustomMetadata("uploadedBy", userId)
                setCustomMetadata("originalName", fileName)
            }

            val storageRef = storage.reference.child(storagePath)

            // 2. Upload with progress tracking
            val uploadTask = storageRef.putFile(uri, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                // Note: this callback is on background thread — safe for emit
            }
            uploadTask.await()    // suspend until upload complete

            // 3. Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // 4. Save to Room immediately (offline cache)
            userDao.updateProfileImage(
                userId = userId,
                imageUrl = downloadUrl,
                imagePath = storagePath,
                imageName = fileName,
                imageSize = fileSize,
                imageMimeType = mimeType,
                isSynced = false           // not yet in Firestore
            )

            // 5. Save to Firestore
            firestore.collection("users").document(userId)
                .update(
                    mapOf(
                        "profileImageUrl" to downloadUrl,
                        "profileImagePath" to storagePath,
                        "profileImageName" to fileName,
                        "profileImageSize" to fileSize,
                        "profileImageMimeType" to mimeType,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            // 6. Mark as synced in Room
            userDao.markImageSynced(userId)

            emit(UploadState.Success(downloadUrl))

        } catch (e: StorageException) {
            emit(UploadState.Error("Storage error: ${e.message}", e))
        } catch (e: FirebaseFirestoreException) {
            // File uploaded but Firestore update failed
            // Room already has the URL so user can still see the image
            emit(UploadState.Error("Metadata save failed: ${e.message}", e))
        } catch (e: Exception) {
            emit(UploadState.Error(e.message ?: "Upload failed", e))
        }

    }.flowOn(Dispatchers.IO)
}
```

---

---

# 9. Upload Progress Tracking

## The UploadState Sealed Class

```kotlin
// domain/model/UploadState.kt
sealed class UploadState {
    object Idle : UploadState()
    object Preparing : UploadState()
    data class Uploading(
        val progress: Float,            // 0.0f to 1.0f
        val bytesTransferred: Long,
        val totalBytes: Long,
        val formattedProgress: String = "${(progress * 100).toInt()}%"
    ) : UploadState()
    data class Success(val downloadUrl: String) : UploadState()
    data class Error(val message: String, val exception: Exception? = null) : UploadState()
}
```

## Tracking Progress with callbackFlow

Firebase's `UploadTask` has a progress listener, but it's callback-based. We convert it to Flow:

```kotlin
fun uploadFileWithProgress(
    storagePath: String,
    uri: Uri,
    mimeType: String
): Flow<UploadState> = callbackFlow {

    trySend(UploadState.Preparing)

    val storageRef = storage.reference.child(storagePath)
    val metadata = storageMetadata { contentType = mimeType }
    val uploadTask = storageRef.putFile(uri, metadata)

    // Progress listener
    val progressListener = uploadTask.addOnProgressListener { snapshot ->
        val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
        trySend(
            UploadState.Uploading(
                progress = progress,
                bytesTransferred = snapshot.bytesTransferred,
                totalBytes = snapshot.totalByteCount
            )
        )
    }

    // Success listener
    val successListener = uploadTask.addOnSuccessListener {
        // Get download URL in a coroutine
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            trySend(UploadState.Success(uri.toString()))
            close()
        }.addOnFailureListener { e ->
            trySend(UploadState.Error("Failed to get URL: ${e.message}", e))
            close(e)
        }
    }

    // Failure listener
    val failureListener = uploadTask.addOnFailureListener { e ->
        trySend(UploadState.Error(e.message ?: "Upload failed", e))
        close(e)
    }

    awaitClose {
        // Remove all listeners when Flow is cancelled
        uploadTask.removeOnProgressListener(progressListener)
        uploadTask.removeOnSuccessListener(successListener)
        uploadTask.removeOnFailureListener(failureListener)
        // Cancel upload if still in progress
        if (!uploadTask.isComplete) uploadTask.cancel()
    }

}.flowOn(Dispatchers.IO)
```

## Showing Progress in Compose UI

```kotlin
@Composable
fun UploadProgressIndicator(state: UploadState) {
    when (state) {
        is UploadState.Idle -> { /* nothing */ }

        is UploadState.Preparing ->
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        is UploadState.Uploading -> Column {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${state.formattedProgress} — " +
                       "${FileUtils.formatFileSize(state.bytesTransferred)} / " +
                       "${FileUtils.formatFileSize(state.totalBytes)}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        is UploadState.Success ->
            Text("✓ Upload complete!", color = MaterialTheme.colorScheme.primary)

        is UploadState.Error ->
            Text("✗ ${state.message}", color = MaterialTheme.colorScheme.error)
    }
}
```

---

---

# 10. All File Types — Images, PDFs, Videos, Any File

## The Key Insight

The upload mechanism is identical for ALL file types. The only difference is:

1. The MIME type you pass to `StorageMetadata`
2. The Storage path (folder organization)
3. How you display it in the UI

## Image Upload

```kotlin
suspend fun uploadImage(userId: String, uri: Uri): Result<UploadResult> = try {
    val mimeType = FileUtils.getMimeType(context, uri) ?: "image/jpeg"
    val storagePath = StoragePaths.userFile(userId, "images", mimeType)
    val metadata = storageMetadata { contentType = mimeType }

    val ref = storage.reference.child(storagePath)
    ref.putFile(uri, metadata).await()
    val downloadUrl = ref.downloadUrl.await().toString()

    Result.success(UploadResult(
        downloadUrl = downloadUrl,
        storagePath = storagePath,
        fileName = FileUtils.getFileName(context, uri),
        mimeType = mimeType,
        sizeBytes = FileUtils.getFileSize(context, uri)
    ))
} catch (e: Exception) { Result.failure(e) }
```

**Display in Compose using Coil:**

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(user.profileImageUrl)
        .crossfade(true)
        .placeholder(R.drawable.ic_placeholder)
        .error(R.drawable.ic_error)
        .build(),
    contentDescription = "Profile photo",
    contentScale = ContentScale.Crop,
    modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
)
```

## PDF Upload

```kotlin
suspend fun uploadPdf(userId: String, uri: Uri): Result<UploadResult> = try {
    val storagePath = StoragePaths.userDocument(
        userId,
        FileUtils.getFileName(context, uri)
    )
    val metadata = storageMetadata { contentType = "application/pdf" }

    val ref = storage.reference.child(storagePath)
    ref.putFile(uri, metadata).await()
    val downloadUrl = ref.downloadUrl.await().toString()

    Result.success(UploadResult(
        downloadUrl = downloadUrl,
        storagePath = storagePath,
        fileName = FileUtils.getFileName(context, uri),
        mimeType = "application/pdf",
        sizeBytes = FileUtils.getFileSize(context, uri)
    ))
} catch (e: Exception) { Result.failure(e) }

// Display PDF — use an intent to open in system PDF viewer:
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
context.startActivity(intent)
```

## Video Upload

```kotlin
suspend fun uploadVideo(userId: String, uri: Uri): Result<UploadResult> = try {
    val mimeType = FileUtils.getMimeType(context, uri) ?: "video/mp4"
    val storagePath = StoragePaths.userFile(userId, "videos", mimeType)
    val metadata = storageMetadata { contentType = mimeType }

    val ref = storage.reference.child(storagePath)
    ref.putFile(uri, metadata).await()
    val downloadUrl = ref.downloadUrl.await().toString()

    Result.success(UploadResult(
        downloadUrl = downloadUrl,
        storagePath = storagePath,
        fileName = FileUtils.getFileName(context, uri),
        mimeType = mimeType,
        sizeBytes = FileUtils.getFileSize(context, uri)
    ))
} catch (e: Exception) { Result.failure(e) }

// Display video thumbnail with Coil:
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(downloadUrl)
        .decoderFactory { result, options, _ ->
            VideoFrameDecoder(result.source, options)  // coil-video
        }
        .build(),
    contentDescription = "Video thumbnail"
)
```

## Universal Upload — Any File Type

```kotlin
// One function to handle ALL file types
suspend fun uploadAnyFile(
    userId: String,
    uri: Uri,
    folder: String = "files"
): Result<UploadResult> = try {
    val mimeType = FileUtils.getMimeType(context, uri) ?: "application/octet-stream"
    val fileName = FileUtils.getFileName(context, uri)
    val storagePath = "users/$userId/$folder/${UUID.randomUUID()}_$fileName"
    val metadata = storageMetadata { contentType = mimeType }

    val ref = storage.reference.child(storagePath)
    ref.putFile(uri, metadata).await()
    val downloadUrl = ref.downloadUrl.await().toString()

    Result.success(UploadResult(
        downloadUrl = downloadUrl,
        storagePath = storagePath,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = FileUtils.getFileSize(context, uri)
    ))
} catch (e: Exception) { Result.failure(e) }
```

## MIME Type Reference

|File Type|MIME Type|Launch Intent|
|---|---|---|
|JPEG image|`image/jpeg`|View via browser/gallery|
|PNG image|`image/png`|View via browser/gallery|
|WebP image|`image/webp`|View via browser/gallery|
|GIF|`image/gif`|View via browser/gallery|
|PDF|`application/pdf`|`Intent.ACTION_VIEW` → PDF app|
|MP4 video|`video/mp4`|`Intent.ACTION_VIEW` → video player|
|MP3 audio|`audio/mpeg`|`Intent.ACTION_VIEW` → music player|
|Word doc|`application/msword`|`Intent.ACTION_VIEW` → Office app|
|Excel|`application/vnd.ms-excel`|`Intent.ACTION_VIEW` → Office app|
|ZIP|`application/zip`|`Intent.ACTION_VIEW` → file manager|
|Any file|`application/octet-stream`|fallback — let OS decide|

---

---

# 11. Download URL — What It Is and How to Use It

## What a Download URL Looks Like

```
https://firebasestorage.googleapis.com/v0/b/my-app.appspot.com/o/users%2Fabc123%2Fprofile%2Fprofile.jpg?alt=media&token=f3a2c1...
```

Parts:

```
Base:  https://firebasestorage.googleapis.com/v0/b/
Bucket: my-app.appspot.com/
Path:  o/users%2Fabc123%2Fprofile%2Fprofile.jpg
Query: ?alt=media&token=f3a2c1...  ← the auth token
```

## Important Facts About Download URLs

```
1. The URL is PERMANENT — it doesn't expire (unlike signed URLs)
2. Anyone who has the URL can access the file
   → Use Security Rules to restrict who CAN generate these URLs
3. The ?token= parameter is required — it's not secret, it's just required
4. If you delete the file, the URL returns 404
5. The URL works in any browser, Coil, Glide, or image loading library directly
6. You can regenerate a new token (revoke old URLs) from the Firebase Console
```

## Getting the Download URL

```kotlin
// After upload:
val downloadUrl = storageRef.downloadUrl.await().toString()

// From an existing path (if you stored the path):
val downloadUrl = storage.reference
    .child("users/abc123/profile/profile.jpg")
    .downloadUrl.await().toString()

// From a full gs:// URL:
val downloadUrl = storage
    .getReferenceFromUrl("gs://my-app.appspot.com/users/abc123/profile.jpg")
    .downloadUrl.await().toString()
```

## Store BOTH URL and Path

Always store BOTH in Firestore and Room:

```
profileImageUrl:  "https://firebasestorage..."  ← for displaying the image
profileImagePath: "users/abc123/profile/profile.jpg"  ← for deleting the file
```

You need the path to delete the file. You need the URL to display it. Don't store just one.

---

---

# 12. Delete Files from Storage

## The Complete Delete Flow

When a user deletes their profile image or post, you must:

1. Delete the file from Firebase Storage
2. Remove the URL and path from Firestore
3. Update Room cache

```kotlin
override suspend fun deleteProfileImage(userId: String): Result<Unit> = try {
    // Get the current storage path from Room (fast, no network)
    val user = userDao.getUserById(userId)
        ?: return Result.failure(Exception("User not found"))

    if (user.profileImagePath.isNotEmpty()) {
        // 1. Delete from Storage using the stored path
        storage.reference
            .child(user.profileImagePath)
            .delete()
            .await()
    }

    // 2. Clear image fields in Firestore
    firestore.collection("users").document(userId)
        .update(
            mapOf(
                "profileImageUrl" to "",
                "profileImagePath" to "",
                "profileImageName" to "",
                "profileImageSize" to 0L,
                "profileImageMimeType" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

    // 3. Clear image fields in Room
    userDao.clearProfileImage(userId)

    Result.success(Unit)
} catch (e: StorageException) {
    // StorageException.ERROR_OBJECT_NOT_FOUND: file already deleted — that's OK
    if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
        // File already gone — still clean up Firestore and Room
        firestore.collection("users").document(userId)
            .update("profileImageUrl", "").await()
        userDao.clearProfileImage(userId)
        Result.success(Unit)
    } else {
        Result.failure(e)
    }
} catch (e: Exception) {
    Result.failure(e)
}
```

---

---

# 13. Complete Repository — Storage + Firestore + Room

## Interface (domain/repository/UserRepository.kt)

```kotlin
interface UserRepository {
    // ── Read ────────────────────────────────────────────────────
    fun getUsers(): Flow<List<User>>
    suspend fun getUserById(id: String): Result<User>

    // ── Write (text data) ────────────────────────────────────────
    suspend fun createUser(user: User): Result<String>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun deleteUser(id: String): Result<Unit>

    // ── File uploads ─────────────────────────────────────────────
    fun uploadProfileImage(userId: String, uri: Uri): Flow<UploadState>
    suspend fun uploadAnyFile(userId: String, uri: Uri, folder: String): Result<UploadResult>
    suspend fun deleteProfileImage(userId: String): Result<Unit>

    // ── Sync ─────────────────────────────────────────────────────
    suspend fun syncFromFirestore(): Result<Unit>
}
```

## Implementation (data/repository/UserRepositoryImpl.kt)

```kotlin
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : UserRepository {

    private val usersRef = firestore.collection("users")

    // ── READ from Room (fast, reactive, offline) ─────────────────

    override fun getUsers(): Flow<List<User>> =
        userDao.getAllUsers().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getUserById(id: String): Result<User> = try {
        val entity = userDao.getUserById(id)
        if (entity != null) Result.success(entity.toDomain())
        else Result.failure(Exception("User $id not found"))
    } catch (e: Exception) { Result.failure(e) }

    // ── CREATE ────────────────────────────────────────────────────

    override suspend fun createUser(user: User): Result<String> = try {
        val ref = usersRef.document()
        val userWithId = user.copy(id = ref.id)

        // 1. Save to Room immediately (fast local write)
        userDao.insertUser(userWithId.toEntity(isSynced = false))

        // 2. Save to Firestore in background
        ref.set(userWithId.toFirestore()).await()

        // 3. Mark synced
        userDao.markSynced(ref.id)

        Result.success(ref.id)
    } catch (e: Exception) { Result.failure(e) }

    // ── UPLOAD PROFILE IMAGE (with progress) ─────────────────────

    override fun uploadProfileImage(
        userId: String,
        uri: Uri
    ): Flow<UploadState> = callbackFlow {

        trySend(UploadState.Preparing)

        val mimeType = FileUtils.getMimeType(context, uri) ?: "image/jpeg"
        val fileName = FileUtils.getFileName(context, uri)
        val fileSize = FileUtils.getFileSize(context, uri)
        val storagePath = StoragePaths.userProfile(userId)

        val storageRef = storage.reference.child(storagePath)
        val metadata = storageMetadata {
            contentType = mimeType
            setCustomMetadata("uploadedBy", userId)
            setCustomMetadata("originalName", fileName)
        }

        val uploadTask = storageRef.putFile(uri, metadata)

        // Progress tracking
        val progressListener = uploadTask.addOnProgressListener { snapshot ->
            val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
            trySend(
                UploadState.Uploading(
                    progress = progress,
                    bytesTransferred = snapshot.bytesTransferred,
                    totalBytes = snapshot.totalByteCount
                )
            )
        }

        // Success
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val downloadUrl = downloadUri.toString()

                // Save to Room and Firestore inside a coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Save to Room
                        userDao.updateProfileImage(
                            userId = userId,
                            imageUrl = downloadUrl,
                            imagePath = storagePath,
                            imageName = fileName,
                            imageSize = fileSize,
                            imageMimeType = mimeType,
                            isSynced = false
                        )
                        // Save to Firestore
                        usersRef.document(userId).update(
                            mapOf(
                                "profileImageUrl" to downloadUrl,
                                "profileImagePath" to storagePath,
                                "profileImageName" to fileName,
                                "profileImageSize" to fileSize,
                                "profileImageMimeType" to mimeType,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        // Mark synced
                        userDao.markImageSynced(userId)

                        trySend(UploadState.Success(downloadUrl))
                        close()
                    } catch (e: Exception) {
                        trySend(UploadState.Error("Save failed: ${e.message}", e))
                        close(e)
                    }
                }
            }
        }

        // Failure
        uploadTask.addOnFailureListener { e ->
            trySend(UploadState.Error(e.message ?: "Upload failed", e))
            close(e)
        }

        awaitClose {
            uploadTask.removeOnProgressListener(progressListener)
            if (!uploadTask.isComplete) uploadTask.cancel()
        }

    }.flowOn(Dispatchers.IO)

    // ── UPLOAD ANY FILE (no progress, simple) ─────────────────────

    override suspend fun uploadAnyFile(
        userId: String,
        uri: Uri,
        folder: String
    ): Result<UploadResult> = try {
        val mimeType = FileUtils.getMimeType(context, uri) ?: "application/octet-stream"
        val fileName = FileUtils.getFileName(context, uri)
        val fileSize = FileUtils.getFileSize(context, uri)
        val storagePath = "users/$userId/$folder/${UUID.randomUUID()}_$fileName"

        val ref = storage.reference.child(storagePath)
        val metadata = storageMetadata { contentType = mimeType }
        ref.putFile(uri, metadata).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        Result.success(UploadResult(
            downloadUrl = downloadUrl,
            storagePath = storagePath,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = fileSize
        ))
    } catch (e: Exception) { Result.failure(e) }

    // ── DELETE PROFILE IMAGE ───────────────────────────────────────

    override suspend fun deleteProfileImage(userId: String): Result<Unit> = try {
        val user = userDao.getUserById(userId)
        if (user?.profileImagePath?.isNotEmpty() == true) {
            try {
                storage.reference.child(user.profileImagePath).delete().await()
            } catch (e: StorageException) {
                if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
            }
        }
        usersRef.document(userId).update("profileImageUrl", "", "profileImagePath", "").await()
        userDao.clearProfileImage(userId)
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── SYNC from Firestore → Room ─────────────────────────────────

    override suspend fun syncFromFirestore(): Result<Unit> = try {
        val snapshot = usersRef.get().await()
        val entities = snapshot.toObjects(UserFs::class.java).map { it.toEntity() }
        userDao.upsertUsers(entities)
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```

---

---

# 14. Complete ViewModel

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────
    var uiState by mutableStateOf(UserUiState())
        private set

    // ── Users from Room (reactive) ────────────────────────────────
    val users: StateFlow<List<User>> = userRepository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // ── Selected file URI from picker ─────────────────────────────
    private var _selectedUri: Uri? = null

    init {
        syncFromCloud()
    }

    // ── Actions ───────────────────────────────────────────────────

    fun onImageSelected(uri: Uri) {
        _selectedUri = uri
        val fileName = FileUtils.getFileName(context, uri)
        val fileSize = FileUtils.getFileSize(context, uri)
        uiState = uiState.copy(
            selectedFileName = fileName,
            selectedFileSizeText = FileUtils.formatFileSize(fileSize),
            selectedLocalUri = uri.toString()
        )
    }

    fun uploadProfileImage(userId: String) {
        val uri = _selectedUri ?: run {
            uiState = uiState.copy(error = "No file selected")
            return
        }

        viewModelScope.launch {
            userRepository.uploadProfileImage(userId, uri)
                .collect { state ->
                    uiState = when (state) {
                        is UploadState.Idle -> uiState
                        is UploadState.Preparing ->
                            uiState.copy(uploadState = state, isUploading = true)
                        is UploadState.Uploading ->
                            uiState.copy(
                                uploadState = state,
                                isUploading = true,
                                uploadProgress = state.progress,
                                uploadProgressText = state.formattedProgress
                            )
                        is UploadState.Success ->
                            uiState.copy(
                                uploadState = state,
                                isUploading = false,
                                uploadProgress = 1f,
                                successMessage = "Upload complete!",
                                selectedLocalUri = null
                            )
                        is UploadState.Error ->
                            uiState.copy(
                                uploadState = state,
                                isUploading = false,
                                error = state.message
                            )
                    }
                }
        }
    }

    fun deleteProfileImage(userId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isDeletingImage = true)
            userRepository.deleteProfileImage(userId)
                .onSuccess {
                    uiState = uiState.copy(
                        isDeletingImage = false,
                        successMessage = "Image deleted"
                    )
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        isDeletingImage = false,
                        error = e.message
                    )
                }
        }
    }

    fun clearError() { uiState = uiState.copy(error = null) }
    fun clearSuccess() { uiState = uiState.copy(successMessage = null) }

    private fun syncFromCloud() {
        viewModelScope.launch {
            userRepository.syncFromFirestore()
        }
    }
}

data class UserUiState(
    val isUploading: Boolean = false,
    val isDeletingImage: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadProgressText: String = "0%",
    val uploadState: UploadState = UploadState.Idle,
    val selectedFileName: String? = null,
    val selectedFileSizeText: String? = null,
    val selectedLocalUri: String? = null,
    val error: String? = null,
    val successMessage: String? = null
)
```

---

---

# 15. Complete Compose UI

```kotlin
@Composable
fun UserProfileScreen(
    userId: String,
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val users by viewModel.users.collectAsStateWithLifecycle()
    val user = users.find { it.id == userId }

    // File picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // File picker for any type
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Profile Image Display ─────────────────────────────────
        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Show local preview if file selected, else show Firestore URL
            val imageModel = uiState.selectedLocalUri ?: user?.profileImageUrl

            if (imageModel != null && imageModel.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "No image",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Edit button overlay
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change photo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Selected File Info ────────────────────────────────────
        if (uiState.selectedFileName != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Selected: ${uiState.selectedFileName}",
                        style = MaterialTheme.typography.bodyMedium)
                    if (uiState.selectedFileSizeText != null) {
                        Text("Size: ${uiState.selectedFileSizeText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Upload Progress ───────────────────────────────────────
        if (uiState.isUploading) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { uiState.uploadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Uploading... ${uiState.uploadProgressText}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Upload Button ─────────────────────────────────────────
        Button(
            onClick = { viewModel.uploadProfileImage(userId) },
            enabled = uiState.selectedFileName != null && !uiState.isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (uiState.isUploading) "Uploading..." else "Upload Profile Image")
        }

        // ── Pick Other File Types ─────────────────────────────────
        OutlinedButton(
            onClick = { filePickerLauncher.launch("application/pdf") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick PDF Document")
        }

        OutlinedButton(
            onClick = { filePickerLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick Any File")
        }

        // ── Delete Image Button ───────────────────────────────────
        if (user?.profileImageUrl?.isNotEmpty() == true) {
            OutlinedButton(
                onClick = { viewModel.deleteProfileImage(userId) },
                enabled = !uiState.isDeletingImage,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isDeletingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Delete Profile Image")
            }
        }

        // ── Error / Success Snackbars ─────────────────────────────
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
                }
            }
        }

        uiState.successMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
```

---

---

# 16. Hilt DI Modules — Full Setup

## di/FirebaseModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore.also {
            it.firestoreSettings = firestoreSettings {
                isPersistenceEnabled = true
            }
        }
    }

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth
}
```

## di/DatabaseModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "app_database")
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
}
```

## di/RepositoryModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

## Room DAO — All Needed Queries

```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Upsert
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("""
        UPDATE users SET 
        profileImageUrl = :imageUrl,
        profileImagePath = :imagePath,
        profileImageName = :imageName,
        profileImageSize = :imageSize,
        profileImageMimeType = :imageMimeType,
        isSynced = :isSynced
        WHERE id = :userId
    """)
    suspend fun updateProfileImage(
        userId: String,
        imageUrl: String,
        imagePath: String,
        imageName: String,
        imageSize: Long,
        imageMimeType: String,
        isSynced: Boolean
    )

    @Query("UPDATE users SET isSynced = 1 WHERE id = :userId")
    suspend fun markSynced(userId: String)

    @Query("UPDATE users SET isSynced = 1 WHERE id = :userId")
    suspend fun markImageSynced(userId: String)

    @Query("""
        UPDATE users SET 
        profileImageUrl = '',
        profileImagePath = '',
        profileImageName = '',
        profileImageSize = 0,
        profileImageMimeType = ''
        WHERE id = :userId
    """)
    suspend fun clearProfileImage(userId: String)

    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<UserEntity>

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
```

---

---

# 17. Security Rules — Storage + Firestore

## Firebase Storage Rules

```javascript
// storage.rules
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    // ── User's own files ──────────────────────────────────────
    match /users/{userId}/{allPaths=**} {
      // Only the authenticated user can read/write their own files
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }

    // ── Post images (public read, auth write) ─────────────────
    match /posts/{postId}/{allPaths=**} {
      allow read: if true;   // anyone can see post images
      allow write: if request.auth != null;
    }

    // ── File size + type validation on upload ─────────────────
    match /users/{userId}/profile/{fileName} {
      allow write: if request.auth.uid == userId
        && request.resource.size < 5 * 1024 * 1024      // max 5 MB
        && request.resource.contentType.matches('image/.*');  // images only
    }

    match /users/{userId}/documents/{fileName} {
      allow write: if request.auth.uid == userId
        && request.resource.size < 10 * 1024 * 1024     // max 10 MB
        && (request.resource.contentType == 'application/pdf'
            || request.resource.contentType.matches('image/.*'));
    }

    // ── Public assets (app uses only) ─────────────────────────
    match /public/{allPaths=**} {
      allow read: if true;
      allow write: if false;   // only you (via Admin SDK) can write here
    }

    // ── Deny everything else ──────────────────────────────────
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

## Firestore Rules (matching the Storage rules)

```javascript
// firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /users/{userId} {
      // Only authenticated owner can read/write their own document
      allow read: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null
        && request.auth.uid == userId
        && request.resource.data.name is string
        && request.resource.data.email is string;
      allow update: if request.auth != null
        && request.auth.uid == userId;
      allow delete: if request.auth != null
        && request.auth.uid == userId;
    }

    // Deny everything not explicitly allowed
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

---

# 18. Upload with WorkManager (Reliable Background Upload)

For uploads that must survive app close, screen rotation, or the user navigating away — use WorkManager. The upload continues even if the app is killed.

```kotlin
// data/worker/UploadProfileImageWorker.kt
@HiltWorker
class UploadProfileImageWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : CoroutineWorker(ctx, params) {

    companion object {
        const val KEY_USER_ID = "userId"
        const val KEY_FILE_URI = "fileUri"
        const val KEY_MIME_TYPE = "mimeType"
        const val KEY_FILE_NAME = "fileName"
        const val KEY_FILE_SIZE = "fileSize"

        // Helper to build WorkRequest
        fun buildWorkRequest(
            userId: String,
            uri: Uri,
            mimeType: String,
            fileName: String,
            fileSize: Long
        ): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<UploadProfileImageWorker>()
                .setInputData(workDataOf(
                    KEY_USER_ID to userId,
                    KEY_FILE_URI to uri.toString(),
                    KEY_MIME_TYPE to mimeType,
                    KEY_FILE_NAME to fileName,
                    KEY_FILE_SIZE to fileSize
                ))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("upload_profile_$userId")
                .build()
    }

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val uriString = inputData.getString(KEY_FILE_URI) ?: return Result.failure()
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "image/jpeg"
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "unknown"
        val fileSize = inputData.getLong(KEY_FILE_SIZE, 0L)

        val uri = Uri.parse(uriString)
        val storagePath = "users/$userId/profile/profile.jpg"

        return try {
            val ref = storage.reference.child(storagePath)
            val metadata = storageMetadata { contentType = mimeType }
            ref.putFile(uri, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            // Update Firestore
            firestore.collection("users").document(userId)
                .update(
                    "profileImageUrl", downloadUrl,
                    "profileImagePath", storagePath,
                    "profileImageName", fileName,
                    "profileImageSize", fileSize
                ).await()

            // Update Room
            userDao.updateProfileImage(userId, downloadUrl, storagePath, fileName, fileSize, mimeType, true)

            Result.success(workDataOf("downloadUrl" to downloadUrl))
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure(workDataOf("error" to e.message))
        }
    }
}

// Schedule from ViewModel:
fun uploadProfileImageViaWorker(userId: String, uri: Uri) {
    val mimeType = FileUtils.getMimeType(context, uri) ?: "image/jpeg"
    val workRequest = UploadProfileImageWorker.buildWorkRequest(
        userId = userId,
        uri = uri,
        mimeType = mimeType,
        fileName = FileUtils.getFileName(context, uri),
        fileSize = FileUtils.getFileSize(context, uri)
    )
    WorkManager.getInstance(context).enqueue(workRequest)
}
```

---

---

# 19. Common Mistakes

|Mistake|Problem|Fix|
|---|---|---|
|Storing the image file in Firestore (as Base64 or bytes)|Firestore 1 MB limit → immediate crash for any real image|Store in Firebase Storage, save URL in Firestore|
|Not storing the storage PATH alongside the URL|Can't delete the file later — URL alone isn't enough|Always store both `downloadUrl` and `storagePath`|
|Using the same file name for every upload (no UUID)|User uploads 2 files — second overwrites first|Use `UUID.randomUUID()` in path unless intentional overwrite|
|Not checking `!snapshot.exists()` after Firestore read|NullPointerException when doc doesn't exist|Always check `snapshot.exists()`|
|Forgetting to cancel the UploadTask when Flow is cancelled|Upload continues after screen leaves → battery + data waste|Cancel in `awaitClose { if (!task.isComplete) task.cancel() }`|
|Using `FirebaseStorage.getInstance()` directly in ViewModel|Untestable, not injected, hard to mock|Inject via Hilt, use repository pattern|
|Not handling `StorageException.ERROR_OBJECT_NOT_FOUND` when deleting|Throws if file already deleted (e.g., deleted from console)|Catch this specific error code and treat as success|
|Requesting `READ_EXTERNAL_STORAGE` on API 33+|Permission denied — Google changed to granular permissions|Use `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO` on API 33+|
|Not updating Room after upload succeeds|UI shows old data until next app restart|Always update Room in the success path|
|Using callback-based `addOnSuccessListener` inside a `suspend fun`|Threading issues — Firebase callbacks run on main thread|Use `.await()` from `kotlinx-coroutines-play-services` instead|
|Trying to upload a large video file on the main thread|ANR (App Not Responding)|Always upload on `Dispatchers.IO` or use WorkManager|
|Not setting `contentType` in `StorageMetadata`|File is served as `application/octet-stream` — browsers can't display images|Always set `contentType = mimeType` in metadata|

---

---

# 20. Interview Questions

**Q: Where should binary files (images, PDFs) be stored in a Firebase app?**

In Firebase Storage, not Firestore. Firestore has a 1 MB per-document limit and charges per read/write operation — storing binary data there would immediately hit the size limit and be expensive. Firebase Storage is built on Google Cloud Storage, designed for files of any size. The standard pattern is: upload the file to Storage, get a download URL, store that URL as a string field in a Firestore document. The URL is then used by Coil or Glide to display the image in the UI.

---

**Q: What is a StorageReference and how do you get a download URL?**

A `StorageReference` is a pointer to a location inside your Firebase Storage bucket. You create one by calling `Firebase.storage.reference.child("path/to/file")`. After uploading with `.putFile(uri).await()`, you call `.downloadUrl.await()` on the same reference to get a permanent `https://` URL. This URL can be used to display the image in any image loading library like Coil, shared with other users, or embedded in web pages.

---

**Q: How do you track upload progress in a clean architecture app?**

Define a sealed class `UploadState` with cases for `Idle`, `Preparing`, `Uploading(progress, bytesTransferred, totalBytes)`, `Success(downloadUrl)`, and `Error(message)`. In the repository, use `callbackFlow` to convert Firebase's `addOnProgressListener` callback to a Flow — emit `UploadState.Uploading` on each progress update, `UploadState.Success` on completion, and close with an error on failure. Use `awaitClose` to remove the listener and cancel the upload task when the Flow is cancelled. In the ViewModel, collect this Flow and update a `UiState` data class. The Composable observes the UiState and shows a `LinearProgressIndicator`.

---

**Q: Why do you store both the download URL and the storage path?**

The download URL (`https://firebasestorage.googleapis.com/...`) is used to display the file. But to delete a file from Storage, you need the `StorageReference` which is constructed from the storage path (`users/abc123/profile/profile.jpg`). You cannot derive the path from the URL reliably, so always store both. If you only store the URL and later need to delete the file, you have no way to remove it from Storage — it will remain there indefinitely consuming storage space.

---

**Q: How do you handle an upload that should survive the user closing the app?**

Use WorkManager with a `CoroutineWorker`. Set a `Constraints` that requires network connectivity. Build a `OneTimeWorkRequest` with the file URI and metadata as input data. WorkManager persists the request to disk and retries it with exponential backoff. Even if the app is killed, Android will restart the Worker when the constraints are met (network available). This is the production pattern for reliable media uploads.

---

---

# 21. Quick Reference Cheat Sheet

## The Upload Pipeline

```
1. User picks file → Uri
2. Get mimeType, fileName, fileSize from Uri via ContentResolver
3. Upload: storage.reference.child(path).putFile(uri, metadata).await()
4. Get URL: storageRef.downloadUrl.await().toString()
5. Save URL + path to Room (immediate, offline cache)
6. Save URL + path to Firestore (background, cloud sync)
7. Display: AsyncImage(model = url)
```

## Storage Reference

```kotlin
val ref = Firebase.storage.reference.child("users/$userId/profile.jpg")
ref.putFile(uri).await()                   // upload
ref.downloadUrl.await().toString()         // get URL
ref.delete().await()                       // delete
ref.metadata.await()                       // get file info
```

## StorageMetadata

```kotlin
val metadata = storageMetadata {
    contentType = "image/jpeg"             // REQUIRED for images to display in browser
    setCustomMetadata("uploadedBy", userId)
    setCustomMetadata("originalName", fileName)
}
ref.putFile(uri, metadata).await()
```

## UploadState Sealed Class

```kotlin
sealed class UploadState {
    object Idle : UploadState()
    object Preparing : UploadState()
    data class Uploading(val progress: Float, val bytesTransferred: Long, val totalBytes: Long) : UploadState()
    data class Success(val downloadUrl: String) : UploadState()
    data class Error(val message: String, val exception: Exception? = null) : UploadState()
}
```

## Three Model Classes

```
UserFs       → Firestore DTO  (@DocumentId, @ServerTimestamp, all = "" defaults)
UserEntity   → Room entity    (@Entity, @PrimaryKey, sync tracking)
User         → Domain model   (pure Kotlin, no Android/Firebase imports)
```

## File Type Picker

```kotlin
imagePickerLauncher.launch("image/*")          // images only
filePickerLauncher.launch("application/pdf")   // PDFs only
filePickerLauncher.launch("video/*")           // videos only
filePickerLauncher.launch("*/*")               // any file
```

## Critical Rule

```
NEVER store images/files IN Firestore
ALWAYS store files in Firebase Storage, store the URL in Firestore
```

---

_Sources: firebase.google.com/docs/storage, firebase.google.com/docs/storage/android/upload-files, firebase.google.com/docs/storage/android/delete-files, developer.android.com/training/data-storage/shared/documents-files_