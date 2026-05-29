# ⚡ Swift — Complete Reference for Kotlin Developers

> Every concept mapped to Kotlin. No fluff. Built for Android devs crossing over to iOS.

---

## 📦 1. Variables & Constants

|Kotlin|Swift|Notes|
|---|---|---|
|`var`|`var`|Mutable|
|`val`|`let`|Immutable (constant)|
|`Int`, `String`, `Double`|`Int`, `String`, `Double`|Same names|

```swift
let name: String = "iOS Dev"   // constant — can't reassign
var age: Int = 25               // mutable
var score = 99.5                // type inferred as Double
var flag = true                 // type inferred as Bool
```

> ✅ Swift has full **type inference** just like Kotlin. You rarely need to write types explicitly.

---

## 🔁 2. Loops

### For Loop

```swift
// Half-open range (like 0 until 5 in Kotlin)
for i in 0..<5 {
    print(i)  // 0, 1, 2, 3, 4
}

// Closed range (like 0..5 in Kotlin)
for i in 0...5 {
    print(i)  // 0, 1, 2, 3, 4, 5
}

// Loop over array
let fruits = ["Apple", "Mango", "Banana"]
for fruit in fruits {
    print(fruit)
}

// With index — like forEachIndexed in Kotlin
for (index, fruit) in fruits.enumerated() {
    print("\(index): \(fruit)")
}

// Stride — like step in Kotlin
for i in stride(from: 0, to: 10, by: 2) {
    print(i)  // 0, 2, 4, 6, 8
}

// Reverse
for i in (0..<5).reversed() {
    print(i)  // 4, 3, 2, 1, 0
}

// Ignore loop variable
for _ in 0..<3 {
    print("Hello")
}
```

### While Loop

```swift
var count = 0
while count < 5 {
    count += 1
}
```

### Repeat-While (Swift's do-while)

```swift
var x = 0
repeat {
    x += 1
} while x < 3
```

---

## 📚 3. Collections

### Array

```swift
var numbers: [Int] = [1, 2, 3, 4, 5]
var names = ["Alice", "Bob"]       // type inferred

// Mutations
names.append("Charlie")            // add to end
names.insert("Zara", at: 0)        // insert at index
names.remove(at: 0)                // remove by index
names.removeFirst()                // remove first
names.removeLast()                 // remove last
names.removeAll()                  // clear

// Access
print(names.count)                 // length
print(names.isEmpty)               // true/false
print(names[0])                    // access by index
print(names.first)                 // Optional — first element
print(names.last)                  // Optional — last element

// Search
names.contains("Bob")              // true/false
names.firstIndex(of: "Bob")        // Optional<Int>

// Sorting
let sorted = names.sorted()                      // ascending
let sortedDesc = names.sorted(by: >)             // descending
let sortedCustom = names.sorted { $0 < $1 }

// Functional ops
let doubled = numbers.map { $0 * 2 }
let evens = numbers.filter { $0 % 2 == 0 }
let total = numbers.reduce(0, +)

// Immutable array
let fixed = [10, 20, 30]           // can't append/remove
```

### Dictionary

```swift
var person: [String: String] = ["name": "Rahul", "city": "Pune"]

person["age"] = "25"               // add / update
person.removeValue(forKey: "age")  // remove key
print(person["name"] ?? "Unknown") // access — returns Optional!
print(person.keys)                 // all keys
print(person.values)               // all values
print(person.count)                // number of pairs
person.isEmpty                     // true/false

// Loop
for (key, value) in person {
    print("\(key): \(value)")
}

// Check existence
if person["name"] != nil {
    print("name key exists")
}

// updateValue — returns old value
let old = person.updateValue("Akash", forKey: "name")
```

### Set

```swift
var tags: Set<String> = ["swift", "ios", "apple"]
tags.insert("xcode")
tags.remove("apple")
tags.contains("ios")               // true
tags.count

// Set operations
let a: Set = [1, 2, 3, 4]
let b: Set = [3, 4, 5, 6]

a.union(b)            // [1,2,3,4,5,6]
a.intersection(b)     // [3,4]
a.subtracting(b)      // [1,2]
a.symmetricDifference(b)  // [1,2,5,6]
```

> 🧠 Accessing a Dictionary key returns an **Optional**. Always use `??` or `if let`.

---

## ❓ 4. Optionals (Null Safety)

|Kotlin|Swift|
|---|---|
|`String?`|`String?`|
|`?: "default"`|`?? "default"`|
|`?.length`|`?.count`|
|`!!`|`!` (force unwrap)|
|`let x = nullable`|`if let x = optional {}`|

