# P0-7 — Object Storage: Why Not PostgreSQL for Images, MinIO vs Cloudflare R2 vs S3

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **The single most expensive architectural mistake at early-stage startups is storing binary files in a relational database.** This note explains exactly why it causes production failures, what object storage is and how to choose between S3/R2/MinIO, and gives you production-grade Kotlin + Spring Boot 3 code for every common pattern.

---

## 1. Why Storing Binary Files in PostgreSQL is Catastrophically Wrong

Let's be precise. PostgreSQL has two mechanisms for binary storage:

1. **`bytea` column**: Stores bytes inline in the table row.
2. **Large Object (lo)**: Stores bytes in a separate internal table (`pg_largeobject`), referenced by an OID.

Both are deeply unsuitable for production image/file storage. Here's exactly why:

### 1.1 PostgreSQL is Not Designed for Blob Throughput

PostgreSQL is an OLTP database. It is optimized for:
- Row-level ACID transactions
- Complex relational queries
- Index-based lookups returning small amounts of data

It is **not** optimized for:
- Streaming large binary payloads
- High-concurrency read/write of multi-MB objects
- Global low-latency delivery of static content

When you store a 2MB profile picture as `bytea`:

```sql
-- What you think you're doing:
INSERT INTO users (id, name, profile_picture) 
VALUES (1, 'Rahul', <2MB bytes>);

-- What actually happens in PostgreSQL internals:
-- 1. The 2MB blob is stored in TOAST (The Oversized-Attribute Storage Technique)
-- 2. PostgreSQL compresses it (if pglz compression is configured)
-- 3. It's split into 2KB TOAST chunks stored in pg_toast_<tableid>
-- 4. Your main table row stores only a pointer to the TOAST chunks
-- 5. Every SELECT that includes this column triggers a TOAST lookup
```

### 1.2 The VACUUM / Bloat Catastrophe

When you update or delete a row with a `bytea` column in PostgreSQL:
1. The old version of the TOAST chunks is marked dead (not immediately freed)
2. VACUUM must reclaim this space
3. For a table with 1 million user avatars at 500KB each = 500GB of TOAST data
4. VACUUM on 500GB of TOAST is extremely slow (hours)
5. During this time, your database bloats, queries slow down, WAL files accumulate

**Real consequence**: At Blinkit scale (10M users, each with a profile picture), this TOAST table is hundreds of gigabytes that needs constant VACUUM attention, slowing down every other database operation.

### 1.3 No Content Delivery Network Integration

When images are in PostgreSQL:
- Client requests image → Your Spring Boot app → JDBC query → PostgreSQL → bytes returned → HTTP response
- Every image request hits your application server AND your database
- No CDN caching is possible (CDNs cache HTTP responses from static URLs, not database queries)
- A user in Chennai fetching a product image that's in your Mumbai PostgreSQL server experiences 30-100ms database latency every time, even for the same image

With S3 + CloudFront:
- First request: CloudFront → S3 → stores in edge cache
- Subsequent requests: CloudFront edge → client (0 database involvement, 5-20ms from nearest edge)

### 1.4 Cost Comparison at Scale

| Approach | 1M product images (avg 200KB each) | Monthly data transfer (100M downloads) |
|---|---|---|
| PostgreSQL bytea | 200GB DB storage at $0.115/GB = $23/month (storage only, ignores performance cost) | Zero CDN, every transfer hits your DB + app server |
| S3 Standard | 200GB at $0.023/GB = $4.60/month | 100M × 200KB = 20TB at $0.09/GB = $1,843 |
| S3 + CloudFront | Same + CloudFront free tier covers first 1TB | Cache hit rate ~95% → only 1TB from S3 = $92 |
| Cloudflare R2 | 200GB at $0.015/GB = $3/month | **$0 egress fees** → Free! |

### 1.5 Scalability Wall

PostgreSQL connections are expensive (each connection is a process, ~5-10MB RAM). At 1,000 concurrent image requests, you need 1,000 database connections. PostgreSQL starts struggling beyond 200-500 connections. You'd need PgBouncer for connection pooling, which adds latency for every image read.

Object storage (S3, R2) has no connection limits. It's HTTP-based, serving from distributed infrastructure designed for exactly this workload.

