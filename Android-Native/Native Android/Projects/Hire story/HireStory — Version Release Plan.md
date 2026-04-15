# HireStory — Version Release Plan

> Ship fast. Validate. Add features. Never build everything at once.

---

## The Core Principle

**Get something real in users' hands as fast as possible.** Each version is a complete working product — not a half-built app. Every version after v1 adds one layer of value.

---

## What Makes HireStory Different From DevByte

DevByte → swipe through developer news articles → tap to read on external website HireStory → swipe through real interview experiences → tap to read full story inside app

DevByte uses a bottom navigation bar. HireStory removes it entirely and replaces with a floating radial FAB — one button that gives access to everything from anywhere. This is the unique navigation contribution on top of the DevByte format.

---

## Navigation Architecture — Applies To All Versions

### Shorts IS the home screen

App opens directly into Shorts. No home tab. No feed as default. Shorts is the root. Identical to how TikTok opens directly to the feed.

### No bottom navigation bar — ever

Not on Shorts. Not on Feed. Not on any screen in any version.

### The Floating Radial FAB

One purple ☰ button. Bottom-right corner. Present on every screen in every version.

```
Tap ☰ → background dims to rgba(0,0,0,0.72)
5 options arc toward top-left:

  ◯ Profile          ← 7 o'clock
  ✏ Add Interview    ← 8 o'clock
  🔖 Saved           ← 9 o'clock
  ⊞ Companies        ← 10 o'clock
  ⊟ Feed             ← 11 o'clock

Tap option → navigate, fan closes
Tap background → fan closes
☰ becomes ✕ when open
Options stagger in 50ms apart — each pops from FAB outward
```

### Back arrow rule

Back arrow top-left appears ONLY when user arrived from a notification or deep link. When user opens app fresh → no back arrow. Shorts is root. Nowhere to go back to.

### Shorts screen header

```
← (only if deep link)    HIRESTORY DISCOVER    ↗
```

Center pill label "HIRESTORY DISCOVER" is always visible. Back arrow only appears when needed.

---

## Version Overview

|Version|What It Is|Key Addition|
|---|---|---|
|v1.0|Foundation|Feed + manually seeded data — first real users|
|v1.1|Discovery|Full-text search + company pages|
|v1.2|Community|Comments + referral system + read limit|
|v1.3|Automation|Crawler + AI — app feeds itself forever|
|v2.0|Growth|Shorts as home + radial FAB + notifications|
|v2.1|Monetisation|RevenueCat subscription ₹199/month|
|v2.2|Web|Next.js SEO pages — Google sends free users|
|v3.0|Intelligence|Personalisation + semantic search + recommendations|
|v3.1|iOS|SwiftUI app on App Store|

---

## v1.0 — Foundation

> First version in production. Feed is the entry point (Shorts comes in v2.0). Manually seeded data.

### What ships

- Spring Boot backend live on Railway
- PostgreSQL with all tables
- 50+ manually seeded real interviews (copy from Reddit and GFG manually)
- Android app — Login, Feed screen, Interview Detail, Companies, Saved, Profile
- Standard nav bar in v1.0 only — temporary, removed in v2.0 when Shorts ships
- Clerk auth — Google login + email login
- Basic feed filtering — company and difficulty
- Bookmark interviews
- Submit interview — goes to PENDING, you approve via Retool
- Admin panel in Retool — approve and reject submissions
- Firebase Analytics + Crashlytics

### What is NOT in v1.0

- No Shorts screen (comes v2.0 — radial FAB and no nav bar come with it)
- No crawler (you seed manually)
- No notifications
- No comments
- No referrals
- No search
- No paywall
- No iOS
- No web

### Note on navigation in v1.0

v1.0 uses a temporary standard bottom nav bar — Home, Companies, Add, Saved, Profile. This gets completely removed in v2.0 when Shorts ships and the radial FAB replaces everything. Do not over-engineer the navigation in v1.0. It is temporary scaffolding.

### Backend phases needed

- Phase 0 — Setup
- Phase 1 — Database schema
- Phase 2 — Core REST API (companies, feed, submit)
- Phase 3 — Authentication
- Phase 9 — Admin panel (Retool)
- Phase 10 — Deploy

### Android phases needed

