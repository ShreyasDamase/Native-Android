# 🛠️ Editor Tooling & Custom Inspectors
### Custom Inspectors, Editor Windows, Property Drawers & Importers

Unity allows developers to extend the Editor layout itself, automating workflows, validating data, and providing designers with custom tools.

---

## 1. Editor Compilation Constraints

> [!WARNING]
> **Compilation Stripping Warning:** 
> Editor classes import the `UnityEditor` namespace. This namespace does **not** compile into player builds. If an editor-related script is included in a folder compiled for the final build, compilation will fail.
> *   **The Solution:** Place all editor extension scripts inside folders named exactly `Editor/` (e.g., `Assets/Scripts/Editor/EnemyEditor.cs`). You can have multiple `Editor/` folders across your project assets directory. Unity compiles these separately and strips them entirely during compilation.

---

## 2. Custom Inspectors (`CustomEditor`)

Custom Inspectors modify the visual presentation of component variables inside the Inspector.

```csharp
// 1. The Target MonoBehaviour Class (Assets/Scripts/Spawner.cs)
using UnityEngine;

public class Spawner : MonoBehaviour
{
    public float radius = 5f;
    public int spawnCount = 10;

    public void SpawnObjects()
    {
        for (int i = 0; i < spawnCount; i++)
        {
            Vector2 randomPoint = (Vector2)transform.position + Random.insideUnitCircle * radius;
            GameObject go = new GameObject("SpawnedUnit");
            go.transform.position = randomPoint;
        }
    }
}
```

```csharp
// 2. The Custom Editor Class (Assets/Scripts/Editor/SpawnerEditor.cs)
using UnityEngine;
using UnityEditor; // Required namespace

[CustomEditor(typeof(Spawner))]
public class SpawnerEditor : Editor
{
    public override void OnInspectorGUI()
    {
        // Draw standard fields configured on the target script
        DrawDefaultInspector();

        Spawner spawner = (Spawner)target;

        GUILayout.Space(10); // UI visual space spacing

        // Create a custom button inside the inspector
        if (GUILayout.Button("Test Spawning"))
        {
            // Register state with Undo manager to support Ctrl+Z reversion
            Undo.RecordObject(spawner, "Test Spawner Action");
            
            spawner.SpawnObjects();

            // Notify Unity the scene has changed so the save prompt triggers on exit
            EditorUtility.SetDirty(spawner);
        }
    }
}
```

---

## 3. Scene GUI & Interactive Handles

Instead of manually editing numeric variables inside text boxes, you can draw interactive lines, circles, and draggable nodes directly inside the Scene view using `Handles` inside an `OnSceneGUI()` method.

```csharp
// Editor script inside Assets/Scripts/Editor/SpawnerEditor.cs
private void OnSceneGUI()
{
    Spawner spawner = (Spawner)target;

    // 1. Draw a visual flat green circle representing the spawn radius in the Scene View
    Handles.color = Color.green;
    Handles.DrawWireArc(spawner.transform.position, Vector3.forward, Vector3.up, 360f, spawner.radius);

    // 2. Draw a draggable handle letting the designer adjust the radius inside the scene directly
    EditorGUI.BeginChangeCheck();
    
    // Draw scale slider handle pointing rightward
    float newRadius = Handles.ScaleValueHandle(
        spawner.radius,
        spawner.transform.position + Vector3.right * spawner.radius,
        Quaternion.identity,
        1.5f,
        Handles.ConeHandleCap,
        1f
    );

    if (EditorGUI.EndChangeCheck())
    {
        Undo.RecordObject(spawner, "Adjusted Spawner Radius");
        spawner.radius = Mathf.Max(0.5f, newRadius); // Enforce minimum radius bound
        EditorUtility.SetDirty(spawner);
    }
}
```

---

## 4. Custom Property Drawers

A Property Drawer controls how custom serializable structures render inside default layouts:

```csharp
// 1. The Serializable Struct (Assets/Scripts/StatSheet.cs)
[System.Serializable]
public struct StatRange
{
    public float min;
    public float max;
}
```

```csharp
// 2. The Custom Drawer (Assets/Scripts/Editor/StatRangeDrawer.cs)
using UnityEditor;
using UnityEngine;

[CustomPropertyDrawer(typeof(StatRange))]
public class StatRangeDrawer : PropertyDrawer
{
    public override void OnGUI(Rect position, SerializedProperty property, GUIContent label)
    {
        // Start property rendering block (draws standard label field)
        EditorGUI.BeginProperty(position, label, property);

        // Draw property label
        position = EditorGUI.PrefixLabel(position, GUIUtility.GetControlID(FocusType.Passive), label);

        // Find sub properties
        SerializedProperty minProp = property.FindPropertyRelative("min");
        SerializedProperty maxProp = property.FindPropertyRelative("max");

        // Split inspector rect width in half
        float halfWidth = position.width / 2f - 5f;

        Rect minRect = new Rect(position.x, position.y, halfWidth, position.height);
        Rect maxRect = new Rect(position.x + halfWidth + 10f, position.y, halfWidth, position.height);

        // Render input text fields side by side without duplicate label text
        EditorGUI.PropertyField(minRect, minProp, GUIContent.none);
        EditorGUI.PropertyField(maxRect, maxProp, GUIContent.none);

        EditorGUI.EndProperty();
    }
}
```

---

## 5. Automated Asset Import Pipeline (`AssetPostprocessor`)

Avoid manually setting texture configurations every time you import files into your assets directory. You can automate sprite formats (such as turning off filters for retro pixel-art) using importer scripts.

```csharp
// Importer Script (Assets/Scripts/Editor/SpritePostprocessor.cs)
using UnityEditor;
using UnityEngine;

public class SpritePostprocessor : AssetPostprocessor
{
    // Automatically executes whenever a texture file is dragged into the Project assets
    private void OnPreprocessTexture()
    {
        TextureImporter importer = (TextureImporter)assetImporter;

        // Apply rules only to files stored in folders containing "PixelArt" in their path
        if (importer.assetPath.Contains("PixelArt"))
        {
            importer.textureType = TextureImporterType.Sprite;
            importer.spritePixelsPerUnit = 16;
            
            // Critical for pixel art sharpness (removes interpolation blurring)
            importer.filterMode = FilterMode.Point;
            importer.textureCompression = TextureImporterCompression.Uncompressed;
        }
    }
}
```
