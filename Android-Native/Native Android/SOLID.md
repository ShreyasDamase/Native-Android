```mermaid
graph TB
    START[🎯 Start SOLID Journey]
    
    START --> SRP[<b>Single Responsibility Principle</b><br/>One class = One reason to change]
    START --> OCP[<b>Open/Closed Principle</b><br/>Open for extension, Closed for modification]
    START --> LSP[<b>Liskov Substitution Principle</b><br/>Subtypes must be substitutable]
    START --> ISP[<b>Interface Segregation Principle</b><br/>Many specific interfaces > One general]
    START --> DIP[<b>Dependency Inversion Principle</b><br/>Depend on abstractions, not concretions]
    
    SRP --> SRP1[Task 1: Detect Multiple Responsibilities]
    SRP --> SRP2[Task 2: Refactor Fat Classes]
    SRP --> SRP3[Task 3: Build Clean Architecture]
    
    SRP1 --> SRPQ{Can you identify<br/>5+ responsibilities?}
    SRPQ -->|Yes| SRP2
    SRPQ -->|No| REVIEW1[📚 Review: What is a 'responsibility'?]
    REVIEW1 --> SRP1
    
    SRP2 --> SRP3
    SRP3 --> SRPD[✅ SRP Mastered!]
    
    OCP --> OCP1[Task 1: Spot Modification Violations]
    OCP --> OCP2[Task 2: Design with Polymorphism]
    OCP --> OCP3[Task 3: Extensible System]
    
    OCP1 --> OCPQ{Can you add features<br/>without modifying code?}
    OCPQ -->|Yes| OCP2
    OCPQ -->|No| REVIEW2[📚 Review: Abstraction & Inheritance]
    REVIEW2 --> OCP1
    
    OCP2 --> OCP3
    OCP3 --> OCPD[✅ OCP Mastered!]
    
    LSP --> LSP1[Task 1: Find Broken Hierarchies]
    LSP --> LSP2[Task 2: Fix Inheritance]
    LSP --> LSP3[Task 3: Rectangle/Square Problem]
    
    LSP1 --> LSPQ{Does subclass break<br/>parent's contract?}
    LSPQ -->|No violations| LSP2
    LSPQ -->|Found violations| REVIEW3[📚 Review: Behavioral Subtyping]
    REVIEW3 --> LSP1
    
    LSP2 --> LSP3
    LSP3 --> LSPD[✅ LSP Mastered!]
    
    ISP --> ISP1[Task 1: Identify Fat Interfaces]
    ISP --> ISP2[Task 2: Segregate Interfaces]
    ISP --> ISP3[Task 3: Multi-Function System]
    
    ISP1 --> ISPQ{Are clients forced to<br/>implement unused methods?}
    ISPQ -->|Yes, found them| ISP2
    ISPQ -->|No| REVIEW4[📚 Review: Interface Cohesion]
    REVIEW4 --> ISP1
    
    ISP2 --> ISP3
    ISP3 --> ISPD[✅ ISP Mastered!]
    
    DIP --> DIP1[Task 1: Detect Tight Coupling]
    DIP --> DIP2[Task 2: Introduce Abstractions]
    DIP --> DIP3[Task 3: Dependency Injection]
    
    DIP1 --> DIPQ{Do high-level modules<br/>depend on low-level details?}
    DIPQ -->|Yes, tightly coupled| DIP2
    DIPQ -->|No| REVIEW5[📚 Review: Abstraction Layers]
    REVIEW5 --> DIP1
    
    DIP2 --> DIP3
    DIP3 --> DIPD[✅ DIP Mastered!]
    
    SRPD --> MASTER
    OCPD --> MASTER
    LSPD --> MASTER
    ISPD --> MASTER
    DIPD --> MASTER
    
    MASTER[🏆 SOLID MASTER<br/>Ready for Clean Architecture!]
    
    MASTER --> NEXT[Next Steps]
    NEXT --> CLEAN[Clean Architecture]
    NEXT --> DDD[Domain-Driven Design]
    NEXT --> PATTERNS[Design Patterns]
    
    style START fill:#4A90E2,stroke:#357ABD,color:#fff
    style MASTER fill:#27AE60,stroke:#1E8449,color:#fff
    
    style SRP fill:#3498DB,stroke:#2874A6,color:#fff
    style OCP fill:#2ECC71,stroke:#239B56,color:#fff
    style LSP fill:#9B59B6,stroke:#7D3C98,color:#fff
    style ISP fill:#F39C12,stroke:#D68910,color:#fff
    style DIP fill:#E74C3C,stroke:#C0392B,color:#fff
    
    style SRPD fill:#27AE60,stroke:#1E8449,color:#fff
    style OCPD fill:#27AE60,stroke:#1E8449,color:#fff
    style LSPD fill:#27AE60,stroke:#1E8449,color:#fff
    style ISPD fill:#27AE60,stroke:#1E8449,color:#fff
    style DIPD fill:#27AE60,stroke:#1E8449,color:#fff
```


