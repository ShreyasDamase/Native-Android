#   Postman to Code - Complete Translation Guide

## 📖 How to Read This Guide

1. Find your Postman request type below
2. See the exact Postman screenshot description
3. Follow the correct implementation
4. Avoid the common mistakes shown

---

## 1️⃣ GET Request with Query Parameters

### 📮 What You See in Postman

```
GET https://api.example.com/products

Params Tab:
┌─────────┬────────┬─────────────────┐
│ KEY     │ VALUE  │ DESCRIPTION     │
├─────────┼────────┼─────────────────┤
│ page    │ 1      │ Page number     │
│ limit   │ 10     │ Items per page  │
│ search  │ phone  │ Search keyword  │
│ status  │ active │ Filter status   │
└─────────┴────────┴─────────────────┘

Headers Tab:
┌───────────────┬──────────────────────┐
│ KEY           │ VALUE                │
├───────────────┼──────────────────────┤
│ Authorization │ Bearer abc123token   │
└───────────────┴──────────────────────┘

Final URL shown:
https://api.example.com/products?page=1&limit=10&search=phone&status=active
```

### ✅ CORRECT Implementation

**Using Fetch:**

```javascript
const params = new URLSearchParams({
  page: 1,
  limit: 10,
  search: 'phone',
  status: 'active'
});

const response = await fetch(`https://api.example.com/products?${params}`, {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer abc123token'
  }
});

const data = await response.json();
```

**Using Axios:**

```javascript
const response = await axios.get('https://api.example.com/products', {
  params: {
    page: 1,
    limit: 10,
    search: 'phone',
    status: 'active'
  },
  headers: {
    'Authorization': 'Bearer abc123token'
  }
});

const data = response.data;
```

**Using TanStack Query:**

```javascript
const { data } = useQuery({
  queryKey: ['products', { page: 1, limit: 10, search: 'phone' }],
  queryFn: async () => {
    const response = await axios.get('https://api.example.com/products', {
      params: { page: 1, limit: 10, search: 'phone', status: 'active' },
      headers: { 'Authorization': 'Bearer abc123token' }
    });
    return response.data;
  }
});
```

### ❌ COMMON MISTAKES

**Mistake #1: Putting query params in body**

```javascript
// ❌ WRONG - Query params don't go in body
fetch('https://api.example.com/products', {
  method: 'GET',
  body: JSON.stringify({ page: 1, limit: 10 })  // WRONG!
});

// ✅ CORRECT - They go in URL
fetch('https://api.example.com/products?page=1&limit=10');
```

**Mistake #2: Manual URL construction errors**

```javascript
// ❌ WRONG - Missing ? or using wrong separators
fetch('https://api.example.com/products&page=1&limit=10');  // Missing ?
fetch('https://api.example.com/products?page=1,limit=10');  // Wrong separator

// ✅ CORRECT - Use ? first, then & for subsequent params
fetch('https://api.example.com/products?page=1&limit=10');
```

**Mistake #3: Forgetting to encode special characters**

```javascript
// ❌ WRONG - Spaces and special chars break URL
const search = 'mobile phone';
fetch(`https://api.example.com/products?search=${search}`);  // Breaks!

// ✅ CORRECT - Use URLSearchParams or encodeURIComponent
const params = new URLSearchParams({ search: 'mobile phone' });
fetch(`https://api.example.com/products?${params}`);
```

---

## 2️⃣ POST Request with JSON Body

### 📮 What You See in Postman

```
POST https://api.example.com/users

Headers Tab:
┌───────────────┬────────────────────────┐
│ KEY           │ VALUE                  │
├───────────────┼────────────────────────┤
│ Content-Type  │ application/json       │
│ Authorization │ Bearer xyz789token     │
└───────────────┴────────────────────────┘

