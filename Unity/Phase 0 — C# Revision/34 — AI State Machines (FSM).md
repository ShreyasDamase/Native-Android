# 34 — AI State Machines (FSM)
### 🔶 Block G — Game Algorithms & DSA

> [!NOTE]
> AI in strategy games isn't actually "intelligent." It's just a strict set of rules. A Barbarian is either: Searching, Walking, or Attacking. It cannot do two at once. To code this without creating a tangled mess of `if/else` statements, we use a **Finite State Machine (FSM)**.

---

## 34.1 — The Problem: The `Update` Spaghetti

When beginners write AI, it looks like this:

```csharp
// ❌ WRONG: Spaghetti AI
void Update()
{
    if (!hasTarget) {
        FindTarget();
    } else {
        if (DistanceToTarget() > attackRange) {
            MoveTowardsTarget();
        } else {
            if (canAttack) {
                Attack();
            }
        }
    }
}
```
This is a nightmare to debug. What happens if the troop is stunned? What if the target dies while we are walking? You end up with 50 boolean flags (`isStunned`, `isWalking`, `isAttacking`).

---

## 34.2 — The Finite State Machine (FSM)

An FSM breaks the AI into distinct **States**. An entity can only be in ONE state at a time.
Each State handles its own logic and decides when to transition to a new State.

### Step 1: Define the States (Using Enums)
The simplest FSM uses an `enum` and a `switch` statement in `Update`. This is perfect for simple enemies.

```csharp
public class SimpleBarbarianAI : MonoBehaviour
{
    private enum State { Searching, Moving, Attacking, Dead }
    private State currentState;
    private Building target;

    void Start()
    {
        currentState = State.Searching;
    }

    void Update()
    {
        switch (currentState)
        {
            case State.Searching:
                target = FindClosestBuilding();
                if (target != null) 
                    currentState = State.Moving;
                break;

            case State.Moving:
                if (target == null || target.IsDestroyed) 
                {
                    currentState = State.Searching; // Target died before we got there!
                    break;
                }
                
                MoveTowards(target.transform.position);
                
                if (Vector3.Distance(transform.position, target.transform.position) <= attackRange)
                {
                    currentState = State.Attacking;
                }
                break;

            case State.Attacking:
                if (target == null || target.IsDestroyed)
                {
                    currentState = State.Searching; // Building destroyed, find a new one!
                    break;
                }
                
                PerformAttackTimer();
                break;
                
            case State.Dead:
                // Do nothing, wait for destroy animation
                break;
        }
    }
}
```
*Why this is great:* If the Barbarian is `Attacking`, it completely ignores the `Searching` and `Moving` logic. The code is isolated.

---

## 34.3 — The Advanced FSM (State Classes)

If your states are complex (e.g. an Archer that kites enemies, flees, and uses abilities), the `switch` statement gets too big. We use the **State Pattern** (OOP).

### 1. The State Interface
```csharp
public interface IState
{
    void Enter();       // Called once when state begins
    void Execute();     // Called every frame
    void Exit();        // Called once when state ends
}
```

### 2. The Context (The AI Controller)
```csharp
public class AdvancedAI : MonoBehaviour
{
    private IState currentState;
    
    // Dependencies the states will need
    public NavMeshAgent agent;
    public Animator anim;
    public Transform target;
    
    public void ChangeState(IState newState)
    {
        currentState?.Exit();
        currentState = newState;
        currentState?.Enter();
    }
    
    void Update()
    {
        currentState?.Execute();
    }
}
```

### 3. The Concrete States
```csharp
public class MovingState : IState
{
    private AdvancedAI ai;
    
    public MovingState(AdvancedAI ai) { this.ai = ai; }
    
    public void Enter()
    {
        ai.anim.SetBool("IsRunning", true);
        ai.agent.SetDestination(ai.target.position);
    }
    
    public void Execute()
    {
        // Check transition condition
        if (Vector3.Distance(ai.transform.position, ai.target.position) < 2f)
        {
            ai.ChangeState(new AttackingState(ai));
        }
    }
    
    public void Exit()
    {
        ai.anim.SetBool("IsRunning", false);
        ai.agent.ResetPath(); // Stop moving
    }
}
```

In this advanced pattern, every state is its own C# class. You can easily add a `StunnedState` or `FleeState` without ever touching the `MovingState` code!

---

## 34.4 — Behavior Trees (The Industry Standard for AAA AI)

FSMs are great, but they scale poorly if you have 50 different states with complex transitions (e.g., Skyrim NPCs). 
For massive games, developers use **Behavior Trees**.

A Behavior Tree evaluates a tree of nodes every frame:
- **Selector Node:** Tries each child node until one succeeds.
- **Sequence Node:** Runs each child node in order until one fails.
- **Action Node:** Actually does something (Move, Attack).

*Example Clash of Clans Behavior Tree:*
```
Selector
 ├── Sequence (Am I attacking?)
 │    ├── IsTargetInRange?
 │    └── AttackTarget()
 ├── Sequence (Am I moving?)
 │    ├── HasTarget?
 │    └── MoveToTarget()
 └── Action (Find a target!)
      └── SearchForTarget()
```
The tree evaluates from top-to-bottom, left-to-right. 

> [!TIP]
> Do not write a custom Behavior Tree from scratch unless you want to study the algorithm. Use a visual node-editor asset like **NodeCanvas** or **Behavior Designer** from the Unity Asset Store. For indie games, an FSM (State Pattern) is almost always enough.

**Next:** [[35 — Offline Timers & Server Auth]]
