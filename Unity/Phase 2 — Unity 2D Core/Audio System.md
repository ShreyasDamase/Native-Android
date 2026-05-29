# 🔊 Audio System in Unity
### AudioSource, AudioMixer, Spatial Audio, Audio Events & Procedural Sound

Great game feel is 50% audio. This guide covers Unity's complete audio pipeline from simple SFX playback to a full mixer architecture with dynamic music systems.

---

## 1. Unity Audio Architecture Overview

```
                         ┌──────────────────────┐
                         │     AudioListener    │ ← One per scene (usually on Camera)
                         └──────────┬───────────┘
                                    │ Receives all 3D spatial audio
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
    ┌─────────▼──────────┐ ┌────────▼──────────┐ ┌───────▼───────────┐
    │   AudioMixerGroup  │ │  AudioMixerGroup  │ │  AudioMixerGroup  │
    │      Master        │ │   Music           │ │   SFX             │
    └─────────┬──────────┘ └────────┬──────────┘ └───────┬───────────┘
              │                     │                     │
    ┌─────────▼──────────┐ ┌────────▼──────────┐ ┌───────▼───────────┐
    │    AudioSource     │ │   AudioSource     │ │  AudioSource (x3) │
    │ (Ambient Wind)     │ │   (Music Layer)   │ │  (SFX Pool)       │
    └────────────────────┘ └───────────────────┘ └───────────────────┘
```

---

## 2. AudioSource Fundamentals

### Core Component Properties

```csharp
[RequireComponent(typeof(AudioSource))]
public class SoundSetupExample : MonoBehaviour
{
    private AudioSource audioSource;

    [SerializeField] private AudioClip shootSound;
    [SerializeField] private AudioClip explosionSound;

    private void Awake()
    {
        audioSource = GetComponent<AudioSource>();

        // ─── Core Properties ────────────────────────────────────────────────
        audioSource.clip    = shootSound;  // The audio clip to play
        audioSource.volume  = 0.75f;       // 0.0 (silent) to 1.0 (full)
        audioSource.pitch   = 1.0f;        // 1.0 = normal, 0.5 = half speed, 2.0 = double speed
        audioSource.loop    = false;       // Loop the clip continuously
        audioSource.playOnAwake = false;   // Don't auto-play when scene starts

        // ─── Spatial Sound (3D) ─────────────────────────────────────────────
        audioSource.spatialBlend  = 1.0f;  // 0 = 2D (no falloff), 1 = full 3D positional
        audioSource.minDistance   = 1f;    // Full volume within this radius
        audioSource.maxDistance   = 30f;   // Silent beyond this radius
        audioSource.rolloffMode   = AudioRolloffMode.Logarithmic; // Natural-sounding falloff

        // ─── Output Routing ─────────────────────────────────────────────────
        // audioSource.outputAudioMixerGroup = sfxMixerGroup; // Route to AudioMixer
    }

    // Play once at full volume
    private void PlayShoot() => audioSource.PlayOneShot(shootSound, 0.8f);

    // PlayOneShot allows overlapping clips without interruption
    // This is essential for rapid SFX like gunfire or footsteps
    private void PlayExplosion() => audioSource.PlayOneShot(explosionSound);

    // Play at a specific world position (fires and forgets — no AudioSource needed)
    public static void PlayAtPosition(AudioClip clip, Vector3 position, float volume = 1f)
        => AudioSource.PlayClipAtPoint(clip, position, volume);
}
```

---

## 3. AudioMixer (Professional Audio Pipeline)

### Setup Steps

1. **Create AudioMixer**: `Assets → Create → Audio Mixer`
2. **Create Groups** inside the mixer: `Master → Music`, `Master → SFX → UI`, `Master → SFX → World`
3. **Expose Parameters**: Right-click Volume on a group → `Expose 'Volume' to script`. Name it e.g. `MusicVolume`

### Controlling Mixer Volume from Code

> [!IMPORTANT]
> AudioMixer volume uses **decibels (dB)**, not linear 0–1. The conversion formula is: `dB = 20 × log10(linear)`. At linear 0.0001 you get -80dB (effectively silence).

