# ZAPP (nick name for understand )

## Quick Commerce Delivery Platform

**Project Proposal & Information Guide**

_Prepared for the Client | May 2026_

---

> This document is your complete reference for what Zapp is, what it contains, how it compares to competitors, and what legal obligations must be met before and after launch. It is written for the business owner — not for developers.

---


# Application Features — Detailed Breakdown (Client View)

This section describes exactly what each of the six Zapp applications can do, written for the business owner — no technical jargon, no backend details. Every feature listed here is delivered as described.

---

## 1. Customer App (Android + iPhone)

**What the customer experiences from opening the app to receiving their order.**

### Onboarding & Account

- Phone number + one‑time password (OTP) login – no passwords to remember or reset
- Profile setup: name (required), profile photo (optional)
- Age verification – under‑18 accounts are blocked (legal requirement)
- Privacy consent screen – clear explanation of what data is collected and why
- Account deletion available inside the app – all personal data removed within 30 days

### Address Management

- Save multiple addresses with custom labels: Home, Work, Other, etc.
- Add address by dropping a pin on Google Maps or typing manually
- Location asked once, saved permanently – no repeated popups asking for location every session
- Edit or delete saved addresses anytime

### Browsing & Shopping

- Home screen with a live delivery time banner: *“Delivering to [address] in ~14 min”* (calculated from real data)
- Horizontal category strip: Grocery, Dairy, Snacks, Beverages, Household, Pharma, Electronics, Stationery
- Reorder section: last 5 orders as one‑tap repeat buttons
- Recommended section: rule‑based suggestions (recently bought, frequently bought together, trending in your area)
- Promotional banners managed from admin – no app update needed
- Search bar with autocomplete and typo tolerance – recent searches saved locally

### Product List & Details

- Filter products by category, brand, price range, in‑stock only, offers only
- Sort by relevance, price low‑high, price high‑low
- Product card shows: image, name, weight/size, MRP struck through, sale price, stock warning if <5 left
- Product detail page: multiple images, full description, ingredients/nutritional info (for food), shelf life promise (e.g., *“Min 3 days remaining for dairy”*)
- “Notify me when back in stock” toggle for out‑of‑stock items
- Substitution suggestion shown if an item is unavailable

### Cart – Transparent Pricing

- Every fee shown in the cart *before* checkout: item total, delivery fee, handling fee, surge fee (if any), GST – no surprises at payment screen
- Surge fee displayed with inline reason: *“High demand in your area”*
- Free delivery threshold shown: *“Add ₹47 more for free delivery”*
- Coupon/promo code field inside cart – discount applied instantly
- Wallet balance shown: *“You have ₹120 in Zapp wallet. Apply?”*
- Cart items reserved for 5 minutes – countdown timer shown: *“Reserved for 4:32”*
- After placing an order, a 5‑minute window to add more items to the same order – banner: *“Add more to this order”*

### Checkout & Payment

- Address confirmation with live map preview – confirmed before any charge
- Free‑text delivery instructions: gate code, floor number, leave at door
- Payment methods: UPI (GPay, PhonePe, Paytm), debit/credit cards, net banking, Zapp wallet, cash on delivery (COD) where available
- Last used payment method is pre‑selected – user confirms with one tap; no auto‑charge
- Order button disables immediately on first tap to prevent double orders – re‑enabled only on confirmed failure
- Payment failure: clear error message, cart preserved – no need to re‑add items
- If payment deducted but order not confirmed, the system waits 30 seconds and queries Razorpay directly – if success, order confirmed; prevents double‑payment bug

### Live Order Tracking

- Live map showing rider’s location – updated every 3‑5 seconds via secure connection
- Order state timeline with timestamps: Placed → Confirmed → Picking → Packed → Picked Up → On the Way → Delivered
- ETA countdown recalculated with each significant rider location change
- One‑tap masked call to rider – rider’s real phone number not exposed
- Shareable tracking link – anyone with the link can watch delivery in a browser, no app needed
- Delivery proof photo shown to customer once rider marks delivered

### After Delivery & Disputes

- Rate product quality (1‑5 stars) and rider behaviour (1‑5 stars) – separate ratings
- Missing item: tap the item from order → upload photo → select reason → target resolution time under 5 minutes for standard cases (refund confirmation or replacement)
- Wrong item: photo upload → swap or refund to original payment method
- Refund tracker shows exact status: *“Refund approved → Processing → Credited to [UPI/Card ending 4242]”* with expected date – never vague “5‑7 business days”
- Refund always goes to original payment method (UPI, card, bank) unless customer explicitly chooses wallet credit – no forced promo codes
- Return initiation visible per item with eligibility: *“Returnable within 7 days”*

### Account & Wallet

- Full order history – every order with details and downloadable GST invoice (PDF)
- Zapp wallet: view balance, transaction history, add money (via Razorpay), withdraw to bank
- Manage saved addresses
- Notification preferences: order updates, offers, new products
- Refer & earn: unique referral link, track referrals, see earned credits
- Help centre: searchable FAQ + chat (bot first → human escalation in <2 minutes, with visible queue)

---

## 2. Rider App (Android only)

**For your delivery partners – from onboarding to daily earnings.**

### Digital Onboarding & KYC

