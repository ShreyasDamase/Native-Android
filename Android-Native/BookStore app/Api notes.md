# Complete API Guide for Book Creation (Backend Focus)

Based on your backend API, here's a detailed guide for creating a book with all the technical specifications you need to implement in your Android app.

---

## **API Endpoint Details**

### **Create Book**

**Endpoint:** `POST /api/books`

**Authentication:** Required (Bearer Token)

**Content-Type:** `application/json`

---

## **Request Structure**

### **Headers**

```http
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json
```

### **Request Body (JSON)**

```json
{
  "title": "The Great Gatsby",
  "caption": "A classic American novel about the Jazz Age",
  "rating": 5,
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD..."
}
```

---

## **Field Specifications**

|Field|Type|Required|Validation|Description|
|---|---|---|---|---|
|`title`|String|✅ Yes|Min: 1 char, Max: ~200 chars|Book title|
|`caption`|String|✅ Yes|Min: 1 char, Max: ~500 chars|Book description/review|
|`rating`|Number|✅ Yes|Min: 1, Max: 5|Rating from 1 to 5 stars|
|`image`|String|✅ Yes|Base64 encoded image with data URI|Book cover image|

---

## **Image Format Details**

### **Base64 Image Structure**

```
data:[MIME_TYPE];base64,[BASE64_ENCODED_DATA]
```

### **Supported MIME Types**

- `image/jpeg` (recommended)
- `image/jpg`
- `image/png`
- `image/webp`

### **Example Base64 Image**

```json
{
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBxMTEhUTExMWFhUXGB4aGBgYGB8dHxsfHx8dHx8eHx8gHiggIB4lHR8dITEhJSkrLi4uHR8zODMtNygtLisBCgoKDg0OGxAQGy0lICUtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLf/..."
}
```

### **Image Processing Requirements**

- **Format:** JPEG, PNG, or WebP
- **Encoding:** Base64 with data URI prefix
- **Recommended max size:** 5MB (before compression)
- **Recommended dimensions:** 800x800px (after compression)
- **Compression quality:** 80% for JPEG

---

## **Complete Example Request**

### **Using cURL**

```bash
curl -X POST http://localhost:3000/api/books \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "To Kill a Mockingbird",
    "caption": "A gripping tale of racial injustice and childhood innocence in the American South",
    "rating": 5,
    "image": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
  }'
```

### **Using Postman**

1. **Method:** `POST`
2. **URL:** `{{base_url}}/api/books`
3. **Headers:**
    - `Authorization`: `Bearer {{accessToken}}`
    - `Content-Type`: `application/json`
4. **Body (raw JSON):**

```json
{
  "title": "1984",
  "caption": "A dystopian social science fiction novel and cautionary tale",
  "rating": 5,
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
}
```

---

## **Response Structure**

### **Success Response (201 Created)**

```json
{
  "_id": "6789abcd1234567890efghij",
  "title": "The Great Gatsby",
  "caption": "A classic American novel about the Jazz Age",
  "rating": 5,
  "image": "https://res.cloudinary.com/your-cloud/image/upload/v1234567890/abc123.jpg",
  "user": "6789abcd1234567890efghij",
  "createdAt": "2025-01-07T10:30:00.000Z",
  "updatedAt": "2025-01-07T10:30:00.000Z",
  "__v": 0
}
```

### **Response Fields Explanation**

|Field|Type|Description|
|---|---|---|
|`_id`|String|MongoDB ObjectId of the book|
|`title`|String|Book title (as submitted)|
|`caption`|String|Book caption/review (as submitted)|
|`rating`|Number|Rating 1-5 (as submitted)|
|`image`|String|**Cloudinary URL** (not Base64 anymore)|
|`user`|String|User ID who created the book|
|`createdAt`|String|ISO 8601 timestamp of creation|
|`updatedAt`|String|ISO 8601 timestamp of last update|
|`__v`|Number|MongoDB version key|

**Important:** The `image` field in the response is a **Cloudinary URL**, not Base64. The backend automatically uploads the Base64 image to Cloudinary and returns the permanent URL.

---

### **Error Responses**

#### **400 Bad Request - Missing Fields**

```json
{
  "message": "Please provide all fields"
}
```

**Trigger:** When any required field (`title`, `caption`, `rating`, `image`) is missing or empty.

---

#### **401 Unauthorized - No Token**

```json
{
  "message": "No authentication token, access denied"
}
```

**Trigger:** When the `Authorization` header is missing or doesn't contain a token.

---