# 🎯 SOLID Principles - System Design Projects (Pure Kotlin)

## 🧠 Learning Strategy
Each project has 2 phases:
1. **BAD VERSION** - Build it violating the principle (understand the pain)
2. **REFACTOR** - Fix it following SOLID (feel the relief)

---

## 1️⃣ Single Responsibility Principle (SRP)
**"A class should have only ONE reason to change"**

### 📦 PROJECT 1: Library Management System
**Difficulty:** Medium

**Requirements:**
Build a library system with these features:
- Add/Remove books from catalog
- Search books by title/author/ISBN
- Borrow/Return books
- Calculate late fees
- Send overdue notifications (email/SMS)
- Generate reports (borrowed books, overdue books)
- Export data to CSV/JSON
- Validate book data (ISBN format, etc.)

**Phase 1 - BAD VERSION:**
Create a single `LibraryManager` class that does EVERYTHING above.

**Phase 2 - REFACTOR:**
Split into separate classes, each with ONE responsibility:
- `BookRepository` - Storage operations
- `BookSearchService` - Search logic
- `BorrowingService` - Borrow/return logic
- `FeeCalculator` - Late fee calculation
- `NotificationService` - Sending notifications
- `ReportGenerator` - Generate reports
- `DataExporter` - Export functionality
- `BookValidator` - Validation logic

**Critical Thinking Questions:**
1. If email provider changes, how many classes need modification?
2. If you add a new report type, what changes?
3. Can you test fee calculation independently?

---

### 🏦 PROJECT 2: Banking Transaction System
**Difficulty:** Hard

**Requirements:**
Build a banking system that handles:
- Account creation/deletion
- Deposits/Withdrawals
- Transfer between accounts
- Transaction history logging
- Fraud detection (simple rules: amount > 10000, multiple rapid transactions)
- Interest calculation for savings accounts
- Account statement generation (PDF/Email)
- Audit trail for compliance
- Currency conversion for international transfers

**Phase 1:** Create `BankingSystem` class doing everything.

**Phase 2:** Refactor following SRP. Design your own class structure!

**Challenge:** Add a new feature "Recurring payments" - should require changes to only 1-2 classes, not the entire system.

---

## 2️⃣ Open/Closed Principle (OCP)
**"Open for extension, Closed for modification"**

### 💳 PROJECT 3: Payment Processing System
**Difficulty:** Medium

**Requirements:**
Build a payment gateway supporting:
- Credit Card (Visa, Mastercard - different validation)
- Debit Card
- PayPal
- Bitcoin
- Bank Transfer
- UPI (India)
- Apple Pay / Google Pay

Each payment method has:
- Different validation rules
- Different processing fees
- Different settlement times
- Different refund policies

**Phase 1 - BAD VERSION:**
```kotlin
class PaymentProcessor {
    fun processPayment(type: String, amount: Double) {
        when (type) {
            "CREDIT_CARD" -> { /* logic */ }
            "PAYPAL" -> { /* logic */ }
            "BITCOIN" -> { /* logic */ }
            // Adding new method requires modifying this class!
        }
    }
}
```

**Phase 2 - REFACTOR:**
Design using:
- Interface: `PaymentMethod`
- Sealed class for payment types
- Strategy pattern
- Each payment method in separate class

