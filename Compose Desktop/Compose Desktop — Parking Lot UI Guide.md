

> For Android developers who know Jetpack Compose and want to build a Desktop UI in plain Kotlin.

---

## Index

1. [Project Setup & Dependencies](#1-project-setup--dependencies)
    - 1.1 [What a bare Kotlin/Gradle project gives you](#11-what-a-bare-kotlingradle-project-gives-you)
    - 1.2 [Dependencies you added and why](#12-dependencies-you-added-and-why)
    - 1.3 [Before / After build.gradle.kts](#13-before--after-buildgradlekts)
2. [Entry Point — Desktop vs Android](#2-entry-point--desktop-vs-android)
3. [Navigation — Multiple Screens](#3-navigation--multiple-screens)
4. [Multiple Windows](#4-multiple-windows)
5. [UI Components](#5-ui-components)
    - 5.1 [Text](#51-text)
    - 5.2 [TextField / OutlinedTextField](#52-textfield--outlinedtextfield)
    - 5.3 [Button](#53-button)
    - 5.4 [RadioButton](#54-radiobutton)
    - 5.5 [Dropdown](#55-dropdown)
    - 5.6 [LazyColumn — Slot List](#56-lazycolumn--slot-list)
    - 5.7 [Card](#57-card)
6. [Screen Designs for Parking Lot](#6-screen-designs-for-parking-lot)
    - 6.1 [Home Screen](#61-home-screen)
    - 6.2 [Vehicle Entry Screen](#62-vehicle-entry-screen)
    - 6.3 [Vehicle Exit Screen](#63-vehicle-exit-screen)
    - 6.4 [Ticket Screen](#64-ticket-screen)
7. [Window Size & Layout](#7-window-size--layout)
8. [Keyboard Shortcuts](#8-keyboard-shortcuts)
9. [Quick Reference — Android vs Desktop](#9-quick-reference--android-vs-desktop)

---

## 1. Project Setup & Dependencies

### 1.1 What a bare Kotlin/Gradle project gives you

When you create **New Project → Kotlin → Gradle Kotlin DSL** in IntelliJ, you get a plain JVM project — enough for business logic (`ParkingLotService`, `TicketRepository`, etc.), but nothing to open a window or draw any UI.

The default `build.gradle.kts` looks like this:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
}

repositories {
    mavenCentral()
}
```

No `@Composable`. No `Window`. Nothing visual compiles yet.

---

### 1.2 Dependencies you added and why

|What you added|Why it was needed|
|---|---|
|`id("org.jetbrains.compose") version "1.11.1"`|Brings the entire Compose Multiplatform runtime. Without this, `@Composable`, `Window`, `application {}` — none of it compiles.|
|`id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"`|Since Kotlin 2.x, the Compose compiler ships as a **separate plugin**. Without it you get _"Kotlin compilation failure — composable function missing"_ errors.|
|`implementation(compose.desktop.currentOs)`|The OS-specific desktop runtime (JVM + Skia renderer). This is what actually draws the window on your screen.|
|`implementation(compose.material)`|Material Design components — `Button`, `TextField`, `RadioButton`, `Card`, `Scaffold`, etc. Without it you only have bare `Canvas` and `Layout`.|
|`google()` repository|AndroidX artifacts ship through Google Maven. Even on Desktop, some Compose internals pull in `androidx.*` packages. Without this you get _"Cannot access AndroidX internal composable function"_.|
|`maven("https://maven.pkg.jetbrains.space/…")`|JetBrains hosts Compose Multiplatform artifacts here. They are not on Maven Central.|

---

### 1.3 Before / After build.gradle.kts

**Before** — plain JVM, no UI possible:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}
```

**After** — Compose Desktop, fully wired:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"               // Compose runtime
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"  // Compose compiler (Kotlin 2.x)
}

repositories {
    mavenCentral()
    google()                                                    // AndroidX resolution
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)   // Window + OS renderer
    implementation(compose.material)            // UI components
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}
```

---

## 2. Entry Point — Desktop vs Android

You know `setContent {}` inside `ComponentActivity`. On Desktop the idea is identical — just different names.

|Android|Compose Desktop|
|---|---|
|`class MainActivity : ComponentActivity()`|`fun main()`|
|`setContent { App() }`|`application { Window(...) { App() } }`|
|`finish()`|`exitApplication()`|
|New `Activity` via `Intent`|Second `Window {}` in the same `application {}` block|

```kotlin
// Main.kt
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Parking Lot"
    ) {
        App()  // your root composable — exactly like setContent {}
    }
}
```

`application {}` is the Desktop equivalent of the Android process lifecycle. Everything composable lives inside it.

---

## 3. Navigation — Multiple Screens

Android has `NavController` and `NavHost`. Compose Desktop has no built-in navigation library — but for an app of this size you don't need one. A `sealed class` + `mutableStateOf` is all it takes.

**Define your screens:**

```kotlin
sealed class Screen {
    object Home          : Screen()
    object VehicleEntry  : Screen()
    object VehicleExit   : Screen()
    data class Ticket(val ticketId: String) : Screen()
}
```

**Root composable wires them together:**

```kotlin
@Composable
fun App() {
    var currentScreen: Screen by remember { mutableStateOf(Screen.Home) }

    when (val screen = currentScreen) {
        is Screen.Home         -> HomeScreen(onNavigate = { currentScreen = it })
        is Screen.VehicleEntry -> VehicleEntryScreen(onBack = { currentScreen = Screen.Home })
        is Screen.VehicleExit  -> VehicleExitScreen(onBack = { currentScreen = Screen.Home })
        is Screen.Ticket       -> TicketScreen(
                                      ticketId = screen.ticketId,
                                      onBack = { currentScreen = Screen.Home }
                                  )
    }
}
```

**To navigate**, pass a lambda down:

```kotlin
// In HomeScreen
Button(onClick = { onNavigate(Screen.VehicleEntry) }) {
    Text("Vehicle Entry")
}
```

> This is the same pattern as passing `onBackClick: () -> Unit` in Android Compose. No library needed.

---

## 4. Multiple Windows

Sometimes you want a second OS window — for example, a pop-up ticket preview after check-in. Control it with a boolean state inside `application {}`.

```kotlin
fun main() = application {
    var showTicketWindow by remember { mutableStateOf(false) }
    var lastTicketId     by remember { mutableStateOf("") }

    // Main window
    Window(onCloseRequest = ::exitApplication, title = "Parking Lot") {
        App(
            onTicketIssued = { id ->
                lastTicketId = id
                showTicketWindow = true
            }
        )
    }

    // Ticket popup — only shown when a ticket is issued
    if (showTicketWindow) {
        Window(
            onCloseRequest = { showTicketWindow = false },
            title = "Ticket #$lastTicketId"
        ) {
            TicketPreview(ticketId = lastTicketId)
        }
    }
}
```

When `showTicketWindow` becomes `true`, Compose opens a second native OS window. Setting it back to `false` closes it.

---

## 5. UI Components

All components below come from `compose.material`. If you have used Material 2 in Android Compose, the API is **identical**.

---

### 5.1 Text

```kotlin
Text(
    text = "Parking Lot System",
    style = MaterialTheme.typography.h4
)
```

Typography scale: `h1` → `h6`, `subtitle1`, `subtitle2`, `body1`, `body2`, `caption`, `button`, `overline`.

---

### 5.2 TextField / OutlinedTextField

```kotlin
var vehicleNumber by remember { mutableStateOf("") }

OutlinedTextField(
    value         = vehicleNumber,
    onValueChange = { vehicleNumber = it },
    label         = { Text("Vehicle Number") },
    placeholder   = { Text("e.g. MH12AB1234") },
    singleLine    = true,
    modifier      = Modifier.fillMaxWidth()
)
```

Use `OutlinedTextField` for a bordered look. Use `TextField` for a filled/underline look. Both have the same props.

---

### 5.3 Button

```kotlin
Button(
    onClick  = { /* call your service */ },
    enabled  = vehicleNumber.isNotBlank(),
    modifier = Modifier.fillMaxWidth()
) {
    Text("Check In")
}
```

For a plain text button (like a back link):

```kotlin
TextButton(onClick = onBack) {
    Text("← Back")
}
```

For an icon + text button:

```kotlin
OutlinedButton(onClick = { }) {
    Icon(Icons.Default.ExitToApp, contentDescription = null)
    Spacer(Modifier.width(4.dp))
    Text("Exit Vehicle")
}
```

---

### 5.4 RadioButton

Used on the **Vehicle Exit** screen to pick which slot to release.

```kotlin
val slots = listOf("A1", "A2", "B1", "B2")
var selectedSlot by remember { mutableStateOf(slots.first()) }

Column {
    slots.forEach { slot ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = (slot == selectedSlot),
                    onClick  = { selectedSlot = slot }
                )
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            RadioButton(
                selected = (slot == selectedSlot),
                onClick  = { selectedSlot = slot }
            )
            Spacer(Modifier.width(8.dp))
            Text(text = slot, style = MaterialTheme.typography.body1)
        }
    }
}
```

> Put `selectable()` on the `Row` so the entire row is tappable, not just the small radio circle.

---

### 5.5 Dropdown

Used to select vehicle type (Car / Bike / Truck).

```kotlin
val vehicleTypes             = listOf("Car", "Bike", "Truck")
var expanded     by remember { mutableStateOf(false) }
var selectedType by remember { mutableStateOf(vehicleTypes[0]) }

ExposedDropdownMenuBox(
    expanded        = expanded,
    onExpandedChange = { expanded = !expanded }
) {
    OutlinedTextField(
        value         = selectedType,
        onValueChange = {},
        readOnly      = true,
        label         = { Text("Vehicle Type") },
        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
    )
    ExposedDropdownMenu(
        expanded        = expanded,
        onDismissRequest = { expanded = false }
    ) {
        vehicleTypes.forEach { type ->
            DropdownMenuItem(
                text    = { Text(type) },
                onClick = { selectedType = type; expanded = false }
            )
        }
    }
}
```

---

### 5.6 LazyColumn — Slot List

```kotlin
@Composable
fun OccupiedSlotList(slots: List<ParkingSlot>) {
    LazyColumn(
        contentPadding    = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(slots) { slot ->
            SlotRow(slot)
        }
    }
}
```

Works exactly the same as Android. `items()`, `itemsIndexed()`, `item {}` — all the same.

---

### 5.7 Card

```kotlin
@Composable
fun SlotRow(slot: ParkingSlot) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Slot ${slot.id}", style = MaterialTheme.typography.subtitle1)
            Text(slot.vehicleNumber, style = MaterialTheme.typography.body2)
            Text(slot.entryTime, style = MaterialTheme.typography.caption)
        }
    }
}
```

---

## 6. Screen Designs for Parking Lot

---

### 6.1 Home Screen

```kotlin
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier              = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text("🅿 Parking Lot System", style = MaterialTheme.typography.h4)
        Spacer(Modifier.height(8.dp))
        Text("Manage entry, exit, and tickets", style = MaterialTheme.typography.subtitle1)
        Spacer(Modifier.height(40.dp))

        Button(
            onClick  = { onNavigate(Screen.VehicleEntry) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vehicle Entry")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick  = { onNavigate(Screen.VehicleExit) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vehicle Exit")
        }
    }
}
```

---

### 6.2 Vehicle Entry Screen

```kotlin
@Composable
fun VehicleEntryScreen(
    service         : ParkingLotService,
    onBack          : () -> Unit,
    onTicketIssued  : (String) -> Unit
) {
    var vehicleNumber by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isError       by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

        Text("Vehicle Entry", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(4.dp))
        Divider()
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value         = vehicleNumber,
            onValueChange = { vehicleNumber = it.uppercase() },
            label         = { Text("Vehicle Number") },
            placeholder   = { Text("e.g. MH12AB1234") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val ticket = service.checkIn(vehicleNumber)
                if (ticket != null) {
                    statusMessage = "✅ Assigned slot: ${ticket.slotId}"
                    isError = false
                    onTicketIssued(ticket.id)
                } else {
                    statusMessage = "❌ No slots available."
                    isError = true
                }
            },
            enabled  = vehicleNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check In")
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text  = statusMessage,
                color = if (isError) MaterialTheme.colors.error
                        else MaterialTheme.colors.primary
            )
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("← Back to Home") }
    }
}
```

---

### 6.3 Vehicle Exit Screen

```kotlin
@Composable
fun VehicleExitScreen(
    service : ParkingLotService,
    onBack  : () -> Unit
) {
    val occupiedSlots  = remember { mutableStateListOf<ParkingSlot>().also { it.addAll(service.getOccupiedSlots()) } }
    var selectedSlotId by remember { mutableStateOf<String?>(null) }
    var receipt        by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

        Text("Vehicle Exit", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(4.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Text("Select the slot to release:", style = MaterialTheme.typography.subtitle2)
        Spacer(Modifier.height(8.dp))

        if (occupiedSlots.isEmpty()) {
            Text("No vehicles currently parked.", style = MaterialTheme.typography.body2)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(occupiedSlots) { slot ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSlotId == slot.id,
                                onClick  = { selectedSlotId = slot.id }
                            )
                            .padding(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedSlotId == slot.id,
                            onClick  = { selectedSlotId = slot.id }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Slot ${slot.id} — ${slot.vehicleNumber}",
                                 style = MaterialTheme.typography.body1)
                            Text("Entry: ${slot.entryTime}",
                                 style = MaterialTheme.typography.caption)
                        }
                    }
                    Divider()
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                selectedSlotId?.let { id ->
                    val result = service.checkOut(id)
                    receipt = result ?: "Error during checkout."
                    occupiedSlots.removeIf { it.id == id }
                    selectedSlotId = null
                }
            },
            enabled  = selectedSlotId != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Out & Generate Receipt")
        }

        if (receipt.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), elevation = 4.dp) {
                Text(receipt, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("← Back to Home") }
    }
}
```

---

### 6.4 Ticket Screen

```kotlin
@Composable
fun TicketScreen(ticketId: String, onBack: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Parking Ticket", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Ticket ID",   style = MaterialTheme.typography.caption)
                Text(ticketId,      style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Text("Please keep this ticket for exit.", style = MaterialTheme.typography.body2)
            }
        }

        Spacer(Modifier.weight(1f))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
```

---

## 7. Window Size & Layout

### Setting a fixed window size

```kotlin
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.rememberWindowState

Window(
    onCloseRequest = ::exitApplication,
    state          = rememberWindowState(size = DpSize(900.dp, 680.dp)),
    title          = "Parking Lot",
    resizable      = false
) {
    App()
}
```

### Two-panel layout (slot list left, actions right)

```kotlin
Row(modifier = Modifier.fillMaxSize()) {

    // Left panel — slot overview
    Box(
        modifier = Modifier
            .weight(0.4f)
            .fillMaxHeight()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        OccupiedSlotList(slots)
    }

    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

    // Right panel — actions
    Box(
        modifier = Modifier
            .weight(0.6f)
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        ActionPanel()
    }
}
```

---

## 8. Keyboard Shortcuts

Desktop apps can respond to keyboard events — useful for power users.

```kotlin
import androidx.compose.ui.input.key.*

Window(
    onCloseRequest = ::exitApplication,
    onKeyEvent     = { keyEvent ->
        when {
            keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown -> {
                // navigate back
                true  // return true = event consumed
            }
            keyEvent.isCtrlPressed && keyEvent.key == Key.Enter
                && keyEvent.type == KeyEventType.KeyDown -> {
                // submit form
                true
            }
            else -> false
        }
    }
) {
    App()
}
```

---

## 9. Quick Reference — Android vs Desktop

|Android Concept|Compose Desktop Equivalent|
|---|---|
|`ComponentActivity` + `setContent {}`|`fun main() = application { Window { } }`|
|`finish()`|`exitApplication()`|
|`Intent` → new `Activity`|Change `currentScreen` state or open second `Window {}`|
|`NavController` / `NavHost`|`sealed class Screen` + `var currentScreen by remember { }`|
|`ViewModel` + `StateFlow`|`remember { mutableStateOf(...) }` or bring `lifecycle-viewmodel`|
|`LazyColumn` / `items()`|Identical — same import, same API|
|`TextField`, `Button`, `RadioButton`|Identical — from `compose.material`|
|`Modifier.clickable`|Same|
|`BackHandler`|`onKeyEvent` with `Key.Escape` in `Window`|
|`Toast.makeText`|Show a `Snackbar` inside a `ScaffoldState` or a status `Text` composable|