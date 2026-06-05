# Local Commerce Delivery Platform — Complete Business Model Reference

## Master Document v1.0 | Pimpri/Pune Market

---

## Part 1: Platform Classification & Model

Your platform is a **hyperlocal local commerce aggregator**. Not Blinkit (dark store model). Not Zomato (restaurant-only). The closest international analogs are DoorDash's local merchant model and Grab's hyperlocal expansion in Southeast Asia.

**Operational Flow:**

```
Customer → Local Store → Rider → Customer
```

No warehouses. No owned inventory. No dark stores. Structurally cheaper than quick commerce, operationally harder than food-only delivery because your product taxonomy is wider: grocery, mithai, bakery, snacks, beverages, tea stalls, restaurants, paan shops.

**Why Pimpri/Pune is a good starting market:**

Pimpri-Chinchwad is a dense industrial and residential zone with high local commerce activity, strong UPI adoption, and a working-class + middle-class demographic mix that regularly uses nearby local stores. Order density and rider supply are more manageable than Mumbai or Bengaluru. Tier-2 adjacent markets also carry lower customer acquisition costs than metro cores.

---

## Part 2: Payment Gateway Cost — The Floor

You must know your cost floor before building any revenue model.

### Gateway Rate Comparison

|Gateway|UPI (under ₹2,000)|Debit/Credit Card|Wallet|Setup Fee|
|---|---|---|---|---|
|Razorpay|₹0 (RBI rule)|2% + 18% GST on fee|2%|₹0|
|Cashfree|₹0|1.75–2% + 18% GST|1.95%|₹0|
|PayU|₹0|2% + 18% GST|2%|₹0|

**Critical rule from RBI:** UPI transactions under ₹2,000 have zero MDR — the bank bears the cost, not you.

### Gateway Cost Per Order (₹300 order)

|Payment Method|Gateway Rate|GST on Fee|Total Gateway Cost|You Net|
|---|---|---|---|---|
|UPI|₹0|₹0|₹0|₹300.00|
|Debit card|₹6.00|₹1.08|₹7.08|₹292.92|
|Credit card|₹6.00|₹1.08|₹7.08|₹292.92|
|Wallet|₹6.00|₹1.08|₹7.08|₹292.92|

**Design implication:** Default checkout to UPI. Offer ₹5 cashback on UPI payments at launch. Every converted COD or card order to UPI saves ₹7 on a ₹300 order. At 100 orders/day that is ₹700/day or ₹21,000/month saved — enough to pay a part-time support person.

---

## Part 3: Competitive Commission Landscape

Understanding incumbent pricing defines your pitch window.

### What Platforms Actually Charge Stores

|Platform|Headline Commission|With Delivery|Additional Charges|Real Total|
|---|---|---|---|---|
|Zomato (store delivery)|7–15%|—|Platform fee, visibility fees|10–18%|
|Zomato (Zomato delivery)|15–25%|Included|Long-distance surcharge (2025)|35–38% total|
|Swiggy|15–25%|Included|Onboarding, ads|30–35%|
|Rapido Ownly (2026)|0%|₹30 flat customer fee|Nothing|Revenue unclear|
|**Your Platform (proposed)**|**8–12%**|**Separate delivery fee**|**Platform fee**|**~13–17% total**|

**Your pitch to any store:** "Zomato takes 35–38% of your order when they deliver. We take 10%, and we bring our own rider. You keep ₹270 of every ₹300 order."

**Rapido lesson:** Zero commission is easy to onboard stores but creates no revenue. Their model has no clear path to profitability. You need a middle path — low enough to win stores, high enough to operate.

---

## Part 4: Commission Structure — Full Detail

### Store Commission by Category

|Store Type|Commission Rate|Rationale|
|---|---|---|
|Restaurant / cloud kitchen|10–12%|Higher margin products, existing platform familiarity|
|Grocery / Kirana|8–10%|Lower margins, high volume potential|
|Mithai / Bakery|8–10%|Perishable, moderate margin|
|Tea stall / Snack shop|5–8%|Very low AOV (₹50–150), small operator|
|Beverage store|8–10%|Moderate AOV, repeat order potential|
|Pharmacy / medical|6–8%|Regulated, low margin, but high trust value|

