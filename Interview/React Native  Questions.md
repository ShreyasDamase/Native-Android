1. **var / let / const**.
 
---
---
==ans:==
# JavaScript Interview Script: var vs let vs const

## Opening Statement
"In JavaScript, we have three ways to declare variables: var, let, and const. They differ mainly in terms of scope, hoisting behavior, and mutability. Let me break down each one."

---

## 1. SCOPE

**"First, let's talk about scope, which is the most important difference."**

- **var** is function-scoped or globally scoped
  - "If I declare var inside a function, it's accessible throughout that entire function"
  - "If declared outside any function, it becomes a global variable"

- **let and const** are block-scoped
  - "They're only accessible within the block where they're declared - like inside an if statement, loop, or any curly braces"

**Example to mention:**
```javascript
if (true) {
  var x = 10;
  let y = 20;
}
console.log(x); // 10 - works fine
console.log(y); // ReferenceError - y is not accessible
```

**"So let and const give us better control over variable scope, reducing bugs from accidental access."**

---

## 2. RE-DECLARATION

**"Second difference is re-declaration."**

- **var** allows re-declaration in the same scope
  - "I can declare var name = 'John' and then var name = 'Jane' again without any errors"
  
- **let and const** do NOT allow re-declaration
  - "If I try to declare let age = 25 and then let age = 30 in the same scope, I'll get a SyntaxError"

**"This makes let and const safer because they prevent accidental redeclaration of variables."**

---

## 3. RE-ASSIGNMENT

**"Third, let's discuss re-assignment - whether we can change the value."**

- **var and let** can be re-assigned
  - "I can do let count = 1 and then count = 2 - no problem"

- **const** cannot be re-assigned
  - "Once I assign const PI = 3.14, I cannot change PI to another value"
  - **Important caveat:** "However, const doesn't make objects or arrays immutable. I can still modify their properties or elements"

**Example to clarify:**
```javascript
const user = { name: 'John' };
user.name = 'Jane';  // This works - modifying property
user = {};           // This throws error - reassigning variable
```

---

## 4. HOISTING

**"Fourth is hoisting behavior."**

- **All three are hoisted** - "meaning the declarations are moved to the top of their scope during compilation"

- **var** is hoisted AND initialized with undefined
  - "So I can access a var variable before its declaration, and I'll get undefined"

- **let and const** are hoisted but NOT initialized
  - "They remain in what's called the Temporal Dead Zone until the line where they're declared"
  - "If I try to access them before declaration, I get a ReferenceError"

**Example:**
```javascript
console.log(x); // undefined - var is initialized
console.log(y); // ReferenceError - let is not initialized
var x = 5;
let y = 10;
```

---

## 5. INITIALIZATION

**"Finally, initialization requirements."**

- **var and let** can be declared without immediate initialization
  - "I can write let x; and assign it later"

- **const** must be initialized when declared
  - "I have to write const PI = 3.14; I cannot just declare const PI;"

---

## BEST PRACTICES (Closing)

**"In modern JavaScript development, here's what we follow:"**

1. **Use const by default** - "It prevents accidental reassignments and makes code more predictable"
2. **Use let when reassignment is needed** - "Like in loops or when a value legitimately needs to change"
3. **Avoid var** - "It's legacy syntax. let and const provide better scoping and prevent common bugs"

**"This approach leads to cleaner, more maintainable code and is the industry standard today."**

---

## Quick Comparison Table (to memorize)

| Feature | var | let | const |
|---------|-----|-----|-------|
| Scope | Function/Global | Block | Block |
| Re-declaration | ✅ Yes | ❌ No | ❌ No |
| Re-assignment | ✅ Yes | ✅ Yes | ❌ No |
| Hoisting | Initialized (undefined) | Not initialized (TDZ) | Not initialized (TDZ) |
| Must Initialize | ❌ No | ❌ No | ✅ Yes |

---

## Pro Tips for Interview

- **If asked which to use:** "I prefer const by default, let when needed, and avoid var"
- **If asked about const with objects:** "const prevents reassignment of the reference, not mutation of the object's properties"
- **If asked about TDZ:** "Temporal Dead Zone is the period between entering scope and the actual declaration where let/const variables cannot be accessed"
---
---



2. **Hoisting**: Different cases (what happens when you use two different ways of declaring function. i.e., `function() { ... }` type and `() => { ... }` arrow function type).
---
---
==ans:==
# JavaScript Interview Script: Hoisting with Different Function Types

## Opening Statement
"Hoisting is JavaScript's behavior of moving declarations to the top of their scope during the compilation phase, before code execution. However, different types of function declarations behave very differently when it comes to hoisting. Let me explain the key differences."

---

## 1. WHAT IS HOISTING?

**"First, let me briefly explain hoisting."**

- "Hoisting is JavaScript's default behavior where declarations are moved to the top of their scope before code execution"
- "This allows us to use some functions and variables before they're actually written in the code"
- **Important:** "Only declarations are hoisted, not initializations"

---

## 2. FUNCTION DECLARATIONS - Fully Hoisted

**"Regular function declarations using the `function` keyword are FULLY hoisted."**

### What gets hoisted:
- Both the declaration AND the function body
- The entire function is available from the start of the scope

### Example:
```javascript
// This works perfectly!
greet(); // Output: "Hello, World!"

function greet() {
  console.log("Hello, World!");
}
```

**"Even though we're calling `greet()` before it's defined, JavaScript has hoisted the entire function declaration to the top, so it works without any errors."**

**Behind the scenes, JavaScript treats it like:**
```javascript
function greet() {
  console.log("Hello, World!");
}

greet(); // Output: "Hello, World!"
```

---

## 3. ARROW FUNCTIONS - NOT Hoisted (Same as Function Expressions)

**"Arrow functions are NOT hoisted in the same way. They behave like function expressions."**

### What happens:
- Arrow functions are assigned to variables
- Only the VARIABLE declaration is hoisted (if using var)
- The function assignment is NOT hoisted
- The variable is in the Temporal Dead Zone if using let/const

### Example with `const` (most common):
```javascript
// This FAILS!
greet(); // ReferenceError: Cannot access 'greet' before initialization

const greet = () => {
  console.log("Hello, World!");
};
```

**"We get a ReferenceError because arrow functions defined with const or let are in the Temporal Dead Zone until the line where they're assigned."**

### Example with `var`:
```javascript
// This also FAILS, but differently!
greet(); // TypeError: greet is not a function

var greet = () => {
  console.log("Hello, World!");
};
```

**Behind the scenes:**
```javascript
var greet; // Declaration is hoisted, initialized as undefined

greet(); // TypeError: greet is not a function (undefined is not a function)

greet = () => { // Assignment happens here
  console.log("Hello, World!");
};
```

**"With var, the variable greet is hoisted and initialized as undefined, so when we try to call it, we get a TypeError because undefined is not a function."**

---

## 4. FUNCTION EXPRESSIONS - Same Behavior as Arrow Functions

**"Traditional function expressions behave exactly like arrow functions."**

### Example:
```javascript
// This FAILS!
greet(); // ReferenceError or TypeError depending on var/let/const

const greet = function() {
  console.log("Hello, World!");
};
```

**"Whether you use `function() {}` syntax or arrow function `() => {}` syntax, if you're assigning it to a variable, it's NOT fully hoisted."**

---

## 5. KEY DIFFERENCES SUMMARY

**"Here's the critical difference to remember:"**

### Function Declaration (Fully Hoisted ✅):
```javascript
sayHello(); // ✅ Works!
function sayHello() {
  console.log("Hello");
}
```

### Arrow Function (NOT Hoisted ❌):
```javascript
sayHello(); // ❌ ReferenceError
const sayHello = () => {
  console.log("Hello");
};
```

### Function Expression (NOT Hoisted ❌):
```javascript
sayHello(); // ❌ ReferenceError
const sayHello = function() {
  console.log("Hello");
};
```

---

## 6. WHY THIS MATTERS

**"Understanding hoisting helps us write better code:"**

1. **Function Declarations:**
   - "Can be called anywhere in their scope"
   - "Good for utility functions you want available throughout the file"

2. **Arrow Functions / Function Expressions:**
   - "Must be defined before use"
   - "Prevents confusion about execution order"
   - "Makes code more predictable and easier to follow"

**"Most modern developers prefer arrow functions despite the lack of hoisting because they force us to write more organized code with clear dependencies."**

---

## 7. TEMPORAL DEAD ZONE (TDZ)

**"When arrow functions are declared with let or const, they're in the TDZ."**

```javascript
// TDZ starts here
console.log(myFunc); // ReferenceError: Cannot access before initialization
// TDZ continues...
const myFunc = () => {}; // TDZ ends here
```

**"The TDZ is the period between entering scope and the actual declaration. During this time, the variable exists but cannot be accessed."**

---

## COMPARISON TABLE

| Feature | Function Declaration | Arrow Function | Function Expression |
|---------|---------------------|----------------|-------------------|
| Syntax | `function name() {}` | `const name = () => {}` | `const name = function() {}` |
| Hoisting | ✅ Fully hoisted | ❌ Not hoisted | ❌ Not hoisted |
| Can call before definition? | ✅ Yes | ❌ No | ❌ No |
| Variable behavior | N/A | Follows let/const rules | Follows let/const rules |
| TDZ | No | Yes (with let/const) | Yes (with let/const) |

---

## INTERVIEW PRO TIPS

**If asked "Are arrow functions hoisted?"**
- "Arrow functions are stored in variables, so only the variable declaration is hoisted (if using var). The function assignment itself is not hoisted. With let/const, they're in the Temporal Dead Zone and throw a ReferenceError if accessed before initialization."

**If asked "What's the difference in hoisting behavior?"**
- "Function declarations are fully hoisted - both declaration and body. Arrow functions and function expressions are NOT fully hoisted because they're assigned to variables. Only the variable declaration is hoisted, not the assignment."

**If asked "Which should I use?"**
- "Use function declarations when you need hoisting or when defining utility functions. Use arrow functions for callbacks, methods, and when you want to avoid hoisting to make code more predictable."

---

## COMMON MISTAKE TO AVOID

**Don't confuse these two:**

```javascript
// This IS hoisted (function declaration)
function myFunc() {
  return "I'm hoisted!";
}

// This is NOT hoisted (function assigned to variable)
const myFunc = function() {
  return "I'm NOT hoisted!";
};

// This is also NOT hoisted (arrow function assigned to variable)
const myFunc = () => {
  return "I'm NOT hoisted either!";
};
```

**"The key is: if you see the `function` keyword at the BEGINNING of the statement, it's a declaration and it's hoisted. If `function` or arrow syntax comes after `=`, it's an expression and NOT fully hoisted."**


# JavaScript Function Types – Complete Reference Table ✅ (Obsidian-Safe)

## Function Types

| Type                          | Syntax                  | Short Example                           |
| ----------------------------- | ----------------------- | --------------------------------------- |
| **Function Declaration**      | `function fn()`         | `function add(a,b){return a+b}`         |
| **Function Expression**       | `const fn = function()` | `const add = function(a,b){return a+b}` |
| **Arrow Function**            | `() => {}`              | `const add = (a,b) => a+b`              |
| **Anonymous Function**        | No name                 | `setTimeout(function(){},1000)`         |
| **Named Function Expression** | Function has name       | `const add = function sum(a,b){}`       |
| **IIFE**                      | Runs immediately        | `(function(){})();`                     |
| **Constructor Function**      | Uses `new`              | `function User(n){this.n=n}`            |
| **Callback Function**         | Passed as argument      | `arr.map(x => x*2)`                     |
| **Higher-Order Function**     | Accepts/returns fn      | `fn => fn()`                            |
| **Async Function**            | `async/await`           | `async function f(){}`                  |
| **Generator Function**        | `function*`             | `function* gen(){yield 1}`              |
| **Method (Object)**           | Inside object           | `{ speak(){ } }`                        |
| **Method (Class)**            | Inside class            | `class A{ run(){} }`                    |

---

## Full Examples (Outside Table ✅)

