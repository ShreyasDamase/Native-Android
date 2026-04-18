# 🗓️ Kotlin Date & Time API — Complete Reference Notes

> **Source:** `java.time` (JVM/Android) — modern, immutable, thread-safe. Use this over legacy `java.util.Date` or `Calendar`. Always.

---

## 📌 Golden Rule — Immutability

```kotlin
val d = LocalDate.now()
d.plusDays(1)      // ❌ does NOT change d
val d2 = d.plusDays(1)  // ✅ always assign to new variable
```

Every operation returns a **new object**. The original is never mutated.

---

## 🧱 Type Chooser — Which Type Do I Need?

|Situation|Type to Use|
|---|---|
|Today's date only|`LocalDate`|
|Year + Month only (no day)|`YearMonth`|
|Date + Time, no timezone|`LocalDateTime`|
|Time only (no date)|`LocalTime`|
|Exact UTC timestamp / epoch|`Instant`|
|Difference in days/months/years|`Period`|
|Difference in hours/minutes/seconds|`Duration`|
|Single-unit time difference|`ChronoUnit.between()`|

---

## 🧱 1. `LocalDate` — Date Only

### Import

```kotlin
import java.time.LocalDate
```

### Create

```kotlin
val today   = LocalDate.now()
val custom  = LocalDate.of(2026, 4, 17)        // year, month, day
val parsed  = LocalDate.parse("2026-04-17")     // ISO 8601 string
```

### Read Properties

```kotlin
today.year           // Int  → 2026
today.month          // Month enum → APRIL
today.monthValue     // Int  → 4
today.dayOfMonth     // Int  → 17
today.dayOfWeek      // DayOfWeek enum → FRIDAY
today.dayOfYear      // Int  → day number in year (e.g. 107)
today.lengthOfMonth()// Int  → days in this month (28/29/30/31)
today.lengthOfYear() // Int  → 365 or 366
today.isLeapYear     // Boolean
```

### Add / Subtract

```kotlin
today.plusDays(5)
today.minusDays(3)
today.plusWeeks(2)
today.minusWeeks(1)
today.plusMonths(1)
today.minusMonths(2)
today.plusYears(1)
today.minusYears(1)
```

### Modify (with)

```kotlin
today.withDayOfMonth(1)   // → first day of same month
today.withMonth(12)        // → same day, December
today.withYear(2025)       // → same month/day, different year
```

### Compare

```kotlin
date1.isBefore(date2)   // Boolean
date1.isAfter(date2)    // Boolean
date1.isEqual(date2)    // Boolean
date1 == date2          // Boolean (structural equality)
date1.compareTo(date2)  // Int: negative / 0 / positive
```

### Convert

```kotlin
today.atStartOfDay()                // → LocalDateTime (00:00:00)
today.atTime(14, 30)                // → LocalDateTime (14:30)
today.atTime(LocalTime.of(9, 0))   // → LocalDateTime
```

### Format

```kotlin
import java.time.format.DateTimeFormatter

val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
val formatted = today.format(formatter)   // "17 Apr 2026"

// Common patterns:
// "yyyy-MM-dd"    → 2026-04-17
// "dd/MM/yyyy"    → 17/04/2026
// "MMM d, yyyy"   → Apr 17, 2026
// "EEEE"          → Friday  (full day name)
// "EEE"           → Fri
```

### Epoch Days

```kotlin
val epochDay = today.toEpochDay()              // Long
val fromEpoch = LocalDate.ofEpochDay(19000L)   // LocalDate
```

---

## 🧱 2. `YearMonth` — Year + Month Only

### Import

```kotlin
import java.time.YearMonth
```

### Create

```kotlin
val ym     = YearMonth.now()
val custom = YearMonth.of(2026, 4)       // April 2026
val parsed = YearMonth.parse("2026-04")  // ISO string
```

### Read Properties

