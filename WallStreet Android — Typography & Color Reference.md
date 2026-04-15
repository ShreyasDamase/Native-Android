# WallStreet Android — Typography & Color Reference

> `MaterialTheme.typography.<token>` · `MaterialTheme.colorScheme.<token>`

---

## Typography Scale (Material 3 — 15 Styles)

|Token|Size (sp)|Weight|Line Height|Use Case|
|---|---|---|---|---|
|`displayLarge`|57|Regular (400)|64|Hero banners, splash screens|
|`displayMedium`|45|Regular (400)|52|Section heroes|
|`displaySmall`|36|Regular (400)|44|Large decorative text|
|`headlineLarge`|32|Regular (400)|40|Page titles, important numerals|
|`headlineMedium`|28|Regular (400)|36|Section headers|
|`headlineSmall`|24|Regular (400)|32|Card headers, sub-sections|
|`titleLarge`|22|Regular (400)|28|AppBar titles, nav headers|
|`titleMedium`|16|Medium (500)|24|Dialog titles, list headers|
|`titleSmall`|14|Medium (500)|20|Subtitles, tab labels|
|`bodyLarge`|16|Regular (400)|24|Primary reading text, paragraphs|
|`bodyMedium`|14|Regular (400)|20|Secondary body copy|
|`bodySmall`|12|Regular (400)|16|Captions, helper text|
|`labelLarge`|14|Medium (500)|20|Buttons, tabs, CTAs|
|`labelMedium`|12|Medium (500)|16|Badges, image annotations|
|`labelSmall`|11|Medium (500)|16|Overlines, micro-labels|

### Quick Usage

```kotlin
Text(text = "Price",    style = MaterialTheme.typography.titleMedium)
Text(text = "Buy",      style = MaterialTheme.typography.labelLarge)
Text(text = "Overview", style = MaterialTheme.typography.bodyMedium)
```

---

## Color Palette — All Tokens

### Primary Brand

