# 17 — Object Pooling in C#
### 🔴 Block C — Memory Management & Performance

> [!IMPORTANT]
> Object pooling is **mandatory** for any object that spawns and dies frequently — bullets, particles, enemies, damage numbers, sound effects. Without pooling, every `Instantiate` allocates memory and every `Destroy` leaves garbage for the GC.

---

## 17.1 — Why Pool?

```
WITHOUT POOLING:
  Fire bullet → Instantiate() → heap allocation → GC eventually runs → STUTTER
  Bullet hits  → Destroy()    → leaves garbage  → GC runs more often → MORE STUTTER

WITH POOLING:
  Fire bullet → Get() from pool → reposition existing object → NO ALLOCATION
  Bullet hits → Return() to pool → deactivate  → NO GARBAGE
```

---

## 17.2 — Simple Manual Pool

```csharp
// The most readable pool — good for learning and small projects
public class SimplePool : MonoBehaviour
{
    [SerializeField] private GameObject prefab;
    [SerializeField] private int initialSize = 20;
    
    private Queue<GameObject> available = new Queue<GameObject>();
    
    void Awake()
    {
        // Pre-warm: create all objects at startup
        for (int i = 0; i < initialSize; i++)
        {
            GameObject obj = Instantiate(prefab, transform);
            obj.SetActive(false);
            available.Enqueue(obj);
        }
    }
    
    public GameObject Get(Vector3 position, Quaternion rotation)
    {
        if (available.Count == 0)
        {
            // Pool exhausted — create a new one (expand the pool)
            Debug.LogWarning($"Pool for {prefab.name} expanded! Consider increasing initialSize.");
            GameObject extra = Instantiate(prefab, transform);
            extra.SetActive(false);
            available.Enqueue(extra);
        }
        
        GameObject obj = available.Dequeue();
        obj.transform.SetPositionAndRotation(position, rotation);
        obj.SetActive(true);
        return obj;
    }
    
    public void Return(GameObject obj)
    {
        obj.SetActive(false);
        available.Enqueue(obj);
    }
}
```

---

## 17.3 — Generic Typed Pool (Production-Quality)

```csharp
// Generic pool — works with any MonoBehaviour + IPoolable
public interface IPoolable
{
    void OnGetFromPool();   // called when retrieved
    void OnReturnToPool();  // called when returned
}

public class TypedPool<T> where T : MonoBehaviour, IPoolable
{
    private readonly T prefab;
    private readonly Transform parent;
    private readonly Queue<T> available;
    private readonly List<T> all;  // track all objects for cleanup
    
    public int AvailableCount => available.Count;
    public int TotalCount => all.Count;
    
    public TypedPool(T prefab, Transform parent, int initialCapacity = 10)
    {
        this.prefab = prefab;
        this.parent = parent;
        available = new Queue<T>(initialCapacity);
        all = new List<T>(initialCapacity);
        
        Prewarm(initialCapacity);
    }
    
    void Prewarm(int count)
    {
        for (int i = 0; i < count; i++)
        {
            T instance = Object.Instantiate(prefab, parent);
            instance.gameObject.SetActive(false);
            available.Enqueue(instance);
            all.Add(instance);
        }
    }
    
    public T Get(Vector3 position, Quaternion rotation)
    {
        T item = available.Count > 0 ? available.Dequeue() : CreateNew();
        
        item.transform.SetPositionAndRotation(position, rotation);
        item.transform.SetParent(null); // detach from pool parent when active
        item.gameObject.SetActive(true);
        item.OnGetFromPool();
        
        return item;
    }
    
    public void Return(T item)
    {
        item.OnReturnToPool();
        item.gameObject.SetActive(false);
        item.transform.SetParent(parent); // re-parent to pool for organization
        available.Enqueue(item);
    }
    
    T CreateNew()
    {
        T instance = Object.Instantiate(prefab, parent);
        all.Add(instance);
        Debug.Log($"Pool<{typeof(T).Name}>: Expanded to {all.Count} objects");
        return instance;
    }
    
    // Cleanup all pool objects (call on scene change)
    public void DestroyAll()
    {
        foreach (T item in all)
            if (item != null) Object.Destroy(item.gameObject);
        available.Clear();
        all.Clear();
    }
}
```

---

## 17.4 — Bullet Pool — Complete Example