```swift
var nickname: String? = nil

// Nil coalescing (Elvis operator)
let display = nickname ?? "Guest"

// Optional chaining
let length = nickname?.count          // returns Int?

// if-let (safe unwrap)
if let name = nickname {
    print("Hello \(name)")
}

// guard-let (early exit — very common)
func greet(_ name: String?) {
    guard let name = name else { return }
    print("Hello \(name)")
}

// Nested optional chaining
let street = person?.address?.street?.name

// Optional in loops
let items: [String?] = ["A", nil, "B", nil, "C"]
let nonNil = items.compactMap { $0 }   // ["A", "B", "C"]
```

### Implicitly Unwrapped Optional (`!` type)

```swift
// Used for IBOutlets and deferred init — acts as regular unless nil
var label: UILabel!                    // declared but used after init
```

> ⚠️ Avoid `!` force-unwrap just like `!!` in Kotlin — use only when guaranteed non-nil.

---

## 🏛️ 5. Classes & Structs

|Feature|`class`|`struct`|
|---|---|---|
|Type|Reference|Value|
|Inheritance|✅|❌|
|Mutability in methods|Always|Needs `mutating`|
|Use for|VC, Services, Managers|Models, Data|

### Class

```swift
class Animal {
    var name: String
    var sound: String

    init(name: String, sound: String) {
        self.name = name
        self.sound = sound
    }

    func speak() {
        print("\(name) says \(sound)")
    }

    deinit {
        // called when deallocated — like onDestroy
        print("\(name) destroyed")
    }
}
```

### Struct

```swift
struct Point {
    var x: Double
    var y: Double

    // Must mark 'mutating' to modify self
    mutating func moveBy(dx: Double, dy: Double) {
        x += dx
        y += dy
    }

    func distanceFromOrigin() -> Double {
        (x * x + y * y).squareRoot()
    }
}

var p = Point(x: 3.0, y: 4.0)
p.moveBy(dx: 1.0, dy: 1.0)
```

---

## 🔑 6. Properties

### Stored Properties

```swift
class User {
    var name: String = ""       // mutable stored
    let id: Int                 // constant stored
    init(id: Int) { self.id = id }
}
```

### Computed Properties (like Kotlin `get`/`set`)

```swift
struct Rectangle {
    var width: Double
    var height: Double

    // Computed property — no storage, calculated on the fly
    var area: Double {
        return width * height
    }

    // With getter and setter
    var perimeter: Double {
        get { 2 * (width + height) }
        set { width = newValue / 4; height = newValue / 4 }
    }
}

var rect = Rectangle(width: 5, height: 3)
print(rect.area)       // 15.0
rect.perimeter = 40    // setter called
```

### Lazy Properties (like Kotlin `by lazy`)

```swift
class DataManager {
    lazy var database = DatabaseService()   // only created when first accessed
}
```

### Property Observers (`willSet` / `didSet`) — like Kotlin `observable`

```swift
class StepCounter {
    var steps: Int = 0 {
        willSet {
            print("About to set steps to \(newValue)")
        }
        didSet {
            print("Steps changed from \(oldValue) to \(steps)")
            if steps > 10000 { print("Goal reached!") }
        }
    }
}

let counter = StepCounter()
counter.steps = 5000   // triggers willSet and didSet
```

### Static Properties and Methods

```swift
struct MathUtils {
    static let pi = 3.14159
    static func square(_ n: Double) -> Double { n * n }
}

MathUtils.pi
MathUtils.square(4)    // 16.0
```

---

## 🧬 7. Inheritance

```swift
class Vehicle {
    var speed: Int

    init(speed: Int) { self.speed = speed }

    func describe() {
        print("Speed: \(speed) km/h")
    }
}

class Car: Vehicle {
    var brand: String

    init(brand: String, speed: Int) {
        self.brand = brand
        super.init(speed: speed)        // must call super.init
    }

    override func describe() {          // override keyword — same as Kotlin
        super.describe()                // call parent
        print("Brand: \(brand)")
    }
}

// final prevents subclassing
final class SportsCar: Car { }
```

---

## 🔒 8. Protocols (Interfaces)