### Function Declaration

```js
function add(a, b) {
  return a + b;
}
```

### Function Expression

```js
const add = function (a, b) {
  return a + b;
};
```

### Arrow Function

```js
const add = (a, b) => a + b;
```

### Anonymous Function

```js
setTimeout(function () {
  console.log("hello");
}, 1000);
```

### Named Function Expression

```js
const add = function sum(a, b) {
  return a + b;
};
// sum is only accessible inside
```

### IIFE

```js
(function () {
  console.log("runs immediately");
})();
```

### Constructor Function

```js
function User(name) {
  this.name = name;
}
const u = new User("Alice");
```

### Callback Function

```js
function greet(cb) {
  cb();
}
greet(() => console.log("hi"));
```

### Higher-Order Function

```js
function multiplier(x) {
  return y => x * y;
}
```

### Async Function

```js
async function fetchData() {
  const res = await fetch(url);
  return res.json();
}
```

### Generator Function

```js
function* count() {
  yield 1;
  yield 2;
}
```

### Object Method

```js
const obj = {
  speak() {
    console.log("hi");
  }
};
```

### Class Method

```js
class Person {
  speak() {
    console.log("hi");
  }
}
```

---

## Key Characteristics Comparison ✅

| Type                 | Hoisted | `this` | `arguments` | `new` |
| -------------------- | ------- | ------ | ----------- | ----- |
| Function Declaration | ✅       | ✅      | ✅           | ✅     |
| Function Expression  | ❌       | ✅      | ✅           | ✅     |
| Arrow Function       | ❌       | ❌      | ❌           | ❌     |
| Constructor Function | ✅       | ✅      | ✅           | ✅     |
| Async Function       | depends | ✅      | ✅           | ❌     |
| Generator Function   | ✅       | ✅      | ✅           | ❌     |
| Method               | ❌       | ✅      | ✅           | ❌     |

---

## Interview Rule (Remember This)

> ✅ Tables → **inline code only**  
> ✅ Code blocks → **outside tables**

You now have a **clean Obsidian note** and **interview-safe reference**.

If you want next:

- 🔥 `this` deep-dive (RN-focused)
    
- 🔥 Closures explained with hooks analogy
    
- 🔥 15 JS interview traps
    
- 🔥 One-page JS revision sheet
    

Say the word.


---
---

2. debounce and throttle functions
==ans==
---
---
# Debounce vs Throttle - Interview Script

## Opening Statement

"Debounce and throttle are performance optimization techniques used to control how often a function executes in response to frequent events. They're essential for preventing performance issues when handling events like scrolling, typing, or resizing."

---

## 1. WHAT PROBLEM DO THEY SOLVE?

**"First, let me explain the problem these techniques solve."**

"Imagine a search box on an e-commerce website. Without optimization, if a user types 'JavaScript' - that's 10 keystrokes - the application would make 10 API calls to the server. This creates several problems:"

- Unnecessary server load
- Wasted network bandwidth
- Poor performance
- Potential rate limiting
- Bad user experience with outdated results

**"Both debounce and throttle solve this by reducing the number of function calls, but they work differently."**

---

## 2. DEBOUNCE - Wait for Pause

**"Debounce delays function execution until after the user stops triggering the event."**

### Key Characteristics:

- **Waits for pause**: Only executes after events stop for a specified time
- **Timer resets**: Each new event cancels the previous scheduled execution
- **Executes once**: Only runs after the delay period passes with no new events

### The Analogy:

**"Think of an elevator."**

- "The elevator waits for people to stop entering before closing the door"
- "Every time someone new enters, the timer resets"
- "Only when everyone has finished entering does the door close"

### How It Works:

```javascript
function debounce(func, delay) {
  let timeoutId;
  
  return function(...args) {
    clearTimeout(timeoutId);  // Cancel previous timer
    timeoutId = setTimeout(() => {
      func.apply(this, args);  // Execute after delay
    }, delay);
  };
}
```

**"The key is `clearTimeout` - it cancels any pending execution when a new event occurs."**

---

## 3. DEBOUNCE USE CASES

**"I use debounce when I only care about the final state after the user stops:"**

### Real-World Examples:

**1. Search Input** (Most Common)

- "Without debounce: User types 'JavaScript' = 10 API calls"
- "With debounce: Wait 300ms after last keystroke = 1 API call"
- **Result**: "90% reduction in API calls"

```javascript
const debouncedSearch = debounce((query) => {
  fetch(`/api/search?q=${query}`)
}, 300);

searchInput.addEventListener('input', (e) => {
  debouncedSearch(e.target.value);
});
```

**2. Form Validation**

- "Validate only after user finishes typing in a field"
- "Prevents showing errors while user is still typing"

**3. Auto-save Feature**

- "Save document 1 second after user stops editing"
- "Prevents constant database writes"

**4. Window Resize**

- "Recalculate layout only after resize completes"
- "Avoids expensive calculations during resizing"

---

## 4. THROTTLE - Regular Intervals

**"Throttle limits function execution to once per specified time interval."**

### Key Characteristics:

- **Regular intervals**: Executes at fixed time intervals
- **Immediate execution**: Runs immediately, then enforces cooldown
- **Timer doesn't reset**: Subsequent calls during cooldown are ignored

### The Analogy:

**"Think of a security gate."**

- "The gate opens immediately when you first press the button"
- "Then it locks for 5 seconds - a cooldown period"
- "No matter how many times you press the button during those 5 seconds, it won't open again until the cooldown ends"

### How It Works:

```javascript
function throttle(func, limit) {
  let inThrottle;
  
  return function(...args) {
    if (inThrottle) return;  // Ignore if in cooldown
    
    func.apply(this, args);  // Execute immediately
    inThrottle = true;       // Start cooldown
    
    setTimeout(() => {
      inThrottle = false;    // End cooldown
    }, limit);
  };
}
```

**"The key is the `inThrottle` flag - it tracks whether we're in the cooldown period."**

---

## 5. THROTTLE USE CASES

**"I use throttle when I need regular updates during continuous events:"**

### Real-World Examples:

**1. Infinite Scroll**

- "Check scroll position every 200ms while user scrolls"
- "Load more content when near bottom"
- "Without throttle: Hundreds of checks per second"

```javascript
const handleScroll = throttle(() => {
  if (isNearBottom()) {
    loadMoreContent();
  }
}, 200);

window.addEventListener('scroll', handleScroll);
```

**2. Mouse Movement / Game Controls**

- "Track mouse position for cursor effects or games"
- "Update 20 times per second (every 50ms)"
- "Smooth performance without overwhelming the system"

**3. Button Click Prevention**

- "Prevent double-submission of forms"
- "Allow click only once every 2 seconds"

**4. Progress Indicators**

- "Update scroll progress bar every 100ms"
- "Show reading progress or video playback position"

---

## 6. KEY DIFFERENCES - THE COMPARISON

**"Here's the critical difference between them:"**

### Timeline Example:

**"Let's say a user triggers an event 10 times rapidly within 1 second, and we set our delay/limit to 500ms:"**

**Normal (No optimization):**

- Events: 10
- Function calls: 10 ❌

**Debounce (500ms):**

- Events: 10
- Function calls: 1 (after 500ms pause) ✅
- **When**: Only executes after events stop

**Throttle (500ms):**

- Events: 10
- Function calls: 2 (at 0ms and 500ms) ✅
- **When**: Executes at regular intervals while events continue

---

## 7. SIDE-BY-SIDE COMPARISON TABLE

| Aspect                | Debounce            | Throttle                  |
| --------------------- | ------------------- | ------------------------- |
| **Execution**         | After pause         | At intervals              |
| **Timer**             | Resets on each call | Doesn't reset             |
| **First call**        | Delayed             | Immediate                 |
| **During events**     | Waits               | Executes periodically     |
| **After events stop** | Executes once       | No additional execution   |
| **Best for**          | Final state matters | Continuous updates needed |

---

## 8. DECISION FRAMEWORK

**"Here's how I decide which to use:"**

### Ask yourself:

1. **"Do I only care about the final result?"** → Use Debounce
2. **"Do I need updates during the action?"** → Use Throttle

### Common Scenarios:

**Use DEBOUNCE for:**

- ✅ Search input
- ✅ Form validation
- ✅ Auto-save
- ✅ Window resize (final calculation)
- ✅ Text input events

**Use THROTTLE for:**

- ✅ Scroll events
- ✅ Mouse movement
- ✅ Window resize (continuous updates)
- ✅ Button spam prevention
- ✅ Progress tracking

---

## 9. PERFORMANCE IMPACT

**"Let me explain the performance benefits with real numbers."**

### Scenario: Search Input

**User types "JavaScript" (10 characters in 1 second)**

**Without optimization:**

```
API calls: 10
Network requests: 10
Server load: HIGH
User sees: 10 sets of results (confusing)
```

**With debounce (300ms):**

```
API calls: 1
Network requests: 1
Server load: LOW (90% reduction!)
User sees: Final results only (clean)
```

**With throttle (300ms):**

```
API calls: 3-4
Network requests: 3-4
Server load: MEDIUM (60-70% reduction)
User sees: Progressive results
```

**"Debounce gives better optimization for search, which is why it's the standard choice."**

---

## 10. COMMON INTERVIEW QUESTIONS & ANSWERS

### Q: "When would you use debounce over throttle?"

**Answer:** "I'd use debounce when I only care about the final state after a user action completes. For example, with search input - I want to wait until the user finishes typing before making an API call. Throttle would cause multiple calls while typing, which is wasteful for search."

### Q: "Can you implement debounce from scratch?"

**Answer:** "Yes, debounce uses a closure to maintain a timer ID. The key is clearing any existing timeout before setting a new one, which resets the delay. Here's the implementation..."

_(Show the code implementation)_

### Q: "What JavaScript concepts are used in these implementations?"

**Answer:**

- **Closures**: To maintain state (timeoutId, inThrottle) between calls
- **Higher-order functions**: Both return functions
- **setTimeout/clearTimeout**: For timing control
- **.apply()**: To maintain the correct 'this' context
- **Rest parameters (...args)**: To accept any number of arguments

### Q: "What's the difference in their timing behavior?"

**Answer:** "Throttle executes immediately on the first call, then enforces a cooldown. Debounce delays all executions until events stop. So throttle is more responsive initially, while debounce waits for completion."

---

## 11. ADVANCED CONCEPTS (Bonus Points)

**"In production, I'm also aware of advanced options:"**

### Leading and Trailing Edge:

- **Leading**: Execute at the start of the event sequence
- **Trailing**: Execute at the end (default for most implementations)

**Example:**

```javascript
// Debounce with leading edge
const search = debounce(searchAPI, 300, { leading: true });
// First keystroke triggers immediately, then waits
```

### Using Lodash (Industry Standard):

**"In real projects, I typically use Lodash's implementations because they're battle-tested and handle edge cases:"**

```javascript
import { debounce, throttle } from 'lodash';

const debouncedSearch = debounce(searchAPI, 300);
const throttledScroll = throttle(handleScroll, 200);
```

---

## 12. COMMON MISTAKES TO AVOID

**"Here are mistakes I've learned to avoid:"**

### ❌ Mistake 1: Creating new debounced function on each render

```javascript
// Wrong - creates new function every time!
<input onChange={() => debounce(handler, 300)()} />

// Correct - create once
const debouncedHandler = debounce(handler, 300);
<input onChange={debouncedHandler} />
```

### ❌ Mistake 2: Using throttle for search

- Creates poor UX with outdated results
- Use debounce instead

### ❌ Mistake 3: Using debounce for scroll

- User sees no updates until scrolling stops
- Use throttle for continuous feedback

---

## CLOSING STATEMENT

**"To summarize:"**

"Both debounce and throttle are essential performance optimization techniques. Debounce waits for events to stop before executing once - ideal for search, validation, and auto-save. Throttle executes at regular intervals during continuous events - perfect for scrolling, tracking, and progress updates."

"The key is understanding when each is appropriate: debounce for final state, throttle for continuous updates. This distinction is crucial for building performant web applications."

