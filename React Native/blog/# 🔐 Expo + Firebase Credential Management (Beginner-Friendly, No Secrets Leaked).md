 Absolutely. You’re **100% right** — without those two missing parts, the story is incomplete.  
What you faced is a **very real, very common beginner trap**, and documenting it is what will make this blog truly valuable.

Below is a **fully revised, complete, end-to-end Medium blog**, including:

- ✅ your **OLD / WRONG flow** (editing `.gitignore` every time)
    
- ❌ why that flow is **dangerous and error-prone**
    
- ✅ how **EAS secrets fixed it permanently**
    
- ✅ how **Play Console deployment** fits into the picture
    
- ✅ how to upload **Play Console JSON safely**
    
- ✅ complete **DO / DON’T checklist**
    
- ✅ beginner → senior mental models
    

You can **copy-paste this entire post to Medium** as-is.

---

# 🔐 Expo + Firebase Credential Management (Beginner-Friendly, No Secrets Leaked)

> **How I went from a broken, risky Firebase setup to a clean, production-grade Expo credential system — explained so even a child can understand it.**

---

## 🚸 Who is this blog for?

This blog is for:

- 🧒 **Absolute beginners** learning Expo & Firebase
    
- 👶 **First-time mobile app developers**
    
- 🧑‍💻 Developers confused about:
    
    - `google-services.json`
        
    - `GoogleService-Info.plist`
        
    - `.gitignore`
        
    - EAS secrets
        
    - `app.config.js` vs `eas.json`
        
- 😵 Anyone who ever did this:
    
    > “Remove Firebase files from `.gitignore`, build, then re-add it and hope I don’t forget”
    

If you’ve ever felt panic during a build — this blog is for you.

---

## 🧠 First: understand the problem (like a child)

Firebase gives us **two sensitive files**:

### Android

- `google-services.json`
    

### iOS

- `GoogleService-Info.plist`
    

These files:

- Connect your app to Firebase
    
- Contain **project IDs, API keys, sender IDs**
    
- Are **private secrets**
    

👉 If you push them to GitHub:

- Anyone can steal your Firebase project
    
- You can get unexpected bills
    
- Your app can be rejected or abused
    

So the big question is:

> **How do we use Firebase without committing Firebase files?**

---

## 🧩 Expo has TWO different worlds (this is critical)

### 🌍 World 1: Your laptop

- You have files
    
- You run commands
    
- You control the filesystem
    

### ☁️ World 2: EAS Build servers

- Expo builds your app
    
- Your laptop files do NOT exist
    
- Only **uploaded secrets & env vars** are available
    

🔥 **EAS cannot magically read your local files**

This misunderstanding causes **90% of Firebase build issues**.

---

## ❌ My OLD (WRONG) approach — and why it was dangerous

Before learning EAS secrets, my flow looked like this:

### ❌ Old flow (DO NOT DO THIS)

1. Firebase files were inside the repo
    
2. They were listed in `.gitignore`
    
3. Before building, I would:
    
    - Open `.gitignore`
        
    - Comment out Firebase ignore rules
        
4. Run `eas build`
    
5. Re-add `.gitignore` rules
    
6. Pray I didn’t forget anything 😬
    

### 🚨 Why this is BAD

- ❌ Human error (easy to forget reverting `.gitignore`)
    
- ❌ Risk of accidentally committing secrets
    
- ❌ Team members may push secrets by mistake
    
- ❌ CI/CD becomes impossible
    
- ❌ Extremely stressful and fragile
    

> **This was the FIRST major issue we were facing.**

If this sounds familiar — you’re not alone.

---

## ✅ The correct mindset shift

> **Build systems should NEVER depend on manually editing `.gitignore`.**

If your deployment requires:

- commenting files in/out
    
- “just don’t forget to revert”
    
- tribal knowledge
    

👉 then the system is broken.

---

## 🗂️ My final file structure (real, production-grade)

```txt
boomm-frontend-v3/
├── app.config.js
├── eas.json
├── google-services/
│   ├── firebase/
│   │   ├── development/
│   │   │   ├── google-services.json
│   │   │   └── GoogleService-Info.plist
│   │   ├── preview/
│   │   │   ├── google-services.json
│   │   │   └── GoogleService-Info.plist
│   │   └── production/
│   │       ├── google-services.json
│   │       └── GoogleService-Info.plist
│   └── playConsole/
│       └── play-submit.json
```

Each environment has:

- Its **own Firebase project**
    
- Its **own credentials**
    
- Zero overlap
    

---

## 🚫 `.gitignore` (NEVER TOUCH THIS AGAIN)

```gitignore
# Firebase
/google-services/**

# Play Console
/google-services/playConsole/play-submit.json
```

🔥 This file should **never change per build**.

---

## 🔑 Step 1: Upload Firebase files as EAS secrets (ONE-TIME FIX)

Instead of removing `.gitignore`, we upload files **once** to Expo.

### Android

```bash
eas secret:create \
  --scope project \
  --type file \
  --name GS_ANDROID_DEV \
  --value ./google-services/firebase/development/google-services.json
```