> [!CAUTION]
> **The hidden cost of bytea storage is not just money — it's reliability.** Your payment processing, order management, and user data all live in the same PostgreSQL instance. Massive TOAST table bloat from image storage causes VACUUM pressure that degrades response times for your critical business queries. At 2 AM during a sale event, your checkout queries slow from 5ms to 800ms because VACUUM is thrashing through 200GB of avatar TOAST data. This has killed startups.

### What About Storing Just the File Path in PostgreSQL?

```sql
-- This is the RIGHT approach
CREATE TABLE products (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255),
    image_key   VARCHAR(512)  -- Store only the S3 key, e.g. "products/123/main.webp"
);
```

Store the file in S3. Store only the **key** (path) in PostgreSQL. Your database stays lean and fast.

---

## 2. Object Storage Fundamentals

Object storage is a storage architecture that manages data as discrete objects (files + metadata), rather than as a hierarchy of files (file system) or blocks (block storage).

### Core Concepts

```
Bucket
├── Key: "products/sku-123/main.webp"        ← Object 1
│   └── Metadata: {content-type: image/webp, size: 45230, custom: {sku: "123"}}
├── Key: "products/sku-123/thumbnail.webp"    ← Object 2
├── Key: "users/uid-456/avatar.jpg"           ← Object 3
└── Key: "invoices/2025/01/inv-789.pdf"       ← Object 4
```

**Bucket**: A globally-unique namespace for objects. Like a top-level directory, but flat — there are no real sub-directories, only key prefixes that look like paths.

**Key**: The full "path" of the object. `products/sku-123/main.webp` is a single key, not a directory hierarchy.

**Object**: The actual file bytes + key + metadata.

**Metadata**: Key-value pairs attached to the object. Can store content-type, custom business data, encoding info.

**Presigned URL**: A time-limited, cryptographically signed URL that grants temporary access to an object without requiring AWS credentials.

### S3-Compatible API

The S3 API has become the industry standard. MinIO, Cloudflare R2, DigitalOcean Spaces, Backblaze B2, and dozens of others implement the same API. This means code written for S3 works on R2 with only endpoint configuration changes.

The core operations:
- `PutObject`: Upload an object
- `GetObject`: Download an object
- `DeleteObject`: Delete an object
- `HeadObject`: Get metadata without downloading
- `ListObjectsV2`: List objects by prefix
- `GeneratePresignedUrl`: Create a temporary URL for upload or download
- `CopyObject`: Copy object within or between buckets

---

## 3. AWS S3 — The Gold Standard

### Pricing Model (as of 2025, us-east-1)

| Component | Cost |
|---|---|
| Storage (Standard) | $0.023 per GB/month |
| PUT, COPY, POST, LIST requests | $0.005 per 1,000 requests |
| GET, SELECT requests | $0.0004 per 1,000 requests |
| Data Transfer OUT to internet | $0.09 per GB (first 10TB/month) |
| Data Transfer IN | Free |
| Data Transfer to CloudFront | Free |

> [!IMPORTANT]
> **Egress costs are the silent killer.** If your app serves images directly from S3 to clients (not through CloudFront), every GB downloaded costs $0.09. At 10TB/month of image serving, that's $900/month — just for serving images. This is why S3 + CloudFront is almost always paired together. CloudFront caches at edge, and data transfer from S3 to CloudFront is free. Data transfer from CloudFront to users is $0.0085/GB (much cheaper than S3 direct).

### Storage Classes

| Class | Use Case | Cost vs Standard |
|---|---|---|
| S3 Standard | Frequently accessed files | 1× (baseline) |
| S3 Standard-IA | Infrequently accessed, e.g. old invoices | 0.46× storage, retrieval fee |
| S3 One Zone-IA | Non-critical, reproducible data | 0.37× storage, one AZ only |
| S3 Glacier Instant | Archives, retrieved in ms | 0.23× storage, higher retrieval |
| S3 Glacier Flexible | Long-term archives, minutes to hours retrieval | 0.13× storage |
| S3 Deep Archive | 7-10 year compliance archives | 0.07× storage, 12hr retrieval |

### Lifecycle Rules — Automatic Cost Optimization