#### **401 Unauthorized - Invalid Token**

```json
{
  "message": "Token is not valid"
}
```

**Trigger:** When the token is:

- Expired
- Malformed
- Invalid signature
- User doesn't exist

---

#### **500 Internal Server Error**

```json
{
  "message": "Error message details"
}
```

**Trigger:** Server-side errors such as:

- Cloudinary upload failure
- Database connection issues
- Unexpected errors

---

## **Form Fields for Android Implementation**

### **1. Title Field**

**What to learn:**

- `OutlinedTextField` or `TextField` in Compose
- Text input state management
- Input validation

**Specifications:**

- **Input Type:** Text (single line)
- **Required:** Yes
- **Min Length:** 1 character
- **Max Length:** 200 characters (recommended)
- **Keyboard Type:** `KeyboardType.Text`
- **Capitalization:** `KeyboardCapitalization.Sentences`
- **IME Action:** `ImeAction.Next`

**Validation Rules:**

```
- Must not be empty or blank
- Trim whitespace before submission
- Show error if empty on submit
```

---

### **2. Caption Field**

**What to learn:**

- Multi-line text input
- Text field with min/max lines
- Character counter (optional)

**Specifications:**

- **Input Type:** Text (multi-line)
- **Required:** Yes
- **Min Length:** 1 character
- **Max Length:** 500 characters (recommended)
- **Min Lines:** 3
- **Max Lines:** 8
- **Keyboard Type:** `KeyboardType.Text`
- **Capitalization:** `KeyboardCapitalization.Sentences`
- **IME Action:** `ImeAction.Default`

**Validation Rules:**

```
- Must not be empty or blank
- Trim whitespace before submission
- Show error if empty on submit
- Optional: Show character count
```

---

### **3. Rating Field**

**What to learn:**

- `Slider` component
- Custom star rating component
- Number input handling

**Specifications:**

- **Input Type:** Integer (1-5)
- **Required:** Yes
- **Min Value:** 1
- **Max Value:** 5
- **Default Value:** 3
- **Step:** 1

**UI Options to Explore:**

1. **Slider:** Simple horizontal slider (1-5)
2. **Star Rating:** Interactive stars (tap to rate)
3. **Button Group:** 5 buttons labeled 1-5
4. **Number Picker:** Android number picker component

**Validation Rules:**

```
- Value must be between 1 and 5
- Convert to Int before sending
```

---

### **4. Image Field**

**What to learn:**

- `ActivityResultContracts.GetContent()` - Pick from gallery
- `ActivityResultContracts.TakePicture()` - Take photo
- Permission handling (`READ_MEDIA_IMAGES`, `CAMERA`)
- Bitmap manipulation
- Base64 encoding
- Image compression
- Coil library for image display

**Specifications:**

- **Input Type:** Image (URI → Base64)
- **Required:** Yes
- **Sources:**
    - Device gallery
    - Camera
- **Processing Steps:**
    1. Get image URI
    2. Load as Bitmap
    3. Compress/resize
    4. Convert to Base64
    5. Add data URI prefix

**Image Processing Details:**

```
Original Image (from gallery/camera)
    ↓
Load as Bitmap
    ↓
Resize/Compress (800x800, JPEG 80%)
    ↓
Convert to ByteArray
    ↓
Encode to Base64 string
    ↓
Add prefix: "data:image/jpeg;base64,"
    ↓
Final format: "data:image/jpeg;base64,/9j/4AAQ..."
```

**Validation Rules:**

```
- Image must be selected
- Must successfully convert to Base64
- Recommended final size: < 500KB
```

---

## **Request Flow Diagram**

```
User fills form
    ↓
Select/Capture Image
    ↓
Validate all fields
    ↓
Convert image to Base64
    ↓
Build JSON request body
    ↓
Add Bearer token to header
    ↓
POST to /api/books
    ↓
Backend receives request
    ↓
Backend validates token
    ↓
Backend uploads Base64 to Cloudinary
    ↓
Backend saves book to MongoDB
    ↓
Backend returns book with Cloudinary URL
    ↓
App receives response
    ↓
Navigate to book list or show success
```

---

## **What You Need to Learn for This Feature**

### **Week 7: Image Handling**