---

## QUICK REFERENCE CARD

### DEBOUNCE

- **What**: Delays until pause
- **When**: After events stop
- **Use for**: Search, validation, auto-save
- **Example**: "Wait 300ms after typing stops"

### THROTTLE

- **What**: Limits to intervals
- **When**: At regular times
- **Use for**: Scroll, mouse move, progress
- **Example**: "Update every 200ms while scrolling"



---
---



3. **Class based** and **Function based components**. Their differences and how can we simulate Lifecycle methods of Class based components using Function based components.
---
---


## Class-Based vs Function Components

---

## Opening Statement

"In React Native, we have two approaches to building components: Class Components and Function Components. Class Components were the traditional way before React 16.8, where we used lifecycle methods to manage component behavior. With the introduction of Hooks, Function Components can now handle everything Class Components do, but with cleaner, more intuitive syntax."

---

## 1. WHAT ARE CLASS COMPONENTS?

**"Let me start with Class Components, which were the original approach."**

### Definition:

- Class Components are ES6 classes that extend `React.Component`
- They have access to built-in lifecycle methods
- They manage state through `this.state` and `this.setState()`

### Basic Structure:

```javascript
class MyScreen extends Component {
  constructor(props) {
    super(props);
    this.state = { count: 0 };
  }
  
  render() {
    return (
      <View>
        <Text>{this.state.count}</Text>
      </View>
    );
  }
}
```

**Key points:**

- Must have a `render()` method that returns JSX
- Uses `this` keyword to access props and state
- Need to bind methods manually

---

## 2. WHAT ARE FUNCTION COMPONENTS?

**"Function Components are a more modern approach using React Hooks."**

### Definition:

- Simple JavaScript functions that return JSX
- Use Hooks like `useState` and `useEffect` for state and side effects
- No `this` keyword needed

### Basic Structure:

```javascript
function MyScreen() {
  const [count, setCount] = useState(0);
  
  return (
    <View>
      <Text>{count}</Text>
    </View>
  );
}
```

**Key points:**

- Much simpler syntax
- Direct access to variables, no `this`
- Just return JSX, no `render()` method

---

## 3. CLASS COMPONENT LIFECYCLE METHODS

**"Class Components have three main lifecycle phases: Mounting, Updating, and Unmounting."**

### Phase 1: MOUNTING (Component Appears)

**constructor() - Runs First**

- "This is where I initialize state and bind methods"
- "Must call `super(props)` before anything else"

```javascript
constructor(props) {
  super(props);
  this.state = { userData: null };
  this.handlePress = this.handlePress.bind(this);
}
```

**componentDidMount() - Runs After First Render**

- "This is where I make API calls, set up subscriptions, or start timers"
- "Perfect for data fetching when screen loads"

```javascript
componentDidMount() {
  // Fetch user data
  fetch('https://api.example.com/user')
    .then(res => res.json())
    .then(data => this.setState({ userData: data }));
    
  // Setup Android back button
  BackHandler.addEventListener('hardwareBackPress', this.handleBack);
}
```

**Real example:** "When building a profile screen, I'd fetch the user's data in `componentDidMount` so it loads as soon as the screen appears."

---

### Phase 2: UPDATING (Component Re-renders)

**componentDidUpdate() - Runs After Every Update**

- "This runs whenever props or state change"
- "I use this to respond to changes, like fetching new data"

```javascript
componentDidUpdate(prevProps, prevState) {
  // If userId prop changed, fetch new user data
  if (prevProps.userId !== this.props.userId) {
    this.fetchUserData();
  }
  
  // Save to storage when count changes
  if (prevState.count !== this.state.count) {
    AsyncStorage.setItem('count', String(this.state.count));
  }
}
```

**Important:** "I always compare `prevProps` or `prevState` to avoid infinite loops. Without this check, updating state inside `componentDidUpdate` would trigger another update endlessly."

**Real example:** "In a chat screen, if the `chatId` prop changes because user switched conversations, I'd use `componentDidUpdate` to load the new chat's messages."

---

### Phase 3: UNMOUNTING (Component Disappears)

**componentWillUnmount() - Runs Before Removal**

- "This is the cleanup phase - I remove listeners, clear timers, cancel API requests"
- "Essential to prevent memory leaks"

```javascript
componentWillUnmount() {
  // Remove back button listener
  BackHandler.removeEventListener('hardwareBackPress', this.handleBack);
  
  // Clear any timers
  clearInterval(this.timer);
  
  // Cancel pending requests
  this.abortController.abort();
}
```

**Real example:** "If I set up a location tracker in `componentDidMount`, I must stop tracking in `componentWillUnmount` when user leaves the screen, otherwise it keeps running in background and drains battery."

---

## 4. SIMULATING LIFECYCLE WITH HOOKS

**"Now, let me show how Function Components with Hooks can replicate all these lifecycle methods."**

### Hook: useState (Replaces this.state)

**Class Component:**

```javascript
this.state = { count: 0 };
this.setState({ count: 1 });
```

**Function Component:**

```javascript
const [count, setCount] = useState(0);
setCount(1);
```

**Advantage:** "I can have multiple useState calls instead of one big state object. This makes state management cleaner and more organized."

---

### Hook: useEffect (Replaces All Lifecycle Methods)

**"useEffect is the most powerful hook - it can simulate componentDidMount, componentDidUpdate, and componentWillUnmount."**

---

#### Pattern 1: componentDidMount (Run Once on Mount)

**Class Component:**

```javascript
componentDidMount() {
  this.fetchData();
}
```

**Function Component:**

```javascript
useEffect(() => {
  fetchData();
}, []); // Empty array = runs ONCE
```

**The Key:** "The empty dependency array `[]` tells React to run this effect only once when component mounts. This is exactly like `componentDidMount`."

**Real example:**

```javascript
useEffect(() => {
  // Fetch user profile when screen loads
  const loadProfile = async () => {
    const profile = await fetchUserProfile();
    setUserData(profile);
  };
  
  loadProfile();
}, []); // Only on mount
```

---

#### Pattern 2: componentDidUpdate (Run When Specific Values Change)

**Class Component:**

```javascript
componentDidUpdate(prevProps) {
  if (prevProps.userId !== this.props.userId) {
    this.fetchUserData();
  }
}
```

**Function Component:**

```javascript
useEffect(() => {
  fetchUserData();
}, [userId]); // Runs when userId changes
```

**The Key:** "By adding `userId` to the dependency array, this effect runs whenever `userId` changes. React automatically handles the comparison for me - no need to check `prevProps`."

**Real example:**

```javascript
useEffect(() => {
  // Search products when search query changes
  const searchProducts = async () => {
    const results = await fetch(`/api/products?q=${searchQuery}`);
    setProducts(results);
  };
  
  searchProducts();
}, [searchQuery]); // Re-run when searchQuery changes
```

---

#### Pattern 3: componentWillUnmount (Cleanup)

**Class Component:**

```javascript
componentDidMount() {
  this.timer = setInterval(() => console.log('tick'), 1000);
}

componentWillUnmount() {
  clearInterval(this.timer);
}
```

**Function Component:**

```javascript
useEffect(() => {
  const timer = setInterval(() => console.log('tick'), 1000);
  
  // Return cleanup function
  return () => {
    clearInterval(timer);
  };
}, []);
```

**The Key:** "The function returned from useEffect runs on cleanup - when component unmounts or before effect runs again. This keeps setup and cleanup logic together in one place."

**Real example:**

```javascript
useEffect(() => {
  // Subscribe to location updates
  const subscription = LocationService.subscribe((location) => {
    setCurrentLocation(location);
  });
  
  // Cleanup: Unsubscribe when component unmounts
  return () => {
    subscription.unsubscribe();
  };
}, []);
```

---

#### Pattern 4: Multiple Effects (Separation of Concerns)

**"Here's a huge advantage of Function Components: I can have multiple useEffect hooks for different purposes."**

**Class Component Problem:**

```javascript
componentDidMount() {
  this.fetchUserData();      // User data
  this.setupPushNotifications(); // Push notifications
  this.trackAnalytics();     // Analytics
}

componentWillUnmount() {
  this.cleanupPushNotifications();
  this.cleanupAnalytics();
}
// All logic mixed together!
```

**Function Component Solution:**

```javascript
// Effect 1: User data
useEffect(() => {
  fetchUserData();
}, [userId]);

// Effect 2: Push notifications
useEffect(() => {
  const subscription = setupPushNotifications();
  return () => subscription.unsubscribe();
}, []);

// Effect 3: Analytics
useEffect(() => {
  trackScreenView('ProfileScreen');
  return () => trackScreenExit('ProfileScreen');
}, []);
// Each concern is separate and clear!
```

**Advantage:** "Each effect handles one responsibility. In Class Components, related logic is split across multiple lifecycle methods, making it harder to understand and maintain."

---

## 5. COMPLETE LIFECYCLE COMPARISON

**"Let me show a real-world example: a chat screen that loads messages and subscribes to new messages."**

### Class Component:

```javascript
class ChatScreen extends Component {
  constructor(props) {
    super(props);
    this.state = { messages: [], loading: true };
  }
  
  componentDidMount() {
    // Load initial messages
    this.loadMessages();
    
    // Subscribe to new messages
    this.subscription = MessageAPI.subscribe(
      this.props.chatId,
      this.handleNewMessage
    );
    
    // Setup back button
    this.backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      this.handleBack
    );
  }
  
  componentDidUpdate(prevProps) {
    // Load new chat when chatId changes
    if (prevProps.chatId !== this.props.chatId) {
      this.loadMessages();
      
      // Resubscribe to new chat
      this.subscription.unsubscribe();
      this.subscription = MessageAPI.subscribe(
        this.props.chatId,
        this.handleNewMessage
      );
    }
  }
  
  componentWillUnmount() {
    // Cleanup
    this.subscription.unsubscribe();
    this.backHandler.remove();
  }
  
  loadMessages = async () => {
    this.setState({ loading: true });
    const messages = await ChatAPI.getMessages(this.props.chatId);
    this.setState({ messages, loading: false });
  };
  
  handleNewMessage = (message) => {
    this.setState(prev => ({
      messages: [...prev.messages, message]
    }));
  };
  
  handleBack = () => {
    // Handle back button
    return true;
  };
  
  render() {
    // Render messages...
  }
}
```

### Function Component (Much Cleaner!):

```javascript
function ChatScreen({ chatId }) {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Effect 1: Load messages when chatId changes
  useEffect(() => {
    const loadMessages = async () => {
      setLoading(true);
      const data = await ChatAPI.getMessages(chatId);
      setMessages(data);
      setLoading(false);
    };
    
    loadMessages();
  }, [chatId]); // Automatically handles the change
  
  // Effect 2: Subscribe to new messages
  useEffect(() => {
    const subscription = MessageAPI.subscribe(
      chatId,
      (message) => setMessages(prev => [...prev, message])
    );
    
    return () => subscription.unsubscribe();
  }, [chatId]);
  
  // Effect 3: Handle back button
  useEffect(() => {
    const backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      () => true
    );
    
    return () => backHandler.remove();
  }, []);
  
  // Render messages...
}
```

**"Notice how the Function Component:**

- Has less boilerplate code
- Each effect handles one responsibility
- Cleanup is right next to setup
- No need to compare prevProps manually
- No 'this' keyword confusion
- Much easier to read and understand"

---

## 6. KEY DIFFERENCES SUMMARY

### Syntax & Structure:

|Aspect|Class Component|Function Component|
|---|---|---|
|Definition|ES6 class|JavaScript function|
|Extends|`React.Component`|Nothing|
|Return|In `render()` method|Direct return|
|State|`this.state`|`useState()`|
|Props access|`this.props`|Function parameters|

### State Management:

**Class:**

```javascript
this.state = { count: 0 };
this.setState({ count: 1 });
```

**Function:**

```javascript
const [count, setCount] = useState(0);
setCount(1);
```

### Lifecycle:

**Class:** Separate methods