```swift
protocol Flyable {
    var maxAltitude: Int { get }        // read-only
    var currentAltitude: Int { get set } // read-write
    func fly()
    func land()
}

// Optional methods via @objc (UIKit pattern)
@objc protocol Delegate {
    @objc optional func didFinish()
}

// Protocol with default implementation via extension
protocol Greetable {
    var name: String { get }
    func greet() -> String
}

extension Greetable {
    func greet() -> String {            // default implementation
        return "Hello, I am \(name)"
    }
}

// Adopting multiple protocols
class Bird: Flyable, Greetable {
    var name = "Eagle"
    var maxAltitude = 3000
    var currentAltitude = 0
    func fly() { currentAltitude = 1000 }
    func land() { currentAltitude = 0 }
}
```

> Kotlin `interface` → Swift `protocol`  
> Default interface methods → Protocol `extension` with default implementation

---

## 🎭 9. Enums (Powerful!)

### Basic

```swift
enum Direction { case north, south, east, west }
var dir: Direction = .north
```

### With Raw Values

```swift
enum Planet: Int {
    case mercury = 1, venus, earth    // auto-increments: 1, 2, 3
}
print(Planet.earth.rawValue)          // 3
let p = Planet(rawValue: 2)           // Optional<Planet>

enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
    case delete = "DELETE"
}
print(HTTPMethod.get.rawValue)        // "GET"
```

### With Associated Values (Sealed Class equivalent)

```swift
enum NetworkResult<T> {
    case success(T)
    case failure(Error)
    case loading
}

enum AppError: Error {
    case notFound(id: Int)
    case serverError(code: Int, message: String)
    case noInternet
}
```

### CaseIterable — iterate all cases

```swift
enum Season: CaseIterable {
    case spring, summer, autumn, winter
}

for season in Season.allCases {
    print(season)
}
print(Season.allCases.count)  // 4
```

### Enum Methods

```swift
enum Direction {
    case north, south, east, west

    func opposite() -> Direction {
        switch self {
        case .north: return .south
        case .south: return .north
        case .east:  return .west
        case .west:  return .east
        }
    }
}
```

---

## 🔀 10. Switch & Pattern Matching

```swift
let score = 85

switch score {
case 90...100:  print("A")
case 80..<90:   print("B")
case 60..<80:   print("C")
default:        print("F")
}

// Multiple values per case
let day = "Monday"
switch day {
case "Saturday", "Sunday":
    print("Weekend")
default:
    print("Weekday")
}

// Where clause (guard condition in switch)
let point = (2, -3)
switch point {
case let (x, y) where x == y:
    print("On diagonal")
case let (x, y) where x > 0 && y > 0:
    print("First quadrant")
default:
    print("Somewhere else")
}

// Matching enum with associated values
switch networkResult {
case .success(let data):
    print(data)
case .failure(let err):
    print(err)
case .loading:
    break
}
```

---

## 🔧 11. Functions

```swift
// Basic
func greet(name: String) -> String {
    return "Hello, \(name)"
}
greet(name: "Rahul")

// Argument label vs parameter name
func move(from start: String, to end: String) {
    print("Moving from \(start) to \(end)")
}
move(from: "Home", to: "Office")   // reads like English

// Omit label with _
func add(_ a: Int, _ b: Int) -> Int { a + b }
add(3, 5)

// Default values
func log(_ message: String, level: String = "INFO") {
    print("[\(level)] \(message)")
}
log("App started")            // [INFO] App started
log("Crash", level: "ERROR")  // [ERROR] Crash

// Multiple return values via Tuple
func minMax(array: [Int]) -> (min: Int, max: Int) {
    return (array.min()!, array.max()!)
}
let result = minMax(array: [3, 1, 8, 2])
print(result.min, result.max)  // 1 8

// Variadic parameters (like vararg in Kotlin)
func sum(_ numbers: Int...) -> Int {
    numbers.reduce(0, +)
}
sum(1, 2, 3, 4, 5)   // 15

// inout — pass by reference (pointer equivalent)
func doubleIt(_ n: inout Int) { n *= 2 }
var num = 5
doubleIt(&num)
print(num)  // 10

// @discardableResult — suppress unused warning
@discardableResult
func saveData() -> Bool { return true }
saveData()  // no warning for ignoring return value
```

---

## 🧩 12. Closures (Lambdas)