**Challenge:** 
1. Add "Buy Now Pay Later" (BNPL) payment WITHOUT modifying existing code
2. Add "Cryptocurrency" category with 5 new coins - should take < 5 minutes

**Critical Questions:**
- Can you add a payment method without recompiling `PaymentProcessor`?
- How do you handle payment method-specific features?

---

### 🎮 PROJECT 4: Game Character System
**Difficulty:** Hard

**Requirements:**
Design RPG character system with:
- Character types: Warrior, Mage, Archer, Rogue, Paladin
- Each has different:
  - Attack calculations (melee, ranged, magic)
  - Defense mechanisms (armor, dodge, shields, magic resist)
  - Special abilities (rage, teleport, stealth, heal)
  - Equipment requirements
  - Level-up bonuses

**Phase 1:** Single `Character` class with giant when/if statements.

**Phase 2:** Design with OCP in mind using:
- Abstract base classes
- Interfaces for behaviors
- Composition over inheritance

**Advanced Challenge:**
1. Add "Dual-class" system (Warrior-Mage hybrid)
2. Add equipment that modifies abilities
3. Add status effects (poison, burn, freeze)

All without modifying existing character classes!

---

## 3️⃣ Liskov Substitution Principle (LSP)
**"Subtypes must be substitutable for their base types"**

### 🚗 PROJECT 5: Vehicle Rental System
**Difficulty:** Medium

**Requirements:**
Build a rental system for:
- Cars (start, stop, accelerate, brake, refuel)
- Electric Cars (start, stop, accelerate, brake, charge - NO refuel!)
- Motorcycles (start, stop, accelerate, brake, refuel)
- Bicycles (start, stop, pedal - NO engine, NO fuel!)
- Scooters (Electric) (start, stop, accelerate, charge)

**Phase 1 - BAD VERSION:**
```kotlin
open class Vehicle {
    open fun startEngine() { }
    open fun refuel() { }
    open fun charge() { }
}

class Bicycle : Vehicle() {
    override fun startEngine() {
        throw UnsupportedOperationException("No engine!")
    }
    override fun refuel() {
        throw UnsupportedOperationException("No fuel!")
    }
}
```

**Phase 2 - REFACTOR:**
Design proper hierarchy where:
- Any `Vehicle` can be used interchangeably
- No methods throw "not supported" exceptions
- Subtypes don't weaken parent contracts

**Hint:** Consider interfaces like `Drivable`, `Fuelable`, `Chargeable`, `Pedaled`

**Critical Test:**
```kotlin
fun rentVehicle(vehicle: Vehicle) {
    vehicle.start()
    vehicle.move()
    vehicle.stop()
    // Should work for ALL vehicles without crashes!
}
```

---

### 📐 PROJECT 6: Shape Rendering Engine
**Difficulty:** Hard

**Requirements:**
Build a geometric engine supporting:
- 2D Shapes: Circle, Rectangle, Triangle, Square
- 3D Shapes: Sphere, Cube, Cylinder, Cone
- Operations: Calculate Area, Perimeter, Volume, Surface Area

**The Trap:**
```kotlin
open class Shape2D(open var width: Double, open var height: Double)

class Square(side: Double) : Shape2D(side, side) {
    override var width: Double
        set(value) {
            field = value
            height = value // 🚨 Breaks expectations!
        }
}
```

**Challenge:** Design hierarchy where Square and Rectangle coexist without LSP violations.

**Critical Questions:**
1. Is Square a subtype of Rectangle in programming?
2. Should 3D shapes inherit from 2D shapes?
3. How to handle shapes with different property requirements?

---

## 4️⃣ Interface Segregation Principle (ISP)
**"Clients shouldn't depend on interfaces they don't use"**

### 🤖 PROJECT 7: Smart Home Automation
**Difficulty:** Medium

**Requirements:**
Design smart device system for:
- Smart Lights (on/off, brightness, color)
- Smart Thermostat (on/off, temperature, mode)
- Smart Lock (lock/unlock, status)
- Smart Speaker (on/off, volume, play/pause, voice commands)
- Security Camera (on/off, record, live stream)
- Smart Plug (on/off only)