- `componentDidMount()`
- `componentDidUpdate()`
- `componentWillUnmount()`

**Function:** One hook - `useEffect()`

- `useEffect(() => {}, [])` = componentDidMount
- `useEffect(() => {}, [dep])` = componentDidUpdate
- `useEffect(() => { return cleanup }, [])` = componentWillUnmount

### Method Binding:

**Class:** Must bind

```javascript
constructor(props) {
  this.handlePress = this.handlePress.bind(this);
}
// or use arrow functions
handlePress = () => {}
```

**Function:** No binding needed

```javascript
const handlePress = () => {}
```

---

## 7. PRACTICAL ADVANTAGES OF FUNCTION COMPONENTS

**"In my React Native development, I prefer Function Components because:"**

### 1. Less Boilerplate

- No constructor needed
- No render method
- No this keyword

### 2. Better Code Organization

- Related logic stays together in one useEffect
- Class Components split logic across lifecycle methods

### 3. Easier Testing

- Pure functions are easier to test
- No need to mock 'this' context

### 4. Custom Hooks for Reusability

```javascript
// Custom hook for form handling
function useForm(initialValues) {
  const [values, setValues] = useState(initialValues);
  
  const handleChange = (name, value) => {
    setValues(prev => ({ ...prev, [name]: value }));
  };
  
  return [values, handleChange];
}

// Use in any component
function LoginScreen() {
  const [formData, updateForm] = useForm({ email: '', password: '' });
  // ...
}
```

**"Custom hooks let me extract and reuse logic across components. This is much harder with Class Components."**

### 5. Performance Optimization

```javascript
// Memoize expensive calculations
const sortedData = useMemo(() => {
  return data.sort((a, b) => a.name.localeCompare(b.name));
}, [data]);

// Prevent unnecessary re-renders
const MemoizedComponent = React.memo(MyComponent);
```

---

## 8. WHEN TO USE EACH

### Use Function Components (95% of cases):

✅ All new components ✅ Simple presentation components ✅ Components with state and effects ✅ When using custom hooks ✅ For better code organization

### Use Class Components:

✅ Legacy codebases ✅ Error Boundaries (no hook equivalent yet) ✅ Some third-party libraries require them ✅ When team prefers them

**"The React team recommends Function Components for all new code. They're the modern standard in React Native development."**

---

## 9. COMMON MISTAKES TO AVOID

### Mistake 1: Missing Cleanup

```javascript
// ❌ Wrong - memory leak!
useEffect(() => {
  const timer = setInterval(() => console.log('tick'), 1000);
}, []);

// ✅ Correct - cleanup
useEffect(() => {
  const timer = setInterval(() => console.log('tick'), 1000);
  return () => clearInterval(timer);
}, []);
```

### Mistake 2: Wrong Dependencies

```javascript
// ❌ Wrong - stale closure
useEffect(() => {
  console.log(userId); // May show old value
}, []); // Missing userId dependency

// ✅ Correct
useEffect(() => {
  console.log(userId);
}, [userId]);
```

### Mistake 3: Infinite Loop

```javascript
// ❌ Wrong - infinite loop!
useEffect(() => {
  setCount(count + 1); // Updates state
}); // No dependency array = runs on every render

// ✅ Correct
useEffect(() => {
  setCount(count + 1);
}, []); // Or add proper dependencies
```

---

## CLOSING STATEMENT

**"To summarize:"**

"Class Components and Function Components both achieve the same goals, but Function Components with Hooks are the modern standard. They offer cleaner syntax, better code organization, and powerful features like custom hooks."

"The lifecycle methods in Class Components - `componentDidMount`, `componentDidUpdate`, and `componentWillUnmount` - can all be simulated using the `useEffect` hook in Function Components, with the added benefit of keeping related logic together."

"For new React Native projects, I always start with Function Components. They're simpler to write, easier to understand, and recommended by the React team."

---

## QUICK REFERENCE CARD

### Lifecycle Mapping:

```
CLASS                          FUNCTION (Hooks)
─────────────────────────────  ──────────────────────────
constructor()              →   useState() initialization
componentDidMount()        →   useEffect(() => {}, [])
componentDidUpdate()       →   useEffect(() => {}, [deps])
componentWillUnmount()     →   useEffect(() => { return cleanup }, [])
this.state                 →   useState()
this.setState()            →   setState from useState
this.props                 →   function parameters
shouldComponentUpdate()    →   React.memo()
```

### When Each Effect Runs:

```javascript
useEffect(() => {})           // Every render
useEffect(() => {}, [])       // Once on mount
useEffect(() => {}, [a, b])   // When a or b changes
useEffect(() => { return f }, []) // f runs on unmount
```

---
---

3. **Context API** — How to use it and what are the places where it can be used (These are expected questions to follow). But then he asked a rather weird question of whether Context API is only used as a solution to Prop drilling or is there any other function to it ?
  ==ANS:== 
---

---

## Opening Statement

"The Context API is React's built-in solution for sharing data across the component tree without manually passing props at every level. While it's commonly known for solving prop drilling, it's actually much more than that - it's essentially a lightweight dependency injection system that allows us to provide shared state, services, or configuration anywhere in our React Native app."

---

## 1. WHAT IS CONTEXT API?

**"Let me explain what Context API is and how it works."**

### Definition:

Context API is a built-in React feature that allows components to share values - whether data or functions - across the component tree without explicitly passing props through each level.

### Core Components:

There are three main parts to using Context:

1. **`createContext()`** - Creates a context object
2. **`Provider`** - Supplies the data to the tree
3. **`useContext()`** - Consumes the data in components

### Simple Analogy:

**"Think of Context as a broadcast system."**

- The Provider is like a radio tower broadcasting a signal
- Any component within range (children of Provider) can tune in and receive the signal using useContext
- No need to physically wire each component to the source

---

## 2. HOW TO USE IT - PRACTICAL EXAMPLE

**"Let me show you a real React Native example with authentication."**

### Step 1: Create the Context

```javascript
// contexts/AuthContext.js
import React, { createContext, useState, useContext } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Create context with default values
const AuthContext = createContext(null);

// Provider component
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Login function
  const login = async (email, password) => {
    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
      const userData = await response.json();
      
      setUser(userData);
      await AsyncStorage.setItem('user', JSON.stringify(userData));
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  // Logout function
  const logout = async () => {
    setUser(null);
    await AsyncStorage.removeItem('user');
  };

  // Check if user is logged in on app start
  const checkAuth = async () => {
    const userData = await AsyncStorage.getItem('user');
    if (userData) {
      setUser(JSON.parse(userData));
    }
    setLoading(false);
  };

  return (
    <AuthContext.Provider 
      value={{ user, login, logout, checkAuth, loading }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// Custom hook for easier consumption
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

```typescript 
//context.tsx
import React, {
  createContext,
  useState,
  useContext,
  ReactNode,
} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

/* ------------------ Types ------------------ */

export interface User {
  id?: string;
  email?: string;
  name?: string;
  // add more fields as needed
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
}

interface AuthProviderProps {
  children: ReactNode;
}

/* ------------------ Context ------------------ */

// NOTE: undefined instead of null is the TS best practice
const AuthContext = createContext<AuthContextType | undefined>(undefined);

/* ------------------ Provider ------------------ */

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const login = async (email: string, password: string): Promise<void> => {
    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      const userData: User = await response.json();

      setUser(userData);
      await AsyncStorage.setItem('user', JSON.stringify(userData));
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  const logout = async (): Promise<void> => {
    setUser(null);
    await AsyncStorage.removeItem('user');
  };

  const checkAuth = async (): Promise<void> => {
    try {
      const userData = await AsyncStorage.getItem('user');
      if (userData) {
        setUser(JSON.parse(userData) as User);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{ user, loading, login, logout, checkAuth }}
    >
      {children}
    </AuthContext.Provider>
  );
};

/* ------------------ Hook ------------------ */

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);

  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
};

```

**Key Points:**

- "I create a custom hook `useAuth` for cleaner consumption and better error handling"
- "The Provider component manages all authentication logic in one place"
- "Any component can access user data and auth methods without prop drilling"

---

### Step 2: Wrap Your App

```javascript
// App.js
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { AuthProvider } from './contexts/AuthContext';
import AppNavigator from './navigation/AppNavigator';

export default function App() {
  return (
    <AuthProvider>
      <NavigationContainer>
        <AppNavigator />
      </NavigationContainer>
    </AuthProvider>
  );
}
```

**Explanation:** "By wrapping the app with AuthProvider, all screens and components now have access to authentication state and methods."

---

### Step 3: Consume in Any Component

```javascript
// screens/ProfileScreen.js
import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useAuth } from '../contexts/AuthContext';

const ProfileScreen = () => {
  const { user, logout } = useAuth();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Welcome, {user?.name}!</Text>
      <Text style={styles.email}>{user?.email}</Text>
      
      <TouchableOpacity style={styles.button} onPress={logout}>
        <Text style={styles.buttonText}>Logout</Text>
      </TouchableOpacity>
    </View>
  );
};