Body Tab: (Select "raw" + "JSON")
{
  "name": "John Doe",
  "email": "john@example.com",
  "age": 30,
  "address": {
    "city": "Mumbai",
    "state": "Maharashtra"
  }
}
```

### ✅ CORRECT Implementation

**Using Fetch:**

```javascript
const response = await fetch('https://api.example.com/users', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer xyz789token'
  },
  body: JSON.stringify({
    name: 'John Doe',
    email: 'john@example.com',
    age: 30,
    address: {
      city: 'Mumbai',
      state: 'Maharashtra'
    }
  })
});

const data = await response.json();
```

**Using Axios:**

```javascript
const response = await axios.post('https://api.example.com/users', 
  {
    name: 'John Doe',
    email: 'john@example.com',
    age: 30,
    address: {
      city: 'Mumbai',
      state: 'Maharashtra'
    }
  },
  {
    headers: {
      'Authorization': 'Bearer xyz789token'
      // Content-Type: application/json is automatic in Axios
    }
  }
);

const data = response.data;
```

**Using TanStack Query:**

```javascript
const mutation = useMutation({
  mutationFn: async (userData) => {
    const response = await axios.post('https://api.example.com/users', userData, {
      headers: { 'Authorization': 'Bearer xyz789token' }
    });
    return response.data;
  }
});

// Use it
mutation.mutate({
  name: 'John Doe',
  email: 'john@example.com',
  age: 30,
  address: { city: 'Mumbai', state: 'Maharashtra' }
});
```

### ❌ COMMON MISTAKES

**Mistake #1: Forgetting JSON.stringify with Fetch**

```javascript
// ❌ WRONG - Fetch needs JSON.stringify
fetch('https://api.example.com/users', {
  method: 'POST',
  body: { name: 'John' }  // WRONG! This sends "[object Object]"
});

// ✅ CORRECT
fetch('https://api.example.com/users', {
  method: 'POST',
  body: JSON.stringify({ name: 'John' })
});
```

**Mistake #2: Missing Content-Type header**

```javascript
// ❌ WRONG - Missing Content-Type
fetch('https://api.example.com/users', {
  method: 'POST',
  body: JSON.stringify({ name: 'John' })  // Server won't understand
});

// ✅ CORRECT
fetch('https://api.example.com/users', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ name: 'John' })
});
```

**Mistake #3: Wrong Axios syntax - data in wrong place**

```javascript
// ❌ WRONG - Data should be second parameter
axios.post('https://api.example.com/users', {
  headers: { 'Authorization': 'Bearer token' },
  data: { name: 'John' }  // WRONG position!
});

// ✅ CORRECT - Data is second param, config is third
axios.post('https://api.example.com/users', 
  { name: 'John' },  // Data here (2nd param)
  { headers: { 'Authorization': 'Bearer token' } }  // Config here (3rd param)
);
```

---

## 3️⃣ POST Request with Form Data (File Upload)

### 📮 What You See in Postman

```
POST https://api.example.com/upload

Headers Tab:
┌───────────────┬──────────────────────┐
│ KEY           │ VALUE                │
├───────────────┼──────────────────────┤
│ Authorization │ Bearer abc123token   │
└───────────────┴──────────────────────┘
⚠️ DON'T manually add Content-Type for form-data!

Body Tab: (Select "form-data")
┌───────────┬──────────────┬──────────────────┐
│ KEY       │ VALUE        │ TYPE             │
├───────────┼──────────────┼──────────────────┤
│ file      │ [File] photo.jpg │ File        │
│ title     │ My Photo     │ Text             │
│ userId    │ 123          │ Text             │
│ tags      │ nature       │ Text             │
│ tags      │ sunset       │ Text             │
└───────────┴──────────────┴──────────────────┘
```

### ✅ CORRECT Implementation

**Using Fetch:**

```javascript
const formData = new FormData();

// Add file
formData.append('file', {
  uri: 'file:///path/to/photo.jpg',
  type: 'image/jpeg',
  name: 'photo.jpg'
});

// Add text fields
formData.append('title', 'My Photo');
formData.append('userId', '123');

