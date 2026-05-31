# 22 — Operator Overloading
### 🔴 Block D — Modern C# (Unity 6 / C# 10+)

> [!NOTE]
> Operator overloading lets you define what `+`, `-`, `*`, `==`, `!=`, `<`, `>` mean for your own types. Unity uses it everywhere — `Vector3 + Vector3`, `Color * float`, `Vector3 * Time.deltaTime` are all overloaded operators. You'll rarely define your own, but you must understand them to read Unity's source code and understand its API behavior.

---

## 22.1 — How Unity Uses Operator Overloading

```csharp
// These work because Unity overloads operators on Vector3:
Vector3 a = new Vector3(1f, 0f, 0f);
Vector3 b = new Vector3(0f, 1f, 0f);

Vector3 sum = a + b;           // Vector3 operator +(Vector3 lhs, Vector3 rhs)
Vector3 diff = a - b;          // Vector3 operator -(Vector3 lhs, Vector3 rhs)
Vector3 scaled = a * 2f;       // Vector3 operator *(Vector3 a, float d)
Vector3 scaled2 = 2f * a;      // Vector3 operator *(float d, Vector3 a)
Vector3 divided = a / 2f;      // Vector3 operator /(Vector3 a, float d)
bool eq = a == b;              // Vector3 operator ==(Vector3 lhs, Vector3 rhs)

// Unity also overloads == on GameObject to allow == null to detect destroyed objects
GameObject obj = null;
if (obj == null) { }           // uses Unity's overloaded == operator
```

---

## 22.2 — Defining Your Own Operators

```csharp
// Custom 2D grid position struct
public struct GridPos
{
    public int X, Y;
    
    public GridPos(int x, int y) { X = x; Y = y; }
    
    // Addition: GridPos + GridPos
    public static GridPos operator +(GridPos a, GridPos b) => new GridPos(a.X + b.X, a.Y + b.Y);
    
    // Subtraction
    public static GridPos operator -(GridPos a, GridPos b) => new GridPos(a.X - b.X, a.Y - b.Y);
    
    // Multiplication by scalar
    public static GridPos operator *(GridPos a, int scale) => new GridPos(a.X * scale, a.Y * scale);
    public static GridPos operator *(int scale, GridPos a) => new GridPos(a.X * scale, a.Y * scale);
    
    // Equality (must implement both == and !=)
    public static bool operator ==(GridPos a, GridPos b) => a.X == b.X && a.Y == b.Y;
    public static bool operator !=(GridPos a, GridPos b) => !(a == b);
    
    // Comparison operators (for sorting)
    public static bool operator <(GridPos a, GridPos b)
    {
        if (a.Y != b.Y) return a.Y < b.Y;
        return a.X < b.X;
    }
    public static bool operator >(GridPos a, GridPos b) => b < a;
    
    // Implicit conversion from Vector2Int
    public static implicit operator GridPos(Vector2Int v) => new GridPos(v.x, v.y);
    
    // Implicit conversion to Vector2Int
    public static implicit operator Vector2Int(GridPos g) => new Vector2Int(g.X, g.Y);
    
    // Explicit conversion to Vector3 (explicit = require cast)
    public static explicit operator Vector3(GridPos g) => new Vector3(g.X, g.Y, 0f);
    
    // Override Equals and GetHashCode when overloading ==
    public override bool Equals(object obj) => obj is GridPos other && this == other;
    public override int GetHashCode() => System.HashCode.Combine(X, Y);
    
    public override string ToString() => $"({X}, {Y})";
}

// Usage
GridPos start = new GridPos(2, 3);
GridPos end = new GridPos(5, 7);
GridPos offset = new GridPos(1, 1);

GridPos path = end - start;             // (3, 4)
GridPos doubled = path * 2;            // (6, 8)
GridPos next = start + offset;         // (3, 4)

bool same = start == end;              // false
bool diff = start != end;              // true

// Implicit conversion works automatically
Vector2Int unityPos = start;           // no cast needed
GridPos fromUnity = new Vector2Int(3, 4); // implicit

// Explicit conversion requires cast
Vector3 worldPos = (Vector3)start;     // (2, 3, 0)
```

---

## 22.3 — Damage Modifier — Practical Operator Use