Start at the **lower end of each band** for the first 90 days. Lock in commission rates for minimum 90-day periods after announcement. Changing rates without notice is the number one reason platforms lose store partners.

### Store Subscription Tiers (Phase 2, Month 4+)

|Tier|Monthly Fee|Commission Rate|Who It Targets|
|---|---|---|---|
|Starter|₹0 (first 30 days)|10%|All new stores|
|Basic|₹0|10%|Low volume, no commitment|
|Growth|₹999/month|6%|Stores doing 30–100 orders/month|
|Pro|₹2,499/month|4%|Stores doing 100+ orders/month|

A store doing 80 orders/month at ₹300 AOV pays:

```
Basic:   80 × ₹300 × 10% = ₹2,400 commission
Growth:  ₹999 + (80 × ₹300 × 6%) = ₹999 + ₹1,440 = ₹2,439
```

The store saves almost nothing at this volume but the subscription signals commitment. Real savings begin at 120+ orders/month.

### Minimum Activity Fee (Critical Addition)

For stores generating fewer than 30 orders/month, the commission revenue does not cover your operational cost of settlement processing, support, and onboarding.

A store with 10 orders/month at ₹150 AOV and 8% commission generates ₹120/month for the platform — against real costs of onboarding labor and settlement processing.

|Store Activity|Monthly Commission Earned|Minimum Fee Applied|You Collect|
|---|---|---|---|
|0–10 orders/month|₹0–₹120|₹149|₹149|
|11–30 orders/month|₹121–₹360|₹99|Max(commission, ₹99)|
|31+ orders/month|₹361+|None|Commission only|

Waive the minimum fee for the first 60 days to ease onboarding friction. After day 60, apply it. This prevents ghost stores — registered but inactive — from clogging your system without contributing revenue.

---

## Part 5: Payment Flow Architecture

### The Cardinal Rule

Money must never flow directly Customer → Store.

**Correct flow, always:**

```
Customer
    ↓
Payment Gateway (Razorpay / Cashfree)
    ↓
Platform Escrow / Ledger (you control this)
    ↓
    ├── Store Settlement (weekly, T+5)
    └── Rider Settlement (weekly, with daily advance option)
```

This architecture is non-negotiable because it is the only model that allows refunds, cancellations, fraud handling, and dispute resolution without manual bank transfers.

### Full Order Money Flow — Step by Step

Using a ₹300 order, 3 km distance, UPI payment:

```
Step 1: Customer places order
        Cart total:      ₹300
        Delivery fee:     ₹40
        Platform fee:      ₹5
        Customer pays:   ₹345

Step 2: Payment Gateway
        Method: UPI
        MDR: ₹0
        Platform receives: ₹345 into escrow

Step 3: Order lifecycle
        Store accepts → prepares → rider picks up → delivers → OTP confirmed

Step 4: Order completed, ledger entries created
        Store credit:    ₹300 − 10% = ₹270  (held for weekly settlement)
        Rider credit:    ₹32               (₹20 base + ₹12 for 3km)
        Platform:        ₹345 − ₹270 − ₹32 = ₹43 gross

Step 5: Settlement (Monday)
        Store receives: ₹270 via NEFT
        Rider receives: ₹32 (or can withdraw ₹300 daily advance earlier)
        Platform retains: ₹43 gross contribution
```

---

## Part 6: Order Economics — Detailed Scenarios

### Scenario A: Poorly Structured (What Not to Do)

|Item|Amount|
|---|---|
|Order value|₹300|
|Delivery fee charged|₹25|
|Platform fee|₹5|
|**Customer pays**|**₹330**|
|Store commission (10%)|₹30|
|Platform fee collected|₹5|
|**Platform collects**|**₹35**|
|Rider payout (₹20 + ₹15 for 3km)|₹35|
|Gateway cost (UPI)|₹0|
|**Platform net**|**₹0**|

You are running the platform for free. Any support cost, infrastructure cost, or dispute puts you in the negative.