// Add multiple values with same key
formData.append('tags', 'nature');
formData.append('tags', 'sunset');

const response = await fetch('https://api.example.com/upload', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer abc123token'
    // DO NOT add Content-Type here!
  },
  body: formData
});

const data = await response.json();
```

**Using Axios:**

```javascript
const formData = new FormData();
formData.append('file', {
  uri: 'file:///path/to/photo.jpg',
  type: 'image/jpeg',
  name: 'photo.jpg'
});
formData.append('title', 'My Photo');
formData.append('userId', '123');
formData.append('tags', 'nature');
formData.append('tags', 'sunset');

const response = await axios.post('https://api.example.com/upload', formData, {
  headers: {
    'Content-Type': 'multipart/form-data',
    'Authorization': 'Bearer abc123token'
  }
});

const data = response.data;
```

**Using TanStack Query:**

```javascript
const uploadMutation = useMutation({
  mutationFn: async (fileData) => {
    const formData = new FormData();
    formData.append('file', fileData.file);
    formData.append('title', fileData.title);
    formData.append('userId', fileData.userId);

    const response = await axios.post('https://api.example.com/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': 'Bearer abc123token'
      }
    });
    return response.data;
  }
});

// Use it
uploadMutation.mutate({
  file: { uri: 'file://...', type: 'image/jpeg', name: 'photo.jpg' },
  title: 'My Photo',
  userId: '123'
});
```

### ❌ COMMON MISTAKES

**Mistake #1: Setting wrong Content-Type for Fetch**

```javascript
// ❌ WRONG - DON'T set Content-Type manually with Fetch
fetch('https://api.example.com/upload', {
  method: 'POST',
  headers: {
    'Content-Type': 'multipart/form-data'  // WRONG! Missing boundary
  },
  body: formData
});

// ✅ CORRECT - Let browser set it automatically
fetch('https://api.example.com/upload', {
  method: 'POST',
  // No Content-Type header!
  body: formData
});
```

**Mistake #2: Trying to JSON.stringify FormData**

```javascript
// ❌ WRONG - FormData is not JSON
const formData = new FormData();
formData.append('file', file);

fetch('https://api.example.com/upload', {
  method: 'POST',
  body: JSON.stringify(formData)  // WRONG! This breaks everything
});

// ✅ CORRECT - Send FormData directly
fetch('https://api.example.com/upload', {
  method: 'POST',
  body: formData  // Send as-is
});
```

**Mistake #3: Wrong file object structure**

```javascript
// ❌ WRONG - Just passing file path as string
formData.append('file', '/path/to/file.jpg');  // WRONG!

// ✅ CORRECT - Proper file object structure
formData.append('file', {
  uri: 'file:///path/to/file.jpg',
  type: 'image/jpeg',
  name: 'file.jpg'
});
```

**Mistake #4: Appending object instead of values**

```javascript
// ❌ WRONG - Can't append objects directly
formData.append('user', { name: 'John', age: 30 });  // Becomes "[object Object]"

// ✅ CORRECT - Append individual fields or stringify
formData.append('userName', 'John');
formData.append('userAge', '30');
// OR
formData.append('user', JSON.stringify({ name: 'John', age: 30 }));
```

---

## 4️⃣ PUT Request (Update)

### 📮 What You See in Postman

```
PUT https://api.example.com/users/123
         ↑ Note the ID in URL path

Headers Tab:
┌───────────────┬────────────────────────┐
│ KEY           │ VALUE                  │
├───────────────┼────────────────────────┤
│ Content-Type  │ application/json       │
│ Authorization │ Bearer token123        │
└───────────────┴────────────────────────┘

Body Tab: (raw + JSON)
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "age": 25
}
```

### ✅ CORRECT Implementation

**Using Fetch:**

```javascript
const userId = 123;

const response = await fetch(`https://api.example.com/users/${userId}`, {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer token123'
  },
  body: JSON.stringify({
    name: 'Jane Doe',
    email: 'jane@example.com',
    age: 25
  })
});