**Phase 1 - BAD VERSION:**
```kotlin
interface SmartDevice {
    fun turnOn()
    fun turnOff()
    fun setBrightness(level: Int) // ❌ Not all devices have brightness
    fun setTemperature(temp: Int) // ❌ Only thermostat
    fun lock() // ❌ Only locks
    fun unlock() // ❌ Only locks
    fun play() // ❌ Only speaker
    fun record() // ❌ Only camera
}

class SmartPlug : SmartDevice {
    override fun turnOn() { /* OK */ }
    override fun turnOff() { /* OK */ }
    override fun setBrightness(level: Int) { /* ??? */ }
    override fun setTemperature(temp: Int) { /* ??? */ }
    // ... forced to implement irrelevant methods!
}
```

**Phase 2 - REFACTOR:**
Create small, cohesive interfaces:
- `Switchable`, `Dimmable`, `ColorChangeable`, `Lockable`, `Playable`, `Recordable`, etc.

**Challenge:** Add new device "Smart TV" that is Switchable + Playable + VolumeControlled. Should compose existing interfaces!

---

### 🖨️ PROJECT 8: Document Processing System
**Difficulty:** Hard

**Requirements:**
Build document processor for:
- Scanners (scan only)
- Printers (print only)
- Fax Machines (fax only)
- Photocopiers (print + scan)
- All-in-One (print + scan + fax)
- Network Printers (print + network settings)
- 3D Printers (print 3D models - different from paper)

Features to handle:
- Paper size configuration
- Color/BW mode
- Duplex printing
- Scan resolution
- Network configuration
- Cloud connectivity

**Phase 1:** Fat interface `IOfficeDevice` with ALL methods.

**Phase 2:** Segregate into focused interfaces.

**Advanced:** Add "Mobile Printer" that prints + has battery status. Should reuse existing interfaces!

---

## 5️⃣ Dependency Inversion Principle (DIP)
**"Depend on abstractions, not concretions"**

### 📱 PROJECT 9: Notification System
**Difficulty:** Medium

**Requirements:**
Build notification service supporting:
- Email (SMTP)
- SMS (Twilio API)
- Push Notifications (Firebase)
- Slack
- Discord
- WhatsApp
- Telegram

Features:
- Send to single recipient
- Send to multiple recipients
- Schedule notifications
- Retry on failure
- Notification templates
- Priority levels (urgent, normal, low)

**Phase 1 - BAD VERSION:**
```kotlin
class NotificationService {
    private val emailSender = EmailSender() // 🚨 Tight coupling!
    private val smsSender = SMSSender() // 🚨 Concrete dependency!
    
    fun sendNotification(type: String, message: String) {
        when (type) {
            "EMAIL" -> emailSender.send(message)
            "SMS" -> smsSender.send(message)
        }
    }
}
```

**Phase 2 - REFACTOR:**
- Create `NotificationChannel` interface
- Inject dependencies via constructor
- Use dependency injection principles

```kotlin
interface NotificationChannel {
    fun send(message: Message)
}

class NotificationService(
    private val channels: List<NotificationChannel>
) {
    // Implementation
}
```

**Challenge:** 
1. Add "Microsoft Teams" channel without modifying NotificationService
2. Create multi-channel notification (send via Email AND SMS)
3. Add fallback mechanism (if Email fails, try SMS)

---

### 🗄️ PROJECT 10: E-commerce Order System
**Difficulty:** Hard

**Requirements:**
Build order processing system with:
- Multiple databases (MySQL, PostgreSQL, MongoDB)
- Multiple payment gateways
- Multiple shipping providers (FedEx, UPS, DHL)
- Multiple notification channels
- Multiple inventory systems

**Architecture layers:**
1. **Controller Layer** - Receives orders
2. **Service Layer** - Business logic
3. **Repository Layer** - Data access
4. **External Services** - Payment, Shipping, Notifications

**Phase 1 - BAD VERSION:**
```kotlin
class OrderService {
    private val database = MySQLDatabase() // 🚨 Tight coupling
    private val payment = StripePayment() // 🚨 Can't switch
    private val shipping = FedExAPI() // 🚨 Locked in
}
```