- All document uploads done inside the app – no paperwork, no office visit (except final in‑person ID check at dark store)
- Aadhaar card (front + back) – verified electronically via government UIDAI OTP
- PAN card – verified against Income Tax API
- Driving licence – verified against Sarathi portal
- Vehicle RC (registration certificate) – photo upload
- Vehicle insurance certificate – photo upload, manual validity check
- Bank account – penny drop verification: ₹1 transferred, account holder name matched
- Live selfie with liveness detection – compared against Aadhaar photo
- Background check (criminal record, address) – rider informed of timeline (usually 24 hours)
- After KYC approval: 5 short training videos + MCQ quiz – must pass before going online
- Physical verification at dark store – store manager meets rider, confirms identity, hands over kit (T‑shirt, delivery bag)

### Going Online & Receiving Orders

- Prominent online/offline toggle – rider must be within configured radius of dark store to go online
- Order notification appears with 30‑second acceptance window – shows: item count, store address, customer distance, **estimated earnings for this trip**
- Auto‑reject if rider does not respond – order moves to next available rider
- Rider sees only orders they can reasonably complete based on current location

### Pickup Process

- At the dark store, rider scans QR code on the packed order bag – cannot mark “picked up” without this scan (proves physical presence)
- Order bag QR code is unique to that order – prevents wrong pickup

### Navigation & Delivery

- One‑tap navigation – opens Google Maps (or HERE Maps based on rider preference) with customer address pre‑loaded
- For prepaid orders: rider enters 4‑digit OTP shown by customer – prevents false delivery claims
- Mandatory delivery photo – camera opens automatically; photo must be taken live (cannot upload from gallery)
- Geotagged photo must be within 300 metres of customer address – system verifies this; cannot mark delivered otherwise
- System prevents marking “delivered” if GPS is more than 300m from address – stops GPS spoof fraud

### Earnings & Payouts

- Per‑trip earnings shown **before accepting** the order – no surprises
- Earnings dashboard: daily, weekly, monthly totals with per‑trip breakdown
- Same‑day payout to bank account via Razorpay payout API
- Incentives: peak hour bonus, order completion milestone bonus, referral bonus
- SOS button – sends GPS location + emergency alert to operations team; insurance details accessible in‑app
- Issue reporting – in‑app chat (not phone) keeps written record of every dispute

### Offline Resilience

- Current order details cached on the phone – works even when network is lost
- Location updates stored locally and batch‑uploaded when connection returns (with original timestamps)
- “Mark delivered” action queued locally, synced when online
- QR scan works offline (decoded locally, synced later)
- Offline indicator shown at top: *“Offline — syncing…”*

---

## 3. Picker App (Android only)

**For dark store staff who pick and pack orders. This is the most operationally critical app.**

### Shift Management

- Tap “Start Shift” to clock in – system checks food safety certificate (FoSTaC) expiry; if expired, shift start is blocked
- Clock out at end of shift – store manager can see who is online and what each picker is working on

### Receiving Orders

- When a new order arrives, picker sees a notification
- Order list is sorted by **bin_code** (physical shelf location: zone‑aisle‑shelf‑bin) – picker follows the shortest walking route through the store (not alphabetical, not random)
- Each item shows: product photo, name, quantity, shelf code, and the barcode to expect

### Picking Items

- Picker walks to the shelf, picks the physical product, scans the barcode – app confirms it is the right product; if wrong barcode, app rejects
- After scanning, tap “Picked” – item turns green on the list
- If item not found: tap “Out of Stock” – must select a reason from dropdown (Genuinely empty / Damaged / Expired / Wrong location) – cannot skip reason
- Out‑of‑stock immediately notifies the customer with a substitution offer – customer has 60 seconds to approve or reject; if rejected or timeout, item is removed and auto‑refund initiated
- For loose items sold by weight (fruits, vegetables): picker enters actual weight – system adjusts price automatically if different from ordered quantity

### FSSAI Compliance – Built‑in

- For every food item, picker must scan the expiry date barcode before marking “Picked”
- App compares expiry date against order date + 45 days (FSSAI mandate for online delivery)
- If less than 45 days remain, app **blocks the pick** – picker cannot pack it; item is forced out‑of‑stock
- This is enforced in software – no override possible. Prevents delivery of near‑expired food and protects you from FSSAI raids.

### Packing & Handover

- After all items are picked, tap “Pack Order” – items are placed into delivery bags
- Bluetooth thermal printer prints a label with order QR code
- Picker scans the printed label to confirm packing – order status changes to “Ready for Pickup”
- Rider is automatically notified that the order is ready

### Performance Tracking (Optional, store manager can enable)

- After each order, picker sees: *“This order: 2m 45s. Your average: 3m 12s”*
- Accuracy score: % of correct picks this week
- Shift leaderboard within the store – shows fastest and most accurate pickers
- If an order exceeds 4 minutes pick time, store manager is automatically notified (SLA breach alert)

---

## 4. Store Manager App (Android) – Phase 2

**For the head of each dark store. Not required for launch, but essential for scaling operations.**

### Live Dashboard

- Real‑time metrics: today’s GMV, order count, average pick time, SLA breach count, complaint count
- Capacity gauge: percentage of max orders processed this hour – colour coded: green (<70%), amber (70‑90%), red (>90%)
- One‑click to see which picker is behind, which order is delayed, which rider is nearby

### Active Orders

- List of all active orders with real‑time status – tap any order to see full details: customer info, items picked, rider assigned
- Filter by status (picking, packed, out for delivery, etc.)

### Rider & Picker Management

- See which riders are currently online, their location on a map, and their current order
- See picker status: online/offline, current assigned order, last activity time

### Inventory Management