```kotlin
ym.year            // Int  → 2026
ym.month           // Month enum → APRIL
ym.monthValue      // Int  → 4
ym.lengthOfMonth() // Int  → 30 (handles leap Feb automatically)
ym.isLeapYear      // Boolean
```

### Navigate

```kotlin
ym.plusMonths(1)    // → 2026-05
ym.minusMonths(1)   // → 2026-03
ym.plusYears(1)     // → 2027-04
ym.minusYears(1)    // → 2025-04
```

### Convert to LocalDate

```kotlin
ym.atDay(1)          // → first day of month
ym.atDay(15)         // → 15th of month
ym.atEndOfMonth()    // → last day of month (28/29/30/31)
```

### Compare

```kotlin
ym1.isBefore(ym2)
ym1.isAfter(ym2)
ym1 == ym2
```

### ⚡ Calendar Offset Trick

```kotlin
// Get the column offset for the first day in a Sun-start grid:
val firstDayOfMonth = ym.atDay(1)
val offset = firstDayOfMonth.dayOfWeek.value % 7
// SUNDAY(7) % 7 = 0 → column 0
// MONDAY(1) % 7 = 1 → column 1
// SATURDAY(6) % 7 = 6 → column 6
```

---

## 🧱 3. `DayOfWeek` — Day Enum

```kotlin
import java.time.DayOfWeek
```

|Constant|`.value`|
|---|---|
|`MONDAY`|1|
|`TUESDAY`|2|
|`WEDNESDAY`|3|
|`THURSDAY`|4|
|`FRIDAY`|5|
|`SATURDAY`|6|
|`SUNDAY`|7|

```kotlin
val day = LocalDate.now().dayOfWeek     // DayOfWeek.FRIDAY
val num = day.value                      // 5
val name = day.name                      // "FRIDAY"
val display = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)  // "Friday"
```

---

## 🧱 4. `Month` Enum

```kotlin
today.month           // Month.APRIL
today.monthValue      // 4  (1 = Jan, 12 = Dec)
today.month.name      // "APRIL"
today.month.value     // 4

Month.APRIL.minLength()   // 30 (min days ever)
Month.FEBRUARY.maxLength()// 29 (max days, leap)
```

---

## 🧱 5. `LocalDateTime` — Date + Time

### Import

```kotlin
import java.time.LocalDateTime
```

### Create

```kotlin
val now    = LocalDateTime.now()
val custom = LocalDateTime.of(2026, 4, 17, 14, 30)          // y,m,d,h,min
val custom2= LocalDateTime.of(2026, 4, 17, 14, 30, 55)      // + seconds
val from   = LocalDate.now().atTime(9, 0)                    // from LocalDate
```

### Read Properties

```kotlin
now.year
now.monthValue
now.dayOfMonth
now.hour
now.minute
now.second
now.nano
now.dayOfWeek
now.toLocalDate()   // extract date part
now.toLocalTime()   // extract time part
```

### Add / Subtract

```kotlin
now.plusHours(2)
now.minusMinutes(30)
now.plusSeconds(45)
now.plusDays(1)
now.plusMonths(1)
```

---

## 🧱 6. `LocalTime` — Time Only

```kotlin
import java.time.LocalTime

val now    = LocalTime.now()
val custom = LocalTime.of(14, 30)        // 14:30
val custom2= LocalTime.of(14, 30, 55)    // 14:30:55

now.hour
now.minute
now.second

now.plusHours(1)
now.minusMinutes(15)

now.isBefore(LocalTime.NOON)
now.isAfter(LocalTime.of(18, 0))
```

Special constants:

```kotlin
LocalTime.MIDNIGHT  // 00:00
LocalTime.NOON      // 12:00
LocalTime.MIN       // 00:00
LocalTime.MAX       // 23:59:59.999999999
```

---

## 🧱 7. `Instant` — UTC Timestamp / Epoch