---

### Scenario B: Viable MVP (Recommended Starting Point)

|Item|Amount|
|---|---|
|Order value|₹300|
|Delivery fee charged|₹40|
|Platform fee|₹5|
|**Customer pays**|**₹345**|
|Store commission (10%)|₹30|
|Delivery fee collected|₹40|
|Platform fee collected|₹5|
|**Total platform collects**|**₹75**|
|Rider payout (₹20 base + ₹12 for 3km)|₹32|
|Gateway cost (UPI)|₹0|
|**Platform gross margin**|**₹43**|

---

### Scenario C: Peak Hour / Rain (Surge)

|Item|Amount|
|---|---|
|Order value|₹400|
|Delivery fee charged|₹45|
|Platform fee|₹8|
|Surge fee|₹15|
|**Customer pays**|**₹468**|
|Store commission (10%)|₹40|
|Delivery + surge collected|₹60|
|Platform fee|₹8|
|**Total platform collects**|**₹108**|
|Rider payout (₹20 + ₹12 + ₹10 peak + ₹20 rain)|₹62|
|Gateway cost (UPI)|₹0|
|**Platform gross margin**|**₹46**|

---

### Scenario D: Small Order Problem (Tea Stall, ₹80 Order)

This scenario is critical because it reveals the unit economics problem for low-AOV stores.

|Item|No Fee Structure|With Tiered Fee|
|---|---|---|
|Order value|₹80|₹80|
|Delivery fee charged|₹40|₹20|
|Platform fee|₹5|₹5|
|Customer pays|₹125|₹105|
|Store commission (6%)|₹4.80|₹4.80|
|Rider payout (2km order)|₹28|₹28|
|Gateway (UPI)|₹0|₹0|
|**Platform net**|**₹21.80**|**₹1.80**|

At ₹40 delivery on an ₹80 order (50% of order value), customer conversion collapses — the delivery fee exceeds the mental threshold. At ₹20 delivery your margin on small orders is nearly zero. This is why small-AOV stores (tea stalls, paan shops) are difficult to monetize through delivery alone and are better treated as **acquisition anchors** — they bring customers onto your platform who then order from higher-AOV stores.

**Tiered delivery fee structure (recommended):**

|Order Value|Delivery Fee|Platform Fee|Customer Pays (on ₹300 order)|
|---|---|---|---|
|Under ₹100|₹15–20|₹3|₹318–323|
|₹100–₹300|₹25–35|₹5|₹330–340|
|₹300–₹600|₹35–45|₹5–8|₹340–353|
|Above ₹600|₹45–55|₹8–10|Dynamic|

---

## Part 7: Rider Compensation — Full Model

### Core Pay Structure

|Component|Amount|Condition|
|---|---|---|
|Base pickup rate|₹20|Every delivery|
|Distance component|₹4/km|Above first 1 km|
|Peak hour bonus|₹10|7–9am, 12–2pm, 7–10pm|
|Rain bonus|₹20–25|Declared rain condition|
|Night bonus|₹15|After 9pm|
|Milestone: 50 orders|₹200 one-time|Cumulative|
|Milestone: 200 orders|₹500 one-time|Cumulative|

### Rider Earnings Per Delivery — Scenarios

|Scenario|Base|Distance|Bonus|**Total**|
|---|---|---|---|---|
|1 km, regular|₹20|₹0|₹0|**₹20**|
|2.5 km, regular|₹20|₹6|₹0|**₹26**|
|3 km, regular|₹20|₹8|₹0|**₹28**|
|3 km, peak hour|₹20|₹8|₹10|**₹38**|
|3 km, rain|₹20|₹8|₹20|**₹48**|
|3 km, peak + rain|₹20|₹8|₹30|**₹58**|
|4 km, night|₹20|₹12|₹15|**₹47**|

### Realistic Daily Earnings Estimate (Pune Market, Hypothesis)

These are projections requiring field validation with actual Pimpri/Pune riders.

