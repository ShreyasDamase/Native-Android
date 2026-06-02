# P0-14 — Quick Commerce System Architecture: Dark Stores, Delivery Model & Apps

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

> **Context**: Quick commerce is the most engineering-intensive form of e-commerce. The 10-minute delivery promise is not a marketing slogan — it's an end-to-end systems engineering constraint that propagates through every layer of the stack. This chapter studies how Blinkit, Zepto, and Swiggy Instamart actually build these systems.

---

## What Is Quick Commerce — The Engineering Contract

Quick commerce is NOT just fast delivery. It is a **fundamentally different operational model** from traditional e-commerce:

| Property | Traditional E-commerce (Amazon) | Quick Commerce (Zepto/Blinkit) |
|----------|--------------------------------|-------------------------------|
| Warehouse size | 200,000+ sqft | 1,500–3,000 sqft |
| SKU count | Millions | 2,000–5,000 per dark store |
| Delivery time | 1–7 days | 8–15 minutes |
| Delivery radius | Nationwide | 1.5–3 km |
| Inventory model | Centralized, optimized for cost | Hyperlocal, optimized for speed |
| Order consolidation | Batch picking | Single-order picking |
| Delivery partner model | Last-mile courier | Gig workers, pre-positioned |

**The engineering contract**: An order placed at 10:00:00 AM must be at the customer's door by 10:10:00 AM. That 10 minutes must be engineered, not hoped for.

### The 10-minute breakdown:

```
T+0:00  — Customer places order
T+0:03  — Order reaches picker's device (order routing + push notification)
T+0:30  — Picker acknowledges order
T+3:00  — All items picked from shelves (avg 8-12 items)
T+4:00  — Order packed, tagged, placed at dispatch counter
T+4:30  — Delivery partner receives assignment on their device
T+5:00  — Partner picks up package from store
T+10:00 — Package at customer's door (0.5–1.5 km away at ~15 km/h on e-bike)
```

Every step has a hard deadline. Missing one cascades into SLA breach.

---

## Dark Store Architecture — The Physical Foundation

### What is a Dark Store?

A dark store is a **micro-fulfillment center** — a small warehouse optimized for fast order picking, with zero customer foot traffic. "Dark" because customers never enter.