```csharp
// The pooled bullet
public class Bullet2D : MonoBehaviour, IPoolable
{
    [SerializeField] private float speed = 15f;
    [SerializeField] private float damage = 25f;
    [SerializeField] private float lifetime = 3f;
    
    private TypedPool<Bullet2D> pool;
    private Rigidbody2D rb;
    private Coroutine lifetimeRoutine;
    
    void Awake() => rb = GetComponent<Rigidbody2D>();
    
    // Called by the pool when handing this bullet out
    public void OnGetFromPool()
    {
        // Fire in the direction we're facing
        rb.linearVelocity = transform.up * speed;
        
        // Auto-return after lifetime
        lifetimeRoutine = StartCoroutine(ReturnAfterLifetime());
    }
    
    // Called by the pool when this bullet is returned
    public void OnReturnToPool()
    {
        rb.linearVelocity = Vector2.zero;
        
        if (lifetimeRoutine != null)
        {
            StopCoroutine(lifetimeRoutine);
            lifetimeRoutine = null;
        }
    }
    
    public void Initialize(TypedPool<Bullet2D> ownerPool) => pool = ownerPool;
    
    IEnumerator ReturnAfterLifetime()
    {
        yield return new WaitForSeconds(lifetime);
        ReturnToPool();
    }
    
    void OnTriggerEnter2D(Collider2D other)
    {
        if (other.TryGetComponent<IDamageable>(out IDamageable target))
        {
            target.TakeDamage(damage);
            SpawnHitEffect();
        }
        ReturnToPool();
    }
    
    void ReturnToPool()
    {
        if (gameObject.activeSelf) // avoid double-return
            pool.Return(this);
    }
    
    void SpawnHitEffect()
    {
        // Use particle pool or VFX manager
        VFXManager.Instance.PlayHitEffect(transform.position);
    }
}

// The gun that uses the pool
public class Gun2D : MonoBehaviour
{
    [SerializeField] private Bullet2D bulletPrefab;
    [SerializeField] private Transform muzzle;
    [SerializeField] private int poolSize = 30;
    [SerializeField] private float fireRate = 0.2f;
    
    private TypedPool<Bullet2D> bulletPool;
    private float nextFireTime;
    
    void Awake()
    {
        bulletPool = new TypedPool<Bullet2D>(bulletPrefab, transform, poolSize);
        
        // Give each bullet a reference to the pool so it can return itself
        // (Pool initializes them — we need to notify them)
    }
    
    void Update()
    {
        if (Input.GetButton("Fire1") && CanFire())
            Fire();
    }
    
    bool CanFire() => Time.time >= nextFireTime;
    
    void Fire()
    {
        nextFireTime = Time.time + fireRate;
        
        Bullet2D bullet = bulletPool.Get(muzzle.position, muzzle.rotation);
        bullet.Initialize(bulletPool); // tell bullet how to return itself
    }
}
```

---

## 17.5 — Unity's Built-In `ObjectPool<T>` (Unity 2021+)

Unity ships with a high-quality generic pool in `UnityEngine.Pool`.

```csharp
using UnityEngine.Pool;

public class ParticlePool : MonoBehaviour
{
    [SerializeField] private ParticleSystem particlePrefab;
    
    private ObjectPool<ParticleSystem> pool;
    
    void Awake()
    {
        pool = new ObjectPool<ParticleSystem>(
            createFunc: CreateParticle,        // how to create a new instance
            actionOnGet: OnGetParticle,        // called when retrieved from pool
            actionOnRelease: OnReturnParticle, // called when returned to pool
            actionOnDestroy: DestroyParticle,  // called if pool is full and object is destroyed
            collectionCheck: true,             // check for double-returns in Editor (disable in builds)
            defaultCapacity: 20,              // initial pool size
            maxSize: 100                       // pool won't grow beyond this
        );
    }
    
    ParticleSystem CreateParticle()
    {
        ParticleSystem ps = Instantiate(particlePrefab, transform);
        ps.gameObject.SetActive(false);
        return ps;
    }
    
    void OnGetParticle(ParticleSystem ps)
    {
        ps.gameObject.SetActive(true);
        ps.Play();
    }
    
    void OnReturnParticle(ParticleSystem ps)
    {
        ps.Stop();
        ps.gameObject.SetActive(false);
    }
    
    void DestroyParticle(ParticleSystem ps)
    {
        Destroy(ps.gameObject);
    }
    
    // Play an effect at a position and auto-return when done
    public void Play(Vector3 position)
    {
        ParticleSystem ps = pool.Get();
        ps.transform.position = position;
        
        StartCoroutine(ReturnWhenDone(ps));
    }
    
    IEnumerator ReturnWhenDone(ParticleSystem ps)
    {
        yield return new WaitUntil(() => !ps.IsAlive());
        pool.Release(ps);
    }
}
```

---

## 17.6 — Multi-Pool Manager (Central Registry)