// Another screen - no prop drilling needed!
// screens/HomeScreen.js
const HomeScreen = () => {
  const { user } = useAuth();
  
  return (
    <View>
      <Text>Hello, {user?.name}</Text>
    </View>
  );
};
```

**Advantage:** "Notice I can access user data in both ProfileScreen and HomeScreen without passing it through props. Even if these screens are deeply nested in different navigators, they can directly access the context."

---

## 3. WHERE TO USE CONTEXT API IN REACT NATIVE

**"Here are the most common use cases in React Native apps:"**

### 1. **Authentication Context** ✅ Most Common

```javascript
const { user, login, logout, isAuthenticated } = useAuth();
```

- User data, JWT tokens, login/logout functions
- Check authentication status across the app
- Navigate based on auth state

**Why:** "Every screen needs to know if user is logged in - navigator needs it for route protection, profile screen needs user data, settings screen needs logout function"

---

### 2. **Theme Context** ✅ Essential for Dark Mode

```javascript
const { theme, isDarkMode, toggleTheme } = useTheme();
```

- Dark/Light mode toggle
- Color schemes, font sizes
- System theme following

**Example:**

```javascript
export const ThemeProvider = ({ children }) => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  
  const theme = {
    colors: isDarkMode ? darkColors : lightColors,
    spacing: { small: 8, medium: 16, large: 24 }
  };
  
  const toggleTheme = () => setIsDarkMode(!isDarkMode);
  
  return (
    <ThemeContext.Provider value={{ theme, isDarkMode, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

// Usage in any component
const MyScreen = () => {
  const { theme } = useTheme();
  return <View style={{ backgroundColor: theme.colors.background }} />;
};
```

---

### 3. **Localization/Language Context** ✅ Internationalization

```javascript
const { t, locale, changeLanguage } = useLanguage();
```

- Multi-language support
- Translation strings
- RTL support

**Example:**

```javascript
<Text>{t('welcome_message')}</Text>
<Button onPress={() => changeLanguage('hi')}>हिंदी</Button>
```

---

### 4. **Network Status Context** ✅ Offline Support

```javascript
const { isConnected, networkType } = useNetwork();
```

- Online/offline detection
- Show connection status banners
- Queue API calls when offline

---

### 5. **Notification Context** ✅ Global Alerts

```javascript
const { showToast, showAlert } = useNotification();
```

- Toast messages, snackbars
- Alert dialogs
- Success/error notifications

**Example:**

```javascript
const { showToast } = useNotification();

const handleSave = async () => {
  await saveData();
  showToast('Data saved successfully!', 'success');
};
```

---

### 6. **Navigation State Context** ✅ Custom Navigation Logic

```javascript
const { currentTab, setTab, navigationHistory } = useNavigation();
```

- Custom tab bar state
- Deep linking parameters
- Navigation analytics

---

### 7. **Settings/Preferences Context** ✅ User Preferences

```javascript
const { fontSize, notifications, autoPlay } = useSettings();
```

- App preferences
- User settings
- Feature flags

---

### 8. **Socket/WebSocket Context** ✅ Real-time Features

```javascript
const { socket, isConnected, emit, on } = useSocket();
```

- Chat applications
- Real-time notifications
- Live updates

**Example:**

```javascript
export const SocketProvider = ({ children }) => {
  const [socket, setSocket] = useState(null);
  const { user } = useAuth();
  
  useEffect(() => {
    if (user) {
      const ws = io('https://api.example.com', {
        query: { userId: user.id }
      });
      setSocket(ws);
      
      return () => ws.disconnect();
    }
  }, [user]);
  
  return (
    <SocketContext.Provider value={{ socket }}>
      {children}
    </SocketContext.Provider>
  );
};
```

---

### 9. **API Client Context** ✅ Service Injection

```javascript
const { api } = useAPI();
```

- Shared API instance with auth headers
- Request interceptors
- Error handling

---

### 10. **Feature Flags Context** ✅ A/B Testing

```javascript
const { isFeatureEnabled } = useFeatureFlags();

if (isFeatureEnabled('new_checkout')) {
  return <NewCheckoutScreen />;
}
```

---

## 4. THE INTERVIEW TRICK QUESTION

> **"Is Context API only used to solve prop drilling, or does it have other purposes?"**

### ❌ Weak Answer:

"Yes, Context API is mainly for avoiding prop drilling when passing data through multiple components."

### ✅ Strong Answer:

**"No, prop drilling is just one use case. Context API is more accurately described as a lightweight dependency injection mechanism in React Native."**

**Here's what I mean:**

1. **Dependency Injection Pattern:**
    
    - "Context allows me to inject services, configurations, or utilities into components without tight coupling"
    - "For example, I can inject an API client, analytics service, or logger across the app"
2. **Architectural Benefits:**
    
    - "It provides a clean separation of concerns - business logic in context, UI in components"
    - "It enables me to swap implementations easily, like switching from REST to GraphQL"
3. **Service Layer:**
    
    - "I use Context to provide singleton services like socket connections, notification systems, or location trackers"
    - "These aren't about prop drilling - they're about providing shared resources"

**Example to Illustrate:**

```javascript
// Analytics service injection
export const AnalyticsProvider = ({ children }) => {
  const analytics = useRef(new AnalyticsService()).current;
  
  const trackEvent = (event, params) => {
    analytics.logEvent(event, params);
  };
  
  const trackScreen = (screenName) => {
    analytics.setCurrentScreen(screenName);
  };
  
  return (
    <AnalyticsContext.Provider value={{ trackEvent, trackScreen }}>
      {children}
    </AnalyticsContext.Provider>
  );
};

// Any component can now track events
const CheckoutScreen = () => {
  const { trackEvent } = useAnalytics();
  
  const handlePurchase = () => {
    trackEvent('purchase_completed', { amount: 99.99 });
  };
};
```

**"This isn't about avoiding prop drilling - it's about providing a service throughout the app. The analytics service is injected once and available everywhere, similar to dependency injection in other languages."**

---

## 5. ADDITIONAL FUNCTIONS BEYOND PROP DRILLING

**"Let me list other key functions Context API serves:"**

### 1. **Configuration Injection**

- API base URLs
- Feature flags
- Environment variables

```javascript
export const ConfigProvider = ({ children }) => {
  const config = {
    apiUrl: process.env.API_URL,
    environment: __DEV__ ? 'development' : 'production',
    enableAnalytics: true
  };
  
  return (
    <ConfigContext.Provider value={config}>
      {children}
    </ConfigContext.Provider>
  );
};
```

---

### 2. **Global Event Bus**

- Cross-component communication
- Decoupled messaging
- Event-driven architecture

```javascript
export const EventProvider = ({ children }) => {
  const [listeners, setListeners] = useState({});
  
  const emit = (event, data) => {
    listeners[event]?.forEach(callback => callback(data));
  };
  
  const on = (event, callback) => {
    setListeners(prev => ({
      ...prev,
      [event]: [...(prev[event] || []), callback]
    }));
  };
  
  return (
    <EventContext.Provider value={{ emit, on }}>
      {children}
    </EventContext.Provider>
  );
};

// Component A
const ComponentA = () => {
  const { emit } = useEvent();
  return <Button onPress={() => emit('refresh', { id: 123 })} />;
};

// Component B (completely unrelated)
const ComponentB = () => {
  const { on } = useEvent();
  
  useEffect(() => {
    on('refresh', (data) => {
      console.log('Refresh triggered:', data);
    });
  }, []);
};
```

---

### 3. **State Management Alternative**

- Simpler than Redux for small apps
- Combines useState + useReducer + Context
- Local state management solution

```javascript
export const AppStateProvider = ({ children }) => {
  const [state, dispatch] = useReducer(appReducer, initialState);
  
  return (
    <AppStateContext.Provider value={{ state, dispatch }}>
      {children}
    </AppStateContext.Provider>
  );
};
```

---

### 4. **Performance Optimization**

- Memoized computations shared across components
- Prevent re-fetching same data
- Caching layer

```javascript
export const CacheProvider = ({ children }) => {
  const cache = useRef(new Map());
  
  const getCached = (key) => cache.current.get(key);
  const setCached = (key, value) => cache.current.set(key, value);
  
  return (
    <CacheContext.Provider value={{ getCached, setCached }}>
      {children}
    </CacheContext.Provider>
  );
};
```

---

## 6. WHEN NOT TO USE CONTEXT API

**"It's important to know when NOT to use Context:"**

### 1. **Frequently Changing State** ❌

```javascript
// Bad - re-renders all consumers on every keystroke
const [searchText, setSearchText] = useState('');
```

- Input fields that update rapidly
- Animation values
- Scroll positions

**Better:** Local state or specialized libraries like Zustand

---

### 2. **High-Performance Requirements** ❌

- Large data sets that update frequently
- Complex calculations on every render
- Games or animations

**Better:** Redux with selectors, or Recoil

---

### 3. **Simple Parent-Child Communication** ❌

```javascript
// Don't use Context for this!
<Parent>
  <Child data={data} />
</Parent>
```

- Direct props are clearer
- Better performance
- Easier to trace

---

## 7. BEST PRACTICES IN REACT NATIVE

**"Here are best practices I follow when using Context API:"**

### 1. **Create Custom Hooks**

```javascript
// ✅ Good
export const useAuth = () => useContext(AuthContext);

// ❌ Bad - consumers have to import both
import { useContext } from 'react';
import { AuthContext } from './AuthContext';
const auth = useContext(AuthContext);
```

---

### 2. **Add Error Boundaries**

```javascript
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

---

### 3. **Split Large Contexts**

```javascript
// ❌ Bad - one huge context
<AppContext.Provider value={{ 
  user, theme, language, settings, notifications, ... 
}}>

// ✅ Good - separate contexts
<AuthProvider>
  <ThemeProvider>
    <LanguageProvider>
      <App />
    </LanguageProvider>
  </ThemeProvider>
</AuthProvider>
```

**Why:** "If I update theme, only components using theme re-render, not those using auth"

---

### 4. **Memoize Context Values**

```javascript
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  
  // ✅ Memoize to prevent unnecessary re-renders
  const value = useMemo(() => ({
    user,
    login,
    logout
  }), [user]);
  
  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
```

---

### 5. **Type Safety with TypeScript**

```typescript
interface AuthContextType {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);
```

---

## 8. CONTEXT VS REDUX IN REACT NATIVE

**"When interviewer asks about Context vs Redux:"**

### Use Context API when:

✅ Small to medium apps ✅ Simple state management needs ✅ 2-5 contexts maximum ✅ State doesn't change frequently ✅ Don't need time-travel debugging

### Use Redux when:

✅ Large, complex apps ✅ Many state updates ✅ Need middleware (logging, API calls) ✅ Team familiar with Redux ✅ Need DevTools for debugging

**"For most React Native apps, Context API is sufficient. I only reach for Redux when complexity justifies the overhead."**

---

## CLOSING STATEMENT

**"To summarize:"**

"The Context API is React Native's built-in solution for sharing data across components. While it's commonly known for solving prop drilling, it's actually a lightweight dependency injection system that serves multiple purposes:"

1. **Prop drilling solution** - Share data without threading through components
2. **Service injection** - Provide shared services like analytics, API clients, sockets
3. **Configuration management** - Inject app config and feature flags
4. **State management** - Alternative to Redux for simpler apps
5. **Event system** - Enable cross-component communication

"In React Native, I use Context extensively for authentication, theming, localization, and providing shared services. It's cleaner than prop drilling and more appropriate than Redux for most apps."

**"The key insight is: Context API is about providing shared resources and services, not just avoiding props. It's an architectural pattern, not just a convenience feature."**

---

## QUICK INTERVIEW RESPONSE

**If asked: "Is Context only for prop drilling?"**

> "No. While Context does solve prop drilling, it's more accurately a dependency injection mechanism. I use it to provide shared services like authentication, theme configuration, socket connections, and API clients throughout my React Native app. It's about architectural clarity and service provision, not just avoiding props. Think of it as React's built-in way to implement the Provider pattern and dependency injection."
---
---


4. Redux - The data flow in Redux. It is a standard question of Redux (Dispatching an action, Reducers used to update the store, and store as a single source of Truth, selectors to get some data from the current state of the store, and so on).
---
---

# Redux Data Flow - Interview Script (TypeScript)

## Opening Statement

"Redux follows a unidirectional data flow architecture, which makes state management predictable and easier to debug. Let me walk you through the complete flow with a practical React Native TypeScript example."

---

## 1. The Store - Single Source of Truth

### What to say:

"The Store is the single source of truth in Redux. It holds the entire application state in one centralized location. In React Native, this could be user data, authentication status, cart items, or any global state."

### Example:

```typescript
// store.ts
import { configureStore } from '@reduxjs/toolkit';
import userReducer from './userSlice';
import cartReducer from './cartSlice';

export const store = configureStore({
  reducer: {
    user: userReducer,
    cart: cartReducer,
  },
});

// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

```typescript
// hooks.ts - Typed hooks for better TypeScript support
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux';
import type { RootState, AppDispatch } from './store';

export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
```

---

## 2. Actions - Describing What Happened

### What to say:

"Actions are plain JavaScript objects that describe what happened in the application. They must have a 'type' property and can carry additional data as payload. Actions are the only way to send data to the store. With TypeScript, we get type safety for our action payloads."

### Example:

```typescript
// types.ts
export interface Product {
  id: string;
  name: string;
  price: number;
  quantity: number;
}

// Traditional action with TypeScript
interface AddToCartAction {
  type: 'cart/addItem';
  payload: Product;
}

const addToCart = (product: Product): AddToCartAction => ({
  type: 'cart/addItem',
  payload: product,
});

// Redux Toolkit action creator (more common now)
// Actions are automatically typed when using createSlice
import { createAction } from '@reduxjs/toolkit';

export const addToCart = createAction<Product>('cart/addItem');
export const removeFromCart = createAction<string>('cart/removeItem');
```

---

## 3. Dispatching Actions - Triggering State Changes

### What to say:

"To initiate a state change, we dispatch actions using the dispatch function. In React Native components, we get dispatch from our custom useAppDispatch hook for proper TypeScript typing. This is how user interactions trigger state updates."

### Example:

```typescript
// ProductScreen.tsx - React Native Component
import React from 'react';
import { View, Button, Text, StyleSheet } from 'react-native';
import { useAppDispatch } from './hooks';
import { addToCart } from './cartSlice';
import { Product } from './types';

interface ProductScreenProps {
  product: Product;
}

const ProductScreen: React.FC<ProductScreenProps> = ({ product }) => {
  const dispatch = useAppDispatch();

  const handleAddToCart = () => {
    // Dispatching the action with full type safety
    dispatch(addToCart({
      id: product.id,
      name: product.name,
      price: product.price,
      quantity: 1,
    }));
  };

  return (
    <View style={styles.container}>
      <Text style={styles.name}>{product.name}</Text>
      <Text style={styles.price}>${product.price.toFixed(2)}</Text>
      <Button title="Add to Cart" onPress={handleAddToCart} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
  },
  name: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  price: {
    fontSize: 16,
    color: '#666',
    marginVertical: 8,
  },
});

export default ProductScreen;
```

---

## 4. Reducers - Pure Functions That Update State

### What to say:

"Reducers are pure functions that take the current state and an action, then return a new state. They must never mutate the existing state directly. Redux Toolkit's createSlice uses Immer under the hood, which allows us to write code that looks like mutations but actually creates immutable updates. With TypeScript, we get compile-time type checking for our state."

### Example:

```typescript
// cartSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Product } from './types';

interface CartState {
  items: Product[];
  totalAmount: number;
}

const initialState: CartState = {
  items: [],
  totalAmount: 0,
};

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    addToCart: (state, action: PayloadAction<Product>) => {
      const existingItem = state.items.find(
        item => item.id === action.payload.id
      );
      
      if (existingItem) {
        existingItem.quantity += 1;
      } else {
        state.items.push(action.payload);
      }
      
      // Recalculate total
      state.totalAmount = state.items.reduce(
        (sum, item) => sum + (item.price * item.quantity), 
        0
      );
    },
    
    removeFromCart: (state, action: PayloadAction<string>) => {
      state.items = state.items.filter(
        item => item.id !== action.payload
      );
      
      state.totalAmount = state.items.reduce(
        (sum, item) => sum + (item.price * item.quantity), 
        0
      );
    },
    
    updateQuantity: (state, action: PayloadAction<{ id: string; quantity: number }>) => {
      const item = state.items.find(item => item.id === action.payload.id);
      if (item) {
        item.quantity = action.payload.quantity;
        state.totalAmount = state.items.reduce(
          (sum, item) => sum + (item.price * item.quantity), 
          0
        );
      }
    },
    
    clearCart: (state) => {
      state.items = [];
      state.totalAmount = 0;
    },
  },
});