- View current stock of every SKU in the store
- Low stock threshold – configurable per product; push notification when any SKU hits threshold
- Expiry tracking – items expiring in <3 days highlighted red; <7 days amber
- Remove damaged or expired items with one tap – item is immediately removed from customer‑visible inventory; customer apps update within 500ms
- Inbound stock: scan product barcode, enter quantity received – inventory updates automatically; printed receiving summary
- Cycle count workflow: system assigns a zone to count; picker scans all items in that zone; discrepancies flagged for manual review
- Shelf mapping: assign or reassign any SKU to a bin_code (zone‑aisle‑shelf‑bin) – picker app automatically uses the new location

### Compliance Management

- FoSTaC certificate status for every picker – expiring soon (amber), expired (red). Expired certificate = picker cannot clock in.
- FSSAI license for the store – expiry date tracked; 30‑day and 7‑day reminders sent to manager
- Weekly expiry audit workflow: manager must scan X% of near‑expiry items every Monday – mandatory, cannot be skipped; recorded in database with timestamp for FSSAI audit trail
- Cold storage temperature log – manual entry in Phase 1; IoT sensor integration in Phase 3

---

## 5. Seller / Partner App (Android + Web) – Phase 3

**For brands and product vendors who supply inventory to your dark stores.**

### Digital Onboarding (Most Document‑Heavy)

All verification happens live against government APIs – no manual checking, no fake documents.

- GST certificate – GSTIN verified against GST portal; name must match PAN and bank account exactly
- PAN card – verified against Income Tax API
- FSSAI license (for food sellers) – license number verified against FSSAI portal; must not be expired
- Trade license or shop establishment certificate – photo upload, manual review
- Bank account – penny drop verification; account holder name must match GST entity name
- Trademark certificate (for branded products) – photo upload
- Cancelled cheque or bank statement – supporting proof
- Business registration certificate (Proprietorship/Partnership/Pvt Ltd)
- Product catalogue – Excel template with: SKU name, MRP, HSN code, barcode, category, images, FSSAI info

**AI‑OCR** auto‑fills fields from document photos – seller confirms, does not type manually.  
**Name match check:** GST entity name vs PAN name vs bank account name – all three must match. Even “Pvt Ltd” vs “Private Limited” mismatch triggers human review (not auto‑rejection).  
On approval: e‑sign seller agreement via DigiLocker or Aadhaar e‑sign.  
Approval time: target 2‑7 days – status communicated at each step.

### Seller Dashboard

- Sales: today, month‑to‑date, year‑to‑date per SKU – revenue, units sold, returns
- Inventory sent vs sold vs returned
- Product performance: views, add‑to‑cart rate, purchase conversion, return rate
- Complaint rate per SKU – auto‑flagged if >5% in 7 days
- Purchase orders received from Zapp with delivery confirmation
- Payout schedule: gross sales – returns – Zapp commission = payout; TDS deduction itemised; invoice download available

### Product Management

- Add new SKU – all required fields enforced before submission; new SKUs go to admin approval queue (not live immediately)
- Edit MRP/price – requires admin approval (prevents sudden price manipulation)
- Bulk upload via Excel template for large catalogues
- Minimum shelf life rule enforced at product level – seller declares shelf life; system checks against FSSAI 45‑day rule

---

## 6. Super Admin Dashboard (Web)

**The control centre for your operations team – built for real‑time incident response.**

### Phase 1 – Emergency Screen (Must Be Built Before Launch)

This screen is what Blinkit lacks and what causes their “delivery stuck for hours” complaints. Your ops team can see and fix any problem within 60 seconds.

**Live map:** every active order as a coloured dot – green (on time), amber (delayed 5+ min), red (delayed 15+ min), grey (stuck/failed). Click any dot to see full order details.

**Store capacity:** any store at >80% capacity is highlighted. One‑click button to **pause new orders** for that store – no code change needed.

**Rider coverage:** any zone with zero online riders is highlighted red. One‑click button to **broadcast a push notification** to offline riders in that zone.

**Payment failure rate:** if success rate drops below 95% in any 5‑minute window, an alert banner appears instantly.

**Complaint spikes:** any store with >5 concurrent unresolved complaints is flagged.

**One‑click actions:**
- Pause a store
- Extend ETA for all active orders in a store
- Trigger an emergency support call
- Broadcast WhatsApp message to all riders in a city

### Store Management

- Add or edit a dark store: name, GPS coordinates, delivery radius (drawn on a map), operating hours, max orders per hour, max concurrent pickers
- All configuration stored in database – editable without code deployment. Adding a new city = adding a database row, not code changes.
- Delivery radius as a polygon (not a simple circle) – handles highways, rivers, inaccessible areas

### Product & Catalogue Management

- Approve or reject new SKU requests from sellers
- Approve price change requests
- Set category margins and commission rates
- Create promotional banners: upload image + set start/end date + target store or city
- Auto‑delist a product if complaint rate >5% in 7 days OR return rate >10% in 30 days

### Finance & Compliance

- View all orders with GST invoice status; bulk download for accounting
- Approve seller payouts and process them
- Refund management: approve, override, or manually process when automated flow fails
- Export GST reports: GSTR‑1, GSTR‑3B ready for your CA
- Audit trail: every order state change, every refund, every inventory change – queryable by order ID, user ID, date range; retained 7 years (GST requirement)

### Fraud Management

- Customer trust score <40: flagged list with breakdown of reasons
- Riders with GPS spoofing flags: reviewed, warned, or suspended
- Sellers with high complaint rates: reviewed, SKUs auto‑delisted
- Anomaly alerts: customer with 5+ refund claims in 30 days, rider with 10+ GPS flag events – automatic flagging for review

---

