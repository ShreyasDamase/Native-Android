# 🧠 AI & Pathfinding in Unity 2D
### A* Algorithm, Steering Behaviors, Behavior Trees & NavMesh2D

Building enemies and NPCs that feel intelligent requires understanding both the pathfinding algorithms and the decision frameworks that drive them. This guide focuses on the mathematical logic and practical Unity implementation.

---

## 1. A* Pathfinding Algorithm

A* is the industry-standard shortest-path algorithm. It combines Dijkstra's breadth-first search (guaranteed shortest path) with a heuristic (a "guess" about which direction to try first) to find paths efficiently.

### Core Formula

$$f(n) = g(n) + h(n)$$

- `g(n)`: The actual cost of the path from the start node to node `n`
- `h(n)`: The heuristic estimate of the cost from `n` to the goal
- `f(n)`: The total estimated cost through node `n`

The algorithm always processes the node with the **lowest f(n)** first, using a priority queue (min-heap).

### Heuristics for 2D Grids

```csharp
// Manhattan Distance: Best for 4-directional grid movement (no diagonals)
// Underestimates actual cost — admissible heuristic — guarantees shortest path
float ManhattanDistance(Vector2Int a, Vector2Int b)
    => Mathf.Abs(a.x - b.x) + Mathf.Abs(a.y - b.y);

// Chebyshev Distance: Best for 8-directional grid movement (allows diagonals)
float ChebyshevDistance(Vector2Int a, Vector2Int b)
    => Mathf.Max(Mathf.Abs(a.x - b.x), Mathf.Abs(a.y - b.y));

// Euclidean Distance: Accurate for any-angle movement but can overestimate on grids
float EuclideanDistance(Vector2Int a, Vector2Int b)
    => Vector2Int.Distance(a, b);
```

### Full A* Implementation for a Tile Grid

```csharp
using System.Collections.Generic;
using UnityEngine;

public class AStarPathfinder
{
    private readonly int width, height;
    private readonly bool[,] walkableMap; // true = walkable, false = blocked

    public AStarPathfinder(bool[,] map)
    {
        walkableMap = map;
        width = map.GetLength(0);
        height = map.GetLength(1);
    }

    public List<Vector2Int> FindPath(Vector2Int start, Vector2Int goal)
    {
        // Min-heap using SortedDictionary: maps f-cost to list of nodes
        var openSet = new SortedList<float, Vector2Int>(new DuplicateKeyComparer());
        var cameFrom = new Dictionary<Vector2Int, Vector2Int>();
        var gCost = new Dictionary<Vector2Int, float>();
        var closed = new HashSet<Vector2Int>();

        gCost[start] = 0f;
        openSet.Add(Heuristic(start, goal), start);

        while (openSet.Count > 0)
        {
            // Get node with lowest f-cost
            var current = openSet.Values[0];
            openSet.RemoveAt(0);

            if (current == goal) return ReconstructPath(cameFrom, goal);

            closed.Add(current);

            // Check all 8 neighbors
            foreach (var neighbor in GetNeighbors(current))
            {
                if (closed.Contains(neighbor)) continue;

                // Cost to move to neighbor (diagonal moves cost slightly more)
                float moveCost = (neighbor.x != current.x && neighbor.y != current.y)
                    ? 1.414f : 1f; // √2 for diagonal

                float tentativeG = gCost[current] + moveCost;

                if (!gCost.ContainsKey(neighbor) || tentativeG < gCost[neighbor])
                {
                    gCost[neighbor] = tentativeG;
                    float fCost = tentativeG + Heuristic(neighbor, goal);
                    cameFrom[neighbor] = current;
                    openSet.Add(fCost, neighbor);
                }
            }
        }

        return null; // No path found
    }

    private float Heuristic(Vector2Int a, Vector2Int b)
        => Mathf.Abs(a.x - b.x) + Mathf.Abs(a.y - b.y); // Manhattan

    private List<Vector2Int> ReconstructPath(Dictionary<Vector2Int, Vector2Int> cameFrom, Vector2Int goal)
    {
        var path = new List<Vector2Int>();
        var current = goal;
        while (cameFrom.ContainsKey(current))
        {
            path.Add(current);
            current = cameFrom[current];
        }
        path.Reverse();
        return path;
    }

    private IEnumerable<Vector2Int> GetNeighbors(Vector2Int node)
    {
        int[] dx = { 1, -1, 0, 0, 1, -1, 1, -1 };
        int[] dy = { 0, 0, 1, -1, 1, -1, -1, 1 };

        for (int i = 0; i < 8; i++)
        {
            var n = new Vector2Int(node.x + dx[i], node.y + dy[i]);
            if (n.x >= 0 && n.x < width && n.y >= 0 && n.y < height && walkableMap[n.x, n.y])
                yield return n;
        }
    }
}

// Helper: SortedList needs unique keys
public class DuplicateKeyComparer : IComparer<float>
{
    public int Compare(float x, float y) => x <= y ? -1 : 1;
}
```