**Phase 2 - REFACTOR:**
Design with dependency inversion:
- Abstract all external dependencies
- Use constructor injection
- Allow swapping implementations at runtime

**Advanced Challenge:**
Build a configuration system where you can switch:
- Database provider via config
- Payment gateway per customer
- Shipping provider per region
- All without code changes!

---

## 🎯 COMPLETE INTEGRATION PROJECT

### 🏪 PROJECT 11: Full E-Commerce Platform (ALL SOLID Principles)
**Difficulty:** Expert

**Apply ALL 5 SOLID principles to build:**

**Modules:**
1. **User Management** (SRP, DIP)
   - Authentication, Registration, Profile
   
2. **Product Catalog** (SRP, OCP, ISP)
   - Products, Categories, Search, Filters
   
3. **Shopping Cart** (SRP, LSP, DIP)
   - Add/Remove items, Calculate totals
   
4. **Order Processing** (ALL 5 principles!)
   - Place order, Payment, Inventory, Shipping
   
5. **Notification System** (OCP, ISP, DIP)
   - Multi-channel notifications
   
6. **Reporting** (SRP, OCP)
   - Sales reports, Inventory reports, Analytics

**Requirements:**
- Must be able to add new product types without modifying core
- Must support multiple payment/shipping providers
- Must be testable (dependency injection)
- Each class has single responsibility
- All interfaces are focused

**Success Criteria:**
1. Adding new feature affects < 3 classes
2. Can swap implementations without recompilation
3. Unit tests cover > 80% code
4. No class > 200 lines
5. No interface > 5 methods

---

## 📊 EVALUATION CHECKLIST

For each project, verify:

### SRP ✅
- [ ] Each class has only ONE reason to change
- [ ] Class names clearly indicate single responsibility
- [ ] Can describe class purpose in one sentence

### OCP ✅
- [ ] Can add features without modifying existing code
- [ ] Uses abstraction (interfaces/abstract classes)
- [ ] New subtypes don't require changing base code

### LSP ✅
- [ ] Subtypes can replace parent without breaking
- [ ] No "not supported" exceptions
- [ ] Preconditions not strengthened, Postconditions not weakened

### ISP ✅
- [ ] No fat interfaces forcing unused implementations
- [ ] Interfaces are focused and cohesive
- [ ] Clients depend only on methods they use

### DIP ✅
- [ ] High-level modules don't depend on low-level modules
- [ ] Both depend on abstractions
- [ ] Dependencies are injected, not instantiated

---

## 🧠 CRITICAL THINKING QUESTIONS

After each project, answer:

1. **What happens when...**
   - A new requirement comes in?
   - You need to change external service?
   - You want to test in isolation?

2. **Design decisions:**
   - Why did you choose interface over abstract class?
   - Why composition over inheritance?
   - Why this class hierarchy?

3. **Trade-offs:**
   - Is your design over-engineered?
   - Is it flexible enough?
   - What's the cognitive load?

---

## 🎓 LEARNING PATH

**Beginner:** Projects 1, 3, 7, 9
**Intermediate:** Projects 2, 4, 6, 8, 10  
**Advanced:** Project 11 (Integration)

**Recommended Order:**
1. Start with SRP (Projects 1-2)
2. Move to OCP (Projects 3-4)
3. Tackle LSP (Projects 5-6)
4. Learn ISP (Projects 7-8)
5. Master DIP (Projects 9-10)
6. Integrate ALL (Project 11)

---

## 💪 CHALLENGE MODE

For each project:
1. ✍️ Draw class diagram BEFORE coding
2. 🧪 Write unit tests for each class
3. 📝 Document WHY design follows principle
4. 🔄 Refactor at least twice
5. 🎯 Add new feature to test extensibility

**Success = Adding new feature takes < 10 minutes and modifies < 2 classes!**


