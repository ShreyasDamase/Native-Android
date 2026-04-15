# HireStory Versioned Product + Schema Roadmap

## Summary
Plan HireStory as a **backend-first private beta** with a soft premium wall, strong trust foundations, and clear separation between the main app and future ingestion/intelligence systems.  
The implementation order should be:

1. **V1**: stable core product and premium-ready foundation  
2. **V2**: trust, moderation, rewards, and referral loops  
3. **V3**: AI and recommendation intelligence  
4. **V4**: scraping/crawling supply engine and scale infra  
5. **V5**: moat features and organic growth loops

The key rule is: **do not build systems before the data and product loop justify them**.

## Version Plan

### V1 — Core Beta Foundation
Goal: launch a useful private beta that proves users can discover, read, submit, and return.

**Backend**
- Finish Clerk auth end-to-end, including local user creation/sync on first authenticated request.
- Stabilize user profile + onboarding storage around:
  - target companies
  - role
  - experience level
  - interview timeline
- Finalize APIs for:
  - feed
  - company page
  - search
  - interview detail
  - submit interview
  - bookmarks
  - comments
  - profile/preferences
- Keep moderation simple:
  - `PENDING_REVIEW`
  - `PUBLISHED`
  - `NEEDS_EDIT`
- Keep the current free-tier read-limit model, but make it a **soft wall**:
  - browsing/search remains open
  - detailed reads are limited
  - advanced tools are premium
- Add the **schema foundation** now for future points and premium, even if the full reward engine is not yet exposed:
  - reward transaction ledger
  - reward balance or computable aggregate
  - subscription/premium entitlement record
  - redemption record
- Keep premium simple in V1:
  - monthly plan
  - stable pricing
  - point redemption allowed
- Do not build recommendation, crawler automation, Redis, or AI bot in V1.

**Frontend**
- Sign in / sign up
- Onboarding
- Feed
- Search
- Company page
- Interview detail
- Submit interview flow
- Bookmarks/comments/profile
- Premium upsell surfaces
- Point balance display
- Soft review-state UX for submitted interviews

**Schema to design in V1**
Design now, even if partially used later:
- `reward_events` or `points_ledger`
- `premium_entitlements`
- `premium_redemptions`
- optional `referral_events` if you want referral lifecycle visibility
- contributor-facing moderation status fields
- trust/review metadata placeholders on interview records

This avoids painful schema redo later.

---

### V2 — Trust, Moderation, Rewards, and Growth
Goal: make the platform trustworthy, fair, and habit-forming.

**Backend**
- Introduce **reward engine** fully:
  - approved interview rewards
  - exceptional interview bonus
  - verified referral rewards
  - useful comment rewards
  - one-time profile enrichment reward later
- Since you chose **100% premium unlock**, allow full subscription payment from points, but protect it with strict validation and anti-abuse rules.
- Add **anti-abuse logic**:
  - no reward for raw app opens or idle activity
  - rate limits for comments/submissions/referrals
  - fraud heuristics for repeat low-quality content
  - referral activation requirements before reward
- Introduce **quality scoring** for submitted interviews:
  - structure completeness
  - specificity
  - content richness
  - spam suspicion
- Introduce **duplicate detection v1**:
  - metadata similarity
  - text similarity
  - same-user repeated submissions
- Add contributor trust score:
  - low-risk contributors move faster through review
  - suspicious contributors face slower/manual review
- Add moderation/admin tools and reviewer states.
- Introduce LinkedIn account linking only if needed for trust/profile enrichment, not before reward abuse is under control.

**Frontend**
- Reward history and point earning breakdown
- Premium redemption UX
- Trust badges
- Review-state animation/status screens
- Better contributor profiles
- Referral flow
- LinkedIn linking UI if included in this phase

**Schema to implement in V2**
- interview quality score
- duplicate suspicion score / matched interview reference
- contributor trust score
- moderation audit fields
- referral state machine fields
- reward reason/type enums
- premium redemption status

This is the right phase to finalize reward and trust schemas because V1 should not overfit before real usage.

---

### V3 — Intelligence Layer
Goal: make premium feel meaningfully smarter.

