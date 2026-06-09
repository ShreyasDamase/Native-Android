
---


**Context:**  
First Jetpack Compose post on r/androiddev.  
Goal: Understand **token-based theming**, **MaterialTheme**, and **how large teams actually do this**.

---

## 1. Problem Statement (Why Design Tokens?)

I want to:

- Match **Figma Token Studio naming exactly**
    
    - `bg.primary`
        
    - `text.muted`
        
    - `icon.dark`
        
- Avoid raw colors in UI
    
- Change colors later without touching UI code
    
- Support light/dark + future themes
    
- See the **same token names in Figma and code**
    

---

## 2. My Initial Token-First Architecture (What I Built)

### 2.1 Raw Palette (never used directly in UI)

```kotlin
object AppColors {
    val White = Color(0xFFFFFFFF)
    val Slate900 = Color(0xFF0F172A)
    val Slate950 = Color(0xFF020617)
    val Pink500 = Color(0xFFEC4899)
}
```

📌 **Rule:**  
Raw colors exist only for **mapping**, never for UI usage.

---

### 2.2 Semantic Tokens (Mirror Figma Tokens)

```kotlin
@Immutable
data class BgTokens(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val inverse: Color
)

@Immutable
data class TextTokens(
    val primary: Color,
    val muted: Color,
    val inverse: Color
)

@Immutable
data class AppColorTokens(
    val bg: BgTokens,
    val text: TextTokens,
    val icon: IconTokens,
    val brand: BrandTokens,
    val status: StatusTokens
)
```

📌 **Key Idea:**  
Semantic tokens describe **intent**, not appearance.

---

### 2.3 Light / Dark Token Mapping

```kotlin
val LightTokens = AppColorTokens(
    bg = BgTokens(
        primary = AppColors.White,
        secondary = AppColors.Pink50,
        tertiary = AppColors.Slate100,
        inverse = AppColors.Slate900
    ),
    text = TextTokens(
        primary = AppColors.Slate900,
        muted = AppColors.Slate600,
        inverse = AppColors.White
    ),
    ...
)

val DarkTokens = AppColorTokens(
    bg = BgTokens(
        primary = AppColors.Slate950,
        secondary = AppColors.Slate900,
        tertiary = AppColors.Slate800,
        inverse = AppColors.White
    ),
    text = TextTokens(
        primary = AppColors.White,
        muted = AppColors.Slate400,
        inverse = AppColors.Slate950
    ),
    ...
)
```

📌 **Benefit:**  
UI never changes when design updates — only token mapping changes.

---

### 2.4 Provide Tokens via CompositionLocal

```kotlin
val LocalAppTokens = staticCompositionLocalOf<AppColorTokens> {
    error("AppColorTokens not provided")
}

@Composable
fun DailyDoTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppTokens provides if (darkTheme) DarkTokens else LightTokens
    ) {
        MaterialTheme(content = content)
    }
}
```

---

### 2.5 Token Access Wrapper (Cleaner UI)

```kotlin
object Tokens {
    val colors: AppColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTokens.current
}
```

**Usage in UI:**

```kotlin
Column(
    modifier = Modifier.background(Tokens.colors.bg.primary)
)

Text(
    text = "Home",
    color = Tokens.colors.text.primary
)
```

---

## 3. Is This Approach Valid?

✅ **Yes.**

It matches **“Fully Custom Design System”** from official docs.

### Official Docs