export const { addToCart, removeFromCart, updateQuantity, clearCart } = cartSlice.actions;
export default cartSlice.reducer;
```

---

## 5. Selectors - Reading Data from Store

### What to say:

"Selectors are functions that extract specific pieces of data from the Redux store. We use the useAppSelector hook in React Native to subscribe to store updates with full TypeScript support. Selectors can also compute derived data, keeping our components clean and performant."

### Example:

```typescript
// cartSlice.ts (continued - add selectors)
import { RootState } from './store';

// Basic selectors with TypeScript
export const selectCartItems = (state: RootState): Product[] => state.cart.items;
export const selectTotalAmount = (state: RootState): number => state.cart.totalAmount;

// Derived selector - computing cart item count
export const selectCartItemCount = (state: RootState): number => 
  state.cart.items.reduce((count, item) => count + item.quantity, 0);

// Selector with parameter (using a selector factory)
export const selectCartItemById = (id: string) => (state: RootState): Product | undefined =>
  state.cart.items.find(item => item.id === id);
```

```typescript
// CartScreen.tsx - Using selectors in component
import React from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity } from 'react-native';
import { useAppSelector, useAppDispatch } from './hooks';
import { 
  selectCartItems, 
  selectTotalAmount, 
  selectCartItemCount,
  removeFromCart 
} from './cartSlice';
import { Product } from './types';

const CartScreen: React.FC = () => {
  const cartItems = useAppSelector(selectCartItems);
  const totalAmount = useAppSelector(selectTotalAmount);
  const itemCount = useAppSelector(selectCartItemCount);
  const dispatch = useAppDispatch();

  const handleRemove = (id: string) => {
    dispatch(removeFromCart(id));
  };

  const renderItem = ({ item }: { item: Product }) => (
    <View style={styles.itemContainer}>
      <View style={styles.itemInfo}>
        <Text style={styles.itemName}>{item.name}</Text>
        <Text style={styles.itemPrice}>${item.price.toFixed(2)}</Text>
        <Text style={styles.itemQuantity}>Qty: {item.quantity}</Text>
      </View>
      <TouchableOpacity 
        style={styles.removeButton}
        onPress={() => handleRemove(item.id)}
      >
        <Text style={styles.removeText}>Remove</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Shopping Cart</Text>
        <Text style={styles.itemCount}>Items: {itemCount}</Text>
      </View>
      
      <FlatList
        data={cartItems}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        ListEmptyComponent={
          <Text style={styles.emptyText}>Your cart is empty</Text>
        }
      />
      
      <View style={styles.footer}>
        <Text style={styles.total}>Total: ${totalAmount.toFixed(2)}</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  itemCount: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  itemContainer: {
    flexDirection: 'row',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  itemInfo: {
    flex: 1,
  },
  itemName: {
    fontSize: 16,
    fontWeight: '600',
  },
  itemPrice: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  itemQuantity: {
    fontSize: 14,
    color: '#999',
    marginTop: 2,
  },
  removeButton: {
    backgroundColor: '#ff4444',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 4,
  },
  removeText: {
    color: '#fff',
    fontWeight: '600',
  },
  emptyText: {
    textAlign: 'center',
    marginTop: 40,
    fontSize: 16,
    color: '#999',
  },
  footer: {
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
  },
  total: {
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'right',
  },
});

export default CartScreen;
```

---

## 6. Complete Data Flow Explanation

### What to say:

"Let me summarize the complete Redux data flow:

1. **User Interaction**: A user taps a button in the React Native app
2. **Dispatch Action**: The component dispatches a fully typed action with type and payload
3. **Reducer Processing**: The reducer receives the action and current state, then calculates the new state
4. **Store Update**: The store updates with the new state returned by the reducer
5. **Selector Re-evaluation**: Components using useAppSelector are notified of changes
6. **UI Re-render**: React Native re-renders only the components whose selected data changed

With TypeScript, we get compile-time type checking at every step, catching errors before runtime. This unidirectional flow makes the application predictable and easier to debug."

---

## 7. Practical React Native Example - Complete Flow with Async

### Example:

```typescript
// types.ts
export interface User {
  id: string;
  name: string;
  email: string;
  token: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface ApiError {
  message: string;
  code?: string;
}
```

```typescript
// userSlice.ts
import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { User, LoginCredentials, ApiError } from './types';
import { RootState } from './store';

interface UserState {
  userInfo: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

const initialState: UserState = {
  userInfo: null,
  isAuthenticated: false,
  loading: false,
  error: null,
};

// Async thunk for API calls with TypeScript
export const loginUser = createAsyncThunk<
  User, // Return type
  LoginCredentials, // Argument type
  { rejectValue: string } // ThunkAPI config
>(
  'user/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await fetch('https://api.example.com/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials),
      });
      
      const data = await response.json();
      
      if (!response.ok) {
        return rejectWithValue(data.message || 'Login failed');
      }
      
      // Save token
      await AsyncStorage.setItem('token', data.token);
      return data.user as User;
    } catch (error) {
      const err = error as Error;
      return rejectWithValue(err.message || 'Network error');
    }
  }
);

export const logoutUser = createAsyncThunk(
  'user/logout',
  async () => {
    await AsyncStorage.removeItem('token');
  }
);

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setUser: (state, action: PayloadAction<User>) => {
      state.userInfo = action.payload;
      state.isAuthenticated = true;
    },
  },
  extraReducers: (builder) => {
    // Login cases
    builder
      .addCase(loginUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.loading = false;
        state.isAuthenticated = true;
        state.userInfo = action.payload;
        state.error = null;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload || 'Login failed';
      })
      // Logout cases
      .addCase(logoutUser.fulfilled, (state) => {
        state.userInfo = null;
        state.isAuthenticated = false;
      });
  },
});

export const { clearError, setUser } = userSlice.actions;
export default userSlice.reducer;

// Typed selectors
export const selectUser = (state: RootState): User | null => state.user.userInfo;
export const selectIsAuthenticated = (state: RootState): boolean => state.user.isAuthenticated;
export const selectUserLoading = (state: RootState): boolean => state.user.loading;
export const selectUserError = (state: RootState): string | null => state.user.error;
```

```typescript
// LoginScreen.tsx
import React, { useState, useEffect } from 'react';
import { 
  View, 
  TextInput, 
  Button, 
  Text, 
  ActivityIndicator, 
  StyleSheet,
  Alert 
} from 'react-native';
import { useAppDispatch, useAppSelector } from './hooks';
import { 
  loginUser, 
  selectUserLoading, 
  selectUserError,
  clearError 
} from './userSlice';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

type RootStackParamList = {
  Login: undefined;
  Home: undefined;
};

type LoginScreenNavigationProp = NativeStackNavigationProp<
  RootStackParamList,
  'Login'
>;

interface LoginScreenProps {
  navigation: LoginScreenNavigationProp;
}

const LoginScreen: React.FC<LoginScreenProps> = ({ navigation }) => {
  const [email, setEmail] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  
  const dispatch = useAppDispatch();
  const loading = useAppSelector(selectUserLoading);
  const error = useAppSelector(selectUserError);

  useEffect(() => {
    if (error) {
      Alert.alert('Error', error);
      dispatch(clearError());
    }
  }, [error, dispatch]);

  const handleLogin = async () => {
    if (!email || !password) {
      Alert.alert('Error', 'Please enter email and password');
      return;
    }

    try {
      await dispatch(loginUser({ email, password })).unwrap();
      navigation.navigate('Home');
    } catch (err) {
      // Error already handled in slice
      console.log('Login failed:', err);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Login</Text>
      
      <TextInput
        style={styles.input}
        placeholder="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        editable={!loading}
      />
      
      <TextInput
        style={styles.input}
        placeholder="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        editable={!loading}
      />
      
      <Button 
        title={loading ? 'Loading...' : 'Login'} 
        onPress={handleLogin}
        disabled={loading}
      />
      
      {loading && (
        <ActivityIndicator 
          size="large" 
          color="#0000ff" 
          style={styles.loader} 
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    justifyContent: 'center',
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    marginBottom: 40,
    textAlign: 'center',
  },
  input: {
    height: 50,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 16,
    marginBottom: 16,
    fontSize: 16,
  },
  loader: {
    marginTop: 20,
  },
});

export default LoginScreen;
```

---

## Key Points to Emphasize

### What to say:

"The key benefits of Redux's data flow with TypeScript are:

1. **Type Safety**: TypeScript catches errors at compile-time, preventing runtime bugs
2. **Predictability**: State mutations are predictable because reducers are pure functions
3. **Debuggability**: With Redux DevTools, we can track every action and state change
4. **Testability**: Pure reducers with typed inputs and outputs are easy to test
5. **Time-travel debugging**: We can replay actions to debug issues
6. **Single source of truth**: All state in one place prevents inconsistencies
7. **IntelliSense Support**: Full autocomplete and type checking in your IDE

In React Native specifically, Redux with TypeScript is excellent for managing complex state like authentication, shopping carts, user preferences, or data that needs to be shared across many screens, all with compile-time type checking."

---

## Common Follow-up Questions

### Q: When would you use Redux vs Context API?

"I'd use Redux for complex state logic, frequent updates, middleware needs, or when I need powerful debugging tools. Redux Toolkit with TypeScript also provides excellent type safety. For simpler state that doesn't change often or is only needed in a few components, Context API is sufficient and lighter weight."

### Q: What is Redux Toolkit and why use it?

"Redux Toolkit is the official recommended way to write Redux logic. It includes createSlice which combines actions and reducers, uses Immer for immutable updates, and includes createAsyncThunk for async operations. It significantly reduces boilerplate and follows best practices by default. With TypeScript, Redux Toolkit provides excellent type inference, making our code more maintainable."

### Q: How do you handle async operations in Redux with TypeScript?

"I use Redux Toolkit's createAsyncThunk with proper TypeScript generics. The first generic is the return type, the second is the argument type, and the third configures the ThunkAPI. It automatically dispatches pending, fulfilled, and rejected actions with full type safety. We handle these in the extraReducers section of our slice."

### Q: How do you ensure type safety across the Redux store?

"I create a RootState type from the store, use custom typed hooks (useAppDispatch and useAppSelector), define interfaces for all state shapes, and use PayloadAction types in reducers. This ensures type checking from dispatch to selector throughout the entire data flow."
---
---

4. useRef and useState
---
---

# useRef vs useState - React Native Interview Script (TypeScript)

## Opening Statement

"Both useState and useRef are React hooks that allow us to persist values between renders, but they serve different purposes and have different behaviors. The key difference is that useState causes re-renders when updated, while useRef does not. Let me explain both with practical React Native examples."

---

## 1. useState - For Reactive UI State

### What to say:

"useState is used when we need to store state that, when changed, should trigger a re-render of the component. It's perfect for any data that affects what the user sees on screen - like form inputs, toggles, counters, or any UI state."

### Key Characteristics:

- ✅ Triggers re-render when updated
- ✅ Used for UI state that needs to be displayed
- ✅ Asynchronous updates (batched)
- ✅ Returns current value and setter function

### Example:

```typescript
// CounterScreen.tsx
import React, { useState } from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';

const CounterScreen: React.FC = () => {
  const [count, setCount] = useState<number>(0);

  const increment = () => {
    setCount(count + 1); // Triggers re-render
    console.log('Count after setState:', count); // Still shows old value!
  };

  console.log('Component rendered'); // This runs on every state change

  return (
    <View style={styles.container}>
      <Text style={styles.text}>Count: {count}</Text>
      <Button title="Increment" onPress={increment} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    fontSize: 32,
    marginBottom: 20,
  },
});

export default CounterScreen;
```

### Real-World React Native Example - Form Input:

```typescript
// LoginForm.tsx
import React, { useState } from 'react';
import { View, TextInput, Text, Button, StyleSheet } from 'react-native';

const LoginForm: React.FC = () => {
  const [email, setEmail] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});

  const handleSubmit = () => {
    const newErrors: { email?: string; password?: string } = {};
    
    if (!email) newErrors.email = 'Email is required';
    if (!password) newErrors.password = 'Password is required';
    
    setErrors(newErrors); // Updates UI with errors
    
    if (Object.keys(newErrors).length === 0) {
      console.log('Form submitted:', { email, password });
    }
  };

  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Email"
        value={email}
        onChangeText={setEmail} // Updates state on every keystroke
        autoCapitalize="none"
      />
      {errors.email && <Text style={styles.error}>{errors.email}</Text>}
      
      <TextInput
        style={styles.input}
        placeholder="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />
      {errors.password && <Text style={styles.error}>{errors.password}</Text>}
      
      <Button title="Login" onPress={handleSubmit} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 20,
  },
  input: {
    height: 50,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  error: {
    color: 'red',
    marginBottom: 16,
  },
});

