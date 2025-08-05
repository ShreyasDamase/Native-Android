**Gradle** is a **build automation tool** used to manage and automate tasks in software development — most commonly in **Android development**.

---

### 🔧 What Is Gradle?

Gradle is an open-source tool that:

- Compiles your code
    
- Downloads dependencies (libraries)
    
- Packages your app into an APK or AAB
    
- Runs tests
    
- Signs and prepares your app for release
    
- Handles multiple build types (debug/release) and product flavors
    

---

### 📦 Why Use Gradle in Android?

1. **Build automation**  
    It handles everything needed to build your Android app from source code.
    
2. **Dependency management**  
    It fetches libraries (e.g., Retrofit, Glide) from repositories like Maven Central or JCenter.
    
3. **Modular builds**  
    Supports splitting your project into modules (app, features, libraries).
    
4. **Custom builds**  
    Allows you to define different versions of your app (e.g., free vs pro, debug vs release).
    
5. **Integration with Android Studio**  
    Android Studio uses Gradle under the hood. Every time you click "Run", Gradle builds your app.
    

---

### 📁 Key Gradle Files in Android

- `build.gradle (Project level)`  
    Manages global settings like repository locations and Gradle version.
    
- `build.gradle (App/module level)`  
    Handles your app’s specific configuration: compile SDK version, dependencies, build types, etc.
    
- `gradle-wrapper.properties`  
    Locks the Gradle version used across teams for consistency.
    

---

### Example: `app/build.gradle`

```groovy
android {
    compileSdk 34

    defaultConfig {
        applicationId "com.example.myapp"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'androidx.core:core-ktx:1.12.0'
}
```

---