# The Opportunity

India's quick commerce sector crossed ₹20,000 crore in GMV in 2024 and is growing at over 40% annually. Blinkit, Zepto, and Swiggy Instamart are all growing fast — but every major platform has the same unresolved problem: **customers do not trust them.**

The complaints are not about speed. Customers accept 15–20 minute delivery. What they cannot accept:

- Paying for an order, then being told the item was out of stock
- Getting a promo code instead of a real refund
- Seeing "Delivered" on the app when nothing arrived — with no proof, no recourse
- Surprise fees appearing only at the final payment screen
- A support chatbot that resolves nothing and escalates to no one
- Refund status showing "5–7 business days" with no updates for two weeks

**The customer who has been deceived once does not need a better discount to switch. They need a platform that simply does not deceive them.**

Zapp is built specifically to earn that customer. Every feature described in this document exists to solve a specific failure that Blinkit, Zepto, or Dunzo made — and made publicly, at scale.

---

# Why Existing Platforms Cannot Fix This

Blinkit, Zepto, and Swiggy cannot fix their trust problems without rebuilding their apps from scratch. Their systems carry years of decisions made in a growth-at-all-costs era. Showing fees upfront requires rearchitecting checkout flows that millions of daily transactions run through. Refunding to original payment methods requires changing settlement logic their finance teams depend on.

They know about these problems. The complaints have been public for years. They have not fixed them because fixing them is expensive and disruptive to a working business.

**Zapp is built with none of those constraints.** Every decision that makes Zapp better than Blinkit is default behaviour from day one.

---

# What Zapp Is — Six Applications, One Platform

Zapp is not one app. It is six purpose-built applications sharing one secure backend. Each application is designed for the specific person who uses it — their environment, their stress, their needs.

---

## 1. Customer App — Android & iPhone

This is what your customers use to browse, order, and track deliveries.

### What makes it better than every existing app

**No surprise fees.** Every charge — delivery fee, handling fee, surge pricing, GST — is shown in the cart before the customer reaches checkout. No fees appear for the first time at the payment screen. This single decision eliminates Blinkit's single highest-volume complaint.

**Real refunds.** When a customer is owed money, it goes back to the UPI account or bank card they paid with — not as a promo code, not as Zapp wallet credit (unless they choose that). A customer whose ₹450 came back to their account within 3 days tells someone. A customer who received a promo code never reorders.

**Delivery proof.** Every delivery ends with a geotagged photo taken at the customer's address. Customers can see this photo. "Marked delivered but never arrived" is not possible — the system requires physical presence at the address before delivery can be marked complete.

**Disputes resolved in-app in under 5 minutes.** Wrong item or missing item: tap the item, upload a photo, choose the issue. A resolution — refund or replacement — comes back within the target of 5 minutes. No email. No chatbot dead-end.

**Honest delivery time.** The estimated time shown is calculated from real data: current store capacity, actual rider locations, actual traffic. Not a fixed "10 minutes" that breaks every evening and during rain.

**Location saved permanently.** Address is asked once, saved to the profile. Never asked again at every app open — which is Blinkit's most-mentioned UX irritant across hundreds of reviews.

**Shareable live tracking.** The customer gets a link they can share with anyone — a family member, a flatmate — who can watch the delivery in a browser without installing any app.

**GST invoice on every order.** Downloaded from order history. Ready for anyone who needs it for expense reimbursement or business accounting.

### What the customer can do — Phase 1

- Sign in with phone number and OTP (no passwords)
- Save multiple delivery addresses with labels (Home, Work, etc.)
- Browse by category, search by name, filter by brand or price
- View full product details including ingredients, shelf life, and all mandatory product information
- See all fees before paying — no checkout surprises
- Pay via UPI, debit/credit card, net banking, or cash on delivery
- Track the order live on a map with a real countdown
- Share the tracking link with anyone
- Report a missing or wrong item in the app and get resolved within 5 minutes
- Download a GST invoice for every order
- View full refund status with a real expected credit date
- Manage saved addresses and notification preferences

---

## 2. Rider App — Android

This is what your delivery partners use from the moment they apply to join Zapp until they complete a delivery.

### Joining Zapp — Digital Verification

Every rider goes through a complete digital verification before being allowed to take a single order. Documents verified electronically against government systems — no manual paperwork, no room for fake documents:

- Aadhaar card verified electronically via the government UIDAI system
- PAN card verified against the Income Tax database
- Driving licence verified against the government Sarathi portal
- Vehicle registration and insurance documents reviewed
- Bank account confirmed by sending a small test transfer and matching the account holder name
- Live selfie with liveness detection — no photos of photos
- Background check (criminal record, address) — rider is notified and given a timeline
- Short training videos and a quiz that must be passed before going online

Physical identity verification happens at the dark store: the store manager meets the rider in person, confirms identity, and hands over the kit.

### How deliveries work

The rider sees orders on a map and has 30 seconds to accept. The notification shows exactly how much they will earn on that trip before they accept — no surprise pay.

At the store, the rider scans a QR code on the packed order bag. This proves physical pickup — the system will not allow marking "picked up" without this scan.

Navigation is provided through Google Maps. The rider cannot mark a delivery complete without:

1. Being physically within 300 metres of the customer's address (GPS verified)
2. Entering the OTP the customer shows them (for prepaid orders)
3. Taking a photo at the delivery location

A rider cannot fake a delivery. GPS location spoofing is detected and flagged automatically.

### Rider earnings