const data = await response.json();
```

**Using Axios:**

```javascript
const userId = 123;

const response = await axios.put(`https://api.example.com/users/${userId}`,
  {
    name: 'Jane Doe',
    email: 'jane@example.com',
    age: 25
  },
  {
    headers: {
      'Authorization': 'Bearer token123'
    }
  }
);

const data = response.data;
```

**Using TanStack Query:**

```javascript
const updateMutation = useMutation({
  mutationFn: async ({ id, userData }) => {
    const response = await axios.put(`https://api.example.com/users/${id}`, userData, {
      headers: { 'Authorization': 'Bearer token123' }
    });
    return response.data;
  }
});

// Use it
updateMutation.mutate({
  id: 123,
  userData: { name: 'Jane Doe', email: 'jane@example.com', age: 25 }
});
```

### ❌ COMMON MISTAKES

**Mistake #1: Putting ID in body instead of URL**

```javascript
// ❌ WRONG - ID should be in URL, not body
axios.put('https://api.example.com/users', {
  id: 123,  // WRONG place!
  name: 'Jane Doe'
});

// ✅ CORRECT - ID in URL path
axios.put('https://api.example.com/users/123', {
  name: 'Jane Doe'
});
```

**Mistake #2: Using POST instead of PUT**

```javascript
// ❌ WRONG - POST is for creating, not updating
fetch('https://api.example.com/users/123', {
  method: 'POST',  // Should be PUT or PATCH
  body: JSON.stringify({ name: 'Jane' })
});

// ✅ CORRECT
fetch('https://api.example.com/users/123', {
  method: 'PUT',  // For full update
  body: JSON.stringify({ name: 'Jane', email: 'jane@example.com' })
});
```

---

## 5️⃣ PATCH Request (Partial Update)

### 📮 What You See in Postman

```
PATCH https://api.example.com/users/123
       ↑ PATCH means partial update

Body Tab: (raw + JSON)
{
  "email": "newemail@example.com"
}
⚠️ Only the field you want to update, not all fields
```

### ✅ CORRECT Implementation

**Using Fetch:**

```javascript
const response = await fetch('https://api.example.com/users/123', {
  method: 'PATCH',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer token123'
  },
  body: JSON.stringify({
    email: 'newemail@example.com'  // Only what you're updating
  })
});
```

**Using Axios:**

```javascript
const response = await axios.patch('https://api.example.com/users/123',
  {
    email: 'newemail@example.com'
  },
  {
    headers: { 'Authorization': 'Bearer token123' }
  }
);
```

### 💡 PUT vs PATCH

```
PUT = Full replacement (send all fields)
{
  "name": "Jane",
  "email": "jane@example.com",
  "age": 25,
  "phone": "1234567890"
}

PATCH = Partial update (send only changed fields)
{
  "email": "newemail@example.com"
}
```

---

## 6️⃣ DELETE Request

### 📮 What You See in Postman

```
DELETE https://api.example.com/users/123
                                    ↑ ID in URL

Headers Tab:
┌───────────────┬────────────────────┐
│ KEY           │ VALUE              │
├───────────────┼────────────────────┤
│ Authorization │ Bearer token123    │
└───────────────┴────────────────────┘

Body Tab: (Usually empty for DELETE)
```

### ✅ CORRECT Implementation

**Using Fetch:**

```javascript
const response = await fetch('https://api.example.com/users/123', {
  method: 'DELETE',
  headers: {
    'Authorization': 'Bearer token123'
  }
});

// DELETE often returns 204 No Content, so no .json() needed
if (response.ok) {
  console.log('Deleted successfully');
}
```

**Using Axios:**

```javascript
const response = await axios.delete('https://api.example.com/users/123', {
  headers: {
    'Authorization': 'Bearer token123'
  }
});

