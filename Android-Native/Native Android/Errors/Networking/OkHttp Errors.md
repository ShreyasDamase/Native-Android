 

 

```markdown
# 🛑 Error: INTERNET Permission Missing (OkHttp - SecurityException)

## 📌 Error Message
```

FATAL EXCEPTION: OkHttp Dispatcher  
Process: com.example.jetpackcomposecourse, PID: 15863  
java.lang.SecurityException: Permission denied (missing INTERNET permission?)  
at java.net.Inet6AddressImpl.lookupHostByName(Inet6AddressImpl.java:150)  
at java.net.Inet6AddressImpl.lookupAllHostAddr(Inet6AddressImpl.java:103)

````

## 📍 Source
- Crash occurred inside **OkHttp Dispatcher** when trying to resolve a hostname to an IP (network request).
- **Root cause**: Missing `INTERNET` permission in `AndroidManifest.xml`.

---

## ✅ Solution

1. Open your manifest:  
   `app/src/main/AndroidManifest.xml`

2. Add this line **just below `<manifest>`**, but **outside `<application>`**:

```xml
<uses-permission android:name="android.permission.INTERNET" />
````

3. Final Manifest structure:
    

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.jetpackcomposecourse">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher">
        ...
    </application>

</manifest>
```

4. Rebuild and run the project again.
    

---

## 🧠 Explanation

- `SecurityException` is thrown when your app performs an action that needs permissions it hasn't declared.
    
- In this case: **networking with OkHttp** requires internet access, so the system blocks it.
    
- Without `INTERNET` permission, **any network call** (OkHttp, Retrofit, Firebase, etc.) will crash.
    

---

## 🛡️ Prevention Tips

- Always add `INTERNET` permission before writing network logic.
    
- Double-check `AndroidManifest.xml` if any SecurityException occurs related to networking.
    
- Use log tags like `AndroidRuntime` to identify crash source.
    

---

#android #permissions #internet #okhttp #securityexception #networking #error

```

Let me know if you want this note adapted for a different structure (e.g. Jetpack Compose specific folder).
```