- Phase 11 — KMP shared module (domain, Ktor, Room, Koin, ViewModels)
- Phase 12 Step 1 — Auth screens
- Phase 12 Step 2 — Home feed
- Phase 12 Step 3 — Interview Detail
- Phase 12 Step 6 — Companies, Saved, Profile (basic)
- Phase 12 Step 7 — Add Interview form
- Phase 12 Step 9 — Firebase Analytics + Crashlytics

### Checklist

- [ ] Backend deployed on Railway and running
- [ ] 50 real interviews manually seeded in database
- [ ] Android app installable via internal APK link
- [ ] Google login works end to end
- [ ] Feed loads real interviews with filters
- [ ] Interview detail opens with full content and rounds
- [ ] Bookmark saves and persists across sessions
- [ ] Submit interview form reaches Retool queue
- [ ] Firebase Analytics events visible in dashboard
- [ ] Crashlytics active and user ID set after login
- [ ] Tested by at least 5 real people — feedback collected

### Checkpoint

Real people using real app with real data. You are getting feedback before building more.

---

## v1.1 — Discovery

> Users can find specific interviews. Companies get their own pages.

### What ships on top of v1.0

- Full-text search across company, role, and interview content
- Search suggestions as you type
- Recent searches saved locally
- Company detail page — all interviews for one company
- Interview count badge per company on grid
- Filter by outcome — Offer Received / Rejected
- Trending companies section on home feed

### Backend phases needed

- Phase 2 Step 3 — PostgreSQL full-text search with tsvector and GIN index

### Android phases needed

- Phase 12 Step 6 — Search screen
- Companies screen update — tap company → company interview list

### Checklist

- [ ] Search returns relevant results for company name, role, keywords
- [ ] Suggestions appear while typing
- [ ] Company detail page shows all interviews for that company
- [ ] Filter by outcome working
- [ ] Trending section visible on home feed

---

## v1.2 — Community

> Users interact. Referral system drives growth. Read limit introduced.

### What ships on top of v1.1

- Comments on interview detail
- Report a comment
- Referral system — share code → both users unlock +10 reads
- Referral code visible in Profile
- Read limit — 25 free reads per month tracked in Redis
- Soft warning banner at 20 reads — "5 reads remaining this month"
- Paywall placeholder at 25 reads — wall shown, no actual payment yet
- User preferences screen on first login — target companies, role, experience level, interview timeline

### Backend phases needed

- Phase 4 — User features (comments, referrals, preferences)
- Phase 5 — Redis caching (read counter specifically)

### Android phases needed

- Phase 12 Step 3 — Comments section in Interview Detail
- Phase 12 Step 6 — Profile referral card
- Onboarding preferences screen after login
- Paywall placeholder screen

### Checklist

- [ ] Comments visible and postable on interview detail
- [ ] Referral code visible in profile
- [ ] Applying referral code adds +10 reads to both users
- [ ] Read counter incrementing correctly — check Redis
- [ ] Warning banner appears at 20 reads
- [ ] Paywall screen shows at 25 reads
- [ ] Preferences screen appears on first login
- [ ] Preferences saved and persisted

---

## v1.3 — Automation

> The crawler goes live. HireStory starts feeding itself forever.

### What ships on top of v1.2

- Web crawler running every 6 hours — Reddit, GeeksForGeeks, Medium blogs
- Spring AI + GPT-4o Mini extracts structured data from raw text
- Auto-publish interviews with confidence score above 80
- Admin review queue for confidence score 50–79
- "Sourced from glassdoor.com" badge on externally sourced interviews
- "User submitted" badge on user-created interviews
- Content grows without any manual input from you

### Backend phases needed

- Phase 5 — Redis URL deduplication keys
- Phase 7 — Web crawler (Jsoup + Spring Scheduler)
- Phase 8 — AI extraction agent (Spring AI + GPT-4o Mini Batch API)

### Android phases needed

- External source banner on Interview Detail — already designed in Stitch

### Checklist

- [ ] Crawler runs on schedule — verified in Railway logs
- [ ] New interviews appear in feed without manual seeding
- [ ] Confidence scores visible in Retool for every crawled interview
- [ ] Auto-published interviews look clean and correctly extracted
- [ ] Source badge showing correctly on crawled interviews
- [ ] Duplicate detection working — same interview from Reddit and GFG appears once only
- [ ] AI extraction cost under $1 for first 1000 interviews — verify in OpenAI dashboard

### This is the version that changes everything

After v1.3 the content engine is running. You stop thinking about data and start thinking about product.

---

## v2.0 — Growth