**Physical layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│                      DARK STORE (~2000 sqft)                     │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ Dry goods│  │  Snacks  │  │ Personal │  │ Dairy/   │        │
│  │ (Aisle 1)│  │ (Aisle 2)│  │  care    │  │ Frozen   │        │
│  │          │  │          │  │ (Aisle 3)│  │ (Cold)   │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                   │
│  ┌──────────────────────────────┐  ┌────────────────────────┐   │
│  │   PICKER STAGING AREA        │  │   PACKING COUNTER      │   │
│  │   (Order bins per aisle)     │  │   (Thermal bags, tape) │   │
│  └──────────────────────────────┘  └────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │               DISPATCH ZONE                               │   │
│  │   (Shelves labeled by order# — delivery partners pick up) │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────┐  ┌─────────────────┐                        │
│  │   STORE MGMT   │  │  WIFI ROUTER +  │                        │
│  │   TABLET       │  │  BACKUP 4G      │ (Critical: dual conn)  │
│  └────────────────┘  └─────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

**Key design decisions:**
- **Fixed slot storage**: Every SKU has a fixed shelf location (like position A3-07). No dynamic slotting. This means pickers memorize the layout in 2 days.
- **Heat-map slotting**: High-velocity items (Maggi, Coca-Cola, eggs) placed nearest to packing counter. Cuts pick time by 40%.
- **Dual connectivity**: WIFI for normal ops + 4G fallback. If the picker's device loses connection, the 10-minute clock keeps running.

---

## Delivery Radius Calculation — Haversine Formula

The foundation of dark store assignment: which store is closest to this delivery address?

```kotlin
package com.yourcompany.geoservice.service

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Haversine formula: calculates great-circle distance between two coordinates.
     *
     * This gives straight-line distance (as the crow flies), NOT road distance.
     * For delivery radius calculations, straight-line is used as the first filter.
     * Actual routing (road distance, traffic) is calculated separately via Google
     * Maps Distance Matrix API or OSRM for final ETA.
     *
     * Performance: O(1), ~1μs per calculation. Safe to call for every incoming order.
     */
    fun haversineDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Is the delivery point within a dark store's service radius?
     *
     * Zepto uses a polygon-based service area (not a perfect circle) because
     * road networks aren't circular — a river or highway might cut off a "nearby"
     * area. The polygon is defined by the ops team per dark store.
     *
     * Simple implementation: circular radius. Production: use PostGIS polygon.
     */
    fun isInServiceRadius(
        storeLat: Double, storeLon: Double,
        deliveryLat: Double, deliveryLon: Double,
        radiusKm: Double = 2.5
    ): Boolean {
        return haversineDistanceKm(storeLat, storeLon, deliveryLat, deliveryLon) <= radiusKm
    }
}

/**
 * Geohashing for zone matching.
 *
 * Problem: "Is this point in this zone?" is a geometric operation.
 * At scale (1000 orders/min), running PostGIS polygon checks for every order
 * is expensive.
 *
 * Geohash solution:
 * - Divide the map into a grid of cells (geohash precision 6 ≈ 1.2km × 0.61km)
 * - Each dark store's service area maps to a set of geohash cells
 * - For an incoming order, compute its geohash (O(1)) and look up which store
 *   covers that cell — a simple Redis hash lookup instead of a geometric query
 *
 * Trade-off: At zone boundaries, a point might be in a cell that spans two stores.
 * Handle edge cases with a secondary precise check.
 */
object GeoHash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(lat: Double, lon: Double, precision: Int = 6): String {
        var minLat = -90.0; var maxLat = 90.0
        var minLon = -180.0; var maxLon = 180.0
        var isLon = true
        var bit = 0; var bitsTotal = 0; var hashVal = 0
        val hash = StringBuilder()

        while (hash.length < precision) {
            if (isLon) {
                val mid = (minLon + maxLon) / 2
                if (lon > mid) { hashVal = (hashVal shl 1) + 1; minLon = mid }
                else { hashVal = hashVal shl 1; maxLon = mid }
            } else {
                val mid = (minLat + maxLat) / 2
                if (lat > mid) { hashVal = (hashVal shl 1) + 1; minLat = mid }
                else { hashVal = hashVal shl 1; maxLat = mid }
            }
            isLon = !isLon
            if (++bitsTotal % 5 == 0) {
                hash.append(BASE32[hashVal])
                bit = 0; hashVal = 0
            }
        }
        return hash.toString()
    }
}
```

---

## Order Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         COMPLETE ORDER FLOW                              │
│                                                                           │
│  Customer App                                                             │
│       │                                                                   │
│       │  POST /orders                                                     │
│       ▼                                                                   │
│  API Gateway ──── Auth & Rate Limiting ──►  Order Service                │
│                                                    │                     │
│                                          ┌─────────┴──────────┐         │
│                                          │                     │         │
│                                   Inventory        Geo Service           │
│                                   Service       (select dark store)      │
│                                          │                     │         │
│                                    Reserve              Find nearest      │
│                                    stock                store with        │
│                                          │              all SKUs          │
│                                          └─────────┬──────────┘         │
│                                                    │                     │
│                                          Payment Service                 │
│                                                    │                     │
│                                          ┌─────────┴─────────┐          │
│                                     Success           Failure            │
│                                          │                │              │
│                                   Commit inventory   Release reservation  │
│                                          │                               │
│                                   Assignment Service                     │
│                                   (find nearest delivery partner)        │
│                                          │                               │
│                                   Picker Notification Service            │
│                                   (push to picker device)                │
│                                          │                               │
│                                   Tracking Service                       │
│                                   (WebSocket, Redis Geospatial)          │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### Spring Boot Microservice Architecture

```
geo-service          → zone detection, dark store selection
inventory-service    → stock management (covered in P0-13)
order-service        → order creation, orchestration
payment-service      → payment gateway integration
assignment-service   → delivery partner assignment
picker-service       → picker app backend (order items, confirmation)
tracking-service     → real-time location updates
notification-service → push notifications (FCM/APNs)
analytics-service    → SLA tracking, metrics
```

---

## Geo Service — Dark Store Selection

```kotlin
package com.yourcompany.geoservice.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class DarkStoreSelectionService(
    private val darkStoreRepository: DarkStoreRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val inventoryClient: InventoryServiceClient
) {
    /**
     * Select the optimal dark store for a given delivery address.
     *
     * Algorithm:
     * 1. Geohash the delivery coordinates → look up which stores cover this cell
     * 2. From candidate stores, filter by service polygon (precise check)
     * 3. Sort by straight-line distance (Haversine)
     * 4. For each store (nearest first): check if all items are in stock
     * 5. Return first store that can fulfill the entire order
     *
     * Why distance isn't the only factor:
     * - A store 2km away with all items beats a store 1km away missing 1 item
     * - A store with a better picker-to-order ratio might be preferred
     *   even if slightly farther (to maintain SLAs during peak hours)
     */
    fun selectDarkStore(
        deliveryLat: Double,
        deliveryLon: Double,
        requiredSkus: List<String>
    ): DarkStoreSelectionResult {
        // Step 1: Geohash fast lookup
        val geohash = GeoHash.encode(deliveryLat, deliveryLon, precision = 6)
        val candidateStoreIds = getCandidateStoresByGeohash(geohash)

        // Step 2: Load candidate stores and validate service polygon
        val candidates = darkStoreRepository.findByIds(candidateStoreIds)
            .filter { store ->
                GeoUtils.isInServiceRadius(
                    store.latitude, store.longitude,
                    deliveryLat, deliveryLon,
                    radiusKm = store.serviceRadiusKm
                )
            }
            .sortedBy { store ->
                GeoUtils.haversineDistanceKm(
                    store.latitude, store.longitude,
                    deliveryLat, deliveryLon
                )
            }

        if (candidates.isEmpty()) {
            return DarkStoreSelectionResult.NoServiceableStore
        }

        // Step 3: Find nearest store that can fulfill all SKUs
        for (store in candidates) {
            val stockCheck = inventoryClient.checkBulkAvailability(
                storeId = store.id,
                skus = requiredSkus
            )

            if (stockCheck.allAvailable) {
                val distanceKm = GeoUtils.haversineDistanceKm(
                    store.latitude, store.longitude,
                    deliveryLat, deliveryLon
                )
                val estimatedDeliveryMinutes = estimateDeliveryTime(distanceKm)

                return DarkStoreSelectionResult.Found(
                    store = store,
                    distanceKm = distanceKm,
                    estimatedDeliveryMinutes = estimatedDeliveryMinutes
                )
            }
        }

        // Check if any individual store has partial stock (for fallback messaging)
        val partialMatches = candidates.map { store ->
            val check = inventoryClient.checkBulkAvailability(store.id, requiredSkus)
            store to check.unavailableSkus
        }.filter { (_, missing) -> missing.isNotEmpty() }

        return DarkStoreSelectionResult.PartialStock(
            unavailableSkus = partialMatches.flatMap { it.second }.distinct()
        )
    }

    /**
     * Estimate delivery time based on distance.
     *
     * This is a simplification. In production, Zepto uses:
     * - Historical delivery time data per zone per time-of-day
     * - Live traffic conditions (Google Maps API)
     * - Current rider density and workload in the zone
     * - Time of day (breakfast rush = more partners pre-positioned near residential)
     */
    private fun estimateDeliveryTime(distanceKm: Double): Int {
        val pickingTimeMinutes = 3
        val packingTimeMinutes = 1
        val handoffTimeMinutes = 1
        val avgDeliverySpeedKmh = 15.0  // e-bike in urban conditions
        val riderTravelMinutes = (distanceKm / avgDeliverySpeedKmh * 60).toInt()

        return pickingTimeMinutes + packingTimeMinutes + handoffTimeMinutes + riderTravelMinutes
    }

    private fun getCandidateStoresByGeohash(geohash: String): List<Long> {
        // Look up pre-computed geohash → store mapping from Redis
        val key = "geohash:stores:$geohash"
        return redisTemplate.opsForSet().members(key)
            ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
    }
}

sealed class DarkStoreSelectionResult {
    object NoServiceableStore : DarkStoreSelectionResult()
    data class Found(
        val store: DarkStore,
        val distanceKm: Double,
        val estimatedDeliveryMinutes: Int
    ) : DarkStoreSelectionResult()
    data class PartialStock(val unavailableSkus: List<String>) : DarkStoreSelectionResult()
}
```

---

## Delivery Partner Assignment — The Assignment Service

This is one of the hardest engineering problems in quick commerce. You need to assign a delivery partner to an order within seconds of order placement.

### Redis Geospatial — Tracking Partner Locations

```kotlin
package com.yourcompany.trackingservice.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class PartnerLocationService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    companion object {
        private const val PARTNER_GEO_KEY = "partners:geo:locations"
        private const val PARTNER_STATUS_PREFIX = "partner:status:"
    }

    /**
     * Redis GEOADD: Store a partner's GPS coordinates.
     *
     * Redis Geospatial is built on top of sorted sets (ZSETs).
     * Internally, coordinates are encoded as a geohash-like integer
     * and stored as the ZSET score.
     *
     * Each partner's app calls this endpoint every 5 seconds while active.
     * With 1000 active partners: 200 GEOADD operations/second — trivial for Redis.
     */
    fun updatePartnerLocation(partnerId: Long, lat: Double, lon: Double) {
        redisTemplate.opsForGeo().add(
            PARTNER_GEO_KEY,
            org.springframework.data.geo.Point(lon, lat),
            partnerId.toString()
        )
        // Also update last-seen timestamp (for stale partner detection)
        redisTemplate.opsForValue().set(
            "partner:lastseen:$partnerId",
            System.currentTimeMillis().toString(),
            java.time.Duration.ofMinutes(10)
        )
    }

    /**
     * GEORADIUS: Find all available partners within N km of a dark store.
     *
     * This is the core of partner assignment. When an order is ready for pickup:
     * 1. Find all partners within 1km of the dark store
     * 2. Filter to those with status AVAILABLE
     * 3. Sort by distance
     * 4. Assign nearest available partner
     *
     * If no partner found within 1km → expand to 2km → expand to 3km (radius expansion)
     */
    fun findNearbyAvailablePartners(
        storeLat: Double,
        storeLon: Double,
        radiusKm: Double = 1.0
    ): List<NearbyPartner> {
        val results = redisTemplate.opsForGeo().radius(
            PARTNER_GEO_KEY,
            org.springframework.data.geo.Circle(
                org.springframework.data.geo.Point(storeLon, storeLat),
                org.springframework.data.geo.Distance(
                    radiusKm * 1000, // Redis uses meters
                    org.springframework.data.redis.connection.RedisGeoCommands.DistanceUnit.METERS
                )
            ),
            org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(20)
        ) ?: return emptyList()

        return results.content
            .mapNotNull { result ->
                val partnerId = result.name.toLongOrNull() ?: return@mapNotNull null
                val distanceM = result.distance?.value ?: return@mapNotNull null
                val status = getPartnerStatus(partnerId)

                if (status == PartnerStatus.AVAILABLE) {
                    NearbyPartner(
                        partnerId = partnerId,
                        distanceMeters = distanceM,
                        lat = result.content.point.y,
                        lon = result.content.point.x
                    )
                } else null
            }
    }

    private fun getPartnerStatus(partnerId: Long): PartnerStatus {
        val statusStr = redisTemplate.opsForValue().get("$PARTNER_STATUS_PREFIX$partnerId")
        return PartnerStatus.valueOf(statusStr ?: "OFFLINE")
    }
}

data class NearbyPartner(
    val partnerId: Long,
    val distanceMeters: Double,
    val lat: Double,
    val lon: Double
)

enum class PartnerStatus {
    AVAILABLE,    // Online, no current delivery
    ON_DELIVERY,  // Currently delivering
    ON_BREAK,     // Logged in but not accepting
    OFFLINE       // Not connected
}
```

### Assignment Service — Radius Expansion Algorithm

```kotlin
package com.yourcompany.assignmentservice.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class AssignmentService(
    private val partnerLocationService: PartnerLocationService,
    private val notificationService: NotificationService,
    private val orderRepository: OrderRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val RADIUS_EXPANSION_STEPS = listOf(1.0, 2.0, 3.0, 5.0) // km
        private const val ACCEPTANCE_TIMEOUT_SECONDS = 30L
    }

    /**
     * Assign order to a delivery partner using radius expansion.
     *
     * Algorithm:
     * 1. Start with 1km radius around dark store
     * 2. Find available partners, sorted by distance
     * 3. Send push notification to nearest available partner
     * 4. Wait up to 30 seconds for acceptance
     * 5. If rejected or timeout: try next partner in the list
     * 6. If no partners in 1km: expand to 2km, repeat
     * 7. If still no assignment at 5km: trigger manual ops alert
     *
     * Why not assign to multiple partners simultaneously?
     * - "Broadcast to all" causes multiple acceptances → you assign to one,
     *   the others are now rejected → bad partner experience → churn
     * - Serial assignment maintains fairness and partner trust
     * - Some platforms (Rapido) do broadcast with first-accept wins,
     *   which increases assignment speed but hurts partner experience
     */
    @Async("assignmentExecutor")
    fun assignDeliveryPartner(
        orderId: String,
        storeLat: Double,
        storeLon: Double
    ): CompletableFuture<AssignmentResult> {
        for (radiusKm in RADIUS_EXPANSION_STEPS) {
            log.info("Searching for partners within ${radiusKm}km for order $orderId")

            val nearbyPartners = partnerLocationService.findNearbyAvailablePartners(
                storeLat, storeLon, radiusKm
            )

            if (nearbyPartners.isEmpty()) {
                log.debug("No available partners within ${radiusKm}km, expanding radius")
                continue
            }

            for (partner in nearbyPartners) {
                val accepted = offerOrderToPartner(orderId, partner)
                if (accepted) {
                    log.info("Order $orderId assigned to partner ${partner.partnerId}")
                    return CompletableFuture.completedFuture(
                        AssignmentResult.Assigned(partner.partnerId)
                    )
                }
            }
        }

        // No partner found even at max radius
        log.error("CRITICAL: Could not assign order $orderId to any partner")
        return CompletableFuture.completedFuture(AssignmentResult.NoPartnerAvailable)
    }

    /**
     * Offer an order to a specific partner and wait for their response.
     *
     * Implementation: Send FCM push notification with the offer.
     * Partner app shows an overlay with "Accept / Decline" buttons and a countdown.
     * Partner response is sent via a WebSocket message or REST callback.
     *
     * The acceptance signal is stored in Redis with a short TTL:
     *   SET order:acceptance:$orderId $partnerId EX 35
     *
     * We poll (or use a blocking read) on this key.
     */
    private fun offerOrderToPartner(orderId: String, partner: NearbyPartner): Boolean {
        notificationService.sendOrderOffer(
            partnerId = partner.partnerId,
            orderId = orderId,
            acceptanceTimeoutSeconds = ACCEPTANCE_TIMEOUT_SECONDS
        )

        // Wait for acceptance (polling Redis with backoff)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < ACCEPTANCE_TIMEOUT_SECONDS * 1000) {
            val acceptedBy = checkAcceptance(orderId)
            when {
                acceptedBy == partner.partnerId -> return true
                acceptedBy != null -> return false // Accepted by someone else (shouldn't happen in serial flow)
                else -> Thread.sleep(500) // Poll every 500ms
            }
        }
        return false // Timeout
    }

    private fun checkAcceptance(orderId: String): Long? {
        // Implementation: check Redis key set by partner response endpoint
        return null // placeholder
    }
}

sealed class AssignmentResult {
    data class Assigned(val partnerId: Long) : AssignmentResult()
    object NoPartnerAvailable : AssignmentResult()
}
```

---

## Real-Time Location Tracking — WebSocket + Redis

The customer app shows the delivery partner moving on a map. This requires:
1. Partner app → Backend: GPS coordinates every 3-5 seconds
2. Backend → Customer app: Updated partner location in near-real-time

```kotlin
package com.yourcompany.trackingservice.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        /**
         * Enable a simple in-memory message broker.
         * In production, replace with STOMP over Redis or RabbitMQ
         * so messages are shared across multiple Spring Boot instances.
         *
         * Without this: if Customer A's WebSocket is on Instance 1,
         * and the location update arrives at Instance 2,
         * Instance 1 never gets it → customer sees partner frozen on map.
         */
        config.enableSimpleBroker("/topic")  // Dev: in-memory
        // Production: config.enableStompBrokerRelay("/topic").setRelayHost("rabbitmq")

        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/tracking")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }
}

@Service
class TrackingWebSocketService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val partnerLocationService: PartnerLocationService,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    /**
     * Called when a delivery partner's app sends a location update.
     *
     * Partner app → POST /api/tracking/location (REST, every 5s)
     *             OR
     * Partner app → WebSocket message to /app/location (WebSocket, every 3s)
     *
     * WebSocket is preferred for partners (lower overhead) but REST is the
     * fallback when WebSocket connection is unstable (mobile network handoffs).
     */
    fun handlePartnerLocationUpdate(
        partnerId: Long,
        lat: Double,
        lon: Double,
        orderId: String?
    ) {
        // 1. Update Redis geospatial index
        partnerLocationService.updatePartnerLocation(partnerId, lat, lon)

        // 2. If partner is on a delivery, broadcast to the customer tracking that order
        if (orderId != null) {
            val update = LocationUpdate(
                partnerId = partnerId,
                lat = lat,
                lon = lon,
                timestamp = System.currentTimeMillis()
            )

            // Publish to WebSocket topic — all subscribers watching this order get updated
            messagingTemplate.convertAndSend(
                "/topic/order/$orderId/location",
                objectMapper.writeValueAsString(update)
            )

            // Also cache in Redis for new WebSocket connections (initial state)
            redisTemplate.opsForValue().set(
                "order:partner:location:$orderId",
                objectMapper.writeValueAsString(update),
                java.time.Duration.ofMinutes(60)
            )
        }
    }
}

data class LocationUpdate(
    val partnerId: Long,
    val lat: Double,
    val lon: Double,
    val timestamp: Long
)
```

> [!WARNING]
> **WebSocket connection management at scale**: With 50,000 active orders simultaneously, you have up to 50,000 WebSocket connections (one per customer watching tracking). A single Spring Boot instance handles ~10,000 WebSocket connections before memory becomes a concern. You need:
> - Multiple instances behind a load balancer with **sticky sessions** (or use Redis pub/sub to broadcast across instances)
> - Connection timeouts to clear abandoned connections (customer closed app)
> - Heartbeat/ping-pong to detect stale connections

---

## Inventory Sync — Dark Store Accuracy

Dark store inventory must be accurate within seconds. Here's why it fails and how to fix it:

**Failure modes:**

| Failure | What Happens | Fix |
|---------|-------------|-----|
| Picker picks item that's actually out of stock | Order accepted → picker can't find item → cancellation | Mandatory scan-to-confirm on pick |
| Return not immediately restocked in system | Item physically back, but still shows 0 in system → missed sales | Automated restock scan at return counter |
| Restock not entered immediately | Supplier delivers 100 units at 2 AM, not entered until 9 AM | Store manager must scan delivery manifest immediately, system integration with supplier EDI |
| Damage not recorded | 5 items damaged, system still shows 5 available → oversell | Mandatory damage reporting workflow in picker app |

```kotlin
// Picker confirmation endpoint — THE key to inventory accuracy
@RestController
@RequestMapping("/api/picker")
class PickerController(private val pickerService: PickerService) {

    /**
     * Picker scans item barcode to confirm it's been picked.
     *
     * If scanned item doesn't match expected SKU → error shown on device
     * If item can't be found → picker reports "not found" → triggers substitution logic
     *
     * Every scan is logged with timestamp, picker ID, and location.
     * This creates a complete audit trail.
     */
    @PostMapping("/confirm-pick")
    fun confirmPick(
        @RequestBody request: PickConfirmRequest,
        @AuthenticationPrincipal picker: PickerPrincipal
    ): ResponseEntity<PickConfirmResponse> {
        return when (val result = pickerService.confirmPick(
            orderId = request.orderId,
            sku = request.scannedSku,
            pickerId = picker.id
        )) {
            is PickResult.Confirmed -> ResponseEntity.ok(
                PickConfirmResponse(success = true, nextItem = result.nextItem)
            )
            is PickResult.WrongItem -> ResponseEntity.badRequest().body(
                PickConfirmResponse(
                    success = false,
                    error = "Wrong item scanned. Expected: ${result.expectedSku}, got: ${request.scannedSku}"
                )
            )
            is PickResult.ItemNotFound -> {
                // Trigger substitution or cancellation flow
                ResponseEntity.ok(
                    PickConfirmResponse(success = false, requiresSubstitution = true)
                )
            }
        }
    }

    /**
     * Report damaged/expired item — removes from available inventory immediately.
     */
    @PostMapping("/report-damage")
    fun reportDamage(
        @RequestBody request: DamageReportRequest,
        @AuthenticationPrincipal picker: PickerPrincipal
    ): ResponseEntity<Unit> {
        pickerService.markItemDamaged(
            sku = request.sku,
            quantity = request.quantity,
            storeId = picker.storeId,
            reason = request.reason
        )
        return ResponseEntity.ok().build()
    }
}

data class PickConfirmRequest(val orderId: String, val scannedSku: String)
data class PickConfirmResponse(
    val success: Boolean,
    val nextItem: String? = null,
    val error: String? = null,
    val requiresSubstitution: Boolean = false
)
data class DamageReportRequest(val sku: String, val quantity: Int, val reason: String)
```

---

## Peak Hour Handling — Predictive Pre-Assignment

The biggest SLA breach risk is breakfast (8-9 AM), lunch (1-2 PM), and dinner (8-9 PM). During these 1-hour windows, order volume spikes 5-10x.

### Pre-positioning Strategy

```kotlin
@Service
class PrePositioningService(
    private val orderHistoryRepository: OrderHistoryRepository,
    private val partnerManagementService: PartnerManagementService,
    private val darkStoreRepository: DarkStoreRepository
) {
    /**
     * Predict demand for the next 30 minutes and position partners accordingly.
     *
     * Algorithm:
     * 1. Look at same time window for the last 30 days
     * 2. Calculate average order volume per zone
     * 3. Send "suggested position" nudge notifications to available partners
     * 4. Partners near high-demand zones get priority in assignment queue
     *
     * This is the difference between:
     * - Reactive: order placed → frantically search for nearest partner (3-4 min)
     * - Proactive: partner already stationed near the dark store (20 seconds)
     */
    @Scheduled(cron = "0 */5 * * * *")  // every 5 minutes
    fun updatePartnerPositioningSuggestions() {
        val now = java.time.LocalDateTime.now()

        darkStoreRepository.findAll().forEach { store ->
            val predictedOrdersNext30Min = predictDemand(store.id, now)

            if (predictedOrdersNext30Min > 10) {
                // Nudge 2x predicted demand partners to pre-position near this store
                val partnersNeeded = predictedOrdersNext30Min * 2 // 2 concurrent orders per partner
                partnerManagementService.sendPositioningSuggestion(
                    storeId = store.id,
                    storeLat = store.latitude,
                    storeLon = store.longitude,
                    suggestedPartnerCount = partnersNeeded
                )
            }
        }
    }

    private fun predictDemand(storeId: Long, dateTime: java.time.LocalDateTime): Int {
        val hourOfDay = dateTime.hour
        val dayOfWeek = dateTime.dayOfWeek

        return orderHistoryRepository.averageOrdersInWindow(
            storeId = storeId,
            hourStart = hourOfDay,
            hourEnd = hourOfDay + 1,
            dayOfWeek = dayOfWeek.value,
            lookbackDays = 30
        )
    }
}
```

---

## SLA Tracking — Per-Order Delivery Time Monitoring

```kotlin
@Entity
@Table(name = "order_sla_tracking")
data class OrderSlaTracking(
    @Id val orderId: String,

    // Key timestamps
    val orderPlacedAt: Instant,
    var pickerAssignedAt: Instant? = null,
    var pickingStartedAt: Instant? = null,
    var pickingCompletedAt: Instant? = null,
    var packingCompletedAt: Instant? = null,
    var partnerAssignedAt: Instant? = null,
    var partnerPickedUpAt: Instant? = null,
    var deliveredAt: Instant? = null,

    // SLA targets (in seconds)
    val targetPickerAssignSeconds: Int = 30,
    val targetPickingCompletedSeconds: Int = 210,  // 3.5 minutes
    val targetPartnerAssignSeconds: Int = 270,     // 4.5 minutes
    val targetDeliverySeconds: Int = 600,          // 10 minutes

    var slaBreached: Boolean = false,
    var breachReason: String? = null
) {
    fun checkSlaStatus(): SlaStatus {
        val now = Instant.now()
        val elapsedSeconds = java.time.Duration.between(orderPlacedAt, now).seconds

        return when {
            deliveredAt != null -> {
                val totalSeconds = java.time.Duration.between(orderPlacedAt, deliveredAt!!).seconds
                if (totalSeconds > targetDeliverySeconds) SlaStatus.BREACHED
                else SlaStatus.MET
            }
            elapsedSeconds > targetDeliverySeconds -> SlaStatus.BREACHED
            elapsedSeconds > targetDeliverySeconds * 0.8 -> SlaStatus.AT_RISK
            else -> SlaStatus.ON_TRACK
        }
    }
}

enum class SlaStatus { ON_TRACK, AT_RISK, BREACHED, MET }

@Component
class SlaMonitor(
    private val slaRepository: OrderSlaTrackingRepository,
    private val alertService: AlertService
) {
    /**
     * Every 30 seconds: find orders at risk of SLA breach.
     * Alert ops team, potentially trigger partner re-assignment if stuck.
     */
    @Scheduled(fixedDelay = 30_000)
    fun monitorActiveSlas() {
        val activeOrders = slaRepository.findActiveOrders()

        activeOrders.forEach { sla ->
            when (sla.checkSlaStatus()) {
                SlaStatus.AT_RISK -> {
                    alertService.sendOpsAlert(
                        level = AlertLevel.WARNING,
                        message = "Order ${sla.orderId} at risk of SLA breach. Elapsed: " +
                                "${java.time.Duration.between(sla.orderPlacedAt, Instant.now()).toMinutes()} min"
                    )
                }
                SlaStatus.BREACHED -> {
                    if (!sla.slaBreached) {
                        sla.slaBreached = true
                        slaRepository.save(sla)
                        alertService.sendOpsAlert(
                            level = AlertLevel.CRITICAL,
                            message = "SLA BREACHED: Order ${sla.orderId}"
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
```

---

## The 10-Minute Promise — What Actually Enables It

This is not just engineering. It's a combination of engineering + operations decisions:

### 1. Pre-staged Inventory (Slotting)
High-velocity SKUs (top 100 by daily volume) are placed within 10 steps of the packing counter. An 8-item order where all items are high-velocity can be picked in 90 seconds.

### 2. Optimized Picker Routes
The picker app doesn't list items in order-they-appear-in-cart. It shows them in **physical shelf order** (aisle by aisle, top to bottom). This eliminates backtracking.

```kotlin
@Service
class PickingRouteOptimizer(
    private val productLocationRepository: ProductLocationRepository
) {
    /**
     * Sort order items by their physical location in the dark store
     * to create an optimal picking route.
     *
     * Simple implementation: sort by (aisle, shelf_number, position)
     *
     * Advanced: Solve as a Traveling Salesman Problem variant,
     * but for 10-15 items TSP is overkill — simple sort suffices.
     */
    fun optimizePickingRoute(
        storeId: Long,
        items: List<OrderItem>
    ): List<OrderItemWithLocation> {
        val locations = productLocationRepository.findByStoreAndSkus(
            storeId = storeId,
            skus = items.map { it.sku }
        ).associateBy { it.sku }

        return items
            .mapNotNull { item ->
                val location = locations[item.sku] ?: return@mapNotNull null
                OrderItemWithLocation(item, location)
            }
            .sortedWith(
                compareBy(
                    { it.location.aisleNumber },
                    { it.location.shelfNumber },
                    { it.location.positionOnShelf }
                )
            )
    }
}
```

### 3. Partner Pre-positioning
During peak hours, partners are incentivized (bonus per order) to be within 500m of a dark store before orders even come in. Assignment time drops from 2-3 minutes to 10-20 seconds.

### 4. Parallel Workflows
Picking and partner assignment happen in PARALLEL, not sequentially:

```
Order received
     │
     ├──────────────────────────────────┐
     │                                  │
     ▼                                  ▼
Start picking (notify picker)    Start partner search
(3-4 minutes)                    (30-60 seconds typically)
     │                                  │
     ▼                                  ▼
Packing complete              Partner standing by at store
     │                                  │
     └──────────────────┬───────────────┘
                        │
                   Handoff (~10 seconds)
                        │
                   Delivery starts
```

The partner arrives at the store *before* packing is complete (or at the same time). Zero waiting.

---

## Slot Management vs Instant Delivery

Some quick commerce players offer **slots** (pre-booked delivery windows) in addition to instant delivery. Slots are easier to engineer:

| Feature | Instant | Slots |
|---------|---------|-------|
| Delivery time | 10-15 min | 1-4 hour window |
| Order batching | Not possible | Multiple orders picked in one route |
| Partner efficiency | 1 order per trip | 3-5 orders per trip (same zone) |
| Engineering complexity | High (real-time everything) | Lower (batch optimization) |
| Customer expectation | Immediate | Flexible |

Slots require **route optimization** (solving VRP — Vehicle Routing Problem) to batch orders efficiently. Instant delivery is simpler per-order but more expensive per-delivery.

> [!IMPORTANT]
> **Why Zepto doesn't do slots**: The 10-minute promise is a **brand differentiator**. The moment you introduce slots, you compete on price with Amazon and Blinkit, where your unit economics are worse. Slots dilute the brand. Engineering the 10-minute promise IS the moat.

---

## Production Configuration — application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: tracking-service

  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      timeout: 1000ms
      # Redis Cluster for production (geo data is per-instance safe)
      cluster:
        nodes: ${REDIS_CLUSTER_NODES}

  websocket:
    message-size-limit: 65536    # 64KB max WebSocket message
    send-buffer-size: 524288     # 512KB per connection send buffer
    send-time-limit: 10000       # 10 second send timeout

# WebSocket thread pool (tune based on concurrent connections)
messaging:
  websocket:
    heartbeat:
      server: 10000   # server sends heartbeat every 10 seconds
      client: 10000   # expect client heartbeat every 10 seconds

# Async executor for assignment service
executor:
  assignment:
    core-pool-size: 10
    max-pool-size: 50
    queue-capacity: 100
    thread-name-prefix: "assignment-"
```

---

## Key Production Pitfalls

> [!CAUTION]
> **Geohash boundary problem**: A delivery address at coordinates on the edge of two geohash cells might not match either store correctly. Always implement a fallback to Haversine distance check when geohash lookup returns no candidates.

> [!WARNING]
> **Stale partner location in Redis**: If a partner closes their app without going offline, their location stays in Redis indefinitely. The `partner:lastseen:$id` key TTL of 10 minutes must be enforced — if a partner's last-seen is older than 5 minutes, treat them as OFFLINE regardless of their status field.

> [!WARNING]
> **WebSocket fan-out under load**: During order status updates, you might broadcast to all customers watching an order. At 50,000 concurrent orders, even a single broadcast to 1 subscriber each is 50,000 WebSocket writes. Use connection pooling and async message delivery. Never hold a DB transaction while writing to WebSocket.

> [!IMPORTANT]
> **Dark store inventory drift**: The physical inventory and digital inventory WILL diverge over time. Mandatory daily stock counting (cycle counts) with discrepancy reconciliation is non-negotiable. Blinkit does continuous cycle counting — different aisles are counted at different times throughout the day to avoid disrupting operations.

> [!NOTE]
> **The "last mile" problem at 10 minutes**: At 2 km distance, a delivery partner on an e-bike takes ~8 minutes. That leaves only 2 minutes for picking + packing + handoff. At 3 km, it's mathematically impossible. This is why quick commerce dark stores need to be within 1.5-2 km of every delivery point. The real constraint is real estate density, not technology.