- Earnings shown before accepting each trip
- Full dashboard showing daily, weekly, and monthly totals
- Same-day payout to bank account
- Peak hour bonuses and completion milestones

---

## 3. Picker App — Android

This is for the dark store staff who physically pick and pack each order.

### Why this app matters most for your operations

The picker is where speed and compliance meet. A 1-minute improvement in average pick time directly reduces the delivery estimate shown to every customer. Zepto's fast pack times come from optimised software, not just fast staff.

### How picking works

When an order arrives, the picker's screen shows every item — not in a random or alphabetical list, but sorted by physical shelf location. The picker follows a route through the store that minimises steps.

For each item:

- The screen shows a photo, the shelf location code, and the barcode to scan
- The picker physically scans the barcode on the product — the app confirms it is the right item
- If the item is not available, the picker must select a reason and the customer is immediately notified with a substitution offer
- For fruits and vegetables sold by weight, the picker enters the actual weight and the price adjusts automatically

When all items are packed, a Bluetooth printer generates a label with a QR code. The rider scans this label at pickup.

### Built-in food safety compliance

Every food item must have its expiry date scanned before packing. If the expiry date is less than 45 days away, the app **blocks packing** — the picker cannot override this. This is an FSSAI legal requirement for online food delivery that Blinkit and Zepto have violated publicly, leading to government raids. On Zapp, it is enforced in software, not by a procedure manual.

A picker whose food safety training certificate (FoSTaC) has expired cannot start a shift. The system checks this at clock-in and blocks access. This cannot be bypassed.

---

## 4. Store Manager App — Android — Phase 2

For the head of each dark store.

What they can see and do:

- Live dashboard showing today's total sales, order count, and average pick time
- A capacity gauge that shows when the store is approaching its order limit — the system automatically extends delivery estimates and alerts the manager before it becomes a problem
- A live list of all active orders with their current status
- Inventory management: which items are running low, which are expiring soon, which have been removed due to damage
- Staff management: who is clocked in, which pickers are active, food safety certificate status for every staff member with expiry alerts
- Compliance tracking: FSSAI license renewal reminders, weekly expiry audit workflow

---

## 5. Seller / Partner App — Android & Web — Phase 3

For brands and product vendors who supply inventory to your dark stores.

What sellers can do:

- Complete digital onboarding with full document verification (GST, FSSAI for food, PAN, bank account — all verified against government systems)
- View their own sales performance: revenue per product, return rate, complaint rate
- See detailed payout statements with itemised deductions and TDS
- Add new products (goes to your admin team for approval before going live)
- Upload large product catalogues in bulk

---

## 6. Super Admin Dashboard — Web

This is the control centre for your operations team. It is the most important tool for managing the business after launch.

### The operations screen — built for emergencies

This screen is the fix for Blinkit's worst public failure: when something goes wrong, their operations team cannot see it or act on it quickly. Blinkit's most common severe complaint is orders stuck in the system for hours with no intervention and no communication.

Your team can see, at a glance:

- Every active order on a live map, colour-coded by status (on time / delayed / stuck)
- Any store approaching full capacity — one click pauses new orders to that store
- Any area with no available riders — one click broadcasts a notification to offline riders nearby
- Payment failure rates the moment they spike
- Any store with multiple simultaneous complaints

One person with this dashboard can see and respond to any problem across your entire operation within 60 seconds.

### What else the admin can do

- Add or adjust dark stores, delivery zones, and operating hours — without any code changes
- Approve new products submitted by sellers
- Manage all refunds including manual overrides when needed
- Download GST reports ready for your CA
- Review all fraud flags across customers, riders, and sellers
- Access a complete audit trail of every order event, refund, and admin action — going back 7 years (required by GST law)

---

# How Zapp Is Better — 12 Specific Differences

|Zapp Does This|Blinkit / Zepto Fail Here|
|---|---|
|All fees shown in cart before checkout|Surge and handling fees appear only at the payment screen|
|Refund to original payment method — UPI, card, bank|Refund as a promo code that forces a repeat purchase on a platform that just failed you|
|Mandatory geotagged delivery photo — taken live at address|"Delivered" marked from anywhere, item never arrived, no evidence|
|In-app dispute resolution in under 5 minutes|Email support taking 4–7 days; chatbot with no human escalation|
|Honest ETA calculated from real store and rider data|A fixed "10 minutes" promise that breaks every evening and during rain|
|Location saved once to profile permanently|Location popup demanded every time the app is opened|
|Out-of-stock shown before payment is taken|Order accepted, payment charged, then cancelled because the item was never in stock|
|Order button disabled after first tap to prevent double orders|Double order placed by tapping twice on a slow network|
|Substitution approval sent to customer before substituting|Wrong product delivered with no warning or consent|
|Expiry date and food safety enforced in software — cannot be bypassed|Manual processes that fail when staff are busy, leading to FSSAI raids|
|Shareable live tracking link for anyone at the delivery address|No way to share order tracking with a family member or flatmate|
|FoSTaC training certificate tracked per staff member|Untrained staff handling food, triggering government action|

---

# Launch Timeline —  Weeks to First Real Order

Three developers. One city. One dark store. Real customer orders by Week 8.