> Shorts becomes the home. Radial FAB replaces nav bar. Notifications bring users back.

### The big change in v2.0

This is the version where the app becomes what it was always designed to be.

- Shorts replaces the feed as the home screen — app opens directly into full-screen swipe
- Bottom nav bar is completely removed
- Radial FAB is introduced — the floating menu accessible from everywhere
- The "HIRESTORY DISCOVER" header label appears on Shorts

### What ships on top of v1.3

- Shorts screen — full screen vertical swipe, one interview per screen
- App opens directly into Shorts — no feed as default
- Radial FAB — ☰ button bottom-right, opens to Feed / Companies / Saved / Add Interview / Profile
- No bottom navigation bar on any screen
- Push notifications — FCM on Android
    - Someone comments on your interview
    - Your submitted interview was published
    - Someone used your referral code
    - New interview from a company you follow
- Onboarding notification sequence — Day 1, Day 3, Day 7
- Feed personalisation — uses preferences from v1.2
- View tracking on Shorts — API call after 2 seconds on a card

### Backend phases needed

- Phase 6 — Notifications (FCM, Firebase Admin SDK, RabbitMQ notification queue)
- Feed personalisation logic (Phase 4 Step 4)
- Shorts view tracking endpoint

### Android phases needed

- Phase 12 Step 4 — Radial FAB with stagger animation
- Phase 12 Step 5 — Shorts screen (VerticalPager, full screen, preload 2 ahead)
- Phase 12 Step 8 — Offline mode
- Phase 12 Step 9 — FCM notification handling
- Remove bottom nav bar — replace all navigation with radial FAB

### Checklist

- [ ] App opens directly to Shorts — no feed default
- [ ] Swipe up — next interview. Swipe down — previous.
- [ ] "HIRESTORY DISCOVER" pill label visible in header
- [ ] No bottom nav bar on any screen
- [ ] Radial FAB opens with stagger animation
- [ ] Each option in FAB navigates correctly
- [ ] FAB present on Shorts, Feed, Companies, Detail, Profile — every screen
- [ ] Push notification received on device
- [ ] Tapping notification opens correct interview
- [ ] View tracking API called after 2 seconds on Shorts card
- [ ] Feed results personalised based on preferences

---

## v2.1 — Monetisation

> Real payments. Premium unlocks unlimited reading.

### What ships on top of v2.0

- RevenueCat integration — monthly subscription ₹199
- Proper paywall screen replacing the placeholder from v1.2
- Premium badge on profile
- Unlimited reads for premium users — no counter check
- Subscription management — cancel, restore purchase
- Premium features: unlimited reads, advanced filters, salary data visible

### Backend phases needed

- RevenueCat webhook endpoint — receives subscription events
- Backend sets `is_premium = true` in users table on subscription
- Backend sets `is_premium = false` on cancellation after billing period

### Android phases needed

- RevenueCat Android SDK
- Full paywall screen with subscription details and purchase flow
- Premium badge in Profile
- Remove read limit check for premium users

### Checklist

- [ ] Purchase flow completes with Google Play test card
- [ ] `is_premium` updates in database after successful purchase — verify in Retool
- [ ] Paywall no longer shows for premium users
- [ ] Subscription visible in Google Play account subscriptions
- [ ] Cancellation handled correctly — premium access ends after current billing period

---

## v2.2 — Web

> SEO pages live. Google starts sending organic users forever.

### What ships on top of v2.1

- Next.js 14 web app deployed on Vercel
- Public SEO pages:
    - `hirestory.com/interview/[id]` — full interview page
    - `hirestory.com/company/[slug]` — all interviews for a company
    - `hirestory.com/` — homepage with featured interviews
- Dynamic OG image per interview — shows when shared on WhatsApp and LinkedIn
- Sitemap auto-generated and submitted to Google Search Console
- "Download App" banner on mobile web — links to Play Store
- Clerk auth on web — users can log in and bookmark from browser

### Why this matters

Every interview becomes a URL Google can index. `hirestory.com/interview/google-sde-bangalore-2024` Someone searches "Google SDE 1 interview experience India" — they land on your page. This is free users forever. No ad spend needed.

### Backend phases needed

- Nothing new — same API already built

### Web phases needed

- Phase 14 — Next.js web (interview page first, then company, then homepage)

### Checklist

