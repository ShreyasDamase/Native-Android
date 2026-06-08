# Comprehensive Zomato & Blinkit Workflow Diagrams

This document outlines the detailed workflows for the Customer App, Rider App, Merchant/Shopkeeper App, and the complete End-to-End Order Lifecycle. It incorporates all edge cases across food delivery (Zomato) and quick commerce (Blinkit) paradigms.

---

## 1. Customer App Flow (Detailed with Edge Cases)

This flowchart tracks the user's journey from app launch to checkout, including failure states and edge cases like inventory loss, out-of-zone locations, and payment gateway failures.

```mermaid
flowchart TD
    %% Auth & Location
    subgraph Location_Auth [1. Auth & Location Check]
        Start([Launch App]) --> CheckAuth{Logged In?}
        CheckAuth -- No --> Login[Enter Phone Number]
        Login --> SendOTP[Send SMS OTP]
        SendOTP --> VerifyOTP{OTP Valid?}
        VerifyOTP -- No --> RetryOTP[Retry / Block after 3 attempts]
        VerifyOTP -- Yes --> AskLoc[Grant Location]
        CheckAuth -- Yes --> AskLoc
        
        AskLoc --> ResolveLoc{Is Serviceable?}
        ResolveLoc -- No --> ShowUnserviceable[Show 'Out of Zone' or 'Coming Soon']
        ResolveLoc -- Yes --> CheckTime{Store Open?}
        CheckTime -- No --> ShowPreOrder[Show 'Store Closed' - Allow Pre-order/Schedule]
        CheckTime -- Yes --> LoadHome[Load Dynamic Homepage]
    end

    %% Discovery & Cart
    subgraph Discovery_Cart [2. Product Discovery & Cart]
        LoadHome --> Browse[Browse / Search Categories]
        Browse --> AddItem[Add Item to Cart]
        AddItem --> CartCheck{Stock Limit Exceeded?}
        CartCheck -- Yes --> BlockAdd[Show 'Max Quantity Reached']
        CartCheck -- No --> ViewCart[Go to Cart]
        
        ViewCart --> ValidateCart{Backend Validates Cart}
        ValidateCart -- "Item went Out of Stock" --> AutoRemove[Auto-remove/suggest replacement]
        ValidateCart -- "Valid" --> Calc[Calculate Surge, Delivery Fee, Taxes]
        AutoRemove --> Calc
    end

    %% Checkout & Edge Cases
    subgraph Checkout [3. Checkout & Payment]
        Calc --> ApplyPromo[Apply Promo Code]
        ApplyPromo --> ValidatePromo{Promo Valid?}
        ValidatePromo -- No --> InvalidPromo[Show 'Coupon Expired / Not Applicable']
        ValidatePromo -- Yes --> ProceedPay[Proceed to Payment]
        InvalidPromo --> ProceedPay
        
        ProceedPay --> SelectPay[Select UPI/Card/COD]
        SelectPay --> PayGateway[Payment Gateway]
        PayGateway --> CheckSuccess{Payment Success?}
        
        CheckSuccess -- "Timeout/Failure" --> PayFail[Mark Order 'Pending Payment']
        PayFail --> RetryPay[Prompt User to Retry]
        RetryPay --> PayGateway
        
        CheckSuccess -- Yes --> OrderSuccess([Order Placed Successfully])
    end

    Location_Auth --> Discovery_Cart
    Discovery_Cart --> Checkout
```

---

## 2. Rider App Flow (Detailed with KYC & Edge Cases)

This diagram outlines the complete lifecycle of a Delivery Partner, from onboarding (with KYC edge cases) to the active delivery shift handling exceptions on the road.

```mermaid
flowchart TD
    %% Onboarding & KYC
    subgraph Onboarding [1. Onboarding & KYC]
        R1([Sign Up via Phone]) --> R2[Select Vehicle & City]
        R2 --> R3[Upload KYC: Aadhaar, PAN, DL, Selfie]
        R3 --> R4[Backend API Verification (OCR + DB Check)]
        R4 --> R5{Verification Status?}
        
        R5 -- "Blurry / Mismatch" --> R6[Reject & Prompt Re-upload]
        R6 --> R3
        R5 -- "Blacklisted" --> R7[Permanent Ban]
        R5 -- "Approved" --> R8[Bank Details & Bag/T-shirt issuance]
        R8 --> R9[Training Modules] --> R10([Account Active])
    end

    %% Active Shift
    subgraph ActiveShift [2. Active Shift & Delivery Execution]
        D1([Go Online]) --> D2[Wait in High-Demand Geofence]
        D2 --> D3[Receive Order Ping]
        
        D3 --> D4{Accept within timeout?}
        D4 -- No --> D5[Missed Ping -> Affects Acceptance Rate / Penalty]
        D5 --> D2
        
        D4 -- Yes --> D6[Navigate to Dark Store / Restaurant]
        D6 --> D7{Arrived at Store?}
        D7 -- Yes --> D8[Wait for Order Prep/Packing]
        D7 -- "GPS Mismatch" --> D9[Block Arrival marking until in radius]
        
        D8 --> D10{Order Ready?}
        D10 -- No --> D11[Wait / Request Merchant Expedite]
        D10 -- Yes --> D12[Scan Bags QR / Confirm Pickup]
        
        D12 --> D13[Navigate to Customer Location]
        D13 --> D14{Customer Available?}
        
        D14 -- No --> D15[Call Customer -> Trigger Support IVR]
        D15 -- "Unreachable after 5 mins" --> D16[Mark as RTO / Cancelled by Support]
        D14 -- Yes --> D17[Handover Order]
        
        D17 --> D18{Payment Type}
        D18 -- COD --> D19[Collect Cash / UPI QR]
        D18 -- Prepaid --> D20[Verify Delivery Code/OTP]
        
        D19 --> D21{Exact Change/Valid Cash?}
        D21 -- No --> D22[Rider Wallet Deduction / Support Escalation]
        D21 -- Yes --> D20
        
        D20 --> D23([Mark Delivered -> Earn Payout])
        D16 --> D23
        D23 --> D2
    end
```

---

## 3. Merchant / Shop Keeper App Flow

The dark store (Blinkit) or restaurant partner (Zomato) workflow for accepting orders, managing inventory, and handing over to riders.

```mermaid
flowchart TD
    %% Order Prep Cycle
    subgraph MerchantApp [Store / Restaurant Partner App]
        M1([App Online]) --> M2[Order Notification Rings]
        
        M2 --> M3{Accept Order?}
        M3 -- No --> M4[Cancellation Penalty & Auto-Refund to User]
        M3 -- Yes --> M5[Order moved to 'Preparing' / 'Picking']
        
        M5 --> M6{Inventory Check during prep}
        M6 -- "Item Out of Stock" --> M7[Mark Item Unavailable]
        M7 --> M8[Trigger Partial Refund to User / Call User for Replacement]
        M6 -- "All Items Available" --> M9[Pack Order]
        
        M9 --> M10[Attach QR Code / Invoice Receipt]
        M10 --> M11[Mark 'Ready for Pickup']
        
        M11 --> M12{Rider Arrived?}
        M12 -- No --> M13[Store wait time increases / Escalate]
        M12 -- Yes --> M14[Scan & Handover to Rider]
        M14 --> M15([Order dispatched from Store])
    end
    
    %% Catalog Management
    subgraph CatalogMgmt [Store Catalog Management]
        C1([Dashboard]) --> C2[Mark Items Out of Stock]
        C1 --> C3[Adjust Store Timings / Surge loads]
        C1 --> C4[View Daily Settlements & Payouts]
        C1 --> C5[Reconcile Damage/Non-Delivery Losses]
    end
```

---

## 4. End-to-End Order Lifecycle & Edge Cases (Macro Flow)

A holistic system-level sequence diagram showing how all apps and backend services interact, including cancellation handling and delays.

```mermaid
sequenceDiagram
    participant User as Customer App
    participant API as Backend (Gateway/OMS)
    participant Merch as Merchant/Store App
    participant Rider as Rider App

    User->>API: 1. Place Order & Pay
    API-->>User: Order Confirmed
    API->>Merch: 2. Order Received Event
    
    %% Merchant Processing
    alt Store rejects or Item Out of Stock
        Merch-->>API: Item missing / Reject
        API-->>User: Push: Order Cancelled / Partial Refund Initiated
    else Store Accepts
        Merch->>API: Order Accepted & Picking
        API->>Rider: 3. Ping Nearest Available Rider
    end
    
    %% Rider Assignment
    alt Rider Ignores Ping
        Rider--xAPI: Missed / Timeout
        API->>Rider: Ping next best Rider
    else Rider Accepts
        Rider->>API: Accepted
        API-->>User: Push: "Rider Assigned"
    end
    
    %% Store & Rider Synchronization
    Rider->>Merch: 4. Rider arrives at Store
    alt Store Delay
        Merch-->>API: Prep delayed
        API-->>User: Update ETA (Delayed)
    else Order Ready
        Merch->>Rider: 5. Handover Bags (QR Scan)
        Rider->>API: Order Picked Up
        API-->>User: Push: "Order is out for delivery"
    end
    
    %% Customer Delivery & Edge Case
    Rider->>User: 6. Rider reaches Customer location
    alt Customer Unreachable
        Rider->>API: Cannot reach customer
        API->>User: Automated IVR Call
        API->>Rider: Authorize Return To Origin (RTO)
    else Customer Available
        Rider->>User: Handover & Code verification
        Rider->>API: 7. Marked Delivered
        API-->>User: Push: "Delivered!" & Trigger Feedback Flow
        API->>Merch: Order Complete - Settle Ledger
    end
```
