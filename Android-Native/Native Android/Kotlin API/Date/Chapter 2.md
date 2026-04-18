
# 🗓️ Kotlin Date & Time API — Chapter 2: Real-Life Examples

> **Chapter 1:** Core API Reference **Chapter 2 (this file):** Real-world use cases — exactly like your calendar project

---

## 📋 Index of Real-Life Problems

|#|Problem|APIs Used|
|---|---|---|
|1|Calendar Grid Generation|`YearMonth`, `LocalDate`, `DayOfWeek`|
|2|Age Calculator|`LocalDate`, `Period`, `ChronoUnit`|
|3|Event Countdown Timer|`LocalDate`, `ChronoUnit`, `Period`|
|4|Subscription / Expiry Tracker|`YearMonth`, `LocalDate`, `Period`|
|5|Birthday Reminder|`LocalDate`, `MonthDay`|
|6|Habit Streak Tracker|`LocalDate`, `ChronoUnit`|
|7|Meeting Scheduler (Working Days)|`LocalDate`, `DayOfWeek`|
|8|Invoice Due Date Calculator|`LocalDate`, `YearMonth`|
|9|Date Picker Validation|`LocalDate`, `Period`|
|10|Time-Ago / Relative Time Label|`LocalDate`, `LocalDateTime`, `ChronoUnit`|

---

---

## 🔷 1. Calendar Grid Generation

> **Your own project — fully explained with clean code**

### 🎯 Problem

Build a 7×6 (42-cell) calendar grid for any given month. Fill empty cells with trailing days from previous month and leading days from next month.

### 🧠 Key Insight

```
[ prev trailing ] [ current month ] [ next leading ]
     offset            1 → N          1 → N
```

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

fun generateCalendarDays(yearMonth: YearMonth): List<CalendarDay> {
    val today        = LocalDate.now()
    val daysInMonth  = yearMonth.lengthOfMonth()
    val firstDay     = yearMonth.atDay(1)

    // Sunday = 0, Monday = 1 ... Saturday = 6
    val offset = firstDay.dayOfWeek.value % 7

    val result = mutableListOf<CalendarDay>()

    for (index in 0 until 42) {
        val calendarDay = when {

            // ── Previous month trailing days ──────────────────────────
            index < offset -> {
                val daysBack = offset - index
                val date = firstDay.minusDays(daysBack.toLong())
                CalendarDay(date, isCurrentMonth = false, isToday = date == today)
            }

            // ── Current month days ────────────────────────────────────
            index < offset + daysInMonth -> {
                val dayNumber = index - offset + 1
                val date = yearMonth.atDay(dayNumber)
                CalendarDay(date, isCurrentMonth = true, isToday = date == today)
            }

            // ── Next month leading days ───────────────────────────────
            else -> {
                val startIndex  = offset + daysInMonth
                val nextDayNumber = index - startIndex + 1
                val date = yearMonth.plusMonths(1).atDay(nextDayNumber)
                CalendarDay(date, isCurrentMonth = false, isToday = date == today)
            }
        }
        result.add(calendarDay)
    }

    return result
}
```

### 🔍 What Each API Does Here

|API|Role|
|---|---|
|`YearMonth.now()`|Get current month|
|`lengthOfMonth()`|Know how many days to fill (28/29/30/31)|
|`atDay(1)`|Get the first `LocalDate` of the month|
|`dayOfWeek.value % 7`|Column offset (0=Sun, 1=Mon … 6=Sat)|
|`minusDays(n)`|Fill previous month trailing days|
|`plusMonths(1).atDay(n)`|Fill next month leading days|
|`date == today`|Highlight today's cell|

### ⚠️ Edge Cases This Handles

- February on leap year → `lengthOfMonth()` returns 29 automatically
- Month starting on Sunday → `offset = 0`, no trailing days needed
- December → `plusMonths(1)` correctly gives January next year
- January → `minusMonths(1)` correctly gives December previous year

---

---

## 🔷 2. Age Calculator

> **Used in:** Profile screens, KYC forms, health apps

### 🎯 Problem

Calculate exact age (years, months, days) from a date of birth.

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

fun calculateAge(dateOfBirth: LocalDate): String {
    val today  = LocalDate.now()
    val period = Period.between(dateOfBirth, today)

    return "${period.years} years, ${period.months} months, ${period.days} days"
}

fun calculateAgeInDays(dateOfBirth: LocalDate): Long {
    return ChronoUnit.DAYS.between(dateOfBirth, LocalDate.now())
}

// Usage
val dob    = LocalDate.of(2000, 6, 15)
println(calculateAge(dob))         // "25 years, 10 months, 2 days"
println(calculateAgeInDays(dob))   // 9437 (approx)
```

