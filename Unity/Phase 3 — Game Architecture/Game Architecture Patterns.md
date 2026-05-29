# 🏛️ Game Architecture Patterns
### State Machines, Singletons, Object Pools & Decoupling Events

Developing complex games requires decoupling separate systems (UI, movement, audio, state tracking), preventing memory garbage, and managing object lifecycles efficiently.

---

## 1. The State Pattern (Finite State Machine / FSM)

Using nested `if/else` checks for movement states (e.g. isGrounded, isJumping, isDashing) becomes hard to maintain. A class-based State Pattern encapsulates behaviors into separate classes.

```
       ┌────────────────────────┐
       │   PlayerStateMachine   │ ◄─── Manages and switches current state
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │      PlayerState       │ ◄─── Base abstract state definition
       └─────┬────────────┬─────┘
             │            │
             ▼            ▼
     [ PlayerIdle ]  [ PlayerJump ] ◄─── Concrete state classes
```

### The State Pattern Implementation

```csharp
using UnityEngine;

// 1. Base State interface
public interface IState
{
    void Enter();
    void Update();
    void FixedUpdate();
    void Exit();
}

// 2. Concrete State: Player Idle
public class PlayerIdleState : IState
{
    private readonly PlayerController player;
    private readonly PlayerStateMachine stateMachine;

    public PlayerIdleState(PlayerController player, PlayerStateMachine stateMachine)
    {
        this.player = player;
        this.stateMachine = stateMachine;
    }

    public void Enter() => Debug.Log("Entered Idle State");
    public void Exit() {}

    public void Update()
    {
        // Read movement input. Transition to Moving state if input exists.
        float horizontal = Input.GetAxisRaw("Horizontal");
        if (horizontal != 0)
        {
            stateMachine.ChangeState(player.MovingState);
        }
    }

    public void FixedUpdate()
    {
        // Apply friction to slow down player
        player.Rigidbody.linearVelocity = new Vector2(0f, player.Rigidbody.linearVelocity.y);
    }
}

// 3. Concrete State: Player Moving
public class PlayerMovingState : IState
{
    private readonly PlayerController player;
    private readonly PlayerStateMachine stateMachine;

    public PlayerMovingState(PlayerController player, PlayerStateMachine stateMachine)
    {
        this.player = player;
        this.stateMachine = stateMachine;
    }

    public void Enter() => Debug.Log("Entered Moving State");
    public void Exit() {}

    public void Update()
    {
        float horizontal = Input.GetAxisRaw("Horizontal");
        if (horizontal == 0)
        {
            stateMachine.ChangeState(player.IdleState);
        }
    }

    public void FixedUpdate()
    {
        float horizontal = Input.GetAxisRaw("Horizontal");
        player.Rigidbody.linearVelocity = new Vector2(horizontal * player.MoveSpeed, player.Rigidbody.linearVelocity.y);
    }
}

// 4. The State Machine Controller
public class PlayerStateMachine
{
    public IState CurrentState { get; private set; }

    public void Initialize(IState startingState)
    {
        CurrentState = startingState;
        CurrentState.Enter();
    }

    public void ChangeState(IState newState)
    {
        CurrentState.Exit();
        CurrentState = newState;
        CurrentState.Enter();
    }
}

// 5. The MonoBehaviour Link
[RequireComponent(typeof(Rigidbody2D))]
public class PlayerController : MonoBehaviour
{
    public Rigidbody2D Rigidbody { get; private set; }
    public float MoveSpeed = 5f;

    public PlayerStateMachine StateMachine { get; private set; }
    public PlayerIdleState IdleState { get; private set; }
    public PlayerMovingState MovingState { get; private set; }

    private void Awake()
    {
        Rigidbody = GetComponent<Rigidbody2D>();
        StateMachine = new PlayerStateMachine();

        IdleState = new PlayerIdleState(this, StateMachine);
        MovingState = new PlayerMovingState(this, StateMachine);
    }

    private void Start()
    {
        StateMachine.Initialize(IdleState);
    }

    private void Update() => StateMachine.CurrentState.Update();
    private void FixedUpdate() => StateMachine.CurrentState.FixedUpdate();
}
```

---

## 2. Reusable Generic Singletons

Instead of manually checking `Instance != null` in `Awake` for every manager class, inherit from these generic templates:

### A. Scene-Specific Generic Singleton
Destroyed automatically on scene transition. Great for local UI or Level managers.
```csharp
using UnityEngine;

public class Singleton<T> : MonoBehaviour where T : MonoBehaviour
{
    private static T instance;
    public static T Instance
    {
        get
        {
            if (instance == null)
            {
                instance = FindFirstObjectByType<T>();
                if (instance == null)
                {
                    GameObject go = new GameObject(typeof(T).Name);
                    instance = go.AddComponent<T>();
                }
            }
            return instance;
        }
    }

    protected virtual void Awake()
    {
        if (instance != null && instance != this)
        {
            Destroy(gameObject);
        }
        else
        {
            instance = this as T;
        }
    }
}
```

### B. Persistent Generic Singleton
Bypasses scene unloads. Ideal for globally accessible backend services.
```csharp
public class PersistentSingleton<T> : MonoBehaviour where T : MonoBehaviour
{
    private static T instance;
    public static T Instance => instance;

    protected virtual void Awake()
    {
        if (instance != null && instance != this)
        {
            Destroy(gameObject);
            return;
        }
        instance = this as T;
        DontDestroyOnLoad(gameObject);
    }
}
// Usage: public class SaveManager : PersistentSingleton<SaveManager> { ... }
```

---

## 3. Modern Built-In Object Pooling (`UnityEngine.Pool`)

Unity 2021+ provides a built-in, highly optimized generic collection pooling class under the `UnityEngine.Pool` namespace. This replaces legacy custom list collections.

```csharp
using UnityEngine;
using UnityEngine.Pool; // Required namespace

public class ModernBulletPool : MonoBehaviour
{
    [SerializeField] private GameObject bulletPrefab;
    [SerializeField] private int defaultPoolCapacity = 20;
    [SerializeField] private int maxPoolSize = 50;

    // Use Unity's built-in ObjectPool structure
    private IObjectPool<GameObject> pool;

    private void Awake()
    {
        // Initialize pool passing creation, activation, deactivation, and destruction methods
        pool = new ObjectPool<GameObject>(
            createFunc: CreateBullet,
            actionOnGet: OnTakeFromPool,
            actionOnRelease: OnReturnedToPool,
            actionOnDestroy: OnDestroyPoolObject,
            collectionCheck: true,
            defaultCapacity: defaultPoolCapacity,
            maxSize: maxPoolSize
        );
    }

    private GameObject CreateBullet()
    {
        // Instantiated objects are childed to the pool transform
        GameObject obj = Instantiate(bulletPrefab, transform);
        
        // Link the bullet script to this pool so it can return itself
        if (obj.TryGetComponent<PooledBullet>(out var bullet))
        {
            bullet.SetPool(pool);
        }
        return obj;
    }

    private void OnTakeFromPool(GameObject bullet)
    {
        bullet.SetActive(true); // Turn object on
    }

    private void OnReturnedToPool(GameObject bullet)
    {
        bullet.SetActive(false); // Hide object
    }

    private void OnDestroyPoolObject(GameObject bullet)
    {
        Destroy(bullet); // Cleanup if capacity is exceeded
    }

    // Call this from a shooting script
    public GameObject GetBullet() => pool.Get();
}
```

And configure the bullet object to release itself back to the pool:
```csharp
public class PooledBullet : MonoBehaviour
{
    private IObjectPool<GameObject> bulletPool;
    [SerializeField] private float lifetime = 3f;

    public void SetPool(IObjectPool<GameObject> pool) => bulletPool = pool;

    private void OnEnable()
    {
        // Return object back to pool after time expires
        Invoke(nameof(DeactivateBullet), lifetime);
    }

    private void OnDisable()
    {
        CancelInvoke();
    }

    private void DeactivateBullet()
    {
        // Release back to the pool system instead of calling Destroy()
        bulletPool.Release(gameObject);
    }
}
```

---

## 4. Observer Pattern (Events System)

Decouple publishers from subscribers using typed events:
```csharp
using System;

public static class EventBus
{
    // High-performance static actions
    public static event Action<int> OnScoreUpdated;
    
    public static void TriggerScoreUpdated(int amount)
    {
        OnScoreUpdated?.Invoke(amount);
    }
}
// UI class subscribes: EventBus.OnScoreUpdated += SetUIText;
// UI class unsubscribes: EventBus.OnScoreUpdated -= SetUIText;
```