### Path Smoothing (Remove Redundant Waypoints)

```csharp
// String-pull algorithm: Remove waypoints that have direct line-of-sight to each other
public static List<Vector2> SmoothPath(List<Vector2> path, LayerMask obstacleLayer)
{
    if (path == null || path.Count <= 2) return path;

    var smoothed = new List<Vector2> { path[0] };
    int currentIndex = 0;

    while (currentIndex < path.Count - 1)
    {
        // Try to reach as far ahead as possible without hitting an obstacle
        int farthest = currentIndex + 1;
        for (int i = path.Count - 1; i > currentIndex + 1; i--)
        {
            Vector2 direction = path[i] - path[currentIndex];
            float distance = direction.magnitude;

            if (!Physics2D.Raycast(path[currentIndex], direction.normalized, distance, obstacleLayer))
            {
                farthest = i;
                break;
            }
        }
        smoothed.Add(path[farthest]);
        currentIndex = farthest;
    }

    return smoothed;
}
```

---

## 2. Steering Behaviors (Craig Reynolds)

Steering behaviors are simple local forces applied each frame to generate emergent intelligent movement. They are cheaper than A* and ideal for flocking, orbiting, and avoidance.

```csharp
public class SteeringAgent : MonoBehaviour
{
    private Rigidbody2D rb;

    [SerializeField] private float maxSpeed = 5f;
    [SerializeField] private float maxSteeringForce = 8f;
    [SerializeField] private float mass = 1f;
    [SerializeField] private float slowingRadius = 3f; // For Arrival behavior

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    // ─── Seek: Steer toward a target at full speed ─────────────────────────────
    public Vector2 Seek(Vector2 targetPosition)
    {
        Vector2 desiredVelocity = (targetPosition - rb.position).normalized * maxSpeed;
        return SteeringForce(desiredVelocity);
    }

    // ─── Flee: Opposite of Seek — run away ─────────────────────────────────────
    public Vector2 Flee(Vector2 threatPosition, float fleeRadius = 5f)
    {
        if (Vector2.Distance(rb.position, threatPosition) > fleeRadius) return Vector2.zero;
        Vector2 desiredVelocity = (rb.position - threatPosition).normalized * maxSpeed;
        return SteeringForce(desiredVelocity);
    }

    // ─── Arrival: Slow down gracefully as you approach ─────────────────────────
    public Vector2 Arrive(Vector2 targetPosition)
    {
        Vector2 offset = targetPosition - rb.position;
        float distance = offset.magnitude;

        // Outside slowingRadius: full speed. Inside: scale down.
        float speed = distance > slowingRadius ? maxSpeed : maxSpeed * (distance / slowingRadius);
        Vector2 desiredVelocity = offset.normalized * speed;
        return SteeringForce(desiredVelocity);
    }

    // ─── Pursuit: Predict target's future position, then Seek it ───────────────
    public Vector2 Pursuit(Rigidbody2D prey)
    {
        float lookAheadTime = Vector2.Distance(rb.position, prey.position) / maxSpeed;
        Vector2 predictedPosition = prey.position + prey.linearVelocity * lookAheadTime;
        return Seek(predictedPosition);
    }

    // ─── Evade: Opposite of Pursuit ────────────────────────────────────────────
    public Vector2 Evade(Rigidbody2D pursuer)
    {
        float lookAheadTime = Vector2.Distance(rb.position, pursuer.position) / maxSpeed;
        Vector2 predictedThreat = pursuer.position + pursuer.linearVelocity * lookAheadTime;
        return Flee(predictedThreat);
    }

    // ─── Wander: Smooth random wandering ───────────────────────────────────────
    private float wanderAngle;
    public Vector2 Wander(float wanderRadius = 2f, float wanderDistance = 3f, float jitter = 0.5f)
    {
        wanderAngle += Random.Range(-jitter, jitter);
        Vector2 circleCenter = rb.linearVelocity.normalized * wanderDistance;
        Vector2 displacement = new Vector2(Mathf.Cos(wanderAngle), Mathf.Sin(wanderAngle)) * wanderRadius;
        return Seek(rb.position + circleCenter + displacement);
    }

    // ─── Separation: Push away from nearby agents (anti-clumping) ──────────────
    public Vector2 Separation(Collider2D[] neighbors, float desiredSeparation = 2f)
    {
        Vector2 steer = Vector2.zero;
        int count = 0;

        foreach (var n in neighbors)
        {
            if (n == null || n.gameObject == gameObject) continue;
            float d = Vector2.Distance(rb.position, n.attachedRigidbody.position);
            if (d < desiredSeparation && d > 0.001f)
            {
                // Push away, stronger when closer (inverse proportion)
                steer += (rb.position - (Vector2)n.transform.position).normalized / d;
                count++;
            }
        }

        if (count > 0) steer /= count;
        return steer.sqrMagnitude > 0 ? SteeringForce(steer.normalized * maxSpeed) : Vector2.zero;
    }

    // ─── Shared utility: Clamp steering force to physical maximum ──────────────
    private Vector2 SteeringForce(Vector2 desiredVelocity)
    {
        Vector2 steering = desiredVelocity - rb.linearVelocity;
        steering = Vector2.ClampMagnitude(steering, maxSteeringForce);
        return steering / mass;
    }

    // ─── Apply combined forces in FixedUpdate ──────────────────────────────────
    public void ApplySteering(Vector2 totalForce)
    {
        rb.AddForce(totalForce, ForceMode2D.Force);
        rb.linearVelocity = Vector2.ClampMagnitude(rb.linearVelocity, maxSpeed);
    }
}
```