export default LoginForm;
```

---

## 2. useRef - For Non-Reactive Values & Direct Access

### What to say:

"useRef is used to store mutable values that persist between renders but don't trigger re-renders when changed. It's perfect for accessing DOM/native elements, storing timers, keeping track of previous values, or storing any data that doesn't affect the UI directly."

### Key Characteristics:

- ✅ Does NOT trigger re-render when updated
- ✅ Mutable - can change `.current` directly
- ✅ Synchronous updates (immediate)
- ✅ Returns a ref object with `.current` property
- ✅ Perfect for accessing native components

### Example:

```typescript
// TimerScreen.tsx
import React, { useState, useRef } from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';

const TimerScreen: React.FC = () => {
  const [seconds, setSeconds] = useState<number>(0);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const startTimer = () => {
    if (intervalRef.current) return; // Already running
    
    intervalRef.current = setInterval(() => {
      setSeconds(prev => prev + 1);
    }, 1000);
    
    console.log('Timer started'); // Component doesn't re-render from this
  };

  const stopTimer = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
      console.log('Timer stopped'); // Component doesn't re-render from this
    }
  };

  const resetTimer = () => {
    stopTimer();
    setSeconds(0); // This DOES trigger re-render
  };

  return (
    <View style={styles.container}>
      <Text style={styles.text}>Seconds: {seconds}</Text>
      <View style={styles.buttonContainer}>
        <Button title="Start" onPress={startTimer} />
        <Button title="Stop" onPress={stopTimer} />
        <Button title="Reset" onPress={resetTimer} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    fontSize: 48,
    marginBottom: 20,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 10,
  },
});

export default TimerScreen;
```

### Real-World React Native Example - TextInput Focus:

```typescript
// FocusExample.tsx
import React, { useRef } from 'react';
import { View, TextInput, Button, StyleSheet } from 'react-native';

const FocusExample: React.FC = () => {
  const emailInputRef = useRef<TextInput>(null);
  const passwordInputRef = useRef<TextInput>(null);

  const focusEmail = () => {
    emailInputRef.current?.focus(); // Direct access to native component
  };

  const focusPassword = () => {
    passwordInputRef.current?.focus();
  };

  const handleEmailSubmit = () => {
    passwordInputRef.current?.focus(); // Auto-focus next field
  };

  return (
    <View style={styles.container}>
      <TextInput
        ref={emailInputRef}
        style={styles.input}
        placeholder="Email"
        returnKeyType="next"
        onSubmitEditing={handleEmailSubmit}
      />
      
      <TextInput
        ref={passwordInputRef}
        style={styles.input}
        placeholder="Password"
        secureTextEntry
        returnKeyType="done"
      />
      
      <View style={styles.buttonContainer}>
        <Button title="Focus Email" onPress={focusEmail} />
        <Button title="Focus Password" onPress={focusPassword} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 20,
  },
  input: {
    height: 50,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 16,
    marginBottom: 16,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 10,
  },
});

export default FocusExample;
```

---

## 3. Key Differences - Side by Side Comparison

### What to say:

"Let me highlight the key differences between useState and useRef:"

| Feature | useState | useRef |
|---------|----------|--------|
| **Re-renders** | ✅ Triggers re-render | ❌ Does NOT trigger re-render |
| **Update timing** | Asynchronous (batched) | Synchronous (immediate) |
| **Usage** | UI state that affects render | Non-UI data, timers, refs to elements |
| **Mutability** | Immutable (create new state) | Mutable (change .current directly) |
| **Access** | `const [value, setValue] = useState()` | `ref.current` |
| **When to use** | Form inputs, toggles, counters, visible data | Timers, previous values, element refs, non-visual data |

### Example Showing Both:

```typescript
// ComparisonExample.tsx
import React, { useState, useRef } from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';

const ComparisonExample: React.FC = () => {
  const [stateCount, setStateCount] = useState<number>(0);
  const refCount = useRef<number>(0);
  const renderCount = useRef<number>(0);

  // Increment render count on every render
  renderCount.current += 1;

  const incrementState = () => {
    setStateCount(stateCount + 1); // Triggers re-render
    console.log('State count:', stateCount); // Shows old value
  };

  const incrementRef = () => {
    refCount.current += 1; // Does NOT trigger re-render
    console.log('Ref count:', refCount.current); // Shows new value immediately
  };

  return (
    <View style={styles.container}>
      <Text style={styles.text}>Renders: {renderCount.current}</Text>
      <Text style={styles.text}>State Count: {stateCount}</Text>
      <Text style={styles.text}>Ref Count: {refCount.current}</Text>
      
      <Button title="Increment State" onPress={incrementState} />
      <Button title="Increment Ref" onPress={incrementRef} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 10,
  },
  text: {
    fontSize: 18,
  },
});

export default ComparisonExample;
```

---

## 4. Common Use Cases in React Native

### useState Use Cases:

```typescript
// Use Case 1: Toggle visibility
const [isVisible, setIsVisible] = useState<boolean>(false);

// Use Case 2: Loading states
const [isLoading, setIsLoading] = useState<boolean>(false);

// Use Case 3: Form data
const [formData, setFormData] = useState({ name: '', email: '' });

// Use Case 4: Selected items
const [selectedId, setSelectedId] = useState<string | null>(null);

// Use Case 5: Modal state
const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
```

### useRef Use Cases:

```typescript
// Use Case 1: Component references
const scrollViewRef = useRef<ScrollView>(null);
const flatListRef = useRef<FlatList>(null);

// Use Case 2: Timers and intervals
const timeoutRef = useRef<NodeJS.Timeout | null>(null);
const intervalRef = useRef<NodeJS.Timeout | null>(null);

// Use Case 3: Previous values
const prevValueRef = useRef<number>(0);

// Use Case 4: Animation values (with Animated API)
const animatedValue = useRef(new Animated.Value(0)).current;

// Use Case 5: WebSocket or subscription references
const wsRef = useRef<WebSocket | null>(null);
```

---

## 5. Advanced Example - Combining Both

### What to say:

"In real-world React Native apps, we often use both hooks together. Here's a practical example of a search feature with debouncing."

### Example:

```typescript
// SearchScreen.tsx
import React, { useState, useRef, useEffect } from 'react';
import { View, TextInput, Text, FlatList, StyleSheet, ActivityIndicator } from 'react-native';

interface SearchResult {
  id: string;
  title: string;
}

const SearchScreen: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState<string>(''); // UI state
  const [results, setResults] = useState<SearchResult[]>([]); // UI state
  const [isLoading, setIsLoading] = useState<boolean>(false); // UI state
  
  const debounceTimer = useRef<NodeJS.Timeout | null>(null); // Non-UI state
  const searchCount = useRef<number>(0); // Track API calls without re-render

  useEffect(() => {
    // Clear previous timer
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }

    // Don't search if query is empty
    if (!searchQuery.trim()) {
      setResults([]);
      return;
    }

    // Set loading state (causes re-render)
    setIsLoading(true);

    // Debounce the search
    debounceTimer.current = setTimeout(() => {
      performSearch(searchQuery);
    }, 500);

    // Cleanup
    return () => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    };
  }, [searchQuery]);

  const performSearch = async (query: string) => {
    searchCount.current += 1; // Increment without re-render
    const currentSearch = searchCount.current;
    
    console.log(`Search #${currentSearch}: ${query}`);

    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Only update if this is still the latest search
      if (currentSearch === searchCount.current) {
        const mockResults: SearchResult[] = [
          { id: '1', title: `Result for "${query}" 1` },
          { id: '2', title: `Result for "${query}" 2` },
          { id: '3', title: `Result for "${query}" 3` },
        ];
        setResults(mockResults);
        setIsLoading(false);
      }
    } catch (error) {
      console.error('Search error:', error);
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Search..."
        value={searchQuery}
        onChangeText={setSearchQuery}
        autoCapitalize="none"
      />

      {isLoading ? (
        <ActivityIndicator size="large" style={styles.loader} />
      ) : (
        <FlatList
          data={results}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <View style={styles.resultItem}>
              <Text>{item.title}</Text>
            </View>
          )}
          ListEmptyComponent={
            searchQuery ? (
              <Text style={styles.emptyText}>No results found</Text>
            ) : null
          }
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  input: {
    height: 50,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 16,
    marginBottom: 16,
  },
  loader: {
    marginTop: 20,
  },
  resultItem: {
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  emptyText: {
    textAlign: 'center',
    marginTop: 20,
    color: '#999',
  },
});

export default SearchScreen;
```

---

## 6. Advanced Example - Scroll to Item

### Example:

```typescript
// ScrollToItemExample.tsx
import React, { useRef, useState } from 'react';
import { 
  View, 
  FlatList, 
  Text, 
  Button, 
  StyleSheet,
  TextInput 
} from 'react-native';

interface Item {
  id: string;
  title: string;
}