**Backend**
- Introduce recommendations only **after enough real user behavior + content volume exists**.
- First recommendation version should be:
  - onboarding-aware ranking
  - history/bookmark/read-aware ranking
  - not a full ML system yet
- Add AI premium features:
  - summarize one interview
  - summarize a set of interviews
  - compare interview patterns
  - Q&A over selected interview context
- Add AI quota model:
  - monthly premium quota
  - optional very small free trial quota or none
  - token or request accounting
- Add premium bot upsell when usage is exhausted.
- Introduce Redis only if one of these is now clearly real:
  - feed caching
  - recommendation cache
  - quota/rate-limit coordination
  - job queue support

**Frontend**
- AI summary actions
- AI assistant/chat UI
- recommendation surfaces
- premium AI quota indicators
- AI upsell flow

**Schema to implement in V3**
- AI usage ledger / quota counters
- recommendation feedback signals
- interaction events needed for ranking
- cached/generated summary records if you want persistence

Do not build recommendation or AI before V2 quality/trust systems exist; otherwise you amplify bad data.

---

### V4 — Scraping/Crawling and Supply Expansion
Goal: expand content supply safely without burdening the main app runtime.

**Backend / Services**
- Introduce scraping/crawling as a **separate service**, not inside the main user-facing backend process.
- The main app remains responsible for:
  - user APIs
  - premium/rewards
  - trust/review state
  - content serving
- The crawler service is responsible for:
  - source fetch
  - extraction
  - normalization
  - candidate interview creation
  - confidence scoring
  - duplicate matching against app content
- Reuse or expand the existing `crawl_jobs` model, but move job execution out of the main app.
- Add ingestion pipeline states:
  - fetched
  - parsed
  - extracted
  - confidence scored
  - duplicate checked
  - queued for review
  - imported / rejected

**Why here, not earlier**
- In V1/V2, content quality and product loop matter more than content volume.
- Scraping too early increases noise, moderation burden, and system complexity.
- Once trust/reward/moderation are stable, ingestion becomes valuable instead of dangerous.

**Schema to implement in V4**
- crawl source registry if needed
- expanded crawl job lifecycle
- extracted candidate content records if needed
- source confidence and dedup references
- ingestion review linkages

---

### V5 — Moat and Organic Growth
Goal: make HireStory naturally recommendable.

**Backend**
- community/campus loops
- contributor recognition systems
- richer trust signals
- share tracking
- maybe company insight layers later

**Frontend**
- shareable pages
- richer public reputation
- community-facing loops
- recognition/badges that feel meaningful

This phase is where the product becomes socially defensible, but only after usefulness and trust are already proven.

## Free vs Premium Boundary
**Free should provide**
- open browsing
- open search and previews
- enough detailed reads to understand value
- company discovery
- interview submission
- comments/bookmarks with some limits
- points earning

**Premium should provide**
- unlimited or much higher detailed reads
- advanced filters
- deeper personalization
- AI assistance later
- stronger discovery leverage
- convenience and speed, not artificial frustration

**Default chosen**
- Keep a **soft wall**, not a hard lock after light usage.

## Tests and Acceptance Criteria
- Auth: first Clerk login creates/fetches local user correctly.
- Free wall: a free user can browse/search broadly without early frustration.
- Premium wall: detailed read limits and premium unlocks enforce correctly.
- Rewards: no points are granted for raw activity; only validated events create ledger records.
- Redemption: a user can fully unlock premium with points, but only through server-side validated balance and rules.
- Duplicate/moderation: suspicious interviews are held/reviewed, not bluntly discarded.
- Crawler boundary: when introduced, crawl execution never blocks user-facing request paths.
- Recommendations/AI: only enabled once enough source data and interaction data exist.

## Assumptions and Defaults
- V1 target is a **private beta**, not a wide public launch.
- Backend is the priority; frontend proceeds in parallel after API contracts stabilize.
- Scraping/crawling is introduced as a **separate service**, not inside the main backend runtime.
- Recommendation system starts **after enough real data exists**, not in V1.
- Rewards can eventually cover **100% of premium cost**, so anti-abuse design is mandatory.
- LinkedIn is optional and belongs after trust/reward basics, not before them.
- Redis is not introduced until a specific bottleneck justifies it.

