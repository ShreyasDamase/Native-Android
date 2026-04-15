Good. The foundation is solid. Let me give you the complete picture of how real-time sync is actually done at an industrial level — without WebSockets, without constant polling abuse.

---

## The Core Problem You Described

You have the same post (feed ID: `FID-123`) appearing in:

- Home feed
- Club feed
- User profile feed

User A likes it on Phone 1. User B is watching it on Phone 2. Both need to see the updated like count — fast, without either phone hammering the backend every 5 seconds forever.

---

## The Industrial Pattern: SSE (Server-Sent Events)

This is exactly what Uber, LinkedIn, and similar apps use for lightweight real-time. Not WebSocket. Not polling. **SSE.**

Here's the mental model:

```
WebSocket  = two-way pipe  (heavy, stateful, both sides talk)
SSE        = one-way pipe  (server pushes, client only listens)
Polling    = client knocks every N seconds regardless of changes
```

Your use case — like count updates, comment count updates, new comments appearing — is **read-only from the client's perspective**. The client never needs to push data through the real-time channel. It only needs to _receive_ updates. SSE is a perfect fit.

### How SSE works technically

```
Phone opens:  GET /api/feeds/FID-123/live
              Connection stays open (HTTP long-lived)
              
Backend sends: data: {"likeCount": 42, "commentCount": 7}\n\n
Backend sends: data: {"likeCount": 43, "commentCount": 7}\n\n
Backend sends: data: {"commentCount": 8}\n\n

Phone closes app → connection drops automatically
```

One persistent HTTP connection per post the user is currently viewing. When they navigate away, Android closes it. No resources wasted.

### vs Uber's model you mentioned

Uber does exactly this — driver location is SSE from server → rider app. The rider's app opens one SSE connection to receive location pings. The driver's app sends location via normal HTTP POST. Backend fans it out through SSE to the rider. That's the whole system.

Your system is the same pattern: User A's like action → normal HTTP POST → backend updates DB → fans out through SSE to anyone subscribed to that post ID.

---

## The Full Architecture

```
User A (Phone 1)                    Backend                      User B (Phone 2)
─────────────────                  ─────────────────             ─────────────────
                                   
[Viewing FID-123]                                               [Viewing FID-123]
                                                                
                              ← SSE connected to FID-123 ──────── opens SSE stream
                                   
POST /feeds/FID-123/like ──────────→                           
                                   1. Write like to DB         
                                   2. Update like count in Redis
                                   3. Push to SSE subscribers  ──→ likeCount: 43
                                   4. Return 200 to Phone 1    
← 200 OK, {likeCount: 43}                                          
[UI updates immediately            
 from API response, not SSE]                                   [UI updates from SSE push]
```

Key insight: **the actor (Phone 1) doesn't need SSE** — they already get the updated count in the API response. SSE is only for observers (Phone 2) who didn't trigger the action.

---

## Android Side: How Room + SSE Fit Together

This answers your original question about Room DB and cross-screen sync.

### The single source of truth pattern

```
SSE update arrives
       ↓
Write to Room DB (update PostEntity where id = FID-123)
       ↓
Room emits Flow automatically to ALL collectors
       ↓
Home feed screen (collecting) → UI updates
Club feed screen (collecting) → UI updates  
Profile feed screen (collecting) → UI updates
```

You write to Room **once**. Every screen that has that post collected updates automatically. This is why Room is the right tool — it's an observable database. You never manually notify screens.

```kotlin
// ViewModel for Home Feed
val posts = postDao.getHomeFeed().stateIn(viewModelScope, ...)

// ViewModel for Club Feed  
val posts = postDao.getClubFeed(clubId).stateIn(viewModelScope, ...)

// Both DAOs query Room. When SSE updates PostEntity FID-123 in Room,
// BOTH flows re-emit with the updated data. Zero extra code.
```

### What to store in Room

Store only flat, primitive fields. No nested lists.

