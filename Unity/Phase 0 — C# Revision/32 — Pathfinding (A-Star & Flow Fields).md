# 32 — Pathfinding (A-Star & Flow Fields)
### 🔶 Block G — Game Algorithms & DSA

> [!NOTE]
> If a Barbarian spawns on the edge of the map, how does it know how to walk around the walls to reach the Town Hall? It uses a Pathfinding Algorithm. This is graph traversal applied to a 2D Grid.

---

## 32.1 — A* (A-Star) Pathfinding Explained

A* is the industry standard for unit pathfinding. It calculates the shortest path from Point A to Point B by checking adjacent grid nodes.

It uses three numbers (costs) to decide which path to take:
1. **G Cost**: The walking distance from the Start Node to the current node.
2. **H Cost (Heuristic)**: The estimated distance from the current node to the End Node.
3. **F Cost**: `G + H`. We always explore the node with the lowest F cost next.

### Step 1: The PathNode
Every tile on your grid needs to hold A* data.

```csharp
public class PathNode
{
    public int x;
    public int y;
    
    public int gCost;
    public int hCost;
    public int fCost { get { return gCost + hCost; } }
    
    public bool isWalkable;
    public PathNode cameFromNode; // Used to trace the path backwards at the end
    
    public PathNode(int x, int y) { this.x = x; this.y = y; isWalkable = true; }
}
```

### Step 2: The Algorithm (Simplified)
```csharp
public List<PathNode> FindPath(int startX, int startY, int endX, int endY)
{
    PathNode startNode = grid.GetGridObject(startX, startY);
    PathNode endNode = grid.GetGridObject(endX, endY);
    
    // OPEN List: Nodes we need to evaluate
    List<PathNode> openList = new List<PathNode> { startNode };
    
    // CLOSED List: Nodes we have already evaluated
    List<PathNode> closedList = new List<PathNode>();
    
    // Initialize costs (infinite G cost for all, except start node)
    
    while (openList.Count > 0)
    {
        // 1. Get the node with the lowest F cost in the Open List
        PathNode currentNode = GetLowestFCostNode(openList);
        
        // 2. Did we reach the end?
        if (currentNode == endNode)
        {
            // Trace the path backwards using 'cameFromNode' and return it
            return CalculatePath(endNode); 
        }
        
        openList.Remove(currentNode);
        closedList.Add(currentNode);
        
        // 3. Check neighbors (Up, Down, Left, Right)
        foreach (PathNode neighbor in GetNeighborList(currentNode))
        {
            if (closedList.Contains(neighbor) || !neighbor.isWalkable) continue;
            
            // 4. Calculate tentative G cost for neighbor
            int tentativeGCost = currentNode.gCost + CalculateDistance(currentNode, neighbor);
            
            if (tentativeGCost < neighbor.gCost) // We found a faster route!
            {
                neighbor.cameFromNode = currentNode;
                neighbor.gCost = tentativeGCost;
                neighbor.hCost = CalculateDistance(neighbor, endNode);
                
                if (!openList.Contains(neighbor)) openList.Add(neighbor);
            }
        }
    }
    
    // Out of nodes, no path found (trapped inside walls!)
    return null; 
}
```

> [!WARNING]
> Running A* on a 100x100 grid requires thousands of calculations. If you have 50 Barbarians, running A* for all 50 in a single `Update()` frame **will drop your game to 5 FPS**. 
> 
> *Solutions:* 
> 1. Use Unity's C# Job System (multithreading) to run A* off the main thread.
> 2. Spread the calculations out (e.g., calculate 5 paths per frame).
> 3. Use **Flow Fields** (see below).

---

## 32.2 — Flow Fields (Massive Swarm Pathfinding)

If you have 500 troops all trying to reach the *same* Town Hall, running A* 500 times is terrible. They all want to go to the same place!

Instead, we use a **Flow Field** (also called a Vector Field).
1. We run an algorithm (like Dijkstra) starting from the Town Hall and flowing outward across the entire map, like water filling a maze.
2. Every tile on the map is given an arrow (a Vector2) pointing towards the neighbor that is closest to the Town Hall.
3. The 500 troops don't run any pathfinding at all. They just look at the tile they are standing on, and walk in the direction the arrow points.

```
Flow Field Example (Target is the X)

→ → ↓
→ → ↓
→ X ←
↑ ↑ ←

A troop standing in the top-left will naturally be carried to the X, just by following the arrows.
```

**Clash of Clans Example:**
In CoC, troops don't actually share a single target (some target gold, some target defenses). So Flow Fields are hard to use perfectly there. CoC uses highly optimized A* with target caching and line-of-sight optimizations. But for games like *Factorio* or *They Are Billions* (where thousands of zombies swarm the base), Flow Fields are mandatory.

---

## 32.3 — Following the Path (The Unit Script)

Once `FindPath()` returns a `List<PathNode>`, the troop must walk along it.

```csharp
public class UnitMovement : MonoBehaviour
{
    private List<Vector3> pathVectorList;
    private int currentPathIndex;
    public float moveSpeed = 3f;

    public void SetPath(List<PathNode> path)
    {
        // Convert PathNodes to World Vector3s
        pathVectorList = new List<Vector3>();
        foreach (PathNode node in path) {
            pathVectorList.Add(grid.GetWorldPosition(node.x, node.y));
        }
        currentPathIndex = 0;
    }

    void Update()
    {
        if (pathVectorList != null && currentPathIndex < pathVectorList.Count)
        {
            Vector3 targetPosition = pathVectorList[currentPathIndex];
            
            // Move towards target
            transform.position = Vector3.MoveTowards(
                transform.position, targetPosition, moveSpeed * Time.deltaTime);

            // Reached the current waypoint?
            if (Vector3.Distance(transform.position, targetPosition) < 0.1f)
            {
                currentPathIndex++; // Move to next node
            }
        }
    }
}
```

---

## 32.4 — Breaking Walls (CoC Specifics)

In Clash of Clans, walls aren't strictly "unwalkable". If a path to the Town Hall takes 100 tiles around a wall, but breaking a wall takes only 10 seconds, the troop will choose to break the wall!

To do this in A*, we don't set walls to `isWalkable = false`. We set them to have a very high **Movement Penalty**.

```csharp
// Inside A* Algorithm:
int tentativeGCost = currentNode.gCost + CalculateDistance(currentNode, neighbor) + neighbor.movementPenalty;

// Normal Tile penalty = 0
// Wall Tile penalty = 500 (It "costs" 500 units of distance to break the wall)
// The A* algorithm will naturally decide if walking 600 tiles around the wall is worse than paying the 500 penalty to break it!
```

**Next:** [[33 — Spatial Partitioning (Quadtrees)]]