```json
{
  "Rules": [
    {
      "ID": "move-old-uploads-to-ia",
      "Status": "Enabled",
      "Filter": {"Prefix": "uploads/"},
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 365,
          "StorageClass": "GLACIER_INSTANT_RETRIEVAL"
        }
      ],
      "NoncurrentVersionTransitions": [
        {
          "NoncurrentDays": 7,
          "StorageClass": "STANDARD_IA"
        }
      ],
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 90
      }
    },
    {
      "ID": "expire-temp-uploads",
      "Status": "Enabled",
      "Filter": {"Prefix": "temp/"},
      "Expiration": {"Days": 1}
    }
  ]
}
```

### Versioning

Versioning keeps all versions of every object. Critical for:
- Preventing accidental deletion (delete marker instead of actual delete)
- Audit trail for compliance
- Rollback capability

```bash
aws s3api put-bucket-versioning \
  --bucket my-prod-bucket \
  --versioning-configuration Status=Enabled
```

**Versioning + Lifecycle = Smart cost control**: Keep current version in Standard, move old versions to Glacier after 7 days, expire after 90 days.

---

## 4. Cloudflare R2 — When It's Dramatically Cheaper Than S3

Cloudflare R2 is S3-compatible object storage with **zero egress fees**. This is not a minor pricing difference — it fundamentally changes the economics.

### R2 Pricing (2025)

| Component | Cost |
|---|---|
| Storage | $0.015 per GB/month |
| Class A operations (write, list) | $4.50 per million |
| Class B operations (read) | $0.36 per million |
| Egress | **$0** |

### When R2 is Dramatically Cheaper Than S3

**Example: Video streaming app with 50TB/month egress**

| | S3 + CloudFront | Cloudflare R2 |
|---|---|---|
| Storage (10TB) | $230/month | $150/month |
| Egress (50TB) | 50,000 GB × $0.0085 = $425 | $0 |
| Total | ~$655/month | ~$150/month |

**Savings: 77%.**

### When R2 is NOT the Right Choice

- You need S3-specific features: S3 Select, S3 Object Lambda, S3 Batch Operations
- You're already deep in AWS ecosystem with IAM, VPC endpoints, etc.
- You need >1M Class A operations/month frequently (write-heavy workloads)
- You need multi-region replication with specific geo controls

### R2 Configuration in Spring Boot

```yaml
# application.yml
cloud:
  aws:
    s3:
      endpoint: https://<account-id>.r2.cloudflarestorage.com
    credentials:
      access-key: <R2_ACCESS_KEY_ID>
      secret-key: <R2_SECRET_ACCESS_KEY>
    region:
      static: auto  # R2 uses "auto" as region
```

---

## 5. MinIO — Self-Hosted S3-Compatible Storage

MinIO is an open-source, S3-compatible object storage server you can run anywhere. It's written in Go and can handle millions of objects.

### When to Use MinIO

- **Development/Testing**: Simulate S3 locally without AWS costs or internet dependency
- **On-premise requirements**: Healthcare, banking regulations that prohibit cloud storage
- **Hybrid cloud**: Data that legally cannot leave your data center
- **Cost control at extreme scale**: If you have petabytes of data and own hardware, MinIO can be cheaper than S3

### Running MinIO with Docker (Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"   # API port (S3-compatible)
      - "9001:9001"   # Console (web UI)
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

volumes:
  minio_data:
```

```bash
docker-compose up -d
# Access console at http://localhost:9001
```

> [!NOTE]
> For production MinIO deployments, use distributed mode with at least 4 nodes and erasure coding for data durability. Single-node MinIO is for development only — a single disk failure loses all data.

---

## 6. Spring Boot + AWS SDK v2 Integration

### Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // AWS SDK v2 — S3
    implementation(platform("software.amazon.awssdk:bom:2.25.0"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:s3-transfer-manager")  // For multipart upload
    implementation("software.amazon.awssdk:url-signer")
    
    // Spring Boot Web (for REST endpoints)
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

### S3 Configuration Bean

```kotlin
// S3Config.kt
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
class S3Config(
    @Value("\${aws.s3.access-key}") private val accessKey: String,
    @Value("\${aws.s3.secret-key}") private val secretKey: String,
    @Value("\${aws.s3.region}")     private val region: String,
    @Value("\${aws.s3.endpoint:}") private val endpoint: String,  // Optional: for MinIO/R2
) {

    private fun credentialsProvider() =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        )

    @Bean
    fun s3Client(): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())

        // Override endpoint for MinIO or Cloudflare R2
        if (endpoint.isNotBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                .forcePathStyle(true)  // MinIO requires path-style URLs
        }

        return builder.build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider())

        if (endpoint.isNotBlank()) {
            builder.endpointOverride(URI.create(endpoint))
        }

        return builder.build()
    }
}
```

```yaml
# application.yml
aws:
  s3:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
    region: ap-south-1
    bucket-name: myapp-prod-media
    # For MinIO (development):
    # endpoint: http://localhost:9000
    # For Cloudflare R2:
    # endpoint: https://<account-id>.r2.cloudflarestorage.com
    # region: auto
```

> [!CAUTION]
> **Never hardcode AWS credentials in source code.** Use environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY), AWS IAM roles (for EC2/ECS), or the AWS SDK's default credential provider chain. Hardcoded credentials in git repos are automatically found by bots scanning GitHub — your S3 bucket will be drained or your AWS bill will skyrocket within hours.

### S3 Service Layer

```kotlin
// S3StorageService.kt
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.InputStream
import java.time.Duration
import java.util.UUID

@Service
class S3StorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${aws.s3.bucket-name}") private val bucketName: String,
) {

    /**
     * Upload a file directly (server receives the file).
     * WARNING: Do NOT use this for user uploads at scale.
     * Use generatePresignedUploadUrl() instead.
     */
    fun uploadFile(
        key: String,
        inputStream: InputStream,
        contentType: String,
        contentLength: Long,
        metadata: Map<String, String> = emptyMap()
    ): String {
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .contentLength(contentLength)
            .metadata(metadata)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength))

        return key
    }

    /**
     * Generate a presigned URL for client-side upload (PUT).
     * Client uploads directly to S3 — your server is NOT in the upload path.
     * 
     * @param key The S3 key (path) where the file will be stored
     * @param contentType The MIME type (must match what client sends)
     * @param expiresIn URL validity duration
     * @return Presigned URL for PUT upload
     */
    fun generatePresignedUploadUrl(
        key: String,
        contentType: String,
        expiresIn: Duration = Duration.ofMinutes(15)
    ): PresignedUploadResponse {

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(expiresIn)
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest)

        return PresignedUploadResponse(
            uploadUrl = presignedPutObjectRequest.url().toString(),
            key = key,
            expiresAt = System.currentTimeMillis() + expiresIn.toMillis()
        )
    }

    /**
     * Generate a presigned URL for reading a private object.
     * Use for private documents, invoices, etc.
     */
    fun generatePresignedDownloadUrl(
        key: String,
        expiresIn: Duration = Duration.ofHours(1)
    ): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiresIn)
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    /**
     * Delete an object from S3.
     */
    fun deleteObject(key: String) {
        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()

        s3Client.deleteObject(request)
    }

    /**
     * Check if an object exists in S3.
     * Used to verify upload completion after presigned URL upload.
     */
    fun objectExists(key: String): Boolean {
        return try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            )
            true
        } catch (ex: NoSuchKeyException) {
            false
        }
    }

    /**
     * Get object metadata without downloading the content.
     */
    fun getObjectMetadata(key: String): HeadObjectResponse {
        return s3Client.headObject(
            HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()
        )
    }

    /**
     * Generate a unique key for user uploads.
     * Pattern: {type}/{userId}/{uuid}.{extension}
     */
    fun generateObjectKey(
        type: String,      // e.g., "avatars", "product-images", "documents"
        userId: String,
        originalFilename: String
    ): String {
        val extension = originalFilename.substringAfterLast('.', "")
        val uuid = UUID.randomUUID().toString()
        return if (extension.isNotEmpty()) "$type/$userId/$uuid.$extension"
        else "$type/$userId/$uuid"
    }
}