- [ ] `hirestory.com/interview/[any-valid-id]` loads real content
- [ ] OG image shows correctly when URL pasted into WhatsApp
- [ ] Google Search Console shows pages being indexed
- [ ] Sitemap submitted and accepted
- [ ] "Download App" banner visible on mobile browser
- [ ] Web login works via Clerk

---

## v3.0 — Intelligence

> The app learns from behaviour. Recommendations feel personalised.

### What ships on top of v2.2

- pgvector embeddings generated for every interview
- "Similar interviews" section at bottom of Interview Detail
- "Because you read Google SDE" personalised recommendation row in Feed
- Trending interviews this week — based on real view data
- Semantic search — "tough coding rounds at FAANG" finds relevant results beyond exact keywords
- Company follow feature — follow Google → get notification when new interview added
- Read history directly drives feed sort order

### Backend phases needed

- pgvector extension enabled on Railway PostgreSQL
- Embedding generation job — runs after each new interview published
- Cosine similarity endpoint — `GET /api/interviews/{id}/similar`
- User preference vector updated as they read

### Checklist

- [ ] Similar interviews section visible on interview detail
- [ ] Semantic search returning relevant results that keyword search would miss
- [ ] Feed order visibly influenced by read history — different for different users
- [ ] Follow company button working on company screen
- [ ] Notification received when followed company gets new interview

---

## v3.1 — iOS

> Same app on iPhone. Shared logic from KMP, new SwiftUI UI.

### What ships

- iOS app on App Store
- All features matching Android v2.1 at minimum
- SwiftUI screens consuming shared KMP ViewModels
- APNs push notifications
- RevenueCat iOS in-app purchase
- Shorts as home screen — same swipe experience on iOS

### KMP phases needed

- Phase 13 — iOS SwiftUI UI (all screens)

### Checklist

- [ ] iOS app on TestFlight — internal testing with 5 people
- [ ] All screens match Android feature parity
- [ ] iOS push notifications working via APNs
- [ ] In-app purchase working on iOS via RevenueCat
- [ ] Shorts swipe experience smooth on iPhone
- [ ] App Store review approved

---

## Git Strategy

### Branches

```
main        → always production. What is live on Railway and Play Store.
develop     → integration. All features merge here first.
feature/    → one branch per feature.

Example flow:
feature/shorts-screen → develop → QA → main → tag v2.0.0
```

### Version tags

```
v1.0.0  → first production release
v1.0.1  → hotfix on v1.0
v1.1.0  → Search and Discovery ships
v1.2.0  → Community ships
v1.3.0  → Automation ships
v2.0.0  → Shorts + radial FAB + notifications
v2.1.0  → Monetisation
v2.2.0  → Web
v3.0.0  → Intelligence
v3.1.0  → iOS
```

Every Play Store release = one Git tag. No exceptions.

---

## Production Status Per Version

|Version|Backend|Android|iOS|Web|
|---|---|---|---|---|
|v1.0|Railway live|Internal APK|—|—|
|v1.1|Railway live|Play Store closed testing|—|—|
|v1.2|Railway live|Play Store open testing|—|—|
|v1.3|Railway live|Play Store production|—|—|
|v2.0|Railway live|Play Store update|—|—|
|v2.1|Railway live|Play Store update|—|—|
|v2.2|Railway live|Play Store update|—|Vercel live|
|v3.0|Railway live|Play Store update|—|Vercel update|
|v3.1|Railway live|Play Store update|App Store live|Vercel update|

---

## Done Criteria — Every Version Must Pass This

Before marking any version complete and moving to next:

- [ ] App starts without crashing
- [ ] Core feature of this version works end to end on a real device
- [ ] At least 3 real people have tested it and given feedback
- [ ] Firebase Analytics shows events coming in correctly
- [ ] No known critical bug exists
- [ ] Git tag created for this version
- [ ] Railway deployment stable — no restarts in last 24 hours

---

## The Version That Changes Everything

**v1.3** is the turning point.

Before v1.3 — you are manually adding interviews. After v1.3 — the crawler runs every 6 hours, GPT-4o Mini extracts and publishes automatically. HireStory becomes a self-sustaining content platform.

**v2.0** is when the product becomes what it was always meant to be.

Before v2.0 — feed-first app with a nav bar like every other app. After v2.0 — Shorts is home, radial FAB is navigation, no nav bar anywhere. HireStory looks and feels like nothing else in the developer tools space.

---

> Ship v1.0 first. Everything else is an upgrade. The world gets beaten one version at a time.