|Topic|What to Learn|Where to Learn|Status|
|---|---|---|---|
|Permissions|Runtime permissions for camera & storage|`developer.android.com/training/permissions/requesting`|☐|
|ActivityResult API|Modern way to handle activity results|`developer.android.com/training/basics/intents/result`|☐|
|Image Picker|Pick images from gallery|`developer.android.com/training/data-storage/shared/photopicker`|☐|
|Camera Intent|Capture photos with camera|`developer.android.com/training/camera/photobasics`|☐|
|Bitmap Operations|Load, resize, compress images|`developer.android.com/topic/performance/graphics`|☐|
|Base64 Encoding|Convert image to Base64|`kotlinlang.org` (Base64 class)|☐|
|Coil Library|Display images in Compose|`coil-kt.github.io/coil/compose/`|☐|

### **Week 7: Form Handling**

|Topic|What to Learn|Where to Learn|Status|
|---|---|---|---|
|TextField State|Manage input state|`developer.android.com/jetpack/compose/state`|☐|
|Form Validation|Validate user inputs|Compose documentation|☐|
|Slider Component|Create rating slider|`developer.android.com/jetpack/compose/components`|☐|
|Button States|Enable/disable based on validation|Compose documentation|☐|
|Error Handling|Show validation errors|Compose documentation|☐|

### **Week 7: API Integration**

|Topic|What to Learn|Where to Learn|Status|
|---|---|---|---|
|Retrofit POST|Make POST requests|`square.github.io/retrofit/`|☐|
|Request Body|Build JSON request|Retrofit documentation|☐|
|Authorization Header|Add Bearer token|OkHttp interceptors|☐|
|Loading States|Show progress indicators|Compose documentation|☐|
|Response Handling|Parse successful response|Retrofit + Moshi/Gson|☐|

---

## **Common Issues & Solutions**

|Issue|Cause|Solution|
|---|---|---|
|"Please provide all fields"|Missing required field|Ensure all 4 fields are present and not empty|
|"Token is not valid"|Expired or invalid token|Implement token refresh or re-login|
|Image upload fails|Image too large|Compress image before encoding (max 800x800)|
|OutOfMemory error|Large Base64 string in memory|Reduce image quality/dimensions more aggressively|
|Network timeout|Large payload size|Compress image to < 500KB final size|
|Permission denied|Camera/storage permission not granted|Request permissions before opening picker|
|Image picker returns null|User cancelled|Handle null URI gracefully|

---

## **Testing Checklist**

### **Manual Testing**

- [ ] Create book with valid data → Should return 201
- [ ] Create book without title → Should return 400
- [ ] Create book without caption → Should return 400
- [ ] Create book without rating → Should return 400
- [ ] Create book without image → Should return 400
- [ ] Create book without token → Should return 401
- [ ] Create book with expired token → Should return 401
- [ ] Create book with large image (5MB+) → Should handle appropriately
- [ ] Create book with PNG image → Should work
- [ ] Create book with JPEG image → Should work
- [ ] Verify response contains Cloudinary URL
- [ ] Verify book appears in GET /api/books
- [ ] Verify book appears in GET /api/books/user

---

## **Backend Processing Flow**

```
POST /api/books
    ↓
Check Authorization header
    ↓
Verify JWT token
    ↓
Extract userId from token
    ↓
Validate request body (title, caption, rating, image)
    ↓
Extract Base64 image data
    ↓
Upload to Cloudinary (external service)
    ↓
Receive Cloudinary URL
    ↓
Create Book document in MongoDB
    {
      title,
      caption,
      rating,
      image: cloudinaryUrl,  // <-- Changed from Base64 to URL
      user: userId
    }
    ↓
Save to database
    ↓
Return book with Cloudinary URL (201)
```

**Note:** You send Base64, but you receive a Cloudinary URL in the response.

---

## **Key Takeaways for Android Development**

1. **Image Handling is Critical:**
    
    - You must convert selected image to Base64 format
    - Always add the data URI prefix: `data:image/jpeg;base64,`
    - Compress images to avoid memory issues
2. **Authentication Required:**
    
    - Every request needs `Authorization: Bearer {token}` header
    - Handle token expiry gracefully
    - Implement automatic token refresh
3. **All Fields Required:**
    
    - Backend validates all 4 fields
    - Implement client-side validation first
    - Show clear error messages
4. **Response Contains URL:**
    
    - Response image is a Cloudinary URL (not Base64)
    - Use this URL to display the book cover
    - Cache this URL locally for offline support
5. **Error Handling:**
    
    - Handle 400, 401, and 500 errors
    - Show user-friendly error messages
    - Implement retry logic for network failures

---

This guide provides all the API specifications you need to implement the book creation feature. Focus on learning the Kotlin/Android concepts needed for each part! 🚀