data class PresignedUploadResponse(
    val uploadUrl: String,
    val key: String,
    val expiresAt: Long
)
```

---

## 7. The Presigned URL Pattern — The Correct Architecture

This is the single most important pattern for file upload in any production API. Your server should NOT be in the upload path.

### Naive (Wrong) Pattern — Server as Upload Proxy

```
Client → [Spring Boot] → S3
         (server receives file, buffers it, re-uploads to S3)
```

**Problems:**
- Every upload consumes a Tomcat thread for the full upload duration (30s for a 50MB video on slow mobile)
- With 100 concurrent uploads, 100 Tomcat threads are blocked
- Your server's RAM is used to buffer large files
- Your server's outbound bandwidth is used to re-upload to S3
- Network round trip: Client → Server (latency + upload time) + Server → S3 (another upload)

### Correct Pattern — Presigned URL Direct Upload

```
1. Client → [Spring Boot] → "Give me a URL to upload my avatar"
2. [Spring Boot] → S3 SDK → generates presigned PUT URL (expires in 15 min)
3. [Spring Boot] → Client: { "uploadUrl": "https://s3.../avatars/uid/uuid.jpg?X-Amz-Signature=...", "key": "avatars/123/abc.jpg" }
4. Client → S3 (DIRECTLY, using presigned URL) — your server is not involved
5. Client → [Spring Boot]: "I finished uploading, here's the key: avatars/123/abc.jpg"
6. [Spring Boot] → verifies object exists in S3 → saves key in PostgreSQL
```

**Benefits:**
- Your server handles only 2 tiny HTTP calls per upload (request URL + confirm)
- S3 handles all the bandwidth and throughput
- Works at any scale (1 or 10,000 concurrent uploads — S3 doesn't care)
- Uploads fail in S3 — your server doesn't know or care, client retries

### REST Controller Implementation

```kotlin
// MediaController.kt
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/api/v1/media")
class MediaController(
    private val s3StorageService: S3StorageService,
    private val userService: UserService,
) {

    /**
     * Step 1: Client requests a presigned upload URL.
     * Client sends: filename, contentType, size (for validation)
     */
    @PostMapping("/upload-url")
    fun requestUploadUrl(
        @RequestBody request: UploadUrlRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<UploadUrlResponse> {

        // Validate file type
        val allowedTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
        if (request.contentType !in allowedTypes) {
            return ResponseEntity.badRequest().build()
        }

        // Validate file size (max 5MB for avatars)
        if (request.fileSizeBytes > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().build()
        }

        // Generate S3 key
        val key = s3StorageService.generateObjectKey(
            type = "avatars",
            userId = userId,
            originalFilename = request.filename
        )

        // Generate presigned URL
        val presignedResponse = s3StorageService.generatePresignedUploadUrl(
            key = key,
            contentType = request.contentType,
            expiresIn = Duration.ofMinutes(15)
        )

        return ResponseEntity.ok(
            UploadUrlResponse(
                uploadUrl = presignedResponse.uploadUrl,
                key = presignedResponse.key,
                expiresAt = presignedResponse.expiresAt,
                instructions = "PUT the file to uploadUrl with Content-Type: ${request.contentType}"
            )
        )
    }

    /**
     * Step 2: Client confirms upload completion.
     * Server verifies the file exists in S3, then saves key to DB.
     */
    @PostMapping("/confirm-upload")
    fun confirmUpload(
        @RequestBody request: ConfirmUploadRequest,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ConfirmUploadResponse> {

        // Security: ensure key belongs to this user
        if (!request.key.startsWith("avatars/$userId/")) {
            return ResponseEntity.status(403).build()
        }

        // Verify object actually exists in S3
        if (!s3StorageService.objectExists(request.key)) {
            return ResponseEntity.badRequest().body(
                ConfirmUploadResponse(
                    success = false,
                    message = "File not found in storage. Please re-upload."
                )
            )
        }

        // Save key to DB
        userService.updateAvatarKey(userId, request.key)

        return ResponseEntity.ok(
            ConfirmUploadResponse(
                success = true,
                message = "Avatar updated successfully"
            )
        )
    }

    /**
     * Get a presigned download URL for a private resource.
     */
    @GetMapping("/download-url/{key}")
    fun getDownloadUrl(
        @PathVariable key: String,
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<Map<String, String>> {

        // Authorization: verify user can access this resource
        // (implement your own authorization logic here)

        val url = s3StorageService.generatePresignedDownloadUrl(
            key = key,
            expiresIn = Duration.ofHours(1)
        )

        return ResponseEntity.ok(mapOf("url" to url))
    }
}

data class UploadUrlRequest(
    val filename: String,
    val contentType: String,
    val fileSizeBytes: Long
)

data class UploadUrlResponse(
    val uploadUrl: String,
    val key: String,
    val expiresAt: Long,
    val instructions: String
)

data class ConfirmUploadRequest(val key: String)

data class ConfirmUploadResponse(
    val success: Boolean,
    val message: String
)
```

### Client-Side Upload (Kotlin/Android example for completeness)

```kotlin
// This runs on the Android client — NOT on your server
suspend fun uploadAvatar(imageFile: File, uploadUrlResponse: UploadUrlResponse) {
    val client = OkHttpClient()
    
    val requestBody = imageFile.asRequestBody("image/jpeg".toMediaType())
    
    val request = Request.Builder()
        .url(uploadUrlResponse.uploadUrl)
        .put(requestBody)
        .addHeader("Content-Type", "image/jpeg")
        .build()
    
    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
        throw IOException("Upload failed: ${response.code}")
    }
    
    // Now call your backend to confirm
    // POST /api/v1/media/confirm-upload with { "key": uploadUrlResponse.key }
}
```

> [!WARNING]
> **The Content-Type header must match exactly.** When you generate the presigned URL with `contentType = "image/jpeg"`, the client PUT request MUST send `Content-Type: image/jpeg`. If they don't match, S3 rejects the request with a `403 SignatureDoesNotMatch` error. The Content-Type is included in the signature calculation.

---

## 8. Bucket Security — Blocking Public Access by Default

**Golden Rule: All buckets should be private by default.** Access is granted via:
1. Presigned URLs (time-limited, per-object)
2. IAM policies (for server-to-server access)
3. CloudFront OAI/OAC (for CDN access)

### S3 Bucket Policy for Private Access

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyPublicAccess",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::myapp-prod-media/*",
      "Condition": {
        "StringNotEquals": {
          "aws:PrincipalArn": [
            "arn:aws:iam::123456789:role/MyAppEC2Role",
            "arn:aws:iam::123456789:role/CloudFrontOAC"
          ]
        }
      }
    },
    {
      "Sid": "AllowCloudFrontRead",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::myapp-prod-media/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::123456789:distribution/EDFDVBD6EXAMPLE"
        }
      }
    }
  ]
}
```

### Block Public Access Settings (AWS Console / Terraform)

```hcl
# Terraform
resource "aws_s3_bucket_public_access_block" "media_bucket" {
  bucket = aws_s3_bucket.media_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
```

> [!CAUTION]
> **S3 bucket misconfiguration is responsible for some of the largest data breaches in history** (Capital One, Twitch, GoDaddy). The default "Block Public Access" settings were added precisely because developers kept accidentally making buckets public. Always verify these 4 settings are `true` in production. Enable AWS Config rules `s3-bucket-public-read-prohibited` and `s3-bucket-public-write-prohibited` to get alerted if this ever changes.

### Presigned URL Expiry Strategy

| Resource Type | Recommended Expiry | Reason |
|---|---|---|
| Profile avatar (upload) | 15 minutes | Upload should complete quickly |
| Document (download) | 1 hour | Time for user to read/download |
| Video (stream) | 4 hours | Long videos take time to watch |
| Invoice/Receipt | 24 hours | User may want to come back |
| Admin report | 30 minutes | Sensitive, short window |
| Temporary upload (before confirmation) | 15 minutes | Prevent abuse |

---

## 9. CDN Integration — CloudFront in Front of S3

### Architecture

```
User (Mumbai) → CloudFront Edge (Mumbai) → [cache hit] → Return cached content
                                         → [cache miss] → S3 (us-east-1) → cache + return
```

First request from Mumbai: ~200ms (Mumbai to us-east-1 + S3 read)
Subsequent requests: ~15ms (CloudFront edge cache)

### CloudFront Distribution Setup

```hcl
# Terraform
resource "aws_cloudfront_distribution" "media_cdn" {
  origin {
    domain_name              = aws_s3_bucket.media_bucket.bucket_regional_domain_name
    origin_id                = "S3-myapp-media"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }

  enabled             = true
  default_root_object = "index.html"

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-myapp-media"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true  # Enable gzip/brotli compression

    cache_policy_id = aws_cloudfront_cache_policy.media_policy.id
  }

  # Price class — only use edges in North America, Europe, Asia
  price_class = "PriceClass_200"

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    acm_certificate_arn = var.acm_cert_arn
    ssl_support_method  = "sni-only"
  }
}
```

### Cache-Control Headers for CDN

```kotlin
// When generating presigned URLs for public images (via CloudFront)
// set appropriate Cache-Control on the objects when uploading:

fun uploadProductImage(key: String, inputStream: InputStream, contentLength: Long): String {
    val request = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType("image/webp")
        .contentLength(contentLength)
        .cacheControl("public, max-age=31536000, immutable") // Cache for 1 year
        .build()

    s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength))
    return "https://cdn.myapp.com/$key"  // CloudFront URL
}
```

> [!NOTE]
> Use `immutable` in Cache-Control only for content-addressable objects where the URL changes when content changes (e.g., include a content hash in the key: `products/sku-123/main-a3f8b2c1.webp`). This tells browsers: "Never check if this URL has new content." If you use `immutable` on a key that can be overwritten, users will see stale images until their browser cache expires.

---

## 10. Image Optimization Pipeline

Uploading raw images from mobile/web and serving them directly is wasteful. A production image pipeline:

```
1. User uploads original (e.g., 4MB JPEG from iPhone camera)
2. S3 triggers Lambda/Cloud Function on PutObject event
3. Lambda:
   a. Downloads from S3
   b. Strips EXIF data (privacy!)
   c. Generates multiple sizes: thumbnail(150px), medium(600px), large(1200px)
   d. Converts to WebP (30-50% smaller than JPEG at same quality)
   e. Uploads all sizes to S3 with appropriate paths
4. Lambda notifies your Spring Boot service (via SQS or direct API call)
5. Spring Boot saves all image keys/URLs to PostgreSQL
```

### S3 Event Notification → SQS → Spring Boot

```yaml
# application.yml — Spring Cloud AWS SQS listener
cloud:
  aws:
    sqs:
      listener:
        auto-startup: true
```

```kotlin
// ImageProcessingListener.kt
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component

@Component
class ImageProcessingListener(
    private val imageProcessingService: ImageProcessingService
) {

    @SqsListener("image-processing-queue")
    fun onImageUploaded(event: S3EventNotification) {
        event.records.forEach { record ->
            val key = record.s3.`object`.key
            val bucket = record.s3.bucket.name

            imageProcessingService.processUploadedImage(bucket, key)
        }
    }
}

data class S3EventNotification(
    val records: List<S3EventRecord>
)

data class S3EventRecord(
    val s3: S3EventData
)

data class S3EventData(
    val bucket: S3BucketData,
    val `object`: S3ObjectData
)

data class S3BucketData(val name: String)
data class S3ObjectData(val key: String, val size: Long)
```

---

## 11. Production Pitfall: Serving Images Through Spring Boot

This bears special emphasis because it's so common.

### The Anti-Pattern

```kotlin
// DO NOT DO THIS IN PRODUCTION
@GetMapping("/images/{key}")
fun getImage(@PathVariable key: String): ResponseEntity<ByteArray> {
    val bytes = s3StorageService.downloadObject(key)
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .body(bytes)
}
```

**What happens at 1,000 concurrent users viewing product images:**
1. 1,000 requests hit your Spring Boot app
2. 1,000 Tomcat threads are allocated
3. Each thread makes a blocking S3 GetObject call
4. Each thread buffers the full image bytes in JVM heap
5. Each thread streams bytes back to client (slow clients = threads held longer)
6. JVM heap: 1,000 × 500KB image = 500MB just for image buffering
7. GC pressure increases, GC pauses increase, all other API calls slow down
8. If images average 200KB and 1,000 users are downloading simultaneously: 200MB/s of bandwidth through your app server

**The right pattern** (already covered): Presigned URLs or CloudFront. Your app returns a URL, client fetches from S3/CDN directly.

```kotlin
// DO THIS INSTEAD
@GetMapping("/products/{id}/image")
fun getProductImageUrl(@PathVariable id: String): ResponseEntity<Map<String, String>> {
    val product = productService.findById(id)
    val imageUrl = if (product.imageKey != null) {
        "https://cdn.myapp.com/${product.imageKey}"  // CloudFront (public images)
        // OR for private:
        // s3StorageService.generatePresignedDownloadUrl(product.imageKey)
    } else {
        "https://cdn.myapp.com/defaults/product-placeholder.webp"
    }
    return ResponseEntity.ok(mapOf("imageUrl" to imageUrl))
}
```

---

## 12. MinIO Java Client — For Local Development

The MinIO client is an alternative to AWS SDK when working with MinIO-specific features or as a simpler API.

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.minio:minio:8.5.9")
}
```

```kotlin
// MinIOConfig.kt
import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("development")  // Only active in development profile
class MinIOConfig(
    @Value("\${minio.endpoint}") private val endpoint: String,
    @Value("\${minio.access-key}") private val accessKey: String,
    @Value("\${minio.secret-key}") private val secretKey: String,
) {
    @Bean
    fun minioClient(): MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()
}
```

```kotlin
// MinIOStorageService.kt
import io.minio.*
import io.minio.http.Method
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Service
@Profile("development")
class MinIOStorageService(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket-name}") private val bucketName: String,
) {

    fun ensureBucketExists() {
        val exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build()
        )
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            )
        }
    }

    fun upload(key: String, inputStream: InputStream, contentType: String, size: Long): String {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(key)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build()
        )
        return key
    }

    fun generatePresignedUploadUrl(key: String, expiryMinutes: Int = 15): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucketName)
                .`object`(key)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .build()
        )
    }

    fun generatePresignedDownloadUrl(key: String, expiryHours: Int = 1): String {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(key)
                .expiry(expiryHours, TimeUnit.HOURS)
                .build()
        )
    }

    fun delete(key: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(key)
                .build()
        )
    }
}
```

---

## 13. Summary Decision Matrix

```
Which object storage should you use?
─────────────────────────────────────────────────────────────────
                    ┌─────────────────────────────────────────┐
                    │     Is data sovereignty required?        │
                    │ (must stay on-premise, HIPAA strict etc) │
                    └──────────────────┬──────────────────────┘
                                       │
                          ┌────────────┴────────────┐
                         YES                        NO
                          │                          │
                    ┌─────▼─────┐         ┌──────────▼──────────┐
                    │   MinIO   │         │ Is egress cost a     │
                    │ (self-    │         │ major concern?       │
                    │ hosted)   │         │ (video, large files, │
                    └───────────┘         │ high download volume)│
                                          └──────────┬──────────┘
                                                     │
                                        ┌────────────┴──────────────┐
                                       YES                          NO
                                        │                            │
                              ┌─────────▼────────┐       ┌──────────▼──────────┐
                              │  Cloudflare R2   │       │   AWS S3            │
                              │  ($0 egress)     │       │  (best ecosystem,   │
                              │                  │       │   Lambda, policies, │
                              └──────────────────┘       │   lifecycle rules)  │
                                                         └─────────────────────┘
```

| Factor | PostgreSQL bytea | AWS S3 | Cloudflare R2 | MinIO |
|---|---|---|---|---|
| Setup complexity | None (already have DB) | Low | Low | Medium |
| Egress cost | Server bandwidth | High | $0 | Your server |
| Scale limit | DB connection limit | Unlimited | Unlimited | Your hardware |
| CDN integration | ❌ | ✅ CloudFront | ✅ Cloudflare CDN | Manual |
| Presigned URLs | ❌ | ✅ | ✅ | ✅ |
| Production suitability | ❌ Never | ✅ Yes | ✅ Yes | ⚠️ Self-hosted |
| Dev/local use | ✅ OK for dev | ✅ | ✅ | ✅ Best |

---

*References: AWS S3 Documentation, Cloudflare R2 Documentation, MinIO Documentation, AWS SDK for Java v2, Pro Spring Boot 3 with Kotlin (Späth & Gutierrez, 2025)*