| Week   | What Is Delivered                                                                                                                          |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Week 1 | All server infrastructure set up and tested. Project structure for all six apps.                                                           |
| Week 2 | Core order system: how orders are created, reserved, paid for, and tracked. Payment integration. Inventory reservation.                    |
| Week 3 | Customer app: login, browsing, cart with transparent fees, checkout, live order tracking.                                                  |
| Week 4 | Rider app: full digital KYC, order acceptance, GPS navigation, QR pickup scan, OTP and photo delivery proof, earnings screen.              |
| Week 5 | Picker app: shift clock-in with food safety check, pick list, barcode scanning, expiry check, label printing. Admin emergency screen live. |
| Week 6 | Full end-to-end testing. Complete order cycle: customer orders → picker packs → rider delivers → GST invoice generated → refund works.     |
| Week 7 | Soft launch to internal testers. UI polish. Order history, dispute upload, refund tracking refined.                                        |
| Week 8 | Go-live. Real payments active. 2–3 onboarded riders, 2–3 pickers. 50–100 products live. First real customer orders.                        |

---

# What Comes After Launch

**Weeks 9–12 (Phase 2):** Store Manager App goes live. Surge pricing and automatic store capacity management. Fraud detection scoring for customers, riders, and sellers. SMS and WhatsApp notification backup.

**Months 4–6 (Phase 3):** Seller / Partner App with full digital onboarding, sales dashboard, and automated payouts. Multi-city support. Hindi language search.

**Months 9+ (Phase 4):** Sponsored product placements (revenue from brands). Loyalty programme. Demand forecasting to prevent stockouts.

---

# Why Build Custom Instead of White-Label Software

White-label quick commerce platforms exist. Here is why they do not work for what Zapp needs to be:

- They cannot enforce FSSAI expiry compliance in the picker app — that requires custom software built specifically for this
- They cannot integrate with India's government verification systems (Aadhaar, PAN, GST, FSSAI, Sarathi) at the depth required for legal KYC
- They charge a per-order fee that grows with your success — the cost compounds with scale
- The 12 competitive differences listed above are product decisions, not features available in off-the-shelf software
- A custom platform is a business asset you own, can extend, and can sell. A white-label dependency is a permanent cost with a ceiling on what you can do

---

# Legal & Regulatory Obligations — What You Must Do

This section covers every legal requirement that applies to Zapp as a quick commerce platform operating in India. These are not optional. Each item is flagged as either a **Launch Requirement** (must be done before the first real order) or a **Ongoing Obligation** (recurring after launch).

> This is an information guide for your awareness and planning. You should engage a CA (Chartered Accountant) and a lawyer before launch. This document does not substitute for legal advice.

---

## Business Registration

**Launch Requirement**

Before applying for any licence or opening a business bank account, the legal entity must be registered.

Your options:

- **Private Limited Company** — Recommended for a funded or investor-backed platform. Separate legal identity. Enables future investment rounds. Registered with the Ministry of Corporate Affairs (MCA). Takes 7–15 days.
- **LLP (Limited Liability Partnership)** — Suitable for two-founder businesses. Limited liability protection. Simpler than a Pvt Ltd.
- **Sole Proprietorship** — Fastest to set up. Only needs GST registration, PAN, and a current account. No separate legal identity; unlimited personal liability. Not suitable if you plan to raise investment.

**Shops and Establishment Act registration** is also required. This must be done within 30 days of starting operations at each dark store location. In Maharashtra (Pune), this is under the Maharashtra Shops and Establishments Act. Applied at the local municipal authority. FSSAI inspectors check for this.

---

## FSSAI — Food Safety

**Launch Requirement**

FSSAI (Food Safety and Standards Authority of India) governs everything related to food sold through your platform.

**Zapp as a platform must hold a Central FSSAI Licence.** This applies to you as the e-commerce operator — even though you are not manufacturing food. The government classifies you as a Food Business Operator (FBO) because you facilitate food sales. Central Licence is required because e-commerce platforms reach customers across state boundaries. Apply on the FoSCoS portal. Processing can take 30–60 days — apply before development is complete, not after.

**Each dark store must have its own separate FSSAI registration or licence.** Basic registration if the store's annual turnover is under ₹12 lakh; State Licence if ₹12 lakh–₹20 crore.

**Your FSSAI licence number must be displayed on the app and website.** This is a legal requirement.

**Every food seller on your platform must provide a valid FSSAI licence.** Zapp verifies this against the FSSAI government portal — an expired or invalid licence blocks the seller from listing food products.

**The 45-day shelf life rule** (FSSAI directive, November 2024): no food product with less than 45 days remaining shelf life may be sold online. This is enforced automatically in the picker app — a picker physically cannot pack a near-expiry item. This rule is actively enforced by the government. Blinkit was raided in Telangana specifically for this violation.

**FoSTaC certificates** (food safety training) are mandatory for every staff member who handles food in your dark stores. Zapp tracks certificate expiry per person and blocks clock-in when a certificate has expired. This is enforced in software.

**Renewal:** FSSAI licences are valid for 1–5 years. The admin dashboard tracks expiry and sends reminders at 30 days and 7 days before expiry.

---

## GST — Goods and Services Tax

**Launch Requirement**

GST registration is mandatory for a quick commerce platform — there is no turnover threshold exemption for e-commerce operators in India. You must register in every state where you have a dark store.

**Tax Collection at Source (TCS):** As an e-commerce operator, Zapp is legally required to deduct 1% TCS from every seller payout (Section 52 of the CGST Act). This is deducted automatically from what you pay sellers. Sellers can claim this as a tax credit. You must file a separate GST return for this (GSTR-8) by the 10th of every month.

**GST invoice on every order:** Zapp automatically generates a compliant GST invoice for every order placed. This includes the correct tax split — CGST + SGST for orders where the seller and customer are in the same state; IGST for inter-state orders.

