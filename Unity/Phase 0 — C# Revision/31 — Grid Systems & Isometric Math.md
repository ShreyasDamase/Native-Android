# 31 — Grid Systems & Isometric Math
### 🔶 Block G — Game Algorithms & DSA

> [!NOTE]
> If you want to build **Clash of Clans**, **Stardew Valley**, or **Civilization**, you need a Grid System. You cannot rely on Unity's raw `transform.position` because buildings must snap to exact tiles, and you must mathematically prove if a tile is "occupied" before letting the player place a building.

---

## 31.1 — The Core Concept: The 2D Array

The foundation of any grid game is a 2D Array (or 2D List). 
The visual GameObjects in Unity are just a *representation* of this underlying math.

```csharp
public class GridSystem
{
    private int width;
    private int height;
    private float cellSize;
    
    // The actual grid data. 0 = empty, 1 = occupied (or use a custom class/struct)
    private int[,] gridArray; 

    public GridSystem(int width, int height, float cellSize)
    {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        
        gridArray = new int[width, height];
    }
}
```

---

## 31.2 — Converting World Space to Grid Space (And Back)

When a player taps the screen to place a Town Hall, you get a world position `(x: 14.2, y: 7.8)`. You must convert this to a grid index `(x: 14, y: 7)` to check if it's empty.

```csharp
// Convert World Position to Grid Index
public void GetXY(Vector3 worldPosition, out int x, out int y)
{
    x = Mathf.FloorToInt(worldPosition.x / cellSize);
    y = Mathf.FloorToInt(worldPosition.y / cellSize);
}

// Convert Grid Index back to World Position (for snapping)
// Returns the bottom-left corner of the cell
public Vector3 GetWorldPosition(int x, int y)
{
    return new Vector3(x, y) * cellSize;
}

// Snap any arbitrary position to the grid center
public Vector3 GetSnappedPosition(Vector3 worldPosition)
{
    GetXY(worldPosition, out int x, out int y);
    // Add half a cell size so the building sits perfectly in the center
    return GetWorldPosition(x, y) + new Vector3(cellSize, cellSize) * 0.5f;
}
```

---

## 31.3 — Placing a Building (Bounds Checking)

Before a player can place a 3x3 Town Hall, you must check if *all 9 tiles* are empty.

```csharp
public bool CanPlaceBuilding(Vector3 mousePos, int buildingWidth, int buildingHeight)
{
    GetXY(mousePos, out int startX, out int startY);
    
    // 1. Check if the building goes out of bounds
    if (startX < 0 || startY < 0 || 
        startX + buildingWidth > width || 
        startY + buildingHeight > height)
    {
        return false;
    }
    
    // 2. Check if every cell is empty
    for (int x = 0; x < buildingWidth; x++)
    {
        for (int y = 0; y < buildingHeight; y++)
        {
            if (gridArray[startX + x, startY + y] != 0) // 0 means empty
            {
                return false; // Found an obstacle!
            }
        }
    }
    
    return true; // All clear!
}

public void PlaceBuilding(Vector3 mousePos, int buildingWidth, int buildingHeight)
{
    if (CanPlaceBuilding(mousePos, buildingWidth, buildingHeight))
    {
        GetXY(mousePos, out int startX, out int startY);
        
        // Mark cells as occupied (e.g. ID = 1)
        for (int x = 0; x < buildingWidth; x++)
        {
            for (int y = 0; y < buildingHeight; y++)
            {
                gridArray[startX + x, startY + y] = 1; 
            }
        }
        
        // Instantiate the visual Unity GameObject at the snapped position
        Vector3 spawnPos = GetWorldPosition(startX, startY);
        Instantiate(townHallPrefab, spawnPos, Quaternion.identity);
    }
}
```

---

## 31.4 — Isometric Grids (The Clash of Clans Look)

Clash of Clans uses an **Isometric Projection**. The grid is rotated by 45 degrees, and squashed vertically by half. This creates a fake 3D look using 2D sprites.

To do this, you just modify the `GetWorldPosition` and `GetXY` math. The underlying `int[,]` array remains perfectly square!

```csharp
// ISOMETRIC MATH MAGIC
// Standard grid is (1,1). Isometric transforms this to look like a diamond.

public Vector3 GetIsometricWorldPosition(int x, int y)
{
    // A standard isometric projection formula
    float xPos = (x - y) * (cellSize * 0.5f);
    float yPos = (x + y) * (cellSize * 0.25f); // Squashed by 50%
    
    return new Vector3(xPos, yPos);
}

public void GetIsometricXY(Vector3 worldPosition, out int x, out int y)
{
    // Reverse the formula above to turn a screen tap into a grid index
    float halfWidth = cellSize * 0.5f;
    float quarterHeight = cellSize * 0.25f;
    
    float exactX = (worldPosition.x / halfWidth + worldPosition.y / quarterHeight) / 2f;
    float exactY = (worldPosition.y / quarterHeight - (worldPosition.x / halfWidth)) / 2f;
    
    x = Mathf.RoundToInt(exactX);
    y = Mathf.RoundToInt(exactY);
}
```

> [!TIP]
> In Unity, you can actually skip writing the Isometric math yourself by using the built-in **Isometric Tilemap** feature (Create -> 2D -> Tilemap -> Isometric Z as Y). However, if you are building Clash of Clans, writing a custom pure-C# Grid class (like above) is highly recommended for performance and A* pathfinding.

---

## 31.5 — Upgrading to a Generic Grid `<T>`

An `int[,]` array is fine for simple obstacles, but in a real game, a cell needs to hold more data (Who owns it? Is it on fire? What's the pathfinding weight?).

Instead of an `int`, we use a generic `Grid<TGridObject>`.

```csharp
public class GridNode
{
    public int x;
    public int y;
    public bool isWalkable = true;
    public Building currentBuilding = null;
    
    public GridNode(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
}

// Now our grid can hold full objects
public class Grid<T>
{
    private T[,] gridArray;
    
    // We pass a Func (delegate) so the grid knows how to construct the objects
    public Grid(int width, int height, float cellSize, Func<int, int, T> createGridObject)
    {
        gridArray = new T[width, height];
        
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                gridArray[x, y] = createGridObject(x, y);
            }
        }
    }
    
    public T GetGridObject(int x, int y) { return gridArray[x, y]; }
}

// Initialization:
Grid<GridNode> myGrid = new Grid<GridNode>(100, 100, 1f, (x, y) => new GridNode(x, y));
```

This generic Grid class is the ultimate foundation. We will use this exact `GridNode` in the next chapter for A* Pathfinding!

**Next:** [[32 — Pathfinding (A-Star & Flow Fields)]]