```swift
// Full syntax
let square: (Int) -> Int = { (n: Int) -> Int in
    return n * n
}

// Shorthand — type inferred
let square2: (Int) -> Int = { n in n * n }

// $0 shorthand (like 'it' in Kotlin)
let square3: (Int) -> Int = { $0 * $0 }

// Trailing closure (last param can go outside parentheses)
let nums = [3, 1, 4, 1, 5]
let sorted = nums.sorted { $0 < $1 }
let sorted2 = nums.sorted() { $0 < $1 }

// Common higher-order functions
let numbers = [1, 2, 3, 4, 5, 6]
let doubled  = numbers.map    { $0 * 2 }           // transform
let evens    = numbers.filter { $0 % 2 == 0 }      // filter
let total    = numbers.reduce(0, +)                 // accumulate
let flat     = [[1,2],[3,4]].flatMap { $0 }         // flatten
let nonNil   = [1, nil, 3, nil].compactMap { $0 }   // remove nils
let any      = numbers.contains { $0 > 4 }         // true
let all      = numbers.allSatisfy { $0 > 0 }        // true
let first    = numbers.first { $0 > 3 }             // Optional(4)

// Storing closure as property
class Button {
    var onTap: (() -> Void)?
    func tap() { onTap?() }
}
let btn = Button()
btn.onTap = { print("Tapped!") }
btn.tap()
```

### @escaping Closures

```swift
// Escaping = closure is stored and called AFTER the function returns
// Common in async callbacks

class Network {
    var completions: [() -> Void] = []

    func fetch(completion: @escaping () -> Void) {
        completions.append(completion)   // stored = must be @escaping
    }
}
```

### Capturing `self` — ⚠️ Memory Leak Prevention

```swift
class ViewModel {
    var name = "Rahul"

    func fetchData() {
        // [weak self] prevents retain cycle — like WeakReference in Kotlin
        someAsyncCall { [weak self] result in
            guard let self = self else { return }
            print(self.name)
        }

        // [unowned self] — use when self is guaranteed alive
        someAsyncCall { [unowned self] in
            print(self.name)
        }
    }
}
```

> 🔥 Always use `[weak self]` in closures that outlive the current scope — same concept as `WeakReference` or `lifecycleScope` in Android.

---

## ⚠️ 13. Error Handling

```swift
// Define errors (like custom Exception in Kotlin)
enum FileError: Error {
    case notFound(path: String)
    case permissionDenied
    case corrupted
}

// Throwing function
func readFile(path: String) throws -> String {
    guard path.hasPrefix("/") else {
        throw FileError.notFound(path: path)
    }
    return "file contents"
}

// try-catch (like Kotlin try-catch)
do {
    let content = try readFile(path: "docs/readme.txt")
    print(content)
} catch FileError.notFound(let path) {
    print("Not found: \(path)")
} catch FileError.permissionDenied {
    print("Access denied")
} catch {
    print("Unknown error: \(error)")  // 'error' is auto-bound
}

// try? — returns Optional (nil on error, like runCatching in Kotlin)
let content = try? readFile(path: "readme.txt")  // String?

// try! — force (crash on error — avoid in production)
let content2 = try! readFile(path: "/readme.txt")

// Propagate error
func processFile(path: String) throws {
    let content = try readFile(path: path)  // no do-catch, passes up
    print(content)
}
```

---

## 🧱 14. Extensions

```swift
// Add methods to existing types — like Kotlin extension functions!
extension String {
    func isPalindrome() -> Bool {
        return self == String(self.reversed())
    }

    var wordCount: Int {
        return self.split(separator: " ").count
    }
}

"racecar".isPalindrome()     // true
"hello world".wordCount      // 2

// Extend Int
extension Int {
    var squared: Int { self * self }
    func times(_ action: () -> Void) {
        for _ in 0..<self { action() }
    }
}

5.squared         // 25
3.times { print("Hi") }  // prints "Hi" 3 times

// Protocol conformance via extension
extension Car: CustomStringConvertible {
    var description: String {
        return "\(brand) at \(speed) km/h"
    }
}
```

---

## 🔬 15. Generics

```swift
// Generic function (like Kotlin generics)
func swap<T>(_ a: inout T, _ b: inout T) {
    let temp = a
    a = b
    b = temp
}

var x = 10, y = 20
swap(&x, &y)
print(x, y)  // 20 10

// Generic class
class Stack<T> {
    private var elements: [T] = []
    func push(_ item: T) { elements.append(item) }
    func pop() -> T? { elements.popLast() }
    var top: T? { elements.last }
    var isEmpty: Bool { elements.isEmpty }
}

var stack = Stack<Int>()
stack.push(1); stack.push(2)
print(stack.pop()!)  // 2

// Constrained generics (like Kotlin where T : Comparable)
func findMax<T: Comparable>(_ array: [T]) -> T? {
    return array.max()
}

findMax([3, 1, 9, 2])     // Optional(9)
findMax(["b", "a", "z"])   // Optional("z")

// Associated type in protocol (like Kotlin associated type)
protocol Container {
    associatedtype Item
    var items: [Item] { get }
    func add(_ item: Item)
}
```