```csharp
using UnityEngine;
using UnityEngine.Audio;

public class AudioMixerController : MonoBehaviour
{
    [SerializeField] private AudioMixer masterMixer;

    // Convert linear slider (0 to 1) to logarithmic dB scale for the mixer
    public void SetMusicVolume(float linearValue)
    {
        float db = linearValue > 0.0001f ? Mathf.Log10(linearValue) * 20f : -80f;
        masterMixer.SetFloat("MusicVolume", db);
    }

    public void SetSFXVolume(float linearValue)
    {
        float db = linearValue > 0.0001f ? Mathf.Log10(linearValue) * 20f : -80f;
        masterMixer.SetFloat("SFXVolume", db);
    }

    // Save and restore mixer state across sessions
    public void SaveMixerSettings()
    {
        masterMixer.GetFloat("MusicVolume", out float musicDB);
        masterMixer.GetFloat("SFXVolume", out float sfxDB);

        PlayerPrefs.SetFloat("MusicDB", musicDB);
        PlayerPrefs.SetFloat("SFXDB", sfxDB);
        PlayerPrefs.Save();
    }

    public void LoadMixerSettings()
    {
        masterMixer.SetFloat("MusicVolume", PlayerPrefs.GetFloat("MusicDB", 0f));
        masterMixer.SetFloat("SFXVolume",   PlayerPrefs.GetFloat("SFXDB", 0f));
    }
}
```

### Mixer Snapshots (Dynamic State Changes)

AudioMixer Snapshots let you smoothly transition between preset configurations (e.g., menu → gameplay → underwater).

```csharp
[SerializeField] private AudioMixer mixer;
[SerializeField] private AudioMixerSnapshot normalSnapshot;
[SerializeField] private AudioMixerSnapshot underwaterSnapshot;
[SerializeField] private AudioMixerSnapshot pausedSnapshot;

// Blend smoothly to the underwater acoustic preset in 1.5 seconds
public void EnterWater()   => underwaterSnapshot.TransitionTo(1.5f);
public void ExitWater()    => normalSnapshot.TransitionTo(0.5f);

// Pause: transition to low-pass filtered, muffled snapshot
public void PauseGame()    => pausedSnapshot.TransitionTo(0.1f);
public void ResumeGame()   => normalSnapshot.TransitionTo(0.1f);
```

---

## 4. Audio Manager (Sound Event System)

An AudioManager decouples sound playback from game logic. Any system broadcasts an audio event; the manager handles pooling and routing.

```csharp
[CreateAssetMenu(fileName = "SoundEvent", menuName = "Audio/Sound Event")]
public class SoundEventSO : ScriptableObject
{
    [Header("Clips (one will be chosen randomly)")]
    public AudioClip[] clips;

    [Header("Variation")]
    [Range(0f, 1f)] public float volume = 1f;
    [Range(0.8f, 1.2f)] public float pitchMin = 0.95f;
    [Range(0.8f, 1.2f)] public float pitchMax = 1.05f;

    [Header("Routing")]
    public AudioMixerGroup outputGroup;

    // Play this sound event on a provided AudioSource
    public void Play(AudioSource source)
    {
        if (clips == null || clips.Length == 0) return;

        source.outputAudioMixerGroup = outputGroup;
        source.volume = volume;
        source.pitch  = Random.Range(pitchMin, pitchMax);

        // Choose a random clip from the array to add variation
        source.PlayOneShot(clips[Random.Range(0, clips.Length)]);
    }
}
```

```csharp
// A pooled audio player that can fire SoundEvents from anywhere
public class AudioManager : MonoBehaviour
{
    [SerializeField] private int poolSize = 10;
    private AudioSource[] sourcePool;
    private int poolIndex;

    private void Awake()
    {
        sourcePool = new AudioSource[poolSize];
        for (int i = 0; i < poolSize; i++)
        {
            var go = new GameObject($"AudioPool_{i}");
            go.transform.SetParent(transform);
            sourcePool[i] = go.AddComponent<AudioSource>();
        }
    }

    // Play a 2D sound event (UI, non-positional)
    public void Play(SoundEventSO soundEvent)
    {
        var source = GetNextSource();
        soundEvent.Play(source);
    }

    // Play a 3D positional sound event at world position
    public void PlayAt(SoundEventSO soundEvent, Vector3 worldPosition)
    {
        var source = GetNextSource();
        source.transform.position = worldPosition;
        source.spatialBlend = 1f;
        soundEvent.Play(source);
    }

    private AudioSource GetNextSource()
    {
        // Round-robin pool rotation
        var src = sourcePool[poolIndex];
        poolIndex = (poolIndex + 1) % poolSize;
        return src;
    }
}
```

---

## 5. Dynamic Music System (Adaptive Audio)

Adaptive music changes based on game state — calm during exploration, intense during combat.