const ScrollToItemExample: React.FC = () => {
  const flatListRef = useRef<FlatList<Item>>(null);
  const [itemIndex, setItemIndex] = useState<string>('');
  
  const data: Item[] = Array.from({ length: 100 }, (_, i) => ({
    id: `${i}`,
    title: `Item ${i}`,
  }));

  const scrollToIndex = () => {
    const index = parseInt(itemIndex);
    if (!isNaN(index) && index >= 0 && index < data.length) {
      flatListRef.current?.scrollToIndex({
        index,
        animated: true,
      });
    }
  };

  const scrollToTop = () => {
    flatListRef.current?.scrollToOffset({
      offset: 0,
      animated: true,
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.controls}>
        <TextInput
          style={styles.input}
          placeholder="Enter index"
          value={itemIndex}
          onChangeText={setItemIndex}
          keyboardType="number-pad"
        />
        <Button title="Scroll to Index" onPress={scrollToIndex} />
        <Button title="Scroll to Top" onPress={scrollToTop} />
      </View>

      <FlatList
        ref={flatListRef}
        data={data}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={styles.item}>
            <Text style={styles.itemText}>{item.title}</Text>
          </View>
        )}
        getItemLayout={(data, index) => ({
          length: 60,
          offset: 60 * index,
          index,
        })}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  controls: {
    padding: 16,
    backgroundColor: '#f5f5f5',
    gap: 10,
  },
  input: {
    height: 40,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    backgroundColor: '#fff',
  },
  item: {
    height: 60,
    justifyContent: 'center',
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  itemText: {
    fontSize: 16,
  },
});

export default ScrollToItemExample;
```

---

## 7. Advanced Example - Tracking Previous Value

### Example:

```typescript
// PreviousValueExample.tsx
import React, { useState, useRef, useEffect } from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';

const usePrevious = <T,>(value: T): T | undefined => {
  const ref = useRef<T>();
  
  useEffect(() => {
    ref.current = value;
  }, [value]);
  
  return ref.current;
};

const PreviousValueExample: React.FC = () => {
  const [count, setCount] = useState<number>(0);
  const prevCount = usePrevious(count);

  return (
    <View style={styles.container}>
      <Text style={styles.text}>Current: {count}</Text>
      <Text style={styles.text}>Previous: {prevCount ?? 'N/A'}</Text>
      <Text style={styles.text}>
        Change: {prevCount !== undefined ? count - prevCount : 0}
      </Text>
      
      <View style={styles.buttonContainer}>
        <Button title="Increment" onPress={() => setCount(count + 1)} />
        <Button title="Decrement" onPress={() => setCount(count - 1)} />
        <Button title="Reset" onPress={() => setCount(0)} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  text: {
    fontSize: 24,
    marginBottom: 10,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 20,
  },
});

export default PreviousValueExample;
```

---

## 8. Performance Considerations

### What to say:

"Understanding when to use each hook is crucial for performance in React Native:"

### useState Performance Tips:

```typescript
// ❌ BAD - Causes unnecessary re-renders
const [user, setUser] = useState({ name: '', email: '', age: 0 });
setUser({ ...user, name: 'John' }); // Re-renders even if email/age unchanged

// ✅ GOOD - Only update what changed
const [userName, setUserName] = useState('');
const [userEmail, setUserEmail] = useState('');
const [userAge, setUserAge] = useState(0);

// ✅ ALSO GOOD - Use callback for complex state
setUser(prevUser => ({ ...prevUser, name: 'John' }));
```

### useRef Performance Tips:

```typescript
// ✅ GOOD - Store animation values
const fadeAnim = useRef(new Animated.Value(0)).current;

// ✅ GOOD - Store subscriptions
const subscription = useRef<Subscription | null>(null);

// ✅ GOOD - Prevent unnecessary API calls
const isMounted = useRef(true);

useEffect(() => {
  fetchData().then(data => {
    if (isMounted.current) {
      setData(data);
    }
  });

  return () => {
    isMounted.current = false;
  };
}, []);
```

---

## 9. Common Mistakes to Avoid

### What to say:

"Here are some common mistakes developers make with these hooks in React Native:"

### Mistake 1: Using useRef when useState is needed

```typescript
// ❌ BAD - UI won't update
const count = useRef(0);
const increment = () => {
  count.current += 1; // UI shows 0 forever
};

// ✅ GOOD - UI updates properly
const [count, setCount] = useState(0);
const increment = () => {
  setCount(count + 1);
};
```

### Mistake 2: Not cleaning up refs

```typescript
// ❌ BAD - Memory leak
const timer = useRef<NodeJS.Timeout>();
timer.current = setInterval(() => { /* ... */ }, 1000);

// ✅ GOOD - Proper cleanup
useEffect(() => {
  const timer = setInterval(() => { /* ... */ }, 1000);
  return () => clearInterval(timer);
}, []);
```

### Mistake 3: Expecting immediate state updates

```typescript
// ❌ BAD - Misunderstanding async nature
const [count, setCount] = useState(0);
setCount(count + 1);
console.log(count); // Still 0!

// ✅ GOOD - Use callback or useEffect
setCount(prev => {
  console.log('Previous:', prev);
  return prev + 1;
});
```

---

## Key Points to Emphasize

### What to say:

"To summarize the key differences:

**useState:**
- Use for any data that affects the UI
- Triggers re-renders on update
- Asynchronous updates
- Examples: form inputs, toggles, counters, loading states

**useRef:**
- Use for values that persist but don't affect UI
- Does NOT trigger re-renders
- Synchronous updates
- Examples: timers, DOM/native element refs, previous values, subscriptions

**In React Native specifically:**
- useRef is perfect for accessing native components (TextInput, ScrollView, FlatList)
- useState is essential for all visible UI state
- Combine both for optimal performance and functionality
- Always clean up refs in useEffect cleanup function"

---

## Common Follow-up Questions

### Q: When would you use useRef instead of useState?

"I use useRef when I need to store a value that persists between renders but doesn't need to trigger a re-render when it changes. Common examples in React Native include storing timer IDs, keeping references to native components like TextInput for focus control, tracking previous values, or storing WebSocket connections. If changing the value should update the UI, I use useState instead."

### Q: Can you explain the .current property of useRef?

"The .current property is a mutable container that holds the actual value. Unlike useState, we can directly mutate .current without triggering a re-render. It's initialized with the value we pass to useRef, and we can read or write to it synchronously at any time. Think of it as a box that holds a value that persists across renders."

### Q: How do you access native components in React Native?

"I use useRef to create a reference to the component, type it properly with TypeScript, and then attach it using the ref prop. For example, `const inputRef = useRef<TextInput>(null)` and `<TextInput ref={inputRef} />`. Then I can call native methods like `inputRef.current?.focus()` or `inputRef.current?.blur()` to control the component imperatively."

### Q: Why doesn't changing ref.current trigger a re-render?

"By design, useRef is meant for values that don't affect rendering. React doesn't track changes to ref.current because it's intentionally kept outside the reactive system. This is actually a feature - it allows us to store values efficiently without causing unnecessary re-renders, which is important for performance in React Native apps."


---
---

4. display the Name of the user and fetch new users on click of a button, whose fetch logic should be in a custom hook. It was from the famous API: `https://randomuser.me/api/.  He asked me to update the list in such a way that users are appended to the beginning of the list and not the end as shown in the above code. It was easy fix. Just update the `updateData` function of the `useData` custom hook, to have the `newData.results[0]` be at the beginning of the `newArray`. Basically update the value of newArray as `const newArray = [newData.results[0], ...data]`. And that was it for the randomuser API.
5. Next he asked me to create a simple `TextInput` component and a button. On clicking that button, the focus should change to the `TextInput`. This was easy, I think he wanted to see if I knew about `useRef`, which I did, so it was a breeze.
6. implement a Countdown timer, which should stop at zero.
7. EventEmitter
8. `Event Capturing` and `Bubbling`
9. debugged for errors in Production , idea of debugging in Prod Env.
10. build a Password Strength Meter (in Codesandbox's React Native template), with `any 5 criteria` of my choice and that we could keep the option open for adding animations to it.
11. [https://www.hackerrank.com/challenges/missing-numbers/problem](https://www.hackerrank.com/challenges/missing-numbers/problem)
12. DSA (Stack & Queue Problem) - Valid Parathensis  
13. React Native Architecture - Turbo Modules & Fabric Architecture  
14. SetTimeOut o/p based question  
15. Count String words problem  
16. promise Api call to make parallel exceution of multiple API
17. Design pattern used in RN  
18. RN Architecture  
19. Timer Problem in react native  
20. Component for calling the API  
21. Native Modules (Bridge Concept)
22. Print the unique pairs (1,2), (3,4).
23. Build a list with an input box. As you type into the input box, the list should be filtered based on the input.
24. How can you optimize a FlatList?
25. How would you optimize the rendering of large images?
26. How can you determine if the phone is in portrait or landscape mode?
27. A question on deep linking in React Native.
28. **Shallow copy v/s Deep copy**
29. **Spread and Rest operator**
```
  ```javascript
console.log("Print 1");

setTimeout(() => {console.log("Print 2")}, 10);

console.log("Print 3");

setTimeout(() => {console.log("Print 4")}, 0);
```
 
35. **Hooks**, and which of them I had used till now. I replied with **useEffect**, **useState**, **useCallback** and **useMemo**.He asked the usual question of how to make **useEffect** equivalent to class based components' lifecycle methods. It is about the **dependency array** being empty, and so on
36.  **useCallback** vs **useMemo**
37. ```javascript
const sum = useMemo(() => a + b, [a, b])
```

He asked me what will happen when `a` and `b` are interchanged. Like if `a = 5`, `b = 10` in the current render, what will happen when `a = 10`, `b = 5`. Basically, he was testing if I knew the comparison done on each render. I was not very sure, so I said it will not recalculate as the sum is the same (I know it is really naive! But I didn't know the answer then), and that react will memoize whenever the result is the same (I told this because I had read about caching being done, when values don't change. Which `'values'` ? That I was not sure!).
38. https://leetcode.com/problems/furthest-building-you-can-reach/description/
39. - Rate Limiter: Create a function `rateLimiter` that takes two arguments: `func` (the function that makes an API request) and `limit` (the maximum number of allowed calls per minute).
40. CSV to JSON: Write a function that parses this CSV string and converts it into a hierarchical JSON structure, where each node contains its child nodes. Nodes with a parentId of null are root nodes, and nodes should be nested recursively within their parents in a children array.
41. This was a high-level design round aimed at assessing my system design skills.  I was asked to design a page consisting of winners from that days contest. The list will update at the end of each contest.  The interviewer asked how I would build this from scratch. Discussion was focused heavily on:modularising the components used on the page, the states and props
- Performance (infinite scrolling vs pagination)
- API structuring (Discussion around contract)
- Caching (How & Where)
- scalability of the components
38. `system scalability, trade-offs during design, handling technical debt, monitoring strategies and on-call`. He also asked about my day-to-day work, my role, and my responsibilities. Towards the end, we talked about Dream11's tech stack, ongoing projects, and work culture.
39. *-Tell me a situation where you went above and beyond to complete a task.*
- *What are your strengths and weeknesses according to your current team-mates.*
- *Tell me a situation where you failed.*
40. Promise.all
41. Promise.any
42. 1. LLD - How to design Autocomplete Search Bar
43. 1. Execute all async tasks parallely
44. 1. MultiThreading Questions
45. question 1: Sorted Array is given to you. In the array exactly 1 number appears more than 25% of the times. find the number  
solution: Gave O(n) solution. he wanted the O(log n). I tried a lot talked about lot of things around 25% & Binary search but didn't get the optimised solution. He asked me to write & run this solution only & gave an option to jump back once done with other question.

46. question 2: LinkedList is given to you. you have to reverse a specific section of the list. you'll get the start and end position for that section  
solution: took time (got confused while moving the references) but solved it. (it was expected to do in 1 pass and i did it)

47. Question: dream11 team selection screen (more specifically validation System that will power the screen).  
Details:

- user will have certain credits
- he can select total 'X' number of players.
- can select max/min players in a specific category (ex: keeper, forward)
- can select max 'Y' players from a squad (i.e. Team)


48.Question 1 - [https://leetcode.com/problems/shortest-unsorted-continuous-subarray/description/](https://leetcode.com/problems/shortest-unsorted-continuous-subarray/description/)

Question 2 - [https://leetcode.com/problems/longest-mountain-in-array/description/](https://leetcode.com/problems/longest-mountain-in-array/description/)

48. write the polyfill for the inbuilt functions like bind, call, apply() and compose().