---

## 🔄 16. Initializers

```swift
class Person {
    var name: String
    var age: Int

    // Designated initializer (primary)
    init(name: String, age: Int) {
        self.name = name
        self.age = age
    }

    // Convenience initializer — must call designated
    convenience init(name: String) {
        self.init(name: name, age: 0)
    }

    // Failable initializer — returns Optional
    init?(id: Int) {
        guard id > 0 else { return nil }
        self.name = "User\(id)"
        self.age = 0
    }
}

let p1 = Person(name: "Rahul", age: 25)
let p2 = Person(name: "Guest")
let p3 = Person(id: -1)   // nil — failable returned nil

// Required init (subclasses must implement)
class Base {
    required init() { }
}
class Child: Base {
    required init() { super.init() }
}
```

---

## 💾 17. Memory Management (ARC)

Swift uses **ARC (Automatic Reference Counting)** — like Android's GC but deterministic.

```
strong reference (default) — keeps object alive
weak reference             — doesn't keep alive, becomes nil
unowned reference          — doesn't keep alive, NOT optional (crash if nil)
```

```swift
class Person {
    var name: String
    var apartment: Apartment?
    init(name: String) { self.name = name }
}

class Apartment {
    var unit: String
    weak var tenant: Person?       // weak — avoids retain cycle
    init(unit: String) { self.unit = unit }
}

var rahul: Person? = Person(name: "Rahul")
var apt: Apartment? = Apartment(unit: "4B")

rahul?.apartment = apt
apt?.tenant = rahul             // weak — no retain cycle!

rahul = nil                     // Person deallocated
print(apt?.tenant)              // nil (weak auto-zeroed)
```

> 🔥 **Retain cycle** = two objects holding strong refs to each other = memory leak.  
> Fix: make one ref `weak`. Android equivalent: use `WeakReference` or `lifecycleScope`.

---

## 🪝 18. Defer

```swift
// defer runs at the end of the scope — like try-finally in Java/Kotlin
func riskyOperation() {
    print("Start")
    defer { print("Cleanup") }   // always runs, even on early return
    defer { print("Also runs") } // multiple defers run in reverse order
    print("Middle")
    // Output: Start, Middle, Also runs, Cleanup
}

// Common use: close resources
func readFile() {
    let file = openFile()
    defer { file.close() }       // guaranteed close
    // ... work with file
}
```

---

## 🏷️ 19. Tuples

```swift
// Group multiple values without a struct
let coordinates = (x: 10, y: 20)
print(coordinates.x)           // 10

// Unnamed tuple
let pair: (Int, String) = (1, "one")
print(pair.0, pair.1)          // 1 one

// Destructuring (like Kotlin destructuring)
let (min, max) = (3, 9)
print(min, max)

// Return multiple values from function
func getUser() -> (name: String, age: Int) {
    return ("Rahul", 25)
}
let user = getUser()
print(user.name, user.age)
```

---

## 🔗 20. Type Aliases

```swift
// Like Kotlin typealias
typealias UserID = Int
typealias CompletionHandler = (Bool, Error?) -> Void
typealias StringDictionary = [String: String]

func fetchUser(id: UserID, completion: CompletionHandler) { }
var config: StringDictionary = ["env": "prod"]
```

---

## 🏷️ 21. Type Casting

```swift
class Shape { }
class Circle: Shape { var radius = 5.0 }
class Square: Shape { var side = 3.0 }

let shapes: [Shape] = [Circle(), Square(), Circle()]

for shape in shapes {
    if let circle = shape as? Circle {    // safe cast (returns Optional)
        print("Circle radius: \(circle.radius)")
    } else if let square = shape as? Square {
        print("Square side: \(square.side)")
    }
}

// is — type check only (like 'is' in Kotlin)
if shapes[0] is Circle { print("It's a circle") }

// as! — force cast (crash if wrong type)
let circle = shapes[0] as! Circle
```

---

## 📤 22. Codable (JSON Parsing — Like Gson/Moshi)