### 🔍 Why Period over ChronoUnit?

|Need|Use|
|---|---|
|Broken down (Y/M/D)|`Period.between()`|
|Just total days|`ChronoUnit.DAYS.between()`|
|Just total months|`ChronoUnit.MONTHS.between()`|

### ⚠️ Edge Case — Birthday this year not yet passed

```kotlin
fun isAdult(dob: LocalDate): Boolean {
    return Period.between(dob, LocalDate.now()).years >= 18
    // ✅ Handles Feb 29 birthdays, year boundaries automatically
}
```

---

---

## 🔷 3. Event Countdown Timer

> **Used in:** Exam apps, festival countdowns, product launch timers

### 🎯 Problem

Show "X days left" until an event. Show "X days ago" if already passed.

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CountdownResult(
    val label: String,
    val days: Long,
    val isPast: Boolean
)

fun getCountdown(eventDate: LocalDate): CountdownResult {
    val today    = LocalDate.now()
    val daysDiff = ChronoUnit.DAYS.between(today, eventDate)

    return when {
        daysDiff > 0  -> CountdownResult("$daysDiff days left",  daysDiff, false)
        daysDiff == 0L-> CountdownResult("Today! 🎉",            0,        false)
        else          -> CountdownResult("${-daysDiff} days ago", -daysDiff, true)
    }
}

// Usage
val examDate = LocalDate.of(2026, 5, 15)
val result   = getCountdown(examDate)
println(result.label)   // "28 days left"
```

### ⚠️ Edge Case — Negative ChronoUnit

```kotlin
// ChronoUnit.DAYS.between(today, pastDate) → NEGATIVE number
// ChronoUnit.DAYS.between(pastDate, today) → POSITIVE number
// Order matters! Always: between(FROM, TO)
```

---

---

## 🔷 4. Subscription / Expiry Tracker

> **Used in:** OTT apps, SaaS dashboards, credit card expiry

### 🎯 Problem

- Show subscription renewal date (monthly/yearly)
- Show expiry warning if < 7 days left
- Credit card expiry: only year + month matters

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class Subscription(
    val name: String,
    val startDate: LocalDate,
    val billingCycleMonths: Int = 1
)

fun getNextRenewalDate(sub: Subscription): LocalDate {
    val today = LocalDate.now()
    var renewal = sub.startDate

    // Roll forward until renewal is in the future
    while (!renewal.isAfter(today)) {
        renewal = renewal.plusMonths(sub.billingCycleMonths.toLong())
    }
    return renewal
}

fun getRenewalStatus(sub: Subscription): String {
    val renewal  = getNextRenewalDate(sub)
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), renewal)

    return when {
        daysLeft <= 3  -> "⚠️ Renews in $daysLeft days — check payment!"
        daysLeft <= 7  -> "🔔 Renewing soon: $daysLeft days"
        else           -> "✅ Active — renews in $daysLeft days"
    }
}

// Credit Card expiry — only YearMonth needed
fun isCreditCardExpired(expiryMonth: Int, expiryYear: Int): Boolean {
    val expiry  = YearMonth.of(expiryYear, expiryMonth)
    val current = YearMonth.now()
    return expiry.isBefore(current)
}

// Usage
val netflix = Subscription("Netflix", LocalDate.of(2025, 1, 10), billingCycleMonths = 1)
println(getRenewalStatus(netflix))

println(isCreditCardExpired(3, 2025))   // true  (expired)
println(isCreditCardExpired(12, 2027))  // false (valid)
```

### 🔍 Why `YearMonth` for credit cards?

Because credit card expiry has **no day** — the card is valid through the last day of the expiry month. `YearMonth` models this exactly.

---

---

## 🔷 5. Birthday Reminder

> **Used in:** Contact apps, HR systems, social apps

### 🎯 Problem

Given a list of birthdays, show who has a birthday today, this week, or this month.

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

data class Contact(val name: String, val birthday: LocalDate)