console.log('Deleted successfully');
```

**Using TanStack Query:**

```javascript
const deleteMutation = useMutation({
  mutationFn: async (userId) => {
    await axios.delete(`https://api.example.com/users/${userId}`, {
      headers: { 'Authorization': 'Bearer token123' }
    });
  },
  onSuccess: () => {
    // Refresh the list
    queryClient.invalidateQueries({ queryKey: ['users'] });
  }
});

// Use it
deleteMutation.mutate(123);
```

### ❌ COMMON MISTAKES

**Mistake #1: Trying to add body to DELETE**

```javascript
// ❌ WRONG - DELETE usually doesn't have body
fetch('https://api.example.com/users', {
  method: 'DELETE',
  body: JSON.stringify({ id: 123 })  // Unusual
});

// ✅ CORRECT - ID in URL
fetch('https://api.example.com/users/123', {
  method: 'DELETE'
});
```

**Mistake #2: Wrong Axios DELETE syntax with body**

```javascript
// ❌ WRONG - Body in wrong place
axios.delete('https://api.example.com/users/123', {
  id: 123  // This is config, not body
});

// ✅ CORRECT - If you really need body in DELETE
axios.delete('https://api.example.com/users/123', {
  data: { reason: 'spam' },  // Use 'data' key for body
  headers: { 'Authorization': 'Bearer token' }
});
```

---

## 7️⃣ Complex Postman - Query Params + Body + Headers

### 📮 What You See in Postman

```
POST https://api.example.com/posts

Params Tab:
┌──────────┬────────┐
│ KEY      │ VALUE  │
├──────────┼────────┤
│ publish  │ true   │
│ notify   │ false  │
└──────────┴────────┘

Headers Tab:
┌───────────────┬─────────────────────┐
│ KEY           │ VALUE               │
├───────────────┼─────────────────────┤
│ Content-Type  │ application/json    │
│ Authorization │ Bearer token123     │
│ X-User-Role   │ admin               │
└───────────────┴─────────────────────┘

Body Tab: (raw + JSON)
{
  "title": "New Post",
  "content": "Post content here",
  "tags": ["tech", "tutorial"]
}

Final URL:
https://api.example.com/posts?publish=true&notify=false
```

### ✅ CORRECT Implementation

**Using Fetch:**

```javascript
const queryParams = new URLSearchParams({
  publish: 'true',
  notify: 'false'
});

const response = await fetch(`https://api.example.com/posts?${queryParams}`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer token123',
    'X-User-Role': 'admin'
  },
  body: JSON.stringify({
    title: 'New Post',
    content: 'Post content here',
    tags: ['tech', 'tutorial']
  })
});

const data = await response.json();
```

**Using Axios:**

```javascript
const response = await axios.post('https://api.example.com/posts',
  {
    title: 'New Post',
    content: 'Post content here',
    tags: ['tech', 'tutorial']
  },
  {
    params: {
      publish: true,
      notify: false
    },
    headers: {
      'Authorization': 'Bearer token123',
      'X-User-Role': 'admin'
    }
  }
);

const data = response.data;
```

### 📝 Remember:

- **Query params** → URL (for filters, flags, options)
- **Body** → Main data being sent
- **Headers** → Authentication, content type, custom headers

---

## 8️⃣ Understanding Postman Variables

### 📮 What You See in Postman

```
POST {{baseUrl}}/users
         ↑ Variable

Headers:
Authorization: Bearer {{token}}
                       ↑ Variable
```

### 🔄 Translation

```javascript
// Replace variables with actual values
const baseUrl = 'https://api.example.com';
const token = 'your-actual-token-here';

