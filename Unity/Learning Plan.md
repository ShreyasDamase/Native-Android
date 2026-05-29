# 🎮 Unity 2D — Complete Learning Plan
### Hobbyist to Professional Developer Roadmap

Welcome to your structured Unity 2D and C# learning space. This plan is designed to take you from a hobbyist building a 2D physics game to a developer ready for a professional transition in 1 year.

---

## 🗺️ The Big Picture Roadmap

```
Phase 0: C# Revision          (2–3 weeks)   ← do this in parallel, not before
Phase 1: Math & Physics        (3–4 weeks)   ← foundation before Unity makes sense
Phase 2: Unity 2D Core         (6–8 weeks)   ← engine + systems
Phase 3: Game Architecture     (4–6 weeks)   ← clean production code structure
Phase 4: Professional Skills   (ongoing)     ← profiling, tools, memory, Git
Phase 5: Ship a Game           (ongoing)     ← everything clicks here
```

> [!NOTE]
> You don't need to finish Phase 0 before starting Phase 1. You run C# revision alongside everything else — check and close gaps in C# as you encounter them in Unity.

---

## 📂 Vault Directory Index

Use these links to navigate directly to each section of your learning vault:

### [[Phase 0 — C# Revision/C# Revision Guide|Phase 0 — C# Revision]]
*   Language fundamentals (Skip vs. Revise lists)
*   Memory management & Garbage Collection (GC) optimization
*   Resources for closing C# knowledge gaps

### [[Phase 1 — Math & Physics/Game Math|Phase 1 — Math & Physics]]
*   [[Phase 1 — Math & Physics/Game Math|Game Math]]: Vectors, Dot Product, Distance/Direction, Lerp, Angles & Rotations, and Cross Product.
*   [[Phase 1 — Math & Physics/2D Physics|2D Physics]]: Forces, velocity, mass/drag, torque, collisions vs. triggers, and `FixedUpdate` rules.

### [[Phase 2 — Unity 2D Core/MonoBehaviour & Component System|Phase 2 — Unity 2D Core]]
*   [[Phase 2 — Unity 2D Core/MonoBehaviour & Component System|MonoBehaviour & Component System]]: GameObjects, parent/child workflows, lifecycle methods, and querying.
*   [[Phase 2 — Unity 2D Core/Physics & Input|Physics & Input]]: Rigidbody2D and the new Input System.
*   [[Phase 2 — Unity 2D Core/Graphics & UI|Graphics & UI]]: Sprites, rendering layers, animations, Cinemachine, Canvas, Tilemaps, Rule Tiles, and VFX Particle Systems.
*   [[Phase 2 — Unity 2D Core/Prefabs & Scenes|Prefabs & Scenes]]: Spawning, scene management, multi-scene workflows, and Data Persistence.
*   [[Phase 2 — Unity 2D Core/ScriptableObjects|ScriptableObjects]]: Data-driven configs.

### [[Phase 3 — Game Architecture/Game Architecture Patterns|Phase 3 — Game Architecture & Tooling]]
*   [[Phase 3 — Game Architecture/Game Architecture Patterns|Game Architecture Patterns]]: State Machines, Singletons, Object Pooling, and Decoupled Events.
*   [[Phase 3 — Game Architecture/Editor Tooling & Custom Inspectors|Editor Tooling & Custom Inspectors]]: Custom editors, property drawers, and menus.
*   [[Phase 3 — Game Architecture/Performance Profiling & Optimization|Performance Profiling & Optimization]]: Unity Profiler, Frame Debugger, draw calls, and GC reduction.
*   [[Phase 3 — Game Architecture/Git for Unity|Git for Unity]]: Version control, metafiles, merge strategies, `.gitignore`, and `.gitattributes`.

### [[Unity Cheat Sheet|Quick Reference Cheat Sheet]]
*   Code snippets, Unity API reference, and lifecycle quick-look.

---

## 🚀 Projects to Build (In Order)

1.  **Lander Game (Current Project)** — Physics, forces, torque, dot product, landing detection, and camera framing.
2.  **Platformer** — State machines, ground checking (raycasts), Coyote time, and animations.
3.  **Top-Down Shooter** — Object pooling (bullets), basic enemy AI, and pathfinding (NavMesh).
4.  **Tower Defense** — ScriptableObjects, wave spawning configurations, and Canvas UI.
5.  **Your Own Finished Game** — Menus, settings, save files, build pipelines, audio mixer, and visual polish.

---

## 📋 Weekly Checklist Template

### Week 1
*   [ ] Install Unity Hub & Unity 6 LTS.
*   [ ] Complete Unity Essentials Path on Unity Learn (Editor navigation).
*   [ ] C# Revision: `class` vs. `struct`, properties, and null checking.
*   [ ] Watch: Freya Holmér Vectors Part 1 (first hour).

### Week 2
*   [ ] Complete Unity Essentials (GameObjects, Components).
*   [ ] Practice `GetComponent` and child/parent traversal.
*   [ ] C# Revision: `out` keyword, static modifiers, events/Action.
*   [ ] Physics: understand Force vs. Velocity and the `FixedUpdate` execution rule.

### Week 3
*   [ ] Rigidbody2D hands-on: apply forces, read and log velocities.
*   [ ] Input System: set up Actions, read movement input, apply to Rigidbody.
*   [ ] C# Revision: `IEnumerator`, Coroutines, Lists, and Dictionaries.
*   [ ] Finish watching Freya Holmér Vectors Part 1.

### Week 4–5
*   [ ] Start Code Monkey Lander tutorial (your exact project).
*   [ ] Sprites, SpriteRenderer, Sorting Layers, and Order in Layer.
*   [ ] Animation basics (Animator Controller, parameters, state changes).
*   [ ] Basic UI: TextMeshPro, Canvas, buttons, and score displays.

### Week 6–8
*   [ ] Junior Programmer Pathway on Unity Learn.
*   [ ] ScriptableObjects: create enemy/item data templates.
*   [ ] Start reading Game Programming Patterns (State chapter).
*   [ ] Build: simple top-down shooter prototype.