|Name|Token|Hex|Preview|
|---|---|---|---|
|Primary Blue|`PrimaryBlue`|`#3B82F6`|![#3B82F6](https://placehold.co/60x20/3B82F6/3B82F6.png)|
|Primary Blue Dark|`PrimaryBlueDark`|`#2563EB`|![#2563EB](https://placehold.co/60x20/2563EB/2563EB.png)|
|Primary Blue Light|`PrimaryBlueLight`|`#60A5FA`|![#60A5FA](https://placehold.co/60x20/60A5FA/60A5FA.png)|

### Status Colors

|Name|Token|Hex|Preview|
|---|---|---|---|
|Success Green|`SuccessGreen`|`#10B981`|![#10B981](https://placehold.co/60x20/10B981/10B981.png)|
|Success Green Light|`SuccessGreenLight`|`#34D399`|![#34D399](https://placehold.co/60x20/34D399/34D399.png)|
|Success Green Dark|`SuccessGreenDark`|`#059669`|![#059669](https://placehold.co/60x20/059669/059669.png)|
|Danger Red|`DangerRed`|`#EF4444`|![#EF4444](https://placehold.co/60x20/EF4444/EF4444.png)|
|Danger Red Light|`DangerRedLight`|`#F87171`|![#F87171](https://placehold.co/60x20/F87171/F87171.png)|
|Danger Red Dark|`DangerRedDark`|`#DC2626`|![#DC2626](https://placehold.co/60x20/DC2626/DC2626.png)|
|Warning Orange|`WarningOrange`|`#F59E0B`|![#F59E0B](https://placehold.co/60x20/F59E0B/F59E0B.png)|
|Warning Yellow|`WarningYellow`|`#FBBF24`|![#FBBF24](https://placehold.co/60x20/FBBF24/FBBF24.png)|

### Dark Mode Surfaces

|Name|Token|Hex|Preview|
|---|---|---|---|
|Dark Background|`DarkBackground`|`#0E1116`|![#0E1116](https://placehold.co/60x20/0E1116/0E1116.png)|
|Dark Surface|`DarkSurface`|`#161B22`|![#161B22](https://placehold.co/60x20/161B22/161B22.png)|
|Dark Surface Variant|`DarkSurfaceVariant`|`#1F2630`|![#1F2630](https://placehold.co/60x20/1F2630/1F2630.png)|
|Dark Text Primary|`DarkTextPrimary`|`#FFFFFF`|![#FFFFFF](https://placehold.co/60x20/FFFFFF/FFFFFF.png)|
|Dark Text Secondary|`DarkTextSecondary`|`#B1BAC4`|![#B1BAC4](https://placehold.co/60x20/B1BAC4/B1BAC4.png)|
|Dark Text Tertiary|`DarkTextTertiary`|`#8B949E`|![#8B949E](https://placehold.co/60x20/8B949E/8B949E.png)|

### Light Mode Surfaces

|Name|Token|Hex|Preview|
|---|---|---|---|
|Light Background|`LightBackground`|`#F5F5F5`|![#F5F5F5](https://placehold.co/60x20/F5F5F5/F5F5F5.png)|
|Light Surface|`LightSurface`|`#FFFFFF`|![#FFFFFF](https://placehold.co/60x20/FFFFFF/FFFFFF.png)|
|Light Surface Variant|`LightSurfaceVariant`|`#F8F9FA`|![#F8F9FA](https://placehold.co/60x20/F8F9FA/F8F9FA.png)|
|Light Text Primary|`LightTextPrimary`|`#1F2937`|![#1F2937](https://placehold.co/60x20/1F2937/1F2937.png)|
|Light Text Secondary|`LightTextSecondary`|`#6B7280`|![#6B7280](https://placehold.co/60x20/6B7280/6B7280.png)|
|Light Text Tertiary|`LightTextTertiary`|`#9CA3AF`|![#9CA3AF](https://placehold.co/60x20/9CA3AF/9CA3AF.png)|

### Borders & Dividers

|Name|Token|Hex|Preview|
|---|---|---|---|
|Border Primary|`BorderPrimary`|`#E5E7EB`|![#E5E7EB](https://placehold.co/60x20/E5E7EB/E5E7EB.png)|
|Border Secondary|`BorderSecondary`|`#F3F4F6`|![#F3F4F6](https://placehold.co/60x20/F3F4F6/F3F4F6.png)|
|Divider|`DividerColor`|`#E5E7EB`|![#E5E7EB](https://placehold.co/60x20/E5E7EB/E5E7EB.png)|

### Badges

|Name|Token|Hex|Preview|
|---|---|---|---|
|Long Bg|`BadgeLongBg`|`#065F46`|![#065F46](https://placehold.co/60x20/065F46/065F46.png)|
|Long Text|`BadgeLongText`|`#10B981`|![#10B981](https://placehold.co/60x20/10B981/10B981.png)|
|Short Bg|`BadgeShortBg`|`#7F1D1D`|![#7F1D1D](https://placehold.co/60x20/7F1D1D/7F1D1D.png)|
|Short Text|`BadgeShortText`|`#EF4444`|![#EF4444](https://placehold.co/60x20/EF4444/EF4444.png)|
|Stock Bg|`BadgeStockBg`|`#1E3A8A`|![#1E3A8A](https://placehold.co/60x20/1E3A8A/1E3A8A.png)|
|Stock Text|`BadgeStockText`|`#60A5FA`|![#60A5FA](https://placehold.co/60x20/60A5FA/60A5FA.png)|
|Crypto Bg|`BadgeCryptoBg`|`#581C87`|![#581C87](https://placehold.co/60x20/581C87/581C87.png)|
|Crypto Text|`BadgeCryptoText`|`#A78BFA`|![#A78BFA](https://placehold.co/60x20/A78BFA/A78BFA.png)|

### Input Fields

|Name|Token|Hex|Preview|
|---|---|---|---|
|Input Bg|`InputFieldBg`|`#FFFFFF`|![#FFFFFF](https://placehold.co/60x20/FFFFFF/FFFFFF.png)|
|Input Border|`InputFieldBorder`|`#E5E7EB`|![#E5E7EB](https://placehold.co/60x20/E5E7EB/E5E7EB.png)|
|Input Border Focused|`InputFieldBorderFocused`|`#3B82F6`|![#3B82F6](https://placehold.co/60x20/3B82F6/3B82F6.png)|

### Sidebar (Dark Mode)

|Name|Token|Hex|Preview|
|---|---|---|---|
|Sidebar Background|`SidebarBackground`|`#0B0F19`|![#0B0F19](https://placehold.co/60x20/0B0F19/0B0F19.png)|
|Sidebar Text Primary|`SidebarTextPrimary`|`#FFFFFF`|![#FFFFFF](https://placehold.co/60x20/FFFFFF/FFFFFF.png)|
|Sidebar Text Secondary|`SidebarTextSecondary`|`#9CA3AF`|![#9CA3AF](https://placehold.co/60x20/9CA3AF/9CA3AF.png)|
|Sidebar Icon Active|`SidebarIconActive`|`#3B82F6`|![#3B82F6](https://placehold.co/60x20/3B82F6/3B82F6.png)|
|Sidebar Icon Inactive|`SidebarIconInactive`|`#6B7280`|![#6B7280](https://placehold.co/60x20/6B7280/6B7280.png)|

### Login Screen

|Name|Token|Hex|Preview|
|---|---|---|---|
|Login Background|`LoginBackground`|`#0A1628`|![#0A1628](https://placehold.co/60x20/0A1628/0A1628.png)|
|Login Surface|`LoginSurface`|`#142038`|![#142038](https://placehold.co/60x20/142038/142038.png)|
|Login Text|`LoginText`|`#F9FAFB`|![#F9FAFB](https://placehold.co/60x20/F9FAFB/F9FAFB.png)|

### Common

|Name|Token|Hex|Preview|
|---|---|---|---|
|White|`White`|`#FFFFFF`|![#FFFFFF](https://placehold.co/60x20/FFFFFF/FFFFFF.png)|
|Black|`Black`|`#000000`|![#000000](https://placehold.co/60x20/000000/000000.png)|
|Transparent|`Transparent`|`#00000000`|_(transparent)_|

---

## MaterialTheme Color Scheme Mapping

### Dark Theme

| Role                 | Token                | Resolved Color                                           | Hex       |
| -------------------- | -------------------- | -------------------------------------------------------- | --------- |
| `primary`            | `PrimaryBlue`        | ![#3B82F6](https://placehold.co/40x16/3B82F6/3B82F6.png) | `#3B82F6` |
| `onPrimary`          | `White`              | ![#FFFFFF](https://placehold.co/40x16/FFFFFF/FFFFFF.png) | `#FFFFFF` |
| `primaryContainer`   | `PrimaryBlueDark`    | ![#2563EB](https://placehold.co/40x16/2563EB/2563EB.png) | `#2563EB` |
| `onPrimaryContainer` | `PrimaryBlueLight`   | ![#60A5FA](https://placehold.co/40x16/60A5FA/60A5FA.png) | `#60A5FA` |
| `background`         | `DarkSurface`        | ![#161B22](https://placehold.co/40x16/161B22/161B22.png) | `#161B22` |
| `onBackground`       | `DarkTextPrimary`    | ![#FFFFFF](https://placehold.co/40x16/FFFFFF/FFFFFF.png) | `#FFFFFF` |
| `surface`            | `DarkBackground`     | ![#0E1116](https://placehold.co/40x16/0E1116/0E1116.png) | `#0E1116` |
| `onSurface`          | `DarkTextPrimary`    | ![#FFFFFF](https://placehold.co/40x16/FFFFFF/FFFFFF.png) | `#FFFFFF` |
| `surfaceVariant`     | `DarkSurfaceVariant` | ![#1F2630](https://placehold.co/40x16/1F2630/1F2630.png) | `#1F2630` |
| `onSurfaceVariant`   | `DarkTextSecondary`  | ![#B1BAC4](https://placehold.co/40x16/B1BAC4/B1BAC4.png) | `#B1BAC4` |
| `error`              | `DangerRed`          | ![#EF4444](https://placehold.co/40x16/EF4444/EF4444.png) | `#EF4444` |
| `outline`            | `BorderPrimary`      | ![#E5E7EB](https://placehold.co/40x16/E5E7EB/E5E7EB.png) | `#E5E7EB` |

### Light Theme

| Role               | Token                 | Resolved Color                                           | Hex       |
| ------------------ | --------------------- | -------------------------------------------------------- | --------- |
| `background`       | `LightBackground`     | ![#F5F5F5](https://placehold.co/40x16/F5F5F5/F5F5F5.png) | `#F5F5F5` |
| `onBackground`     | `LightTextPrimary`    | ![#1F2937](https://placehold.co/40x16/1F2937/1F2937.png) | `#1F2937` |
| `surface`          | `LightSurface`        | ![#FFFFFF](https://placehold.co/40x16/FFFFFF/FFFFFF.png) | `#FFFFFF` |
| `onSurface`        | `LightTextPrimary`    | ![#1F2937](https://placehold.co/40x16/1F2937/1F2937.png) | `#1F2937` |
| `surfaceVariant`   | `LightSurfaceVariant` | ![#F8F9FA](https://placehold.co/40x16/F8F9FA/F8F9FA.png) | `#F8F9FA` |
| `onSurfaceVariant` | `LightTextSecondary`  | ![#6B7280](https://placehold.co/40x16/6B7280/6B7280.png) | `#6B7280` |

---

## Quick Usage Snippets

```kotlin
// Text with typography + color
Text(
    text = "+2.45%",
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary
)

// Error state
Text(
    text = "Trade failed",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.error
)

// Card surface
Card(colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface
)) { ... }

// Button
Button(colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary
)) {
    Text("Buy", style = MaterialTheme.typography.labelLarge)
}

// Badge (Long position)
Box(modifier = Modifier.background(BadgeLongBg, RoundedCornerShape(4.dp))) {
    Text("LONG", color = BadgeLongText, style = MaterialTheme.typography.labelSmall)
}
```


| Token                | Light                                             | Dark                                              |
| -------------------- | ------------------------------------------------- | ------------------------------------------------- |
| `primary`            | ![](https://placehold.co/40x16/3B82F6/3B82F6.png) | ![](https://placehold.co/40x16/3B82F6/3B82F6.png) |
| `onPrimary`          | ![](https://placehold.co/40x16/FFFFFF/FFFFFF.png) | ![](https://placehold.co/40x16/FFFFFF/FFFFFF.png) |
| `primaryContainer`   | ![](https://placehold.co/40x16/2563EB/2563EB.png) | ![](https://placehold.co/40x16/2563EB/2563EB.png) |
| `onPrimaryContainer` | ![](https://placehold.co/40x16/60A5FA/60A5FA.png) | ![](https://placehold.co/40x16/60A5FA/60A5FA.png) |
| `background`         | ![](https://placehold.co/40x16/F5F5F5/F5F5F5.png) | ![](https://placehold.co/40x16/161B22/161B22.png) |
| `onBackground`       | ![](https://placehold.co/40x16/1F2937/1F2937.png) | ![](https://placehold.co/40x16/FFFFFF/FFFFFF.png) |
| `surface`            | ![](https://placehold.co/40x16/FFFFFF/FFFFFF.png) | ![](https://placehold.co/40x16/0E1116/0E1116.png) |
| `onSurface`          | ![](https://placehold.co/40x16/1F2937/1F2937.png) | ![](https://placehold.co/40x16/FFFFFF/FFFFFF.png) |
| `surfaceVariant`     | ![](https://placehold.co/40x16/F8F9FA/F8F9FA.png) | ![](https://placehold.co/40x16/1F2630/1F2630.png) |
| `onSurfaceVariant`   | ![](https://placehold.co/40x16/6B7280/6B7280.png) | ![](https://placehold.co/40x16/B1BAC4/B1BAC4.png) |
| `error`              | ![](https://placehold.co/40x16/EF4444/EF4444.png) | ![](https://placehold.co/40x16/EF4444/EF4444.png) |
| `outline`            | ![](https://placehold.co/40x16/E5E7EB/E5E7EB.png) | ![](https://placehold.co/40x16/E5E7EB/E5E7EB.png) |