```kotlin
@Entity
data class PostEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val likeCount: Int,        // ← SSE updates this
    val commentCount: Int,     // ← SSE updates this  
    val bookmarkCount: Int,
    val isLikedByMe: Boolean,  // ← optimistic update on like action
    val isBookmarkedByMe: Boolean,
    val createdAt: Long,
    val feedType: String       // "HOME", "CLUB", "PROFILE" — for filtering
)
```

No `List<Comment>` inside PostEntity. Comments are a separate table, loaded separately when user opens the post detail screen.

---

## Stale Data Problem: The Two Scenarios

### Scenario 1 — User is actively viewing the post

SSE connection is open. Updates arrive in real-time. Room gets written. UI updates. Done.

### Scenario 2 — User returns to the app after being away

This is where SSE alone isn't enough. When the user comes back (app foreground):

```kotlin
// In your ViewModel or Repository
fun onAppForegrounded() {
    // Re-fetch the posts currently visible on screen
    // This is a normal HTTP GET, not SSE
    viewModelScope.launch {
        val freshPosts = api.getHomeFeed(page = 0)
        postDao.upsertAll(freshPosts)  // Room updates, Flow re-emits
    }
}
```

This is the standard pattern. Instagram does this. LinkedIn does this. One refresh on foreground, then SSE takes over for live updates while the user is active.

---

## The Moderation/Pending Post Problem

You asked: post is under moderation. No one should trigger API calls during that period. When it goes live, the author should be notified.

**Answer: FCM for this specific case.**

SSE is for "user is actively watching this post right now." FCM is for "something important happened while you weren't looking." These are different problems.

```
Post submitted → status: PENDING
       ↓
Moderator approves → status: PUBLISHED
       ↓
Backend sends FCM to post author: "Your post is live!"
       ↓
User taps notification → app opens → fetches fresh data
```

No polling anywhere. No wasted connections. The backend triggers the FCM push exactly once when the state changes. FCM's unreliability doesn't matter here because it's a notification, not a data sync mechanism — if the user misses the push, they'll see it published when they open the app anyway.

---

## Handling FCM Unreliability (Your Concern)

You're right that FCM can be cancelled by the OS or dropped under load. The solution is to never treat FCM as the data carrier — only treat it as a **wake-up signal**.

```
FCM payload: { "type": "POST_UPDATE", "postId": "FID-123" }
       ↓
Android receives it
       ↓
App makes a fresh HTTP GET /feeds/FID-123
       ↓
Got the real data from backend (reliable HTTP)
       ↓
Update Room → Flow re-emits → UI updates
```

FCM just says "hey, go check." The actual data comes from a regular API call. If FCM drops, user just sees stale data until next app open — which is acceptable.

---

## Concurrency: Preventing Data Overwrites

Your concern: two users interact simultaneously. Their updates shouldn't overwrite each other.

On the backend, like counts should never be set directly:

```sql
-- WRONG: race condition
UPDATE posts SET like_count = 43 WHERE id = 'FID-123'

-- RIGHT: atomic increment
UPDATE posts SET like_count = like_count + 1 WHERE id = 'FID-123'
```

In Spring:

```kotlin
@Modifying
@Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
fun incrementLikeCount(id: String)
```

The database handles the race. Both increments are applied. No count is lost.

For comments, each comment is its own row insert — no conflict possible.

---

## What to Build, In Order

Given your app is not grown yet, here's the pragmatic sequence:

**Phase 1 (now):** Room as single source of truth + optimistic updates on like/bookmark. No real-time at all. User sees their own action immediately (optimistic), and sees others' changes on next app open or manual refresh.

**Phase 2 (when you have active users on same post):** Add SSE endpoint for post detail screen only — the screen where both users are most likely viewing the same post simultaneously.

**Phase 3 (when you have scale):** Add Redis pub/sub between your backend instances so SSE works correctly when you have multiple servers. Without this, User A on Server 1 and User B on Server 2 won't see each other's updates through SSE.

Starting with Phase 1 means you ship faster, the architecture is already correct (Room as source of truth), and you add SSE on top without changing anything — just add the write path into Room.