```kotlin
import java.time.Instant

val now  = Instant.now()
val epoch= now.toEpochMilli()     // Long milliseconds since 1970-01-01T00:00:00Z
val from = Instant.ofEpochMilli(1713369600000L)

// Convert from Android System time:
val instant = Instant.ofEpochMilli(System.currentTimeMillis())

// Convert Instant → LocalDate (needs timezone)
import java.time.ZoneId
val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
```

> Use `Instant` for backend timestamps, logs, comparing events across timezones.

---

## 🧱 8. `Period` — Date-Based Difference

```kotlin
import java.time.Period

val start = LocalDate.of(2000, 1, 1)
val end   = LocalDate.now()

val period = Period.between(start, end)
period.years    // Int
period.months   // Int (0–11)
period.days     // Int (0–30)

// ⚠️ days + months + years together = total period
// They are NOT independent totals
```

Create a period directly:

```kotlin
val p = Period.of(1, 6, 0)  // 1 year, 6 months, 0 days
val p2 = Period.ofMonths(3)
val p3 = Period.ofDays(10)

// Apply to a date:
LocalDate.now().plus(Period.ofMonths(6))
```

---

## 🧱 9. `Duration` — Time-Based Difference

```kotlin
import java.time.Duration

val start = LocalDateTime.of(2026, 4, 17, 9, 0)
val end   = LocalDateTime.of(2026, 4, 17, 17, 30)

val duration = Duration.between(start, end)
duration.toHours()    // Long → 8
duration.toMinutes()  // Long → 510
duration.toSeconds()  // Long
duration.toMillis()   // Long

// Create directly:
val d = Duration.ofHours(2)
val d2= Duration.ofMinutes(90)
val d3= Duration.ofSeconds(3600)
```

> Use `Duration` for time-of-day differences, timers, countdowns.

---

## 🧱 10. `ChronoUnit` — Single-Unit Differences

```kotlin
import java.time.temporal.ChronoUnit

val days   = ChronoUnit.DAYS.between(startDate, endDate)    // Long
val months = ChronoUnit.MONTHS.between(startDate, endDate)  // Long
val years  = ChronoUnit.YEARS.between(startDate, endDate)   // Long
val hours  = ChronoUnit.HOURS.between(startTime, endTime)   // Long
```

Available units: `NANOS`, `MICROS`, `MILLIS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`, `WEEKS`, `MONTHS`, `YEARS`, `DECADES`, `CENTURIES`.

> Simpler than `Period` when you need ONE number (e.g. "how many total days between two dates").

---

## 🧱 11. `DateTimeFormatter` — Formatting & Parsing

```kotlin
import java.time.format.DateTimeFormatter
import java.util.Locale

// Format a date
val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
LocalDate.now().format(fmt)       // "17 Apr 2026"

// Parse a string into LocalDate
val date = LocalDate.parse("17-04-2026", DateTimeFormatter.ofPattern("dd-MM-yyyy"))

// Built-in formatters (no pattern needed):
DateTimeFormatter.ISO_LOCAL_DATE        // 2026-04-17
DateTimeFormatter.ISO_LOCAL_DATE_TIME   // 2026-04-17T14:30:00
DateTimeFormatter.BASIC_ISO_DATE        // 20260417
```

### Common Pattern Letters

|Pattern|Example|Meaning|
|---|---|---|
|`yyyy`|2026|4-digit year|
|`MM`|04|2-digit month number|
|`MMM`|Apr|Short month name|
|`MMMM`|April|Full month name|
|`dd`|17|2-digit day|
|`d`|17|Day (no padding)|
|`EEE`|Fri|Short day name|
|`EEEE`|Friday|Full day name|
|`HH`|14|Hour (24h)|
|`hh`|02|Hour (12h)|
|`mm`|30|Minute|
|`ss`|55|Second|
|`a`|PM|AM/PM|

---

## 🔁 Edge Cases Handled Automatically