---

## 3. Flocking (Boids Algorithm)

Three rules create emergent group behavior: **Separation** (avoid crowding), **Alignment** (match neighbors' velocity), **Cohesion** (move toward group center).

```csharp
public class Boid : MonoBehaviour
{
    private Rigidbody2D rb;
    [SerializeField] private float perceptionRadius = 3f;
    [SerializeField] private float maxSpeed = 4f;
    [SerializeField] private float maxForce = 6f;

    [SerializeField] private float separationWeight = 1.8f;
    [SerializeField] private float alignmentWeight = 1.0f;
    [SerializeField] private float cohesionWeight = 1.0f;

    private void Awake() => rb = GetComponent<Rigidbody2D>();

    private void FixedUpdate()
    {
        Collider2D[] neighbors = Physics2D.OverlapCircleAll(transform.position, perceptionRadius);

        Vector2 separation = Vector2.zero, alignment = Vector2.zero, cohesion = Vector2.zero;
        int count = 0;

        foreach (var neighbor in neighbors)
        {
            if (neighbor.gameObject == gameObject) continue;
            var nrb = neighbor.attachedRigidbody;
            if (!nrb) continue;

            float d = Vector2.Distance(rb.position, nrb.position);

            // Separation: push apart
            separation += (rb.position - nrb.position).normalized / Mathf.Max(d, 0.001f);
            // Alignment: match velocity direction
            alignment += nrb.linearVelocity;
            // Cohesion: move toward center of mass
            cohesion += nrb.position;
            count++;
        }

        if (count > 0)
        {
            alignment  = Steer(alignment / count);
            cohesion   = Steer((cohesion / count) - rb.position);
            separation = Steer(separation / count);
        }

        Vector2 totalForce = separation * separationWeight
                           + alignment  * alignmentWeight
                           + cohesion   * cohesionWeight;

        rb.AddForce(totalForce);
        rb.linearVelocity = Vector2.ClampMagnitude(rb.linearVelocity, maxSpeed);

        // Orient sprite toward velocity direction
        if (rb.linearVelocity.sqrMagnitude > 0.01f)
        {
            float angle = Mathf.Atan2(rb.linearVelocity.y, rb.linearVelocity.x) * Mathf.Rad2Deg - 90f;
            transform.rotation = Quaternion.Euler(0, 0, angle);
        }
    }

    private Vector2 Steer(Vector2 desired)
    {
        if (desired.sqrMagnitude < 0.001f) return Vector2.zero;
        return Vector2.ClampMagnitude(desired.normalized * maxSpeed - rb.linearVelocity, maxForce);
    }
}
```

---

## 4. Behavior Trees

A Behavior Tree (BT) is a decision structure that hierarchically composes simple **leaf behaviors** (actions, conditions) into complex, readable AI logic.

### Node Types

```
Composite nodes (control flow):
  Sequence:  ✓✓✓ → Succeeds only if ALL children succeed (logical AND)
  Selector:  ✓✗✗ → Succeeds if ANY child succeeds (logical OR)
  Parallel:  Runs all children simultaneously

Decorator nodes (modify):
  Inverter:  Flips child result (NOT gate)
  Repeater:  Runs child N times or until failure

Leaf nodes (actual work):
  Action:    Execute a behavior (move, attack, play animation)
  Condition: Check a state (isPlayerVisible, hasAmmo)
```

### Minimal Behavior Tree Implementation

```csharp
// Result states for each node evaluation
public enum BTStatus { Success, Failure, Running }

// Base class for all nodes
public abstract class BTNode
{
    public abstract BTStatus Evaluate();
}

// ─── Composite: Sequence (AND) ────────────────────────────────────────────────
public class Sequence : BTNode
{
    private readonly List<BTNode> children;
    public Sequence(List<BTNode> children) => this.children = children;

    public override BTStatus Evaluate()
    {
        foreach (var child in children)
        {
            var status = child.Evaluate();
            if (status != BTStatus.Success) return status; // Fail or Running → stop
        }
        return BTStatus.Success;
    }
}

// ─── Composite: Selector (OR) ─────────────────────────────────────────────────
public class Selector : BTNode
{
    private readonly List<BTNode> children;
    public Selector(List<BTNode> children) => this.children = children;

    public override BTStatus Evaluate()
    {
        foreach (var child in children)
        {
            var status = child.Evaluate();
            if (status != BTStatus.Failure) return status; // Success or Running → stop
        }
        return BTStatus.Failure;
    }
}

// ─── Decorator: Inverter ──────────────────────────────────────────────────────
public class Inverter : BTNode
{
    private readonly BTNode child;
    public Inverter(BTNode child) => this.child = child;

    public override BTStatus Evaluate() => child.Evaluate() switch
    {
        BTStatus.Success => BTStatus.Failure,
        BTStatus.Failure => BTStatus.Success,
        _ => BTStatus.Running
    };
}

// ─── Leaf: Condition ──────────────────────────────────────────────────────────
public class ConditionNode : BTNode
{
    private readonly System.Func<bool> condition;
    public ConditionNode(System.Func<bool> condition) => this.condition = condition;
    public override BTStatus Evaluate() => condition() ? BTStatus.Success : BTStatus.Failure;
}

// ─── Leaf: Action ─────────────────────────────────────────────────────────────
public class ActionNode : BTNode
{
    private readonly System.Func<BTStatus> action;
    public ActionNode(System.Func<BTStatus> action) => this.action = action;
    public override BTStatus Evaluate() => action();
}

// ─── Full Enemy AI Using Behavior Tree ───────────────────────────────────────
public class EnemyAI : MonoBehaviour
{
    [SerializeField] private Transform player;
    [SerializeField] private float detectionRange = 8f;
    [SerializeField] private float attackRange = 1.5f;
    [SerializeField] private float moveSpeed = 3f;

    private BTNode behaviorTree;
    private Rigidbody2D rb;

    private void Awake()
    {
        rb = GetComponent<Rigidbody2D>();

        // Build tree: Attack if in range, else Chase if detected, else Patrol
        behaviorTree = new Selector(new List<BTNode>
        {
            // Branch 1: Attack
            new Sequence(new List<BTNode>
            {
                new ConditionNode(() => Vector2.Distance(transform.position, player.position) < attackRange),
                new ActionNode(Attack)
            }),
            // Branch 2: Chase
            new Sequence(new List<BTNode>
            {
                new ConditionNode(() => Vector2.Distance(transform.position, player.position) < detectionRange),
                new ActionNode(Chase)
            }),
            // Branch 3: Patrol (fallback)
            new ActionNode(Patrol)
        });
    }

    private void Update() => behaviorTree.Evaluate();

    private BTStatus Attack()
    {
        Debug.Log("Attacking!");
        rb.linearVelocity = Vector2.zero;
        return BTStatus.Success;
    }

    private BTStatus Chase()
    {
        Vector2 dir = ((Vector2)player.position - rb.position).normalized;
        rb.linearVelocity = dir * moveSpeed;
        return BTStatus.Running;
    }

    private BTStatus Patrol()
    {
        // Simple back-and-forth movement as fallback
        rb.linearVelocity = new Vector2(Mathf.Sin(Time.time) * moveSpeed, 0);
        return BTStatus.Running;
    }
}
```

---

## 5. Line of Sight & Awareness

```csharp
public class EnemySight : MonoBehaviour
{
    [SerializeField] private Transform player;
    [SerializeField] private float sightRange = 10f;
    [SerializeField] private float fieldOfView = 120f; // Total FOV angle in degrees
    [SerializeField] private LayerMask obstacleMask;

    // Returns true if the player is within FOV and not blocked by obstacles
    public bool CanSeePlayer()
    {
        Vector2 toPlayer = (Vector2)player.position - (Vector2)transform.position;
        float distance = toPlayer.magnitude;

        if (distance > sightRange) return false;

        // Check angle between enemy's forward and direction to player
        float angle = Vector2.Angle(transform.up, toPlayer);
        if (angle > fieldOfView * 0.5f) return false;

        // Raycast to check for obstacles between enemy and player
        RaycastHit2D hit = Physics2D.Raycast(transform.position, toPlayer.normalized, distance, obstacleMask);
        return hit.collider == null; // No obstacle blocking
    }

    private void OnDrawGizmosSelected()
    {
        // Visualize FOV cone in Scene view
        Gizmos.color = Color.yellow;
        float halfAngle = fieldOfView * 0.5f;
        Vector3 leftBound = Quaternion.Euler(0, 0, halfAngle) * transform.up;
        Vector3 rightBound = Quaternion.Euler(0, 0, -halfAngle) * transform.up;
        Gizmos.DrawRay(transform.position, leftBound * sightRange);
        Gizmos.DrawRay(transform.position, rightBound * sightRange);
        Gizmos.DrawWireSphere(transform.position, sightRange);
    }
}
```