```csharp
// Central manager that holds pools for different prefab types
public class PoolManager : MonoBehaviour
{
    public static PoolManager Instance { get; private set; }
    
    [System.Serializable]
    public class PoolConfig
    {
        public string Tag;
        public GameObject Prefab;
        public int InitialSize = 10;
    }
    
    [SerializeField] private PoolConfig[] poolConfigs;
    
    private Dictionary<string, Queue<GameObject>> pools = new Dictionary<string, Queue<GameObject>>();
    
    void Awake()
    {
        Instance = this;
        
        foreach (PoolConfig config in poolConfigs)
        {
            Queue<GameObject> queue = new Queue<GameObject>(config.InitialSize);
            
            for (int i = 0; i < config.InitialSize; i++)
            {
                GameObject obj = Instantiate(config.Prefab, transform);
                obj.SetActive(false);
                queue.Enqueue(obj);
            }
            
            pools[config.Tag] = queue;
        }
    }
    
    public GameObject Get(string tag, Vector3 position, Quaternion rotation)
    {
        if (!pools.ContainsKey(tag))
        {
            Debug.LogError($"No pool found for tag: {tag}");
            return null;
        }
        
        Queue<GameObject> queue = pools[tag];
        GameObject obj;
        
        if (queue.Count == 0)
        {
            Debug.LogWarning($"Pool '{tag}' is empty — consider increasing size");
            obj = Instantiate(prefabLookup[tag], transform);
        }
        else
        {
            obj = queue.Dequeue();
        }
        
        obj.transform.SetPositionAndRotation(position, rotation);
        obj.SetActive(true);
        return obj;
    }
    
    public void Return(string tag, GameObject obj)
    {
        obj.SetActive(false);
        obj.transform.SetParent(transform);
        pools[tag].Enqueue(obj);
    }
}

// Usage in any script
public class GunWithManager : MonoBehaviour
{
    [SerializeField] private Transform muzzle;
    
    void Fire()
    {
        GameObject bullet = PoolManager.Instance.Get("Bullet", muzzle.position, muzzle.rotation);
        // bullet moves itself and calls Return("Bullet", gameObject) when done
    }
}
```

---

## 17.7 — 🎮 2D vs 3D: Pool Differences

### 🎮 2D — Projectile & Particle Pool
```csharp
// 2D bullet pool — uses Rigidbody2D for velocity
public class BulletPool2D : MonoBehaviour
{
    private Queue<Rigidbody2D> pool = new Queue<Rigidbody2D>();
    
    public Rigidbody2D Get(Vector2 position, Vector2 velocity)
    {
        Rigidbody2D rb = pool.Count > 0 ? pool.Dequeue() : CreateNew();
        rb.transform.position = position;
        rb.linearVelocity = velocity;
        rb.gameObject.SetActive(true);
        return rb;
    }
    
    public void Return(Rigidbody2D rb)
    {
        rb.linearVelocity = Vector2.zero;
        rb.gameObject.SetActive(false);
        pool.Enqueue(rb);
    }
}
```

### 🎮 3D — Decal & Shell Casing Pool
```csharp
// 3D specific: shell casing ejection pool
public class ShellPool3D : MonoBehaviour
{
    [SerializeField] private Rigidbody shellPrefab;
    
    private Queue<Rigidbody> pool = new Queue<Rigidbody>();
    
    public Rigidbody EjectShell(Vector3 position, Vector3 ejectionForce, Vector3 torque)
    {
        Rigidbody shell = pool.Count > 0 ? pool.Dequeue() : CreateNew();
        
        shell.transform.position = position;
        shell.transform.rotation = Random.rotation;
        shell.gameObject.SetActive(true);
        
        // Reset physics state (important in 3D!)
        shell.linearVelocity = Vector3.zero;
        shell.angularVelocity = Vector3.zero;
        
        shell.AddForce(ejectionForce, ForceMode.Impulse);
        shell.AddTorque(torque, ForceMode.Impulse);
        
        // Return after 5 seconds
        StartCoroutine(ReturnAfterDelay(shell, 5f));
        return shell;
    }
    
    IEnumerator ReturnAfterDelay(Rigidbody shell, float delay)
    {
        yield return new WaitForSeconds(delay);
        shell.gameObject.SetActive(false);
        pool.Enqueue(shell);
    }
    
    Rigidbody CreateNew() => Instantiate(shellPrefab, transform);
}
```

---

## 📝 Summary

| Pattern | Use Case | Notes |
| :--- | :--- | :--- |
| Simple `Queue<T>` pool | Small, single-type pools | Easy to understand |
| Generic `TypedPool<T>` | Any MonoBehaviour + IPoolable | Reusable across projects |
| Unity `ObjectPool<T>` | Particles, effects | Built-in, well-tested |
| `PoolManager` singleton | Multiple prefab types | Central registry, string-tagged |

**Key Rules:**
- Pre-warm at startup (in `Awake`) — never during gameplay
- Always check if object is still active before returning (avoid double-return)
- Return from `OnTrigger/Collision` AND lifetime timer — use a guard check
- Pool particles too — not just bullets

**Previous:** [[16 — String Handling & StringBuilder]] | **Next:** [[18 — Structs for Performance]]
