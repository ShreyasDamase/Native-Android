# 33 — Spatial Partitioning (Quadtrees)
### 🔶 Block G — Game Algorithms & DSA

> [!IMPORTANT]
> If an archer spawns, it needs to find the *nearest* target. If you have 200 buildings and 100 troops on the map, and every troop loops through all 200 buildings every second to find the closest one... that's `100 * 200 = 20,000` distance checks per frame! Your game will freeze. You need **Spatial Partitioning**.

---

## 33.1 — The Problem: O(N²) Collision / Targeting

If you write this code, your game will lag when troop counts get high:

```csharp
// ❌ THE BEGINNER WAY: O(N) check per troop (Total O(N^2))
Building FindClosestBuilding()
{
    Building closest = null;
    float closestDist = float.MaxValue;
    
    // Looping through EVERY building in the entire game!
    foreach (Building b in GameManager.AllBuildings)
    {
        float dist = Vector3.Distance(transform.position, b.transform.position);
        if (dist < closestDist)
        {
            closestDist = dist;
            closest = b;
        }
    }
    return closest;
}
```

If the archer is in the top-left of the map, it is wasting CPU calculating the exact distance to a Gold Mine in the bottom-right corner. We need a way to say: **"Only give me buildings in my immediate vicinity."**

---

## 33.2 — Spatial Hashing (The Grid Solution)

The simplest form of spatial partitioning is the **Grid** we already built in File 31! 

Instead of a 1-to-1 building grid, we create "Chunks" (e.g., 10x10 tiles per chunk). When a building is placed, it registers itself to the Chunk it belongs to.

When a troop needs a target, it asks the Chunk it is currently standing in: "Do you have any buildings?" If no, it checks the adjacent chunks. It completely ignores chunks on the other side of the map!

```csharp
// 1. A Chunk holds a list of entities currently inside it
public class MapChunk
{
    public List<Building> buildingsInChunk = new List<Building>();
}

// 2. The Troop only checks its local chunk
Building FindClosestInChunk()
{
    // Find which chunk the troop is in
    int chunkX = Mathf.FloorToInt(transform.position.x / CHUNK_SIZE);
    int chunkY = Mathf.FloorToInt(transform.position.y / CHUNK_SIZE);
    
    MapChunk myChunk = grid.GetChunk(chunkX, chunkY);
    
    // Now we only loop through 5 buildings instead of 200!
    Building closest = null;
    float closestDist = float.MaxValue;
    
    foreach (Building b in myChunk.buildingsInChunk)
    {
        float dist = Vector3.Distance(transform.position, b.transform.position);
        if (dist < closestDist) { closestDist = dist; closest = b; }
    }
    
    return closest;
}
```
*Note: You actually need to check the 8 neighboring chunks too, in case the closest building is just across the chunk border!*

---

## 33.3 — The Quadtree (The Advanced Solution)

A **Quadtree** is a Tree data structure where each node has exactly four children. 
It recursively divides 2D space into four quadrants.

```text
 _______________________
|           |           |
|  TopLeft  | TopRight  |
|___________|___________|
|           |           |
| BottomLeft| BotRight  |
|___________|___________|
```

### How it works:
1. You start with one massive square covering the whole map.
2. If you place more than `X` objects (e.g., 4) in that square, it **splits** into 4 smaller squares.
3. As objects cluster together (like a dense town center), the Quadtree splits those specific areas into tiny, highly precise squares. Empty areas of the map remain as one giant square.

### Why is it better than a Grid?
If half your map is totally empty forest, a Grid still allocates memory for hundreds of empty chunks. A Quadtree leaves the empty forest as one giant node, saving massive amounts of memory and search time.

### A Basic Quadtree Implementation

```csharp
public class Quadtree
{
    private Rect boundary;          // The physical space this node covers
    private int capacity = 4;       // Max objects before splitting
    private List<Building> points;  // Objects in this node
    
    private Quadtree nw, ne, sw, se; // The 4 children nodes
    private bool isDivided = false;
    
    public Quadtree(Rect boundary)
    {
        this.boundary = boundary;
        points = new List<Building>();
    }
    
    public bool Insert(Building b)
    {
        if (!boundary.Contains(b.position)) return false; // Not my problem!
        
        if (points.Count < capacity)
        {
            points.Add(b);
            return true; // Fits here!
        }
        
        if (!isDivided) Subdivide();
        
        // Pass it down to the children
        if (nw.Insert(b)) return true;
        if (ne.Insert(b)) return true;
        if (sw.Insert(b)) return true;
        if (se.Insert(b)) return true;
        
        return false;
    }
    
    private void Subdivide()
    {
        float x = boundary.x; float y = boundary.y;
        float w = boundary.width / 2; float h = boundary.height / 2;
        
        ne = new Quadtree(new Rect(x + w, y + h, w, h));
        nw = new Quadtree(new Rect(x, y + h, w, h));
        se = new Quadtree(new Rect(x + w, y, w, h));
        sw = new Quadtree(new Rect(x, y, w, h));
        isDivided = true;
    }
    
    // The magic Query method: Give it a search area (like the Archer's sight range)
    // and it quickly returns only the objects inside that area!
    public List<Building> Query(Rect searchArea, List<Building> found)
    {
        if (!boundary.Overlaps(searchArea)) return found; // Skip entire quadrant!
        
        foreach (Building p in points)
        {
            if (searchArea.Contains(p.position)) found.Add(p);
        }
        
        if (isDivided)
        {
            nw.Query(searchArea, found);
            ne.Query(searchArea, found);
            sw.Query(searchArea, found);
            se.Query(searchArea, found);
        }
        
        return found;
    }
}
```

### How the Archer Uses the Quadtree:
```csharp
// 1. Create a search radius box around the archer (e.g., 5x5 area)
Rect visionBox = new Rect(transform.position.x - 2.5f, transform.position.y - 2.5f, 5f, 5f);

// 2. Query the Quadtree (Takes O(log n) time instead of O(N)!)
List<Building> nearbyBuildings = globalQuadtree.Query(visionBox, new List<Building>());

// 3. Now only loop through the 3 or 4 buildings returned to find the exact closest one.
```

---

## 33.4 — Physics2D.OverlapCircle (Unity's Built-In Solution)

Wait! Doesn't Unity have `Physics2D.OverlapCircle`? Yes!

Under the hood, Unity's physics engine (`Box2D` / `PhysX`) uses something similar to a Quadtree (a Dynamic Bounding Volume Tree, or BVH). 

If your game has a small number of troops (e.g. 50), just use Unity's physics queries!
```csharp
Collider2D[] hitColliders = Physics2D.OverlapCircleAll(transform.position, sightRadius, buildingLayer);
```

**When to write a custom Quadtree/Grid?**
- When you have **thousands** of units (like *Vampire Survivors* or a massive RTS). Unity's physics overhead (Rigidbodies, colliders) becomes too slow, and you need pure C# math on a background thread.
- When you are doing **Deterministic Multiplayer** (Clash of Clans). Unity's physics are NOT strictly deterministic across different devices. You must write custom math to ensure the replay on an iPhone calculates exactly the same as on an Android.

**Next:** [[34 — AI State Machines (FSM)]]
