# 📦 ScriptableObjects
### Data Containers & Data-Driven Game Architecture

`ScriptableObjects` are data containers that exist as assets in your project files rather than components on GameObjects in a scene. They decouple data configurations from execution scripts, optimize memory, and enable modular architectures.

---

## 1. Core Benefits

*   **Memory Optimization**: Traditional MonoBehaviours store properties *locally*. If you spawn 200 goblin prefabs with stats configured on the script, each copy allocates memory for those stats on the heap. With `ScriptableObjects`, the stats exist once as an asset; all 200 instances reference that single asset in memory, reducing the memory footprint.
*   **Designer Workflow**: Level designers can customize item stats, dialogs, and balance values inside the inspector without modifying scripts.
*   **Version Control**: Data configurations are saved as separate `.asset` text blocks rather than updating `.unity` scene files, preventing merge conflicts.

---

## 2. Defining & Referencing ScriptableObjects

```csharp
using UnityEngine;

[CreateAssetMenu(fileName = "NewWeaponData", menuName = "Equipment/Weapon Data")]
public class WeaponData : ScriptableObject
{
    [Header("Visual Configs")]
    public string weaponName;
    public Sprite weaponIcon;

    [Header("Gameplay Variables")]
    public int damageAmount = 25;
    public float attackRate = 0.5f;
}
```

Reference this asset inside a MonoBehaviour component:
```csharp
public class PlayerCombat : MonoBehaviour
{
    // Drag your "WeaponData" asset into this slot in the Editor Inspector
    [SerializeField] private WeaponData weaponData; 
    
    private float nextFireTime;

    private void Update()
    {
        if (Input.GetButton("Fire1") && Time.time >= nextFireTime)
        {
            Attack();
            nextFireTime = Time.time + weaponData.attackRate;
        }
    }

    private void Attack()
    {
        Debug.Log($"Attacking with {weaponData.weaponName}. Dealt {weaponData.damageAmount} damage!");
    }
}
```

---

## 🏛️ Advanced Architecture Patterns (Ryan Hipple Patterns)

ScriptableObjects can act as an architectural backbone, decoupling systems cleanly.

### Pattern A: ScriptableObject Variables
Share state (e.g. Player Health) between separate scripts (e.g. Combat and UI) without using Singletons or hard references.

```csharp
using UnityEngine;

[CreateAssetMenu(fileName = "NewFloatVariable", menuName = "Variables/Float Variable")]
public class FloatVariable : ScriptableObject
{
    public float Value;
}
```
*   The Player script writes to it: `playerHealthSO.Value -= damage;`
*   The HealthBar UI script reads from it: `healthBarImage.fillAmount = playerHealthSO.Value / maxHealth;`

---

### Pattern B: ScriptableObject Event Channels (Observer Pattern)
A modular event system where publishers and subscribers are decoupled.

```csharp
using System;
using System.Collections.Generic;
using UnityEngine;

[CreateAssetMenu(fileName = "NewGameEvent", menuName = "Events/Game Event Channel")]
public class GameEventChannel : ScriptableObject
{
    private readonly List<Action> listeners = new List<Action>();

    public void Raise()
    {
        // Loop backward to allow listeners to unsubscribe during callback safely
        for (int i = listeners.Count - 1; i >= 0; i--)
        {
            listeners[i]?.Invoke();
        }
    }

    public void Subscribe(Action listener) => listeners.Add(listener);
    public void Unsubscribe(Action listener) => listeners.Remove(listener);
}
```
*   **Publisher** (e.g. Enemy): Raises the event when destroyed.
*   **Subscriber** (e.g. QuestManager or ScoreUI): Listens to the event to increment scores or unlock goals, with zero hard references between the Enemy class and the UI/Quest systems.

---

### Pattern C: Runtime Sets
Tracks active objects in a scene dynamically without requiring `FindObjectsOfType`.

```csharp
using System.Collections.Generic;
using UnityEngine;

[CreateAssetMenu(fileName = "NewRuntimeSet", menuName = "Architecture/Runtime Set")]
public class EnemyRuntimeSet : ScriptableObject
{
    public List<GameObject> Items = new List<GameObject>();

    public void Add(GameObject go) { if (!Items.Contains(go)) Items.Add(go); }
    public void Remove(GameObject go) { if (Items.Contains(go)) Items.Remove(go); }
}
```
*   Each Enemy adds itself to this asset in `OnEnable()`, and removes itself in `OnDisable()`.
*   Radar UI or GameManagers read this list directly to track remaining enemy counts.

---

## 3. Abstract Behavior Strategy Patterns

Instead of storing just data, ScriptableObjects can wrap executable algorithms.

```csharp
using UnityEngine;

// 1. Define base abstract Ability SO
public abstract class CharacterAbility : ScriptableObject
{
    public abstract void TriggerAbility(GameObject parent);
}

// 2. Concrete Strategy A: Dash Ability
[CreateAssetMenu(menuName = "Abilities/Dash")]
public class DashAbility : CharacterAbility
{
    public float dashForce = 20f;
    
    public override void TriggerAbility(GameObject parent)
    {
        if (parent.TryGetComponent<Rigidbody2D>(out var rb))
        {
            rb.AddForce(parent.transform.right * dashForce, ForceMode2D.Impulse);
        }
    }
}
```

---

## ⚠️ The Build Reset Pitfall

> [!WARNING]
> **Data Persistence Trap:** 
> Modifying ScriptableObject fields at runtime (`weaponData.damageAmount = 50;`) will persist inside the Unity Editor even after you exit Play Mode. 
> However, **in a compiled build, modified values are reset when the game exits.** 
> Use ScriptableObjects strictly for read-only default configurations. Runtime player changes (such as inventory contents or upgraded stats) must be saved via JSON serialization or PlayerPrefs.