```swift
// Codable = Encodable + Decodable (like @Serializable in Kotlin)
struct User: Codable {
    let id: Int
    let name: String
    let email: String
}

// Decode JSON → Swift object
let jsonString = """
{
    "id": 1,
    "name": "Rahul",
    "email": "rahul@example.com"
}
"""
let jsonData = jsonString.data(using: .utf8)!
let user = try! JSONDecoder().decode(User.self, from: jsonData)
print(user.name)  // "Rahul"

// Encode Swift object → JSON
let encoded = try! JSONEncoder().encode(user)
let jsonOutput = String(data: encoded, encoding: .utf8)!

// Custom key mapping (like @SerializedName in Gson)
struct Product: Codable {
    let productName: String
    let priceUSD: Double

    enum CodingKeys: String, CodingKey {
        case productName = "product_name"    // maps snake_case JSON
        case priceUSD    = "price_usd"
    }
}

// Decode array
let usersJSON = "[{\"id\":1,\"name\":\"A\"}, {\"id\":2,\"name\":\"B\"}]"
let users = try! JSONDecoder().decode([User].self,
    from: usersJSON.data(using: .utf8)!)
```

---

## ⚡ 23. Async/Await (Swift Concurrency — like Kotlin Coroutines)

|Kotlin|Swift|
|---|---|
|`suspend fun`|`async func`|
|`launch { }`|`Task { }`|
|`async { }`|`async let`|
|`Dispatchers.IO`|Actor / `Task.detached`|
|`withContext`|`await`|
|`Flow`|`AsyncStream`|

```swift
// Async function
func fetchUser(id: Int) async throws -> User {
    let url = URL(string: "https://api.example.com/user/\(id)")!
    let (data, _) = try await URLSession.shared.data(from: url)
    return try JSONDecoder().decode(User.self, from: data)
}

// Call async function
Task {
    do {
        let user = try await fetchUser(id: 1)
        print(user.name)
    } catch {
        print("Error: \(error)")
    }
}

// Parallel execution (like async/await in Kotlin)
async let user = fetchUser(id: 1)
async let profile = fetchProfile(id: 1)
let (u, p) = try await (user, profile)  // both run simultaneously

// Main thread update
Task { @MainActor in
    self.label.text = "Updated"          // like withContext(Dispatchers.Main)
}

// Actor — thread-safe class (like Mutex in Kotlin)
actor BankAccount {
    private var balance: Double = 0

    func deposit(_ amount: Double) {
        balance += amount
    }

    func getBalance() -> Double {
        return balance
    }
}

let account = BankAccount()
await account.deposit(100)
let bal = await account.getBalance()
```

---

## 🖼️ 24. SwiftUI Basics (like Jetpack Compose!)

> SwiftUI and Jetpack Compose are very similar — both declarative UI!

|Jetpack Compose|SwiftUI|
|---|---|
|`@Composable`|`View` protocol (body)|
|`remember { }`|`@State`|
|`State<T>`|`@Binding`|
|`ViewModel`|`@ObservableObject`|
|`collectAsState()`|`@ObservedObject`|
|`LazyColumn`|`List` / `LazyVStack`|
|`Column`|`VStack`|
|`Row`|`HStack`|
|`Box`|`ZStack`|
|`Modifier`|`.modifier()` / view modifiers|

```swift
import SwiftUI

// Simple View
struct ContentView: View {
    var body: some View {
        Text("Hello, World!")
            .font(.title)
            .foregroundColor(.blue)
            .padding()
    }
}

// @State — local mutable state (like remember { mutableStateOf() })
struct CounterView: View {
    @State private var count = 0

    var body: some View {
        VStack {
            Text("Count: \(count)")
                .font(.largeTitle)
            Button("Tap Me") {
                count += 1
            }
            .padding()
            .background(Color.blue)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
    }
}

// @Binding — pass state down (like state hoisting in Compose)
struct ToggleView: View {
    @Binding var isOn: Bool

    var body: some View {
        Toggle("Switch", isOn: $isOn)
    }
}

// @ObservableObject — ViewModel
class UserViewModel: ObservableObject {
    @Published var users: [User] = []      // like StateFlow / LiveData
    @Published var isLoading = false

    func fetchUsers() {
        isLoading = true
        Task {
            // fetch...
            await MainActor.run {
                self.users = []
                self.isLoading = false
            }
        }
    }
}

struct UserListView: View {
    @StateObject private var viewModel = UserViewModel()   // owns VM

    var body: some View {
        List(viewModel.users, id: \.id) { user in
            Text(user.name)
        }
        .onAppear {
            viewModel.fetchUsers()
        }
    }
}

// Layout
struct LayoutDemo: View {
    var body: some View {
        VStack(spacing: 16) {                    // vertical stack
            HStack {                              // horizontal stack
                Image(systemName: "star.fill")
                Text("Featured")
            }
            ZStack {                              // layered stack
                Color.blue.frame(height: 100)
                Text("Overlay text").foregroundColor(.white)
            }
            Spacer()                             // pushes views apart
        }
        .padding()
    }
}
```

