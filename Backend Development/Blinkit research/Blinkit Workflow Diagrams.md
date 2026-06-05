# Blinkit Detailed Workflow Diagrams

This document contains comprehensive workflow diagrams and business process flows for the Blinkit application. It details both the Customer and Rider apps, including authentication, KYC onboarding, and real-time order execution.

## 1. Customer App Flow (Detailed)

This flowchart illustrates the complete customer journey: Authentication, Location resolution, Product Discovery, and Checkout.

```mermaid
flowchart TD
    %% Sub-flow: Authentication & Location
    subgraph Auth_Location [1. Auth & Location]
        C1([Launch App]) --> C2{Is Session Active?}
        C2 -- No --> C3[Enter Phone Number]
        C3 --> C4[Receive SMS OTP]
        C4 --> C5[Submit & Verify OTP]
        C5 --> C6[Grant Location Permission]
        C2 -- Yes --> C6
        C6 --> C7{Is Address Serviceable?}
        C7 -- No --> C8[Show 'We are coming soon' UI]
        C7 -- Yes --> C9[Assign Dark Store & Load Homepage]
    end

    %% Sub-flow: Discovery & Cart
    subgraph Discovery_Cart [2. Discovery & Cart]
        C9 --> C10{User Action}
        C10 -->|Search| C11[Type in Search Bar]
        C10 -->|Browse| C12[Scroll Categories & Widgets]
        C11 --> C13[View Product Details]
        C12 --> C13
        C13 --> C14[Add to Cart]
        C14 --> C15[Update Quantity]
        C15 --> C16[Backend: Validate Limits & Inventory]
        C16 --> C17[Recalculate: Cart Total + Delivery Fee + Taxes]
    end

    %% Sub-flow: Checkout
    subgraph Checkout [3. Checkout & Payment]
        C17 --> C18[Proceed to Checkout]
        C18 --> C19[Apply Promo Code / Coupons]
        C19 --> C20[Select Payment Method]
        C20 --> C21{Payment Type}
        C21 -->|UPI / Wallet| C22[Invoke App Intent]
        C21 -->|Card| C23[Enter OTP via Gateway]
        C21 -->|COD| C24[Confirm Cash Order]
        C22 & C23 & C24 --> C25{Payment Success?}
        C25 -- No --> C26[Retry Payment]
        C25 -- Yes --> C27([Order Placed Successfully])
    end

    Auth_Location --> Discovery_Cart
    Discovery_Cart --> Checkout
```

## 2. Rider Registration & KYC Flow

The flow a delivery partner takes to join the platform. It involves document verification (KYC), vehicle registration, and background checks.

```mermaid
flowchart TD
    %% Registration and Document Upload
    subgraph Onboarding [Rider Onboarding & KYC]
        R1([Download Rider App]) --> R2[Enter Phone Number & OTP]
        R2 --> R3[Basic Details: Name, City]
        R3 --> R4[Select Vehicle Type: Bike, Cycle, EV]
        
        R4 --> R5[KYC Document Upload]
        R5 --> R6[Aadhaar Card Front/Back]
        R6 --> R7[PAN Card]
        R7 --> R8[Driving License]
        R8 --> R9[Take Selfie]
        
        R9 --> R10[Bank Account Details]
        R10 --> R11[Backend: Verification API / Manual QA]
        R11 --> R12{Status}
        
        R12 -- Rejected --> R13[Re-upload Invalid Documents]
        R13 --> R11
        R12 -- Approved --> R14[Watch Safety/Training Videos]
        R14 --> R15([Account Activated])
    end
```

## 3. Rider Delivery Flow (Active Shift)

The lifecycle of an active delivery partner fulfilling an order.

```mermaid
flowchart TD
    %% Active Order Cycle
    subgraph DeliveryCycle [Order Execution]
        D1([Go Online / Start Shift]) --> D2[Wait in Geofence Zone]
        
        D2 --> D3[Receive Order Ping]
        D3 --> D4{Accept within 30s?}
        D4 -- No --> D5[Missed Ping / Penalty]
        D5 --> D2
        
        D4 -- Yes --> D6[Navigate to Dark Store]
        D6 --> D7[Arrive at Store & Wait]
        D7 --> D8[Store Packer Assigns Bags]
        D8 --> D9[Scan Bag QR Codes]
        
        D9 --> D10[Confirm Pickup & Start Trip]
        D10 --> D11[Navigate to Customer Location]
        D11 --> D12[Arrive at Destination]
        D12 --> D13[Call Customer if needed]
        
        D13 --> D14[Handover Items]
        D14 --> D15{Requires Cash/Code?}
        D15 -- Yes --> D16[Collect Cash / Enter OTP]
        D15 -- No --> D17[Mark as Delivered]
        D16 --> D17
        
        D17 --> D18([Order Complete - Earn Payout])
        D18 --> D2
    end
```

## 4. End-to-End System Integration Sequence

A unified sequence diagram showing how the Customer App, Backend Services, Dark Store (Order Management), and Rider App communicate synchronously and asynchronously.

```mermaid
sequenceDiagram
    participant Cust as Customer App
    participant Auth as API Gateway & Services
    participant DS as Dark Store (OMS)
    participant RiderApp as Rider App
    
    Cust->>Auth: Place Order (Payment Success)
    Auth->>DS: OrderCreated Event
    
    Note over DS: Picking & Packing (2-5 mins)
    DS->>Auth: OrderPacked Event
    
    Auth->>RiderApp: Assign Order Ping (Broadcasting)
    RiderApp-->>Auth: Accept Order
    
    Note over RiderApp,DS: Rider travels to Dark Store
    RiderApp->>DS: Arrive at Store
    DS->>RiderApp: Handover Bags (Rider Scans QR)
    
    RiderApp->>Auth: Picked Up Event
    Auth-->>Cust: Push Notification: "Rider is on the way"
    
    Note over RiderApp,Cust: Rider navigates to Customer
    RiderApp->>Cust: Arrive & Handover
    
    RiderApp->>Auth: Delivered Event
    Auth-->>Cust: Push Notification: "Order Delivered"
```
