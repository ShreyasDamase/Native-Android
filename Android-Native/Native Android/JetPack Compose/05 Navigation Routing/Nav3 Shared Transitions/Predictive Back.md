# Predictive Back

## 📌 Purpose
Predictive back allows users to see a visual preview of the previous screen *while* they are dragging from the edge of the device, before they commit to the back action. Nav3 has built-in support for this via the `predictivePopTransitionSpec`.

> [!NOTE]
> Predictive back animations are natively supported in Android 14+ (API 34+). On Android 13 (API 33), they must be enabled via developer options.

## 🔧 Setup & Implementation

### 1. Enable in Manifest
You must tell the OS that your app handles back events natively.

```xml
<!-- AndroidManifest.xml -->
<application
    android:enableOnBackInvokedCallback="true"
    ...>
```

### 2. Nav3 Configuration
In `NavDisplay`, you define what the screen looks like during the "preview" drag phase, and what happens when the user completes the gesture.

```kotlin
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation3.NavDisplay
import androidx.navigation3.ui.entryProvider

@Composable
fun PredictiveBackApp(backStack: List<Any>, onBack: () -> Unit) {
    NavDisplay(
        backStack = backStack,
        
        // Regular back transition (used if they tap the back arrow in UI)
        popTransitionSpec = {
            fadeIn(tween(300)) + slideInHorizontally { -it } togetherWith
            fadeOut(tween(300)) + slideOutHorizontally { it }
        },
        
        // Predictive back preview (shows DURING edge swipe gesture)
        predictivePopTransitionSpec = {
            // Keep it simple: slightly scale down the current screen and fade in the old one
            fadeIn(tween(200)) togetherWith 
            fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
        },
        
        entryProvider = entryProvider { /* ... */ },
        onBack = onBack
    )
}
```

## 🧠 How Predictive Back Works in Nav3
1. **Initiation:** The user starts swiping back from the left/right edge of the screen.
2. **Preview Phase:** The `predictivePopTransitionSpec` plays interactively, tied to the user's finger. They see a preview of the previous screen.
3. **Commit:** If the user drags far enough and releases, `onBack` is called (popping the `backStack`), and the `popTransitionSpec` plays out to complete the exit.
4. **Cancel:** If the user drags back to the edge and releases, the gesture is cancelled, and the animation smoothly reverses back to the current screen state.

## ⚠️ Predictive Back + Shared Elements

**Known Issue:** Currently, combining experimental Shared Element transitions with predictive back can cause edge-case glitches. Because the user is interactively dragging a transition that spans two screens, the interpolator can sometimes jump or clip unexpectedly if the gesture is cancelled rapidly.

**Workaround:** For screens heavily relying on `sharedBounds`, use a slightly simplified `predictivePopTransitionSpec` (like a pure fade) instead of complex spatial transforms, or rely on the default behavior until the Compose `SharedTransitionApi` becomes completely stable.

## 🧪 Testing Predictive Back

You cannot test this easily on an emulator running API 32 or lower.

**To test on device/emulator:**
1. Use an Android 13 (API 33) or Android 14+ (API 34+) device.
2. Go to **Android Settings → Developer Options**.
3. Enable **"Predictive back animations"**.
4. Run your app, navigate forward, and slowly swipe from the edge of the screen to see the preview.

Alternatively, via ADB:
```bash
adb shell settings put global enable_back_invoked_callback true
```
*(Or add `android.app.predictive_back=true` in `~/.android/adb_usb.ini`)*

## 💡 Interview Q&A

**Q: Why do we need a separate `predictivePopTransitionSpec` from `popTransitionSpec`?**
A: `popTransitionSpec` is an automated, fire-and-forget animation (e.g., clicking a back button). `predictivePopTransitionSpec` is interactively driven by the user's finger. Complex animations (like heavy horizontal sliding) often feel unnatural or broken when scrubbing back and forth with a finger. The predictive spec is usually simpler (like a subtle scale down) to feel more physically attached to the swipe gesture.