|Hours Worked|Orders (est.)|Avg. per Order|Gross|Fuel (est.)|**Net**|
|---|---|---|---|---|---|
|4 hours|8–10|₹26|₹208–260|₹60|**₹148–200**|
|7 hours|16–20|₹28|₹448–560|₹100|**₹348–460**|
|10 hours|22–28|₹30|₹660–840|₹140|**₹520–700**|

**Confidence level: Medium.** These numbers are derived from Blinkit video data (Blinkit riders earned ~₹157/hour net in a dense market). At MVP launch your order density will be lower, meaning riders wait longer between orders and effective hourly earnings drop. Early riders will need to be briefed honestly: low volume initially, improving as the platform grows.

**Blinkit reality check for calibration:**

Blinkit riders on normal days (no incentives) earned ₹12–17 base per order with distance additions bringing totals to ₹25–30. A rider completing 21 orders in 5 hours earned ₹918 gross, spent ₹130 on fuel — net ₹788 for 5 hours (₹157/hour). Your ₹20 base is slightly more generous than Blinkit's base but without Blinkit's scale incentive programs.

### Rider Settlement and Payout

|Item|Detail|
|---|---|
|Settlement cycle|Weekly, Monday|
|Daily advance|Up to ₹300–500 (like Blinkit's Pocket feature)|
|Minimum retained balance|₹100 (for COD reconciliation)|
|Statement|Per-order breakdown, viewable in app|
|Payout method|IMPS to registered bank account|

Every rupee must be explainable. The number one rider complaint across all video research is not knowing why a payout is a specific amount. Build the per-order earnings breakdown screen before launch — it is a retention tool, not a reporting feature.

---

## Part 8: Revenue Streams — Phased

### Phase 1 (MVP, Months 1–6)

|Revenue Stream|Source|Estimate per Order|
|---|---|---|
|Store commission|8–10% of order value|₹24–30 (₹300 AOV)|
|Platform fee|Charged to customer|₹5|
|Delivery fee margin|Collected − rider paid|₹8–13|
|**Total per order**||**₹37–48**|

### Phase 2 (Growth, Months 6–18)

|Revenue Stream|Model|Monthly Estimate|
|---|---|---|
|Featured store listings|₹99–299/month/store|₹2,000–10,000|
|Banner ads (home screen)|Local businesses|₹500–2,000/month|
|Store subscription plans|₹999–2,499/month|Variable|
|Surge fee margin|During peak/rain|₹5–10/order|

### Phase 3 (Post-PMF, Month 18+)

|Revenue Stream|Model|
|---|---|
|Customer loyalty subscription|₹49–99/month for free/reduced delivery|
|Merchant data reports|Monthly analytics sold to stores|
|B2B bulk delivery|Local businesses using platform for inter-store transfers|
|White-label store pages|Stores pay for custom landing pages|

---

## Part 9: Break-Even Analysis — Corrected

The original document used ₹30,000/month fixed costs and derived 27 orders/day. That number is operational breakeven only — it excludes customer acquisition, rider onboarding, and early-stage support overhead, which are the dominant costs in months 1–6.

### Three-Scenario Break-Even Model

|Cost Item|Lean (Solo Founder)|Realistic (2–3 people)|Scaled (5 people)|
|---|---|---|---|
|Cloud infra|₹5,000|₹10,000|₹25,000|
|Customer support|₹0 (founder)|₹15,000|₹40,000|
|Rider onboarding|₹2,000|₹5,000|₹10,000|
|Store onboarding|₹2,000|₹5,000|₹10,000|
|Customer acquisition|₹5,000|₹20,000|₹60,000|
|Miscellaneous|₹3,000|₹5,000|₹15,000|
|**Total monthly fixed**|**₹17,000**|**₹60,000**|**₹1,60,000**|
|**Gross margin/order**|₹37|₹37|₹40|
|**Orders/day to break even**|**~15**|**~54**|**~136**|
|**Orders/month to break even**|**~460**|**~1,620**|**~4,000**|

**The correct framing for your planning:**

```
Operational break-even (lean):    ~15 orders/day
Realistic break-even (3 people):  ~54 orders/day
Scale target (5 people):         ~136 orders/day
```

At 100 orders/day with ₹40 gross margin: ₹1.2 lakh/month gross contribution before fixed costs. Against a ₹60,000 realistic fixed cost base, this generates ₹60,000/month surplus to reinvest or compensate founders.

---

## Part 10: Settlement Cycles — Full Detail

### Store Settlement

|Parameter|Specification|
|---|---|
|Cycle|Weekly (Monday payout for Mon–Sun)|
|Hold period|T+5 minimum (refund window)|
|Method|NEFT / IMPS to registered bank|
|Negative balance handling|Carried forward to next cycle|
|Dispute hold|Disputed order amount held until resolved|
|Early payout option (Phase 2)|1–2% fee on advance amount|

Weekly settlement is industry standard — Zomato (Wednesday credits), Swiggy (weekly), Blinkit (weekly). It is not a penalty. However, very small merchants operating on daily cash cycles (tea stall owner paying suppliers daily) may experience friction. The solution is an **on-demand early payout feature** at a small fee, not instant settlement for all.

### Rider Settlement

|Parameter|Specification|
|---|---|
|Cycle|Weekly (Monday)|
|Daily advance|Up to ₹300–500/day|
|Minimum retained balance|₹100|
|Payout method|IMPS to bank|
|Statement|Per-order breakdown in app|
|COD hold|Cash collected − deposited = deducted from weekly payout|

### Settlement Flow Diagram

```
Monday–Sunday: Orders flow, ledger entries created per order

Sunday end of day:
  Platform calculates:
    Each store → gross orders − commissions − refunds − disputes = net payable
    Each rider → total deliveries × per-order rate + bonuses − COD pending − advances = net payable

Monday:
  Batch NEFT/IMPS transfers to all stores and riders
  Statements generated and pushed to app
  New cycle begins
```

---

## Part 11: Ledger Architecture

Every rupee in the system must trace back to a ledger entry. This is the difference between a platform that scales cleanly and one where reconciliation becomes a full-time job at 500+ orders/day.

### Minimum Ledger Tables

|Table|Key Fields|Purpose|
|---|---|---|
|orders|order_id, store_id, customer_id, gross_value, status, created_at|Master order record|
|payments|payment_id, order_id, method, gateway_txn_id, amount, status|Payment capture|
|commissions|commission_id, order_id, store_id, rate, amount|Store commission entries|
|delivery_fees|fee_id, order_id, charged_to_customer, paid_to_rider, platform_margin|Delivery economics|
|platform_fees|fee_id, order_id, amount|Platform fee per order|
|rider_earnings|earning_id, rider_id, order_id, base, distance_km, distance_pay, bonus_type, bonus_amount, total|Rider pay per delivery|
|store_settlements|settlement_id, store_id, period_start, period_end, gross, commission_deducted, refunds_deducted, net, payout_status|Weekly store payout|
|rider_payouts|payout_id, rider_id, period, total_earned, advances_withdrawn, cod_pending, net_payout, status|Weekly rider payout|
|refunds|refund_id, order_id, reason, amount, initiated_by, status, created_at|Refund tracking|
|disputes|dispute_id, order_id, raised_by, category, resolution, amount_adjusted, status|Dispute resolution|
|cod_transactions|cod_id, rider_id, order_id, amount_collected, amount_deposited, status|Cash on delivery tracking|
|advances|advance_id, rider_id, amount, fee_charged, status, created_at|Daily advance withdrawals|

---

## Part 12: COD Policy

**Recommendation: Do not launch with COD. Add it in Month 4–6 after core workflow is stable.**

### Why COD is an MVP Risk

|Risk|Description|
|---|---|
|Working capital exposure|At 200 orders/day with 25% COD, ₹15,000–₹25,000 of platform money sits with riders daily|
|Reconciliation complexity|Every COD order requires deposit confirmation before settlement|
|Rider fraud surface|Rider can collect cash, go inactive, dispute amount|
|Support overhead|COD disputes are the most labor-intensive to resolve|

### COD Transition Plan

|Phase|Policy|
|---|---|
|MVP (Month 1–3)|Online payment only (UPI, cards). Offer ₹5 cashback on UPI to drive adoption.|
|Month 4–6|Enable COD with ₹10–15 COD handling fee charged to customer. Rider required to deposit at store.|
|Month 6+|Full COD support with automated reconciliation in ledger.|

---

## Part 13: The Four Dispute Buckets

All delivery platform complaints reduce to four categories. Every P0 feature in your apps must map to one of these.

|Dispute Type|Example|Required Feature|
|---|---|---|
|**Money**|"Why did I receive ₹240 instead of ₹270?"|Commission ledger, deduction history, refund trail|
|**Delivery**|"My order never arrived" / "Wrong address"|OTP confirmation, photo proof, live tracking, rider-customer chat|
|**Trust**|"Rider behaved inappropriately"|Rating system, complaint workflow, account suspension trigger, admin review queue|
|**Earnings**|"Why did I earn ₹23 for this order?"|Per-order earnings breakdown, incentive explanation, weekly statement|

---

## Part 14: What to Validate in the Field (Hypotheses, Not Facts)

The following numbers in this document are hypotheses requiring real-world validation. Do not treat them as final.

|Hypothesis|What to Validate|How|
|---|---|---|
|₹20 base + ₹4/km rider rate|Whether Pune riders find this acceptable given fuel costs and wait time|Talk to 20 local delivery riders before launch|
|₹25–45 delivery fee|Whether customers in Pimpri accept this fee on orders of various sizes|Small pilot with 50 test orders across AOV ranges|
|₹300 average order value|Whether local store orders average this or are lower|Survey 10 local stores for their typical ticket size|
|54 orders/day realistic break-even|Whether this is achievable within 90 days of launch|Track weekly and adjust fixed costs accordingly|
|8–10% commission acceptance|Whether local kirana and food stores will onboard at this rate|Approach 20 stores before building anything|

**The single most important pre-launch action:** Before writing a line of code, walk into 20 stores in your target locality and ask two questions: "Would you pay 8–10% commission per order for delivery customers?" and "What do you currently pay for delivery or customer acquisition?" Their answers will validate or invalidate the entire commission model.

---

## Part 15: What Is Not a Hypothesis (Established Practice)

These items do not require validation — they are supported by decades of marketplace precedent and the specific evidence from this research.

|Decision|Why It Is Not a Hypothesis|
|---|---|
|Customer pays platform, not store|Every marketplace from Amazon to Zomato uses this. Payment control is the prerequisite for refunds, disputes, and fraud handling.|
|Ledger-based accounting from day one|Every fintech and marketplace that skipped this has paid for it in reconciliation debt. Retrofitting a ledger onto an existing transaction system is extremely costly.|
|Weekly settlement, not instant|Zomato, Swiggy, Blinkit, Meesho, Flipkart all use delayed settlement. Float is necessary for refunds, disputes, and chargeback handling.|
|Phased revenue streams|Commission-only models are fragile. Platform fee and delivery margin must exist from day one. Ads and subscriptions are Phase 2.|
|Do not change incentive rates without 30-day notice|Every rider complaint video documents this as the primary cause of disengagement.|

---

## Part 16: One-Page Summary Table

|Parameter|Specification|Confidence|
|---|---|---|
|Commission (restaurants)|10–12%|High — validate with 20 stores before launch|
|Commission (kirana/grocery)|8–10%|High|
|Commission (tea/snack)|5–8%|High|
|Minimum activity fee|₹99–149/month below 30 orders|Medium|
|Delivery fee (₹100–300 orders)|₹25–35|Medium — needs price testing|
|Platform fee|₹5|High|
|Rider base rate|₹20|Medium — validate with local riders|
|Rider distance rate|₹4/km above 1km|Medium|
|Peak bonus|₹10|Medium|
|Rain/night bonus|₹15–25|Medium|
|Settlement cycle|Weekly (Monday)|High|
|Daily rider advance|Up to ₹300–500|High|
|Break-even (realistic)|~54 orders/day|High|
|Gateway preference|UPI-first (₹0 MDR under ₹2,000)|High|
|COD at launch|No — add Month 4–6|High|
|Ledger architecture|Required from day one|High|