```kotlin
// January 31 + 1 month → February 28 (or 29 on leap year)
LocalDate.of(2026, 1, 31).plusMonths(1)   // → 2026-02-28

// December + 1 month → January next year
YearMonth.of(2026, 12).plusMonths(1)       // → 2027-01

// Leap year Feb:
YearMonth.of(2024, 2).lengthOfMonth()      // → 29
YearMonth.of(2025, 2).lengthOfMonth()      // → 28
```

---

## 🔥 Calendar Grid Use Case (Full Pattern)

```kotlin
val yearMonth   = YearMonth.now()
val daysInMonth = yearMonth.lengthOfMonth()
val firstDay    = yearMonth.atDay(1)
val today       = LocalDate.now()
val offset      = firstDay.dayOfWeek.value % 7  // Sun=0, Mon=1 ... Sat=6

for (index in 0 until 42) {
    when {
        // Previous month trailing days
        index < offset -> {
            val prevDay = yearMonth.minusMonths(1).atEndOfMonth()
                .minusDays((offset - index - 1).toLong())
            // isCurrentMonth = false
        }
        // Current month days
        index < offset + daysInMonth -> {
            val dayNumber = index - offset + 1
            val date = yearMonth.atDay(dayNumber)
            // isCurrentMonth = true, isToday = date == today
        }
        // Next month leading days
        else -> {
            val startIndex = offset + daysInMonth
            val nextDayNumber = index - startIndex + 1
            val date = yearMonth.plusMonths(1).atDay(nextDayNumber)
            // isCurrentMonth = false
        }
    }
}
```

---

## 🗺️ Mental Model Summary

```
Instant         →  machine time (UTC epoch)
        ↓ + ZoneId
LocalDateTime   →  human date + time, no timezone
        ↓ split
LocalDate       →  date only (year, month, day)
YearMonth       →  month container (no day)
LocalTime       →  time only (hour, minute, second)

Period          →  difference in dates  (years, months, days)
Duration        →  difference in times  (hours, minutes, seconds)
ChronoUnit      →  difference in ONE unit
```

---

## ⭐ Quick Reference — Most Used APIs

```kotlin
// Today
LocalDate.now()

// Specific date
LocalDate.of(2026, 4, 17)

// Days in month
YearMonth.of(2026, 4).lengthOfMonth()   // 30

// First / last day of month
ym.atDay(1)
ym.atEndOfMonth()

// Navigate
date.plusDays(n)
date.minusDays(n)
ym.plusMonths(n)
ym.minusMonths(n)

// Day of week (1=Mon, 7=Sun)
date.dayOfWeek.value

// Calendar grid offset (Sun-start)
date.dayOfWeek.value % 7

// Comparison
date1.isBefore(date2)
date1.isAfter(date2)
date1 == date2

// Total days between two dates
ChronoUnit.DAYS.between(start, end)

// Period (broken down)
Period.between(start, end).years
Period.between(start, end).months

// Format
date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
```

---

## 🎯 Interview Summary

> "I use `java.time` APIs like `LocalDate` and `YearMonth` because they are immutable, thread-safe, and handle edge cases like leap years and month-end rollovers automatically. Every operation returns a new object, making them safe for reactive/state-based UI like Compose."

---

## 🚨 Common Mistakes

|Mistake|Fix|
|---|---|
|`d.plusDays(1)` and using `d`|Assign: `val d2 = d.plusDays(1)`|
|`month.value` vs `monthValue`|Both work on `LocalDate`; `month` is the enum|
|`Period.between().months` returns full period months|It's 0–11, not total months — use `ChronoUnit.MONTHS` for total|
|Assuming 42 grid cells always show 6 rows|Some months fit in 4 or 5 rows if offset is small|
|Hardcoding 28 for February|Use `lengthOfMonth()` — leap year = 29|

---

_Notes generated: April 17, 2026 | Sources: kotlinlang.org, developer.android.com, dev.java_