🔗 [https://developer.android.com/develop/ui/compose/designsystems/custom](https://developer.android.com/develop/ui/compose/designsystems/custom)

> “You are not forced to use Material.  
> You can implement a fully custom design system.”

---

## 4. Feedback from r/androiddev (What I Learned)

---

### 4.1 Compose Correctness (Immutability)

From u/Aln_ua:

> Don’t forget `@Immutable` and `@ReadOnlyComposable`

✅ Why:

- Improves recomposition performance
    
- Prevents unnecessary UI updates
    

---

### 4.2 MaterialTheme Is Still Important (Adapter Pattern)

Material components use `MaterialTheme.colorScheme` internally.

If tokens are not mapped, Material components may fallback to defaults.

**Best practice:**

```kotlin
val colorScheme = if (darkTheme) {
    darkColorScheme(
        primary = Tokens.colors.brand.primary,
        background = Tokens.colors.bg.primary,
        surface = Tokens.colors.bg.secondary,
        error = Tokens.colors.status.error
    )
} else {
    lightColorScheme(
        primary = Tokens.colors.brand.primary,
        background = Tokens.colors.bg.primary,
        surface = Tokens.colors.bg.secondary,
        error = Tokens.colors.status.error
    )
}

MaterialTheme(
    colorScheme = colorScheme,
    content = content
)
```

📌 **Mental Model:**  
**Tokens = source of truth**  
**MaterialTheme = adapter layer**

---

## 5. Advanced Idea: Token Enforcement (u/houseband23)

### 5.1 Problem

Even with tokens, devs can still write:

```kotlin
Text(
    color = Color.Red,
    fontSize = 22.sp
)
```

This breaks design consistency.

---

### 5.2 Solution: Wrapper Components

```kotlin
@Composable
fun TokenText(
    text: String,
    token: TextToken
) {
    Text(
        text = text,
        style = token.style
    )
}
```

---

### 5.3 Dynamic Tokens Example

```kotlin
object Tokens {
    val H1 = TextToken(
        TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)
    )

    val Primary = ColorToken(
        light = Color.Blue,
        dark = Color.Cyan
    )
}

data class TextToken(val style: TextStyle)

data class ColorToken(
    val light: Color,
    val dark: Color
) {
    val color: Color
        @Composable get() =
            if (isSystemInDarkTheme()) dark else light
}
```

**Usage:**

```kotlin
TokenText(
    "Hello",
    Tokens.H1 + Tokens.Primary
)
```

---

### 5.4 Is This Required?

❌ **No (for beginners)**  
✅ **Yes (for large teams)**

📌 This is a **later-stage enforcement mechanism**.

---

## 6. Material Theme Builder (Clarification)

### What it is:

- Tool to generate **Material color schemes**
    
- Best for **Material-first apps**
    

🔗 [https://material-foundation.github.io/material-theme-builder/](https://material-foundation.github.io/material-theme-builder/)

### Why I didn’t use it (this project):

- My design language comes from **Figma Token Studio**
    
- Token names matter
    
- Want 1:1 parity with design
    

📌 **Decision:**  
Use Material Theme Builder in a **future project**, not this one.

---

## 7. Comparison of All Approaches

|Approach|When to Use|
|---|---|
|Material Theme Builder|Small / Material-first apps|
|Extend MaterialTheme|Small additions|
|Token-first (mine)|Branded apps, Figma parity|
|Token enforcement (wrappers)|Large teams|

All are **valid**. Context matters.

---

## 8. Learning Path (Very Important)

Correct order of learning:

1. Understand colors & theming
    
2. Learn CompositionLocal
    
3. Mirror Figma tokens in code
    
4. Map tokens → MaterialTheme
    
5. Add wrapper components
    
6. Enforce via lint / review
    

📌 I am between **step 3 → 4** (perfectly normal).

---

## 9. Repo

🔗 [https://github.com/ShreyasDamase/DailyDo](https://github.com/ShreyasDamase/DailyDo)

---

## 10. Final Takeaway

- I’m a beginner — and that’s okay
    
- My approach is **documented and valid**
    
- I don’t need to enforce everything now
    
- This system can evolve over time
    

> **Goal right now:**  
> Understand tokens, not enforce them.

---

## 11. References

- Custom Design Systems in Compose  
    [https://developer.android.com/develop/ui/compose/designsystems/custom](https://developer.android.com/develop/ui/compose/designsystems/custom)
    
- Anatomy of a Theme  
    [https://developer.android.com/develop/ui/compose/designsystems/anatomy](https://developer.android.com/develop/ui/compose/designsystems/anatomy)
    
- Material Theme Builder  
    [https://material-foundation.github.io/material-theme-builder/](https://material-foundation.github.io/material-theme-builder/)
    

---

 
## 12. Community Feedback (Discord Thread)

### 12.1 Animate Token Changes (borninbronx)

One missing piece in the current approach: **animating color changes** when the theme switches dynamically.

- Material handles this internally
- Worth adding animated transitions for theme switches (e.g., `animateColorAsState`)

📌 **Status:** Not yet implemented — planned for future iteration.

---

### 12.2 Material's Token System vs. Design Token Standard (bleeding182)

Material uses tokens internally, but its format predates the **official Design Token standard** that Figma Token Studio and other tools follow.

- Material's system ≠ W3C Design Token spec
- The newer standard is tracked at 🔗 [https://www.designtokens.org](https://www.designtokens.org/)

**Tools that follow the newer standard:**

- 🔗 [https://terrazzo.app/docs/](https://terrazzo.app/docs/)
- 🔗 [https://styledictionary.com/](https://styledictionary.com/)

📌 **Implication:** Looking at Material source for token inspiration has limited value here — the specs are different generations.

---

### 12.3 Design Token Spec Status (pickledrick)

The draft spec at `/tr/drafts/format/` is **not stable** — do not use it as a reference.

✅ Use the stable spec: 🔗 [https://www.designtokens.org/tr/2025.10/](https://www.designtokens.org/tr/2025.10/)

---

### 12.4 Tokens Object Pattern Is Valid (borninbronx)

> Wrapping `LocalAppTokens.current` in a `Tokens` object is exactly what `MaterialTheme.colorScheme.primary` does under the hood.

✅ Confirmed — the pattern is idiomatic.

---

### 12.5 On MaterialTheme Mapping (borninbronx)

Whether to map tokens → `MaterialTheme.colorScheme` depends on your needs:

- If using **Material widgets directly**, mapping helps avoid fallback to Material defaults
- If building a **fully custom system**, mapping is optional

📌 Already covered in Section 4.2 — mapping is the recommended path for this project.

---

### 12.6 Over-Engineering Assessment (borninbronx)

> "There's some overhead in code — as for over-engineering, it depends on whether you actually need all that or not."

📌 For a solo/learning project, the current depth is fine. Revisit if the project scales.















 
 
---

# GUIDE 1 — FIGMA DESIGNER (NON-DEVELOPER)

This guide is **only for the Figma designer**.  
He does **not** need to know React Native or Tamagui.

---

## 1️⃣ GLOBAL RULES (FOLLOW EXACTLY)

- Use **Figma Variables only**
    
- One **Light theme** only (Dark will be generated later)
    
- Every visible UI value **must come from a variable**
    
- Variables **must be applied to real components**
    

---

## 2️⃣ NAMING CONVENTION (FINAL DECISION)

- **Use lowercase**
    
- **Dot notation**
    
- **Semantic names**
    
- **H1 / H2 ARE TOKENS**, not font sizes
    

Examples:

```
h1
button.primary.bg
card.radius
icon.primary
```

This is intentional and final.

---

## 3️⃣ TOKEN CATEGORIES (CREATE EXACTLY THESE)

### A. TYPOGRAPHY (NO font.size.h1)

Create **TEXT TOKENS**, not size tokens.

```
h1
h2
h3
h4
body.lg
body.md
body.sm
caption
```

Each token defines:

- Font family: **D Sans**
    
- Font weight
    
- Font size
    
- Line height
    
- Letter spacing (if needed)
    

👉 Designer decides values  
👉 Developer consumes token as-is

---

### B. SPACING

```
space.xs
space.sm
space.md
space.lg
space.xl
space.2xl
```

Used for:

- Padding
    
- Margin
    
- Gaps
    

---

### C. RADIUS (NO CAPITAL R)

```
radius.none
radius.sm
radius.md
radius.lg
radius.full
```

Used by:

- Buttons
    
- Cards
    
- Inputs
    
- Chips
    
- Toggles
    

---

### D. COLORS — SEMANTIC ONLY

#### Background

```
bg.screen
bg.surface
bg.card
bg.input
bg.overlay
```

#### Text

```
text.primary
text.secondary
text.muted
text.inverse
```

#### Border

```
border.default
border.subtle
border.strong
border.focus
border.error
```

#### Icon

```
icon.primary
icon.secondary
icon.muted
icon.inverse
```

#### Status

```
status.success
status.warning
status.error
status.info
```

#### Brand

```
brand.primary
brand.secondary
brand.accent
```

---

## 4️⃣ COMPONENT TOKENS (MANDATORY)

### Buttons

```
button.primary.bg
button.primary.text
button.primary.border

button.secondary.bg
button.secondary.text

button.disabled.bg
button.disabled.text
```

### Cards

```
card.bg
card.border
card.radius
card.shadow
```

### Inputs / Search

```
input.bg
input.text
input.placeholder
input.border.default
input.border.focus
input.border.error
```

### Chips / Tags

```
chip.bg
chip.text
chip.border
```

### Toggle / Switch / Checkbox

```
toggle.track.on
toggle.track.off
toggle.thumb
```

### Bottom Tab / Navigation

```
nav.bg
nav.icon.active
nav.icon.inactive
nav.label.active
nav.label.inactive
```

---

## 5️⃣ APPLY TOKENS (CRITICAL)

- Button background → `button.primary.bg`
    
- Card radius → `card.radius`
    
- Screen padding → `space.lg`
    
- Title text → `h1`
    

If a variable is **not applied**, it is **invalid**.

Summary Table :::;
 
---

 

|Category|Token Name|Used For|
|---|---|---|
|Typography|h1|Screen titles, primary headers|
|Typography|h2|Section headers|
|Typography|h3|Card titles|
|Typography|h4|Subsection titles|
|Typography|body.lg|Main paragraph text|
|Typography|body.md|Default body text|
|Typography|body.sm|Secondary text|
|Typography|caption|Meta text, timestamps, labels|
|Spacing|space.xs|Icon gaps, micro spacing|
|Spacing|space.sm|Small inner padding|
|Spacing|space.md|Default padding|
|Spacing|space.lg|Screen padding|
|Spacing|space.xl|Section separation|
|Spacing|space.2xl|Major layout gaps|
|Radius|radius.none|Flat elements|
|Radius|radius.sm|Inputs, chips|
|Radius|radius.md|Buttons|
|Radius|radius.lg|Cards, modals|
|Radius|radius.full|Avatars, pills|
|Background|bg.screen|App background|
|Background|bg.surface|Lists, containers|
|Background|bg.card|Cards|
|Background|bg.input|Inputs, search|
|Background|bg.overlay|Modals, sheets|
|Text Color|text.primary|Main text|
|Text Color|text.secondary|Supporting text|
|Text Color|text.muted|Disabled / hint|
|Text Color|text.inverse|On dark / brand bg|
|Border|border.default|Cards, inputs|
|Border|border.subtle|Dividers|
|Border|border.strong|Emphasis|
|Border|border.focus|Focused input|
|Border|border.error|Error state|
|Icon|icon.primary|Main icons|
|Icon|icon.secondary|Secondary icons|
|Icon|icon.muted|Disabled icons|
|Icon|icon.inverse|On dark bg|
|Brand|brand.primary|Primary CTA|
|Brand|brand.secondary|Secondary actions|
|Brand|brand.accent|Highlights|
|Status|status.success|Success states|
|Status|status.warning|Warnings|
|Status|status.error|Errors|
|Status|status.info|Info banners|
|Button|button.primary.bg|Primary button background|
|Button|button.primary.text|Primary button text|
|Button|button.primary.border|Primary button outline|
|Button|button.secondary.bg|Secondary button background|
|Button|button.secondary.text|Secondary button text|
|Button|button.disabled.bg|Disabled button background|
|Button|button.disabled.text|Disabled button text|
|Card|card.bg|Card background|
|Card|card.border|Card outline|
|Card|card.radius|Card corner radius|
|Card|card.shadow|Card elevation|
|Input|input.bg|Input background|
|Input|input.text|Input text|
|Input|input.placeholder|Placeholder text|
|Input|input.border.default|Default input border|
|Input|input.border.focus|Focused input border|
|Input|input.border.error|Error input border|
|Chip|chip.bg|Chip background|
|Chip|chip.text|Chip text|
|Chip|chip.border|Chip outline|
|Toggle|toggle.track.on|Toggle ON track|
|Toggle|toggle.track.off|Toggle OFF track|
|Toggle|toggle.thumb|Toggle thumb|
|Navigation|nav.bg|Bottom tab background|
|Navigation|nav.icon.active|Active tab icon|
|Navigation|nav.icon.inactive|Inactive tab icon|
|Navigation|nav.label.active|Active tab label|
|Navigation|nav.label.inactive|Inactive tab label|
|Chart|chart.primary|Main graph line|
|Chart|chart.secondary|Secondary graph line|
|Chart|chart.grid|Grid lines|
|Chart|chart.label|Axis labels|

---

---

# GUIDE 2 — REACT NATIVE / TAMAGUI (YOU)

This guide is **only for implementation**.

You **mirror** what the designer creates.  
You do **not rename** anything.

---

## 1️⃣ TYPOGRAPHY (H1 IS A TOKEN)

```ts
import { createFont } from 'tamagui'

export const dSans = createFont({
  family: 'D Sans',

  size: {
    h1: 32,
    h2: 28,
    h3: 24,
    h4: 20,
    bodyLg: 16,
    bodyMd: 14,
    bodySm: 12,
    caption: 11,
  },

  lineHeight: {
    h1: 40,
    h2: 36,
    h3: 32,
    h4: 28,
    bodyLg: 24,
    bodyMd: 20,
    bodySm: 18,
    caption: 16,
  },

  weight: {
    regular: '400',
    medium: '500',
    bold: '700',
  },
})
```

---

## 2️⃣ TOKENS (FOUNDATION)

```ts
import { createTokens } from 'tamagui'

export const tokens = createTokens({
  space: {
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 24,
    '2xl': 32,
    true: 12,
  },

  radius: {
    none: 0,
    sm: 6,
    md: 10,
    lg: 16,
    full: 999,
    true: 10,
  },

  color: {
    white: '#FFFFFF',
    black: '#000000',

    brandPrimary: '#5B5FFF',

    gray100: '#F7F7F7',
    gray300: '#E0E0E0',
    gray600: '#6B6B6B',
    gray900: '#121212',

    success: '#2ECC71',
    warning: '#F5A623',
    error: '#E53935',
    info: '#2D9CDB',
  },
})
```

---

## 3️⃣ LIGHT THEME (SEMANTIC MAPPING)

```ts
export const lightTheme = {
  bgScreen: tokens.color.white,
  bgSurface: tokens.color.gray100,
  bgCard: tokens.color.white,
  bgInput: tokens.color.gray100,

  textPrimary: tokens.color.gray900,
  textSecondary: tokens.color.gray600,
  textMuted: tokens.color.gray600,
  textInverse: tokens.color.white,

  borderDefault: tokens.color.gray300,
  borderFocus: tokens.color.brandPrimary,
  borderError: tokens.color.error,

  iconPrimary: tokens.color.gray900,
  iconSecondary: tokens.color.gray600,
  iconMuted: tokens.color.gray600,

  buttonPrimaryBg: tokens.color.brandPrimary,
  buttonPrimaryText: tokens.color.white,

  statusSuccess: tokens.color.success,
  statusWarning: tokens.color.warning,
  statusError: tokens.color.error,
}
```

---

## 4️⃣ DARK THEME (AUTO-DERIVED / AI-READY)

```ts
export const darkTheme = {
  bgScreen: '#0E0E0E',
  bgSurface: '#161616',
  bgCard: '#1C1C1C',
  bgInput: '#222222',

  textPrimary: '#FFFFFF',
  textSecondary: '#CCCCCC',
  textMuted: '#888888',
  textInverse: '#000000',

  borderDefault: '#2A2A2A',
  borderFocus: tokens.color.brandPrimary,
  borderError: tokens.color.error,

  iconPrimary: '#FFFFFF',
  iconSecondary: '#CCCCCC',

  buttonPrimaryBg: tokens.color.brandPrimary,
  buttonPrimaryText: '#FFFFFF',
}
```

---

## 5️⃣ TAMAGUI CONFIG (FINAL)

```ts
import { createTamagui } from 'tamagui'
import { dSans } from './fonts'
import { tokens } from './tokens'

export const config = createTamagui({
  fonts: {
    heading: dSans,
    body: dSans,
  },

  tokens,

  themes: {
    light: lightTheme,
    dark: darkTheme,
  },
})
```

---

## 6️⃣ HOW YOU USE IT

```tsx
<Text fontSize="$h1" lineHeight="$h1">
  Title
</Text>

<Button
  backgroundColor="$buttonPrimaryBg"
  color="$buttonPrimaryText"
  borderRadius="$radius.md"
/>
```

No hex.  
No guessing.  
No duplication.

---

## FINAL ALIGNMENT STATEMENT (FOR TEAM)

> **Designer owns tokens.  
> Developer mirrors tokens.  
> Tokens are the contract.**

---

 