fun getBirthdayStatus(contact: Contact): String {
    val today         = LocalDate.now()
    val birthdayThisYear = contact.birthday.withYear(today.year)

    // If birthday already passed this year, check next year
    val nextBirthday = if (birthdayThisYear.isBefore(today)) {
        birthdayThisYear.plusYears(1)
    } else {
        birthdayThisYear
    }

    val daysUntil = ChronoUnit.DAYS.between(today, nextBirthday)

    return when {
        daysUntil == 0L  -> "🎂 ${contact.name}'s birthday is TODAY!"
        daysUntil <= 7   -> "🎁 ${contact.name}'s birthday in $daysUntil days"
        daysUntil <= 30  -> "📅 ${contact.name}'s birthday in $daysUntil days"
        else             -> "${contact.name} — ${daysUntil} days"
    }
}

// Check if birthday is today regardless of year
fun isBirthdayToday(birthday: LocalDate): Boolean {
    val today = LocalDate.now()
    return MonthDay.from(birthday) == MonthDay.from(today)
}

// Usage
val contacts = listOf(
    Contact("Rahul", LocalDate.of(1998, 4, 18)),
    Contact("Priya", LocalDate.of(2000, 12, 25)),
)
contacts.forEach { println(getBirthdayStatus(it)) }
```

### 🔍 `MonthDay` — The Hidden Gem

```kotlin
import java.time.MonthDay

// MonthDay strips the year — perfect for recurring annual events
val birthday = MonthDay.of(4, 18)         // April 18 (any year)
val today    = MonthDay.from(LocalDate.now())

birthday == today   // true if today is April 18
```

---

---

## 🔷 6. Habit Streak Tracker

> **Used in:** Fitness apps, language learning apps (Duolingo style)

### 🎯 Problem

Track how many consecutive days a user completed a habit. Reset streak if they missed a day.

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun calculateStreak(completedDates: List<LocalDate>): Int {
    if (completedDates.isEmpty()) return 0

    val sorted = completedDates.sortedDescending()
    val today  = LocalDate.now()

    // Streak must include today or yesterday to be "active"
    if (ChronoUnit.DAYS.between(sorted.first(), today) > 1) return 0

    var streak = 1
    for (i in 0 until sorted.size - 1) {
        val daysBetween = ChronoUnit.DAYS.between(sorted[i + 1], sorted[i])
        if (daysBetween == 1L) {
            streak++
        } else {
            break  // gap found, streak ends
        }
    }
    return streak
}

fun isHabitDoneToday(completedDates: List<LocalDate>): Boolean {
    return completedDates.contains(LocalDate.now())
}

// Usage
val habitDays = listOf(
    LocalDate.now(),
    LocalDate.now().minusDays(1),
    LocalDate.now().minusDays(2),
    LocalDate.now().minusDays(4),  // gap here — streak resets
)
println(calculateStreak(habitDays))   // 3
```

### 🔍 Key Pattern

```kotlin
// Days between two consecutive dates should be exactly 1 for a streak
ChronoUnit.DAYS.between(yesterday, today) == 1L   // ✅ consecutive
ChronoUnit.DAYS.between(twoDaysAgo, today) == 2L  // ❌ gap
```

---

---

## 🔷 7. Meeting Scheduler — Skip Weekends

> **Used in:** HR systems, booking apps, delivery date calculators

### 🎯 Problem

Schedule a meeting N working days from today. Skip Saturdays and Sundays.

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.DayOfWeek

fun addWorkingDays(startDate: LocalDate, workingDays: Int): LocalDate {
    var date  = startDate
    var added = 0

    while (added < workingDays) {
        date = date.plusDays(1)
        if (date.dayOfWeek != DayOfWeek.SATURDAY &&
            date.dayOfWeek != DayOfWeek.SUNDAY) {
            added++
        }
    }
    return date
}

fun isWeekend(date: LocalDate): Boolean {
    return date.dayOfWeek == DayOfWeek.SATURDAY ||
           date.dayOfWeek == DayOfWeek.SUNDAY
}

fun getNextWorkingDay(date: LocalDate): LocalDate {
    var next = date
    while (isWeekend(next)) {
        next = next.plusDays(1)
    }
    return next
}