```csharp
public class AdaptiveMusicController : MonoBehaviour
{
    [SerializeField] private AudioSource ambientLayer;  // Always playing: calm loop
    [SerializeField] private AudioSource combatLayer;   // Fades in during combat
    [SerializeField] private AudioSource bossLayer;     // Only during boss fights

    [SerializeField] private float fadeDuration = 2f;

    private Coroutine fadeRoutine;

    public void TransitionToCombat()
    {
        CrossfadeLayers(combatLayer, fadeDuration);
    }

    public void TransitionToAmbient()
    {
        CrossfadeLayers(ambientLayer, fadeDuration);
    }

    private void CrossfadeLayers(AudioSource fadeIn, float duration)
    {
        if (fadeRoutine != null) StopCoroutine(fadeRoutine);
        fadeRoutine = StartCoroutine(CrossfadeRoutine(fadeIn, duration));
    }

    private IEnumerator CrossfadeRoutine(AudioSource target, float duration)
    {
        var allSources = new[] { ambientLayer, combatLayer, bossLayer };
        float elapsed = 0f;

        while (elapsed < duration)
        {
            elapsed += Time.unscaledDeltaTime;
            float t = elapsed / duration;

            foreach (var src in allSources)
            {
                // Fade in the target; fade out all others
                src.volume = Mathf.Lerp(src.volume, src == target ? 1f : 0f, t);
            }

            yield return null;
        }

        // Ensure clean final state
        foreach (var src in allSources)
            src.volume = src == target ? 1f : 0f;
    }
}
```

---

## 6. Procedural Audio: Pitch Randomization for Variety

Firing the same audio clip every time sounds robotic. Add subtle randomization:

```csharp
public class WeaponAudio : MonoBehaviour
{
    [SerializeField] private AudioClip[] gunShotVariants;
    [SerializeField] private AudioMixerGroup sfxGroup;
    [SerializeField] private float volumeMin = 0.7f, volumeMax = 0.9f;
    [SerializeField] private float pitchMin = 0.9f, pitchMax = 1.15f;

    private AudioSource audioSource;

    private void Awake()
    {
        audioSource = GetComponent<AudioSource>();
        audioSource.outputAudioMixerGroup = sfxGroup;
    }

    public void PlayShot()
    {
        audioSource.volume = Random.Range(volumeMin, volumeMax);
        audioSource.pitch  = Random.Range(pitchMin, pitchMax);
        audioSource.clip   = gunShotVariants[Random.Range(0, gunShotVariants.Length)];
        audioSource.Play();
    }
}
```

---

## 7. Footstep System (Surface-Aware Audio)

```csharp
[System.Serializable]
public class SurfaceAudio
{
    public string surfaceTag;
    public AudioClip[] footstepClips;
}

public class FootstepAudioController : MonoBehaviour
{
    [SerializeField] private SurfaceAudio[] surfaces;
    [SerializeField] private LayerMask groundLayer;
    [SerializeField] private float stepInterval = 0.4f;

    private AudioSource audioSource;
    private Rigidbody2D rb;
    private float stepTimer;

    private void Awake()
    {
        audioSource = GetComponent<AudioSource>();
        rb = GetComponent<Rigidbody2D>();
    }

    private void Update()
    {
        // Only play footsteps when moving horizontally and grounded
        bool isMoving = Mathf.Abs(rb.linearVelocity.x) > 0.1f;
        bool isGrounded = IsGrounded(out string surfaceTag);

        if (!isMoving || !isGrounded) { stepTimer = 0; return; }

        stepTimer += Time.deltaTime;
        if (stepTimer >= stepInterval)
        {
            stepTimer = 0f;
            PlayFootstep(surfaceTag);
        }
    }

    private bool IsGrounded(out string surfaceTag)
    {
        surfaceTag = "Default";
        var hit = Physics2D.Raycast(transform.position, Vector2.down, 1.1f, groundLayer);
        if (hit.collider != null)
        {
            surfaceTag = hit.collider.tag;
            return true;
        }
        return false;
    }

    private void PlayFootstep(string tag)
    {
        foreach (var surface in surfaces)
        {
            if (surface.surfaceTag == tag && surface.footstepClips.Length > 0)
            {
                audioSource.pitch = Random.Range(0.9f, 1.1f);
                audioSource.PlayOneShot(surface.footstepClips[Random.Range(0, surface.footstepClips.Length)]);
                return;
            }
        }
    }
}
```

---

## 8. Audio Performance Tips

```
┌────────────────────────────────────────────────────────────────────────┐
│                      AUDIO OPTIMIZATION RULES                          │
├────────────────────────────────────────────────────────────────────────┤
│ ✅ Set Load Type = "Compressed in Memory" for music (long clips)       │
│ ✅ Set Load Type = "Decompress on Load" for short SFX (<1MB)           │
│ ✅ Use Mono for 3D positional audio (stereo is wasted in 3D space)     │
│ ✅ Pool AudioSources — don't AddComponent at runtime                   │
│ ✅ Limit simultaneous AudioSources to 32 on mobile (hardware cap)      │
│ ✅ Compress format: Vorbis (quality 70%) for music, ADPCM for SFX      │
│ ❌ Don't use AudioSource.PlayClipAtPoint() in tight loops (GC alloc)  │
│ ❌ Don't load AudioClips in Update() — use preloading                  │
│ ❌ Don't call AudioSource.Play() and immediately check isPlaying()     │
└────────────────────────────────────────────────────────────────────────┘
```