```csharp
// A struct representing a damage modifier
public readonly struct DamageModifier
{
    public readonly float Multiplier;
    public readonly float FlatBonus;
    
    public DamageModifier(float multiplier = 1f, float flatBonus = 0f)
    {
        Multiplier = multiplier;
        FlatBonus = flatBonus;
    }
    
    // Apply to a base damage value
    public float Apply(float baseDamage) => baseDamage * Multiplier + FlatBonus;
    
    // Stack two modifiers together
    public static DamageModifier operator +(DamageModifier a, DamageModifier b)
        => new DamageModifier(a.Multiplier * b.Multiplier, a.FlatBonus + b.FlatBonus);
    
    // Scale a modifier by a factor
    public static DamageModifier operator *(DamageModifier mod, float scale)
        => new DamageModifier(mod.Multiplier * scale, mod.FlatBonus * scale);
    
    public static bool operator ==(DamageModifier a, DamageModifier b)
        => Mathf.Approximately(a.Multiplier, b.Multiplier) && Mathf.Approximately(a.FlatBonus, b.FlatBonus);
    
    public static bool operator !=(DamageModifier a, DamageModifier b) => !(a == b);
    
    // Identity modifier — no effect
    public static readonly DamageModifier Identity = new DamageModifier(1f, 0f);
    
    public override bool Equals(object obj) => obj is DamageModifier d && this == d;
    public override int GetHashCode() => System.HashCode.Combine(Multiplier, FlatBonus);
    public override string ToString() => $"x{Multiplier} +{FlatBonus}";
}

// Usage in game code
public class AttackCalculator : MonoBehaviour
{
    public float CalculateFinalDamage(float baseDamage, DamageModifier[] modifiers)
    {
        DamageModifier combined = DamageModifier.Identity;
        foreach (DamageModifier mod in modifiers)
            combined += mod; // uses our overloaded + operator
        
        return combined.Apply(baseDamage);
    }
}

// Clean usage at call site
DamageModifier fireBuff = new DamageModifier(1.5f, 10f);   // 50% more + 10 flat
DamageModifier critBonus = new DamageModifier(2.0f, 0f);   // double damage
DamageModifier combined = fireBuff + critBonus;             // stacked
float damage = combined.Apply(100f);                        // 100 * 3.0 + 10 = 310f
```

---

## 22.4 — Implicit vs Explicit Conversions

```csharp
public struct Percentage
{
    private readonly float value; // 0f to 1f internally
    
    private Percentage(float normalized) { value = Mathf.Clamp01(normalized); }
    
    // Implicit conversion FROM float (treat 0-100 as percentage)
    // Caller doesn't need to cast: Percentage p = 75f; → 0.75f internally
    public static implicit operator Percentage(float hundredBased)
        => new Percentage(hundredBased / 100f);
    
    // Implicit conversion TO float (returns 0-1)
    public static implicit operator float(Percentage p) => p.value;
    
    // Multiplication with float
    public static float operator *(float amount, Percentage p) => amount * p.value;
    public static float operator *(Percentage p, float amount) => p.value * amount;
    
    public override string ToString() => $"{value * 100f:F1}%";
}

// Extremely clean usage
Percentage critChance = 25f;           // implicit: 25f → 0.25f
Percentage armorReduction = 30f;       // 30f → 0.30f

float damage = 100f * (1f - armorReduction); // 100 * 0.70 = 70f — uses implicit operator
bool didCrit = Random.value < critChance;     // < 0.25f — uses implicit float conversion

Debug.Log(critChance); // "25.0%" — uses ToString()
```

---

## 22.5 — When to Overload (and When Not To)

```csharp
// ✅ GOOD use cases:
// - Mathematical types (Vector, Matrix, Complex, Quaternion)
// - Value/measurement types (Distance, Angle, Percentage, Duration)
// - Game-specific value types (DamageModifier, StatMultiplier, GridPos)

// ❌ BAD use cases:
// - Classes with identity (Enemy, Player, GameManager)
// - Types where + doesn't have an obvious mathematical meaning
// - Anything where overloading would be surprising or confusing

// Unity's own Vector3 is the gold standard for when to overload
// It makes this line natural:
Vector3 movement = transform.forward * speed * Time.deltaTime;
// Without overloading: Vector3.Scale(transform.forward, new Vector3(speed, speed, speed) * Time.deltaTime)
```

---

## 📝 Summary

| Operator | Syntax | Common Use Case |
| :--- | :--- | :--- |
| `+` | `operator +(A a, A b)` | Combine vectors, stack modifiers |
| `-` | `operator -(A a, A b)` | Difference between positions |
| `*` | `operator *(A a, float f)` | Scale by scalar |
| `/` | `operator /(A a, float f)` | Divide by scalar |
| `==` / `!=` | `operator ==(A a, A b)` | Value equality (structs) |
| `<` / `>` | `operator <(A a, A b)` | Comparison/sorting |
| `implicit` | `implicit operator T(A a)` | Auto-convert, obvious and safe |
| `explicit` | `explicit operator T(A a)` | Convert, but requires cast |

> [!TIP]
> Always override `Equals()` and `GetHashCode()` when you overload `==`. The compiler will warn you if you forget — take the warning seriously.

**Previous:** [[21 — Static Classes & Utility Patterns]] | **Next:** [[23 — C# Patterns for 2D Unity]]