const response = await axios.post(`${baseUrl}/users`,
  { name: 'John' },
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

---

## 🎯 Quick Checklist: Postman → Code

When you see a Postman request, ask:

### ✅ Step 1: What's the METHOD?

- `GET` → Fetch data
- `POST` → Create new
- `PUT` → Full update
- `PATCH` → Partial update
- `DELETE` → Remove

### ✅ Step 2: Check PARAMS tab

- If has params → Add to URL with `?` and `&`
- In Axios: use `params` config option

### ✅ Step 3: Check HEADERS tab

- Copy all headers to `headers` config
- Don't add `Content-Type` for FormData in Fetch
- Replace `{{variables}}` with actual values

### ✅ Step 4: Check BODY tab

- **If "none"** → No body needed (common in GET, DELETE)
- **If "raw" + "JSON"** → Use `JSON.stringify()` in Fetch, object in Axios
- **If "form-data"** → Use FormData, append each field
- **If "x-www-form-urlencoded"** → Use URLSearchParams

### ✅ Step 5: Check URL path

- IDs usually in URL: `/users/123`
- Not in body or params

---

## 🚨 Top 10 Mistakes - Quick Reference

|❌ Mistake|✅ Correct|
|---|---|
|Query params in POST body|Query params in URL with `?`|
|Forgetting `JSON.stringify` with Fetch|Always stringify objects for Fetch|
|Setting `Content-Type` for FormData in Fetch|Let browser set it automatically|
|Missing `await` on `.json()`|`await response.json()`|
|ID in body instead of URL|`/users/${id}`|
|Using `response.data.data` with Fetch|Just `response.json()`|
|Appending objects to FormData|Append individual values or stringify|
|Wrong parameter order in Axios|`axios.post(url, data, config)`|
|Using POST for updates|Use PUT/PATCH for updates|
|Mixing Fetch and Axios response syntax|Fetch: `.json()`, Axios: `.data`|

---

## 💡 Pro Tips

### 1. Read Postman URL Bar First

```
The final URL shown in Postman tells you everything:
https://api.example.com/users/123?status=active&page=1
                           ↑         ↑
                      Path param   Query params
```

### 2. Headers Order Doesn't Matter

```javascript
// These are the same
headers: {
  'Authorization': 'Bearer token',
  'Content-Type': 'application/json'
}

headers: {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer token'
}
```

### 3. Test in Postman First

Before coding, make sure the request works in Postman. Then translate it exactly.

### 4. Console.log Everything

```javascript
console.log('Request:', url, method, body);
console.log('Response:', response);
console.log('Data:', data);
```

### 5. Common Header Names

```javascript
'Content-Type'        // What you're sending
'Authorization'       // Auth token
'Accept'             // What you want back
'X-API-Key'          // API key
'X-Custom-Header'    // Custom headers usually start with X-
```

---

## 📚 Practice Exercise

### Here's a Postman Request - Try Converting It!

```
PUT https://api.shop.com/products/789

Params:
- notify_users: true

Headers:
- Authorization: Bearer shop_token_xyz
- Content-Type: application/json

Body (JSON):
{
  "name": "Updated Product",
  "price": 1299.99,
  "stock": 50
}
```

<details> <summary>Click to see the answer</summary>

```javascript
// Using Axios (recommended)
const response = await axios.put('https://api.shop.com/products/789',
  {
    name: 'Updated Product',
    price: 1299.99,
    stock: 50
  },
  {
    params: {
      notify_users: true
    },
    headers: {
      'Authorization': 'Bearer shop_token_xyz'
    }
  }
);

// Using Fetch
const queryParams = new URLSearchParams({ notify_users: 'true' });
const response = await fetch(`https://api.shop.com/products/789?${queryParams}`, {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer shop_token_xyz'
  },
  body: JSON.stringify({
    name: 'Updated Product',
    price: 1299.99,
    stock: 50
  })
});
const data = await response.json();
```

</details>

---

## 🎓 Learning Path

**Day 1-2**: Practice GET requests with query parameters  
**Day 3-4**: Practice POST with JSON body  
**Day 5-6**: Practice FormData file uploads  
**Day 7**: Practice PUT, PATCH, DELETE  
**Week 2**: Mix everything together

After 2 weeks of referring to this guide (not copy-pasting!), you'll remember it naturally! 🚀