```bash
eas secret:create \
  --scope project \
  --type file \
  --name GS_ANDROID_PREVIEW \
  --value ./google-services/firebase/preview/google-services.json
```

```bash
eas secret:create \
  --scope project \
  --type file \
  --name GS_ANDROID_PROD \
  --value ./google-services/firebase/production/google-services.json
```

---

### iOS

```bash
eas secret:create \
  --scope project \
  --type file \
  --name GS_IOS_DEV \
  --value ./google-services/firebase/development/GoogleService-Info.plist
```

```bash
eas secret:create \
  --scope project \
  --type file \
  --name GS_IOS_PREVIEW \
  --value ./google-services/firebase/preview/GoogleService-Info.plist
```

```bash
eas secret:create \
  --scope project \
  --type file \
  --name GS_IOS_PROD \
  --value ./google-services/firebase/production/GoogleService-Info.plist
```

---

### ✅ Verify once

```bash
eas secret:list
```

If you see all 6 → you’re done **forever**.

---

## 🧠 What EAS secrets ACTUALLY do (important)

When you upload a file secret:

- Expo stores it **encrypted**
    
- During build:
    
    - Writes it to a temporary file
        
    - Exposes its **path** as an env variable
        

Example:

```txt
process.env.GS_ANDROID_PROD
```

❌ NOT file content  
✅ A **temporary file path** on the build server

---

## ⚠️ Second mistake we hit: `eas.json` trap

Old tutorials say:

```json
"googleServicesFile": "$GS_ANDROID_PROD"
```

🚫 **This no longer works**  
Expo removed this support.

You will get:

```
"googleServicesFile is not allowed"
```

---

## ✅ Correct solution: `app.config.js`

Secrets → env vars → consumed here.

### Variant detection

```js
const VARIANT = process.env.APP_VARIANT || process.env.EAS_BUILD_PROFILE;

const IS_DEV = VARIANT === 'development';
const IS_PREVIEW = VARIANT === 'preview';
const IS_PROD = VARIANT === 'production';
```

---

### Android Firebase config

```js
android: {
  package: getAndroidPackage(),
  googleServicesFile:
    process.env.GS_ANDROID_DEV && IS_DEV
      ? process.env.GS_ANDROID_DEV
      : process.env.GS_ANDROID_PREVIEW && IS_PREVIEW
      ? process.env.GS_ANDROID_PREVIEW
      : process.env.GS_ANDROID_PROD,
}
```

---

### iOS Firebase config

```js
ios: {
  bundleIdentifier: getIosBundleId(),
  googleServicesFile:
    process.env.GS_IOS_DEV && IS_DEV
      ? process.env.GS_IOS_DEV
      : process.env.GS_IOS_PREVIEW && IS_PREVIEW
      ? process.env.GS_IOS_PREVIEW
      : process.env.GS_IOS_PROD,
}
```

---

## 🧱 Why this works (child-level explanation)

1. `eas build` starts
    
2. Expo injects secrets as env vars
    
3. `app.config.js` runs
    
4. Firebase file path is resolved
    
5. Gradle / Xcode reads it
    
6. Build succeeds 🎉
    

No `.gitignore` hacks.  
No stress.  
No leaks.

---

## 🚀 Build commands

### Preview

```bash
APP_VARIANT=preview eas build -p android --profile preview
```

### Production

```bash
eas build -p android --profile production
eas submit -p android --profile production
```

---

## 🟢 Play Console deployment (the OTHER missing piece)

File:

```txt
google-services/playConsole/play-submit.json
```

This is:

- ❌ NOT Firebase
    
- ✅ Google Play Console service account key
    

Used only for:

```bash
eas submit
```

---

### Upload Play Console key (optional but recommended)

```bash
eas secret:create \
  --scope project \
  --type file \
  --name PLAY_STORE_SERVICE_ACCOUNT \
  --value ./google-services/playConsole/play-submit.json
```

Then in `eas.json`:

```json
"submit": {
  "production": {
    "android": {
      "serviceAccountKeyPath": "$PLAY_STORE_SERVICE_ACCOUNT",
      "track": "internal"
    }
  }
}
```

👉 Now **even submit credentials are secure**.

---

## ✅ DOs and ❌ DON’Ts (FINAL CHECKLIST)

### ✅ DO

- Use EAS secrets for Firebase
    
- Keep `.gitignore` permanent
    
- Separate Firebase projects per env
    
- Consume secrets in `app.config.js`
    
- Upload Play Console key (optional)
    

### ❌ DON’T

- Edit `.gitignore` for builds
    
- Commit Firebase files
    
- Put Firebase paths in `eas.json`
    
- Share Firebase files on Slack/Drive
    
- Reuse prod Firebase for dev
    

---

## 🧠 Final mental model (tattoo this)

> **Never change `.gitignore` to build an app.**  
> **Build systems must be deterministic.**

---

## 🎯 Final words

This journey:

- Starts with confusion
    
- Hits scary errors
    
- Ends with a **rock-solid, senior-grade setup**
    

If you struggled with this — you’re not bad.  
Expo + Firebase credential management is **genuinely hard**.

Hopefully this blog saved you days of pain.

Happy building 🚀