---

## 📱 25. UIKit Basics (ViewController Lifecycle)

|Android|iOS UIKit|
|---|---|
|`Activity`|`UIViewController`|
|`Fragment`|`UIViewController` (child)|
|`onCreate()`|`viewDidLoad()`|
|`onStart()`|`viewWillAppear()`|
|`onResume()`|`viewDidAppear()`|
|`onPause()`|`viewWillDisappear()`|
|`onStop()`|`viewDidDisappear()`|
|`onDestroy()`|`deinit`|
|`RecyclerView`|`UITableView` / `UICollectionView`|
|`Intent`|`segue` / `present()` / `push()`|

```swift
import UIKit

class HomeViewController: UIViewController {

    // IBOutlet — like findViewById / ViewBinding
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var tableView: UITableView!

    override func viewDidLoad() {
        super.viewDidLoad()
        // UI is ready — setup here
        setupUI()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // About to show
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // Fully visible
    }

    // IBAction — like onClick
    @IBAction func buttonTapped(_ sender: UIButton) {
        navigateToDetail()
    }

    func navigateToDetail() {
        let vc = DetailViewController()
        navigationController?.pushViewController(vc, animated: true)
    }

    private func setupUI() {
        titleLabel.text = "Home"
        tableView.delegate = self
        tableView.dataSource = self
    }
}

// UITableView (like RecyclerView)
extension HomeViewController: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView,
                   numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    func tableView(_ tableView: UITableView,
                   cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(
            withIdentifier: "Cell", for: indexPath)
        cell.textLabel?.text = items[indexPath.row]
        return cell
    }

    func tableView(_ tableView: UITableView,
                   didSelectRowAt indexPath: IndexPath) {
        print("Tapped: \(items[indexPath.row])")
    }
}
```

---

## 🔐 26. Access Control

|Kotlin|Swift|Scope|
|---|---|---|
|`public`|`public`|Everywhere|
|`internal` (default)|`internal` (default)|Within module|
|`private`|`private`|Within type/extension|
|—|`fileprivate`|Within same .swift file|
|`protected`|❌ None|Use class hierarchy|
|`open`|`open`|Subclassable across modules|

```swift
public class APIClient {
    public var baseURL: String = ""
    internal var session = URLSession.shared
    fileprivate var cache: [String: Data] = [:]
    private var authToken: String = ""

    open func request() { }          // can be overridden outside module
}
```

---

## 🔤 27. Strings

```swift
let city = "Pune"
let temp = 36

// Interpolation (same as Kotlin)
print("City: \(city), Temp: \(temp)°C")

// Multiline
let message = """
    Dear \(city),
    Temperature today is \(temp)°C.
    """

// Methods
let s = "Hello, Swift!"
s.uppercased()             // "HELLO, SWIFT!"
s.lowercased()             // "hello, swift!"
s.contains("Swift")        // true
s.count                    // 13
s.hasPrefix("Hello")       // true
s.hasSuffix("Swift!")      // true
s.replacingOccurrences(of: "Swift", with: "World")
s.split(separator: ",")    // ["Hello", " Swift!"]
s.trimmingCharacters(in: .whitespaces)

// Check empty
s.isEmpty
s.count == 0

// String to Int / Int to String
let n = Int("42")           // Optional(42)
let str = String(42)        // "42"
let str2 = "\(42)"          // "42"
```

---

## 📋 28. Result Type (Kotlin Result<T>)

```swift
// Swift's built-in Result<Success, Failure>
func divide(_ a: Int, _ b: Int) -> Result<Int, Error> {
    guard b != 0 else {
        return .failure(NSError(domain: "Math", code: 0,
            userInfo: [NSLocalizedDescriptionKey: "Division by zero"]))
    }
    return .success(a / b)
}

switch divide(10, 2) {
case .success(let value):
    print("Result: \(value)")
case .failure(let error):
    print("Error: \(error.localizedDescription)")
}

// get() throws
let value = try? divide(10, 2).get()  // Optional(5)
```

---

## 🎯 29. Where Clause (Kotlin `where` / `filter`)