**E-invoicing (IRN):** When Zapp's annual GMV crosses ₹5 crore, every invoice must be registered with the government's Invoice Registration Portal (IRP) and carry a unique Invoice Reference Number (IRN). This is built into Zapp's invoice system from the start so the transition is automatic.

**GST returns to file every month:**

- GSTR-1 (details of all outward supplies): due by the 11th of each month
- GSTR-3B (summary return and tax payment): due by the 20th of each month
- GSTR-8 (TCS collected from sellers): due by the 10th of each month

All three reports are exportable from the admin dashboard in the format your CA needs. All records must be retained for 7 years — stored automatically in the system.

---

## Income Tax — Payments to Riders and Pickers

**Ongoing Obligation**

Payments made to riders and pickers are subject to TDS (Tax Deducted at Source) under the Income Tax Act 2025 (which replaced the 1961 Act from 1 April 2026). The applicable section and rate for gig/contractor payments should be confirmed with your CA, as the sections have been renumbered under the new Act.

TDS must be deposited monthly and a quarterly TDS return must be filed. Razorpay's payout system handles TDS deduction automatically — you configure the rate once.

---

## Labour Law & Gig Worker Compliance — 2026 Update

**Launch Requirement — this area changed significantly in late 2025**

The Social Security Code 2020 became fully operative in late 2025. This is the most important regulatory change affecting delivery platforms in 2026.

**What changed:** Platform workers (riders, pickers) are no longer in a legal grey area. Aggregator platforms like Zapp now have defined legal obligations toward gig workers.

**Universal Account Number (UAN):** Every rider and picker must be mapped to a UAN — a unique government identifier for social security tracking. This is now mandatory regardless of whether the worker is classified as a contractor or employee. Zapp collects UAN during onboarding.

**Social security contribution:** As an aggregator, Zapp must contribute 1%–2% of gig-related annual turnover to the government's social security fund for platform workers. The exact rate is set by the Central Government. Your CA must advise on the current applicable rate and filing process.

**EPF and ESI:** For riders and pickers who meet the employee classification threshold — employee contribution 12% (EPF) and 0.75% (ESI); employer contribution 12% (EPF) and 3.25% (ESI). Whether your riders qualify as employees or contractors is a legal question that must be answered by your labour law consultant. This classification is actively litigated in the gig economy context.

**Professional Tax (Maharashtra):** Applicable in Pune/Maharashtra. Deducted from employed staff salaries per the state slab. Filed and remitted monthly or quarterly.

**Appointment / engagement letters:** Every rider and picker must receive a formal engagement letter before starting work. This is required under the new Labour Codes. The letter must state payment terms, applicable deductions, and how disputes are raised.

**Rider insurance:** ₹10 lakh life insurance per rider is industry standard (Blinkit and Swiggy standard). A third-party group insurance policy covers this. Insurance details are accessible by riders inside the app. Personal accident insurance for riders during active deliveries is also strongly recommended.

---

## DPDP Act 2023 — Data Protection

**Launch Requirement | Full compliance deadline: 13 May 2027 | Maximum penalty: ₹250 crore per violation**

The Digital Personal Data Protection Act 2023 and its Rules 2025 govern how Zapp collects, stores, and uses customer data.

**What Zapp must do:**

**Consent at signup:** Before collecting any personal data, the app shows a clear consent screen — plain language, not buried in a terms document — explaining what is being collected (name, phone number, address, payment data, device ID, order history), why, and who it is shared with. The user must actively tap "I agree." Pre-ticked boxes or scroll-past consent are not valid under this law.

**Data minimisation:** Zapp collects only what is needed for the platform to function. No data collected "for future use" without a stated purpose.

**Right to deletion:** Every customer can request account deletion from within the app settings. On deletion, all personal information (name, phone, address, device data) is deleted or anonymised within 30 days. Order transaction records are retained in anonymised form for 7 years for GST compliance.

**Data breach notification:** If a data breach occurs, Zapp must notify the Data Protection Board of India within 72 hours and affected customers without unreasonable delay.

**Minor protection:** The platform blocks account creation for anyone under 18. An age confirmation is required during signup.

**Third-party sharing:** Customer data is shared only with Razorpay (payment processing), the KYC verification provider, and Google Maps (navigation). No other sharing without updated consent.

---

## Legal Metrology — Product Labelling

**Launch Requirement**

The Legal Metrology (Packaged Commodities) Rules require that every product listed on an e-commerce platform displays mandatory information digitally — not just on the physical package. This is an existing legal obligation that applies immediately.

Every product listing on Zapp must show:

- Manufacturer or packer name and full address
- Country of origin
- Net quantity (weight, volume, or count)
- MRP inclusive of all taxes
- Best-before or expiry date (for food products)
- Customer care contact of the manufacturer
- Green dot (vegetarian) or red/brown dot (non-vegetarian) symbol for all food products

Zapp enforces these fields at the product submission stage — a seller cannot submit a product for approval without completing all mandatory fields. This removes the compliance burden from manual review.

**Country-of-origin filter (effective 1 July 2027):** A new government rule (Legal Metrology Amendment Rules 2026) requires that all e-commerce platforms provide a searchable and sortable filter for "country of origin" on any imported product. This must be live by 1 July 2027. Zapp's product catalogue is built to support this from day one.

---

## Consumer Protection — Dark Patterns

**Launch Requirement**

The Consumer Protection (E-Commerce) Rules 2020 and the CCPA's Dark Pattern Guidelines apply to Zapp.

**What this means in practice:**