```mermaid
graph TB
    START[🎯 SOLID System Design Journey]
    
    START --> PHASE1[Phase 1: Build BAD Version<br/>Experience the Pain]
    START --> PHASE2[Phase 2: Refactor to SOLID<br/>Feel the Relief]
    
    PHASE1 --> SRP[Single Responsibility Principle]
    PHASE1 --> OCP[Open/Closed Principle]
    PHASE1 --> LSP[Liskov Substitution Principle]
    PHASE1 --> ISP[Interface Segregation Principle]
    PHASE1 --> DIP[Dependency Inversion Principle]
    
    SRP --> SRP_P1[📦 Project 1: Library System<br/>One class doing EVERYTHING]
    SRP --> SRP_P2[🏦 Project 2: Banking System<br/>God class handling all operations]
    
    SRP_P1 --> SRP_BAD[❌ LibraryManager<br/>8 responsibilities in 1 class]
    SRP_BAD --> SRP_PAIN[Pain Points:<br/>• Change email? Touch everything<br/>• Test fees? Need entire system<br/>• 500+ lines of code]
    
    SRP_PAIN --> SRP_REFACTOR[✅ Refactor into:<br/>BookRepository<br/>BorrowingService<br/>FeeCalculator<br/>NotificationService<br/>ReportGenerator<br/>DataExporter]
    
    SRP_REFACTOR --> SRP_WIN[🎉 Win:<br/>• Change email? Only 1 class<br/>• Test fees? Isolated<br/>• Each class < 100 lines]
    
    OCP --> OCP_P1[💳 Project 3: Payment System<br/>when statements everywhere]
    OCP --> OCP_P2[🎮 Project 4: Game Characters<br/>Giant if-else blocks]
    
    OCP_P1 --> OCP_BAD[❌ PaymentProcessor<br/>when type -><br/>CREDIT, PAYPAL, BITCOIN<br/>Modify for every new method!]
    
    OCP_BAD --> OCP_PAIN[Pain Points:<br/>• Add BNPL? Modify existing<br/>• 20 payment types? Massive when<br/>• Ripple effects everywhere]
    
    OCP_PAIN --> OCP_REFACTOR[✅ Refactor with:<br/>interface PaymentMethod<br/>CreditCardPayment<br/>PayPalPayment<br/>BitcoinPayment<br/>Strategy Pattern]
    
    OCP_REFACTOR --> OCP_WIN[🎉 Win:<br/>• Add BNPL? New class only<br/>• Zero modification to core<br/>• Add 5 cryptos in 5 mins]
    
    LSP --> LSP_P1[🚗 Project 5: Vehicle Rental<br/>Broken inheritance hierarchy]
    LSP --> LSP_P2[📐 Project 6: Shape Engine<br/>Square/Rectangle problem]
    
    LSP_P1 --> LSP_BAD[❌ Bicycle extends Vehicle<br/>startEngine throws exception<br/>refuel throws exception<br/>Violates contract!]
    
    LSP_BAD --> LSP_PAIN[Pain Points:<br/>• rentVehicle crashes on Bicycle<br/>• Can't trust substitution<br/>• Defensive coding needed]
    
    LSP_PAIN --> LSP_REFACTOR[✅ Refactor with:<br/>Drivable interface<br/>Fuelable interface<br/>Chargeable interface<br/>Composition over inheritance]
    
    LSP_REFACTOR --> LSP_WIN[🎉 Win:<br/>• Any vehicle works anywhere<br/>• No unexpected exceptions<br/>• Trust the abstractions]
    
    ISP --> ISP_P1[🤖 Project 7: Smart Home<br/>Fat interface problem]
    ISP --> ISP_P2[🖨️ Project 8: Office Devices<br/>Forced implementations]
    
    ISP_P1 --> ISP_BAD[❌ interface SmartDevice<br/>turnOn, turnOff, setBrightness<br/>setTemp, lock, unlock, play, record<br/>SmartPlug implements all 😱]
    
    ISP_BAD --> ISP_PAIN[Pain Points:<br/>• Plug has setBrightness?<br/>• Empty implementations everywhere<br/>• Confusing API]
    
    ISP_PAIN --> ISP_REFACTOR[✅ Segregate into:<br/>Switchable<br/>Dimmable<br/>Lockable<br/>Playable<br/>Compose as needed]
    
    ISP_REFACTOR --> ISP_WIN[🎉 Win:<br/>• SmartPlug: Switchable only<br/>• Clear contracts<br/>• Easy to extend]
    
    DIP --> DIP_P1[📱 Project 9: Notification System<br/>Tight coupling hell]
    DIP --> DIP_P2[🗄️ Project 10: E-commerce Order<br/>Concrete dependencies]
    
    DIP_P1 --> DIP_BAD[❌ NotificationService<br/>private val email = EmailSender<br/>private val sms = SMSSender<br/>Locked to implementations!]
    
    DIP_BAD --> DIP_PAIN[Pain Points:<br/>• Can't test without real email<br/>• Switch to SendGrid? Rewrite<br/>• Can't mock dependencies]
    
    DIP_PAIN --> DIP_REFACTOR[✅ Depend on abstractions:<br/>interface NotificationChannel<br/>Constructor injection<br/>List of NotificationChannel<br/>Dependency Injection]
    
    DIP_REFACTOR --> DIP_WIN[🎉 Win:<br/>• Easy testing with mocks<br/>• Swap provider via config<br/>• Add Teams without touching code]
    
    SRP_WIN --> INTEGRATION
    OCP_WIN --> INTEGRATION
    LSP_WIN --> INTEGRATION
    ISP_WIN --> INTEGRATION
    DIP_WIN --> INTEGRATION
    
    INTEGRATION[🏆 PROJECT 11<br/>Full E-Commerce Platform<br/>Apply ALL 5 Principles]
    
    INTEGRATION --> MODULES[System Modules]
    MODULES --> M1[User Management<br/>SRP + DIP]
    MODULES --> M2[Product Catalog<br/>SRP + OCP + ISP]
    MODULES --> M3[Shopping Cart<br/>SRP + LSP + DIP]
    MODULES --> M4[Order Processing<br/>ALL 5 PRINCIPLES]
    MODULES --> M5[Notifications<br/>OCP + ISP + DIP]
    MODULES --> M6[Reporting<br/>SRP + OCP]
    
    M1 --> SUCCESS
    M2 --> SUCCESS
    M3 --> SUCCESS
    M4 --> SUCCESS
    M5 --> SUCCESS
    M6 --> SUCCESS
    
    SUCCESS[✅ Success Criteria<br/>• New feature affects less than 3 classes<br/>• Can swap implementations<br/>• High test coverage<br/>• Clean architecture]
    
    SUCCESS --> MASTER[🎓 SOLID MASTER<br/>Ready for:<br/>Clean Architecture<br/>Domain-Driven Design<br/>Microservices]
    
    style START fill:#4A90E2,stroke:#357ABD,color:#fff,stroke-width:3px
    style PHASE1 fill:#E74C3C,stroke:#C0392B,color:#fff
    style PHASE2 fill:#27AE60,stroke:#1E8449,color:#fff
    
    style SRP fill:#3498DB,stroke:#2874A6,color:#fff
    style OCP fill:#2ECC71,stroke:#239B56,color:#fff
    style LSP fill:#9B59B6,stroke:#7D3C98,color:#fff
    style ISP fill:#F39C12,stroke:#D68910,color:#fff
    style DIP fill:#E74C3C,stroke:#C0392B,color:#fff
    
    style SRP_BAD fill:#C0392B,stroke:#922B21,color:#fff
    style OCP_BAD fill:#C0392B,stroke:#922B21,color:#fff
    style LSP_BAD fill:#C0392B,stroke:#922B21,color:#fff
    style ISP_BAD fill:#C0392B,stroke:#922B21,color:#fff
    style DIP_BAD fill:#C0392B,stroke:#922B21,color:#fff
    
    style SRP_WIN fill:#27AE60,stroke:#1E8449,color:#fff
    style OCP_WIN fill:#27AE60,stroke:#1E8449,color:#fff
    style LSP_WIN fill:#27AE60,stroke:#1E8449,color:#fff
    style ISP_WIN fill:#27AE60,stroke:#1E8449,color:#fff
    style DIP_WIN fill:#27AE60,stroke:#1E8449,color:#fff
    
    style INTEGRATION fill:#8E44AD,stroke:#6C3483,color:#fff,stroke-width:4px
    style MASTER fill:#F39C12,stroke:#D68910,color:#fff,stroke-width:4px
```