```swift
// In for loop
for num in 1...20 where num % 3 == 0 {
    print(num)  // 3, 6, 9, 12, 15, 18
}

// In switch case
switch score {
case let x where x >= 90: print("A")
case let x where x >= 80: print("B")
default: print("F")
}

// In generic constraints
func process<T: Equatable & Comparable>(a: T, b: T)
    where T: CustomStringConvertible {
    print(a < b ? a : b)
}
```

---

## 🧠 30. @propertyWrapper (Kotlin Delegated Properties)

```swift
// Like 'by' delegate in Kotlin
@propertyWrapper
struct Clamped<T: Comparable> {
    var value: T
    let range: ClosedRange<T>

    var wrappedValue: T {
        get { value }
        set { value = min(max(newValue, range.lowerBound), range.upperBound) }
    }

    init(wrappedValue: T, _ range: ClosedRange<T>) {
        self.range = range
        self.value = min(max(wrappedValue, range.lowerBound), range.upperBound)
    }
}

struct Player {
    @Clamped(0...100) var health: Int = 100
}

var player = Player()
player.health = 150    // clamped to 100
player.health = -10    // clamped to 0
print(player.health)   // 0
```

---

## 📌 31. Subscripts

```swift
// Custom subscript — like operator[] in C++
struct Matrix {
    var data: [[Double]]

    subscript(row: Int, col: Int) -> Double {
        get { data[row][col] }
        set { data[row][col] = newValue }
    }
}

var m = Matrix(data: [[1, 2], [3, 4]])
print(m[0, 1])   // 2.0
m[1, 0] = 99.0
```

---

## 🔭 32. Pointer Summary — When You'd Actually Need Them

```
In iOS development: NEVER in normal app code.
Swift uses ARC (Automatic Reference Counting) — memory is managed.

What Swift DOES have for C interop:
  UnsafePointer<T>         read-only pointer
  UnsafeMutablePointer<T>  read-write pointer
  UnsafeRawPointer         untyped pointer
  withUnsafePointer { }    temporary pointer to a value

'inout' is the closest thing to a reference/pointer in practice:
  func increment(_ n: inout Int) { n += 1 }
  var x = 5
  increment(&x)   // x is now 6
```

---

## 📋 Quick Reference Cheat Sheet

```
// VARIABLES
var x = 10            // mutable
let y = 20            // constant (val)
var z: Int? = nil     // optional (nullable)

// LOOPS
for i in 0..<5 { }               // 0 to 4
for i in 0...5 { }               // 0 to 5
for item in array { }
for (i, item) in array.enumerated() { }
while condition { }
repeat { } while condition

// COLLECTIONS
var arr: [Int] = [1,2,3]
var dict: [String: Int] = ["a": 1]
var set: Set<Int> = [1, 2, 3]

// OPTIONALS
let x: Int? = nil
let val = x ?? 0                 // nil coalescing
if let x = x { }                // safe unwrap
guard let x = x else { return } // guard unwrap

// FUNCTIONS
func name(label param: Type) -> Return { }
func name(_ param: Type) -> Return { }  // no label

// CLOSURES
let fn: (Int) -> Int = { $0 * 2 }
array.map { $0 * 2 }
array.filter { $0 > 0 }
array.reduce(0, +)

// CLASSES
class Foo: Bar, Protocol { }
override func method() { super.method() }
final class NoSubclass { }

// STRUCTS
struct Foo: Protocol {
    mutating func change() { }
}

// ENUMS
enum State { case active, inactive }
enum Result<T> { case success(T); case failure(Error) }

// ERROR HANDLING
func foo() throws { throw SomeError.case }
do { try foo() } catch { print(error) }
let x = try? foo()     // Optional

// ASYNC
func fetch() async throws -> Data { }
Task { let d = try await fetch() }
```

---

## 🚀 Your iOS Learning Roadmap

```
Week 1: Language (this doc)
Week 2: SwiftUI — VStack/HStack/ZStack, @State, @Binding, List
Week 3: @ObservableObject, @StateObject, MVVM in SwiftUI
Week 4: Networking — URLSession, async/await, Codable
Week 5: Navigation — NavigationStack, sheets, TabView
Week 6: UIKit basics — UIViewController, UITableView (for legacy/jobs)
Week 7: Core Data / SwiftData (like Room)
Week 8: App Store deployment — signing, provisioning, Xcode archives
```

> 💡 You already think in MVVM, sealed classes, coroutines, and Compose.  
> SwiftUI + async/await + enums = everything you know, different syntax.  
> **You're closer than you think. Ship something in Week 2.**