- **No hidden fees:** Fees cannot appear for the first time at the payment screen. Zapp shows all fees in the cart. Blinkit's current practice of revealing surge fees at checkout is explicitly non-compliant with these guidelines.
- **No fake countdown timers** or artificial urgency.
- **No manipulated MRP** — the MRP shown must be the actual manufacturer price, not inflated to make discounts appear larger.
- **Refund policy displayed upfront** with the exact timeline stated. Zapp's target is 3 business days to original payment source.
- **Grievance officer:** You must appoint a named individual as the platform's Grievance Officer and display their name and contact email/phone in the app. They must acknowledge complaints within 48 hours and resolve within 1 month.
- **No fake reviews:** The platform cannot filter, manipulate, or incentivise reviews. Every rating submitted by a customer must appear as submitted.

---

## RBI — Payments

**Launch Requirement**

Zapp uses Razorpay as its payment processor. Razorpay is a licensed Payment Aggregator regulated by the Reserve Bank of India (RBI). This means Zapp does not need its own RBI payment licence — Razorpay's licence covers the transactions.

**Card tokenisation:** RBI mandates that no business in the payment chain stores raw card numbers. Zapp uses Razorpay's tokenisation system — no card data is ever stored on Zapp's servers.

**Zapp wallet — important limitation for Phase 1:** The Zapp wallet in Phase 1 is used only for refund credits — money Zapp owes a customer is held temporarily and applied to their next order. If the wallet is later expanded to allow customers to add their own money and spend it freely, it may require a separate RBI Prepaid Payment Instrument (PPI) licence. Legal advice is required before expanding wallet features beyond refund credits.

---

## IT Act 2000 — Platform Obligations

**Launch Requirement**

As an online intermediary, Zapp must publish and maintain:

- A Privacy Policy (in the app, not just on a website)
- Terms of Service
- Refund and Return Policy

These must be in English. A regional language version is strongly recommended for non-English-speaking customers.

Zapp must also have a mechanism for users to report abusive content (reviews, uploaded photos) and respond to government or court data requests within legally specified timeframes.

---

## Compliance Calendar — What Gets Filed and When

|Obligation|Due Date|Governing Law|
|---|---|---|
|GSTR-1 (sales data)|11th of every month|GST Act|
|GSTR-3B (summary return + tax payment)|20th of every month|GST Act|
|GSTR-8 (TCS from sellers)|10th of every month|GST Act, Section 52|
|TDS deposit on rider/picker payments|7th of the following month|Income Tax Act 2025|
|TDS return (quarterly)|31st of month after quarter end|Income Tax Act 2025|
|EPF deposit|15th of every month|Social Security Code|
|ESI deposit|15th of every month|Social Security Code|
|Professional Tax (Maharashtra)|As per state schedule|Maharashtra PT Act|
|FSSAI licence renewal|Before expiry — tracked in admin|FSS Act 2006|
|FoSTaC renewal per staff member|Before expiry — tracked per person|FSS Act 2006|
|Shops & Establishment renewal|Annual|Maharashtra S&E Act|
|Data breach notification to government|Within 72 hours of discovery|DPDP Act 2023|
|Retain all GST records|7 years minimum|GST Act|
|Country-of-origin filter live on platform|By 1 July 2027|LM (PC) Amendment Rules 2026|
|Full DPDP compliance|By 13 May 2027|DPDP Rules 2025|

---

## Pre-Launch Legal Checklist

These must be done before the first real paying customer uses the platform:

- [ ] Legal entity registered (Pvt Ltd, LLP, or Proprietorship)
- [ ] PAN for the entity obtained
- [ ] GST registration complete — GSTIN confirmed
- [ ] FSSAI Central Licence application submitted _(apply early — 30–60 days processing)_
- [ ] Shops and Establishment registration for each dark store location
- [ ] Razorpay account live and production payments active
- [ ] Grievance Officer appointed — name and contact visible in the app
- [ ] Privacy Policy, Terms of Service, and Refund Policy published in the app
- [ ] Rider group insurance policy arranged
- [ ] UAN registration process ready for first rider and picker onboarding
- [ ] CA engaged for monthly GST filings from Month 1
- [ ] Labour law consultant engaged for gig worker compliance
- [ ] Engagement/appointment letters prepared for riders and pickers

---

# What "Done" Looks Like After 8 Weeks

At the end of Week 8, the following must all be true — these are the conditions for go-live:

- A customer can browse, place an order, pay, and track it live on a map
- A rider can complete KYC digitally, go online, accept an order, pick it up with a QR scan, navigate to the customer, deliver with OTP + photo, and see their earnings
- A picker can clock in (with food safety certificate check), pick items by scanning barcodes, handle out-of-stock with customer notification, check expiry dates (with automatic block on near-expiry food), and print the delivery label
- The admin can see a live map of all active orders, pause a store that is overloaded, and manually process a refund
- Every order automatically generates a compliant GST invoice
- A customer cannot accidentally place a double order by tapping twice
- Inventory can never go negative — no order is accepted for an item that is not in stock
- Food with less than 45 days of shelf life cannot be packed — blocked in software

---


Blinkit has the largest network in India and cannot fix its trust problem without rebuilding from scratch. Zepto has the fastest technology and a compliance record that invites government action. Dunzo had more funding than either and collapsed by expanding before unit economics worked.

The gap in the market is not speed. Every major platform delivers in 15 minutes. The gap is **what happens when something goes wrong** — and something always goes wrong.

The customer whose ₹450 refund came back to their UPI account in 3 days becomes a loyal customer. The customer who received a promo code never reorders. That difference, multiplied across thousands of orders, is the business.



---

__