// Usage
val today    = LocalDate.now()
val meetingDate = addWorkingDays(today, 5)
println("Meeting scheduled on: $meetingDate")
// If today is Friday Apr 17 → meeting is Friday Apr 24
// (skips Sat Apr 18 + Sun Apr 19)
```

### ⚠️ Pro Tip — Also skip public holidays

```kotlin
val publicHolidays = listOf(
    LocalDate.of(2026, 1, 26),   // Republic Day
    LocalDate.of(2026, 8, 15),   // Independence Day
)

fun isWorkingDay(date: LocalDate): Boolean {
    return !isWeekend(date) && date !in publicHolidays
}
```

---

---

## 🔷 8. Invoice Due Date Calculator

> **Used in:** Finance apps, billing systems, freelance tools

### 🎯 Problem

Calculate invoice due dates (NET 30, NET 60, end-of-month billing).

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.YearMonth

enum class PaymentTerms { NET_15, NET_30, NET_45, NET_60, END_OF_MONTH }

fun calculateDueDate(invoiceDate: LocalDate, terms: PaymentTerms): LocalDate {
    return when (terms) {
        PaymentTerms.NET_15      -> invoiceDate.plusDays(15)
        PaymentTerms.NET_30      -> invoiceDate.plusDays(30)
        PaymentTerms.NET_45      -> invoiceDate.plusDays(45)
        PaymentTerms.NET_60      -> invoiceDate.plusDays(60)
        PaymentTerms.END_OF_MONTH-> YearMonth.from(invoiceDate).atEndOfMonth()
    }
}

fun isOverdue(dueDate: LocalDate): Boolean {
    return dueDate.isBefore(LocalDate.now())
}

fun getDaysOverdue(dueDate: LocalDate): Long {
    return if (isOverdue(dueDate)) {
        ChronoUnit.DAYS.between(dueDate, LocalDate.now())
    } else 0L
}

// Usage
val invoiceDate = LocalDate.of(2026, 3, 15)
val dueDate     = calculateDueDate(invoiceDate, PaymentTerms.NET_30)
println(dueDate)              // 2026-04-14
println(isOverdue(dueDate))   // true (it's April 17 now)
println("${getDaysOverdue(dueDate)} days overdue")   // 3 days overdue
```

### 🔍 `atEndOfMonth()` use case

```kotlin
// End-of-month billing: invoice raised on any date → due last day of that month
YearMonth.from(LocalDate.of(2026, 3, 7)).atEndOfMonth()   // → 2026-03-31
YearMonth.from(LocalDate.of(2026, 2, 3)).atEndOfMonth()   // → 2026-02-28
```

---

---

## 🔷 9. Date Picker Validation

> **Used in:** Booking apps, travel apps, form validation

### 🎯 Problem

Validate user-selected dates: can't select past dates, max range 30 days, check-out must be after check-in.

### ✅ Solution

```kotlin
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class DateValidationResult {
    object Valid : DateValidationResult()
    data class Invalid(val reason: String) : DateValidationResult()
}

fun validateCheckInDate(selected: LocalDate): DateValidationResult {
    val today = LocalDate.now()
    return when {
        selected.isBefore(today)  ->
            DateValidationResult.Invalid("Cannot select a past date")
        selected.isAfter(today.plusMonths(3)) ->
            DateValidationResult.Invalid("Cannot book more than 3 months in advance")
        else -> DateValidationResult.Valid
    }
}

fun validateDateRange(checkIn: LocalDate, checkOut: LocalDate): DateValidationResult {
    val nights = ChronoUnit.DAYS.between(checkIn, checkOut)
    return when {
        !checkOut.isAfter(checkIn) ->
            DateValidationResult.Invalid("Check-out must be after check-in")
        nights > 30 ->
            DateValidationResult.Invalid("Maximum stay is 30 nights")
        nights < 1  ->
            DateValidationResult.Invalid("Minimum stay is 1 night")
        else -> DateValidationResult.Valid
    }
}

// Usage
val checkIn  = LocalDate.of(2026, 5, 1)
val checkOut = LocalDate.of(2026, 5, 8)

when (val result = validateDateRange(checkIn, checkOut)) {
    is DateValidationResult.Valid   -> println("✅ Dates valid — ${ChronoUnit.DAYS.between(checkIn, checkOut)} nights")
    is DateValidationResult.Invalid -> println("❌ ${result.reason}")
}
```

---

---

## 🔷 10. Time-Ago / Relative Time Labels

> **Used in:** Social feeds, chat apps, notification timestamps

### 🎯 Problem

Convert a past date/time into a human-readable "2 hours ago", "3 days ago", "last month" label.

### ✅ Solution

```kotlin
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun getTimeAgo(pastDateTime: LocalDateTime): String {
    val now     = LocalDateTime.now()
    val seconds = ChronoUnit.SECONDS.between(pastDateTime, now)
    val minutes = ChronoUnit.MINUTES.between(pastDateTime, now)
    val hours   = ChronoUnit.HOURS.between(pastDateTime, now)
    val days    = ChronoUnit.DAYS.between(pastDateTime.toLocalDate(), now.toLocalDate())
    val months  = ChronoUnit.MONTHS.between(pastDateTime, now)
    val years   = ChronoUnit.YEARS.between(pastDateTime, now)

    return when {
        seconds < 60   -> "Just now"
        minutes < 60   -> "$minutes min ago"
        hours   < 24   -> "$hours hour${if (hours > 1) "s" else ""} ago"
        days    < 7    -> "$days day${if (days > 1) "s" else ""} ago"
        days    < 30   -> "${days / 7} week${if (days / 7 > 1) "s" else ""} ago"
        months  < 12   -> "$months month${if (months > 1) "s" else ""} ago"
        else           -> "$years year${if (years > 1) "s" else ""} ago"
    }
}

// Date-only version (for posts, comments)
fun getDateAgo(pastDate: LocalDate): String {
    val today = LocalDate.now()
    val days  = ChronoUnit.DAYS.between(pastDate, today)
    return when {
        days == 0L  -> "Today"
        days == 1L  -> "Yesterday"
        days < 7    -> "$days days ago"
        days < 30   -> "${days / 7} weeks ago"
        else        -> pastDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
}

// Usage
val postTime = LocalDateTime.now().minusHours(3).minusMinutes(20)
println(getTimeAgo(postTime))   // "3 hours ago"

val postDate = LocalDate.now().minusDays(1)
println(getDateAgo(postDate))   // "Yesterday"
```

---

---

## 🧠 Pattern Summary — What API to Reach For

|Real-World Task|API Pattern|
|---|---|
|Grid cell → date mapping|`offset + index math` + `atDay()` + `plusMonths()`|
|Exact age (Y/M/D)|`Period.between(dob, today)`|
|"X days left" countdown|`ChronoUnit.DAYS.between(today, event)`|
|Subscription renewal|`plusMonths(n)` loop|
|Credit card expiry|`YearMonth.isBefore()`|
|Birthday regardless of year|`MonthDay.from(date)`|
|Consecutive streak|`ChronoUnit.DAYS.between()` == 1 check|
|Skip weekends|`dayOfWeek == DayOfWeek.SATURDAY/SUNDAY`|
|End-of-month billing|`YearMonth.from(date).atEndOfMonth()`|
|Stay length validation|`ChronoUnit.DAYS.between(checkIn, checkOut)`|
|"2 hours ago" label|`ChronoUnit.HOURS/DAYS/MONTHS.between()` cascade|

---

## ⚡ Golden Patterns to Memorize

```kotlin
// ① Navigate months safely (no crashes, no wrong dates)
yearMonth.plusMonths(1).atDay(1)          // first day next month
yearMonth.minusMonths(1).atEndOfMonth()   // last day prev month

// ② Day-of-week offset for calendar grid
date.dayOfWeek.value % 7   // Sun=0, Mon=1 … Sat=6

// ③ Total days between two dates
ChronoUnit.DAYS.between(startDate, endDate)

// ④ Broken-down age/period
Period.between(start, end).years   // + .months + .days

// ⑤ Consecutive days check (streak)
ChronoUnit.DAYS.between(date1, date2) == 1L

// ⑥ Skip weekends
date.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

// ⑦ Last day of any month (even Feb leap)
YearMonth.from(date).atEndOfMonth()

// ⑧ Is past / future / today
date.isBefore(LocalDate.now())
date.isAfter(LocalDate.now())
date == LocalDate.now()

// ⑨ Birthday match any year
MonthDay.from(birthday) == MonthDay.from(LocalDate.now())

// ⑩ Relative time label order
seconds → minutes → hours → days → weeks → months → years
```

---

_Chapter 2 — Real-Life Examples | Generated: April 17, 2026_ _← Chapter 1: Kotlin_DateTime_API_Notes.md_