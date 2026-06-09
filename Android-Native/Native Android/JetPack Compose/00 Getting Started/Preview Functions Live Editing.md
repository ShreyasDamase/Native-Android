Let me get more detailed information about Preview functions and Live Edit from the official Android documentation.

![favicon](https://claude.ai/_next/image?url=https%3A%2F%2Fwww.google.com%2Fs2%2Ffavicons%3Fdomain%3Dandroid.com%26sz%3D32&w=32&q=75)

Fetched Preview your UI with composable previews | Jetpack Compose | Android Developers

android.com

# **Preview Functions & Live Editing in Jetpack Compose: Comprehensive Guide**

Based on the latest Android documentation and development practices for 2024-2025, here's a detailed overview of Preview Functions and Live Editing in Jetpack Compose:

## **1. Preview Functions (@Preview)**

### **Basic Preview Setup**

A composable is defined by a function and annotated with @Composable. To enable a preview of this composable, create another composable, annotated with @Composable and @Preview [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/):

kotlin

```kotlin
@Composable
fun SimpleComposable() {
    Text("Hello World")
}

@Preview
@Composable
fun SimpleComposablePreview() {
    SimpleComposable()
}
```

### **Key Benefits of Preview Functions**

One of the primary benefits of using @Preview composables is to avoid reliance on the emulator in Android Studio. You can save the memory-heavy startup of the emulator for more final look-and-feel changes, and @Preview's ability to make and test small code changes with ease [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/).

Android Studio offers extended features for Jetpack Compose without having to open an emulator or connect to a device compared to Android Views, allowing a faster iterative process for developer [Java vs Kotlin: Which One Should You Learn in 2025? | by Ramesh Fadatare | JavaGuides | Medium](https://medium.com/javaguides/java-vs-kotlin-which-one-should-you-learn-in-2025-73e0d18de508).

## **2. Preview Customization Parameters**

### **Dimensions & Sizing**

kotlin

```kotlin
@Preview(widthDp = 50, heightDp = 50)
@Composable
fun SquareComposablePreview() {
    Box(Modifier.background(Color.Yellow)) {
        Text("Hello World")
    }
}
```

### **Device Configurations**

In Android Studio Flamingo, you can edit the device parameter of the Preview annotation to define configurations for your composables in different devices [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/):

kotlin

```kotlin
@Preview(device = "id:pixel_4")
@Composable
fun PixelPreview() {
    MyComposable()
}
```

### **System UI & Background**

kotlin

```kotlin
@Preview(
    showSystemUi = true,
    showBackground = true,
    backgroundColor = 0xFF00FF00
)
@Composable
fun SystemUIPreview() {
    Text("Hello World")
}
```

### **Locale & UI Mode**

kotlin

```kotlin
@Preview(
    locale = "fr-rFR",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun LocalizedDarkPreview() {
    Text(text = stringResource(R.string.greeting))
}
```

## **3. Advanced Preview Features**

### **LocalInspectionMode**

You can read from the LocalInspectionMode CompositionLocal to see if the composable is rendered in a preview. If the composition is rendered in a preview, LocalInspectionMode.current evaluates to true [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/):

kotlin

```kotlin
@Composable
fun GreetingScreen(name: String) {
    if (LocalInspectionMode.current) {
        // Show this text in a preview window:
        Text("Hello preview user!")
    } else {
        // Show this text in the app:
        Text("Hello $name!")
    }
}
```

### **Multipreview Templates**

androidx.compose.ui:ui-tooling-preview 1.6.0-alpha01+ introduces Multipreview API templates: @PreviewScreenSizes, @PreviewFontScales, @PreviewLightDark, and @PreviewDynamicColors [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/):

kotlin

```kotlin
@PreviewLightDark
@PreviewScreenSizes
@Composable
fun MultiPreviewExample() {
    MyComposable()
}
```

### **Custom Multipreview Annotations**

kotlin

```kotlin
@Preview(
    name = "small font",
    group = "font scales",
    fontScale = 0.5f
)
@Preview(
    name = "large font",
    group = "font scales",
    fontScale = 1.5f
)
annotation class FontScalePreviews

@FontScalePreviews
@Composable
fun HelloWorldPreview() {
    Text("Hello World")
}
```

### **Preview Parameter Provider**

To provide sample data, create a class that implements PreviewParameterProvider and returns the sample data as a sequence [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/):

kotlin

```kotlin
class UserPreviewParameterProvider : PreviewParameterProvider<User> {
    override val values = sequenceOf(
        User("Elise"),
        User("Frank"),
        User("Julia")
    )
}

@Preview
@Composable
fun UserProfilePreview(
    @PreviewParameter(UserPreviewParameterProvider::class) user: User
) {
    UserProfile(user)
}
```

## **4. Live Edit Feature**

### **What is Live Edit?**

Live Edit introduces a new way to edit your app's Jetpack Compose UI by instantly deploying code changes to the running application on a physical device or emulator. This means that you can make changes to your app's UI and immediately see their effect on the running application [Kotlin vs Java: A Comprehensive Comparison for Developers](https://www.imaginarycloud.com/blog/kotlin-vs-java).

Live Edit is a feature that lets you update composables in emulators and physical devices in real time [The golden age of Kotlin and its uncertain future - ShiftMag](https://shiftmag.dev/kotlin-vs-java-2392/).

### **Latest Live Edit Features (2024-2025)**

Android Studio Flamingo introduces a plethora of new features, including Jetpack Compose, Material 3 templates, and live editing capabilities for Compose UIs [Kotlin vs Java for Android Development, Which Is Better in 2024? | by Asian Digital Hub | DevSphere | Medium](https://medium.com/devsphere/kotlin-vs-java-for-android-development-which-is-better-in-2024-a545a8c29c8b).

Live Edit introduces a new way to edit your app's Jetpack Compose UI by instantly deploying code changes to the running application on a physical device or emulator [Java vs Kotlin: A Comprehensive Comparison for Developers](https://codefinity.com/blog/Java-vs-Kotlin:-A-Comprehensive-Comparison-for-Developers).

### **How Live Edit Works**

Android Studio embraces this approach with Jetpack Compose by providing tools that don't require a full build to inspect, modify values, and verify the final result [The golden age of Kotlin and its uncertain future - ShiftMag](https://shiftmag.dev/kotlin-vs-java-2392/).

## **5. Interactive Features**

### **Interactive Mode**

The interactive mode lets you interact with a preview similarly to how you would on a device running your program, like a phone or tablet. The interactive mode is isolated in a sandbox environment (meaning, isolated from other previews), where you can click elements and enter user input in the preview [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/).

### **Code Navigation**

You can hover over a preview to see the outlines of the composables contained within. Clicking on a composable outline triggers your editor view to navigate to its definition [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/).

### **Run Preview on Device**

You can run a specific @Preview on an emulator or a physical device. The preview is deployed within the same project app as a new Activity, so it shares the same context and permissions [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/).

## **6. Limitations & Best Practices**

### **Preview Limitations**

Because of the way previews are rendered within Android Studio, they are lightweight and don't require the whole Android framework to render them. However, this comes with the following limitations: No network access, No file access, Some Context APIs may not be fully available [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/).

### **ViewModel Best Practices**

Previews are limited when using ViewModel within a composable. The previews system is not capable of constructing all of the parameters passed to a ViewModel, such as repositories, use cases, managers, or similar [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/).

**Solution:**

kotlin

```kotlin
@Composable
fun AuthorScreen(viewModel: AuthorViewModel = viewModel()) {
    AuthorScreen(
        name = viewModel.authorName,
        posts = viewModel.posts
    )
}

@Preview
@Composable
fun AuthorScreenPreview(
    name: String = sampleAuthor.name,
    posts: List<Post> = samplePosts[sampleAuthor]
) {
    AuthorScreen(name = name, posts = posts)
}
```

## **7. Complete @Preview Annotation Parameters**

You can always 'ctrl or ⌘ + click' the @Preview annotation in Android Studio for a full list of parameters [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/):

kotlin

```kotlin
annotation class Preview(
    val name: String = "",
    val group: String = "",
    @IntRange(from = 1) val apiLevel: Int = -1,
    val widthDp: Int = -1,
    val heightDp: Int = -1,
    val locale: String = "",
    @FloatRange(from = 0.01) val fontScale: Float = 1f,
    val showSystemUi: Boolean = false,
    val showBackground: Boolean = false,
    val backgroundColor: Long = 0,
    @UiMode val uiMode: Int = 0,
    @Device val device: String = Devices.DEFAULT,
    @Wallpaper val wallpaper: Int = Wallpapers.NONE,
)
```

## **8. Dynamic Color Preview (Latest Feature)**

If you've enabled dynamic color in your app, use the wallpaper attribute to switch wallpapers and see how your UI reacts to different users' chosen wallpaper. This feature requires Compose 1.4.0 or higher [Jetpack Compose vs XML: Android UI development compared - Qubika](https://qubika.com/blog/jetpack-compose-vs-xml-android-u/).

## **Conclusion**

Preview Functions and Live Editing in Jetpack Compose provide a powerful development workflow that significantly reduces development time by eliminating the need for constant app rebuilds and emulator launches. For the best experience developing with Compose, download and install Android Studio. It includes many smart editor features, such as new project templates and the ability to immediately preview your Compose UI and animations [Jetpack Compose vs XML for Android UI Development](https://www.aubergine.co/insights/jetpack-compose-vs-xml-a-comprehensive-comparison-for-android-ui-development).

The combination of comprehensive preview customization, real-time Live Edit capabilities, and interactive preview modes makes Jetpack Compose development highly efficient and developer-friendly in 2025.

Retry

Claude does not have the ability to run the code it generates yet.

[Claude can make mistakes.  
Please double-check cited sources.](https://support.anthropic.com/en/articles/8525154-claude-is-providing-incorrect-or-misleading-responses-what-s-going-on)

  

Sonnet 4