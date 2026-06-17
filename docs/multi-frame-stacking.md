# Multi-Frame Stacking for Origin Camera

> *A complete guide to understanding, designing, and implementing multi-frame stacking in a zero-post-processing camera pipeline.*

---

## Table of Contents

1. [What Multi-Frame Stacking Is](#1-what-multi-frame-stacking-is)
2. [Why This Matters for Origin Camera](#2-why-this-matters-for-origin-camera)
3. [How a Single Frame Works (And Where It Falls Short)](#3-how-a-single-frame-works-and-where-it-falls-short)
4. [The Multi-Frame Pipeline Explained](#4-the-multi-frame-pipeline-explained)
5. [Frame Count: How Many to Capture](#5-frame-count-how-many-to-capture)
6. [Timing: The Gap Between Frames](#6-timing-the-gap-between-frames)
7. [Exposure Strategy for Stacked Capture](#7-exposure-strategy-for-stacked-capture)
8. [Sensor Resolution and the Megapixel Tradeoff](#8-sensor-resolution-and-the-megapixel-tradeoff)
9. [Adaptive Frame Scaling by Sensor Resolution](#9-adaptive-frame-scaling-by-sensor-resolution)
10. [Alignment: The Hardest Part](#10-alignment-the-hardest-part)
11. [Merge Strategies](#11-merge-strategies)
12. [Weighting Schemes](#12-weighting-schemes)
13. [Ghosting and Motion Handling](#13-ghosting-and-motion-handling)
14. [Memory and Performance Budget](#14-memory-and-performance-budget)
15. [Storage Implications](#15-storage-implications)
16. [The Role of Sensor Readout Speed](#16-the-role-of-sensor-readout-speed)
17. [Camera2 API vs CameraX for MFS](#17-camera2-api-vs-camerax-for-mfs)
18. [Burst Capture on CameraX](#18-burst-capture-on-camerax)
19. [YUV vs RAW for Multi-Frame Capture](#19-yuv-vs-raw-for-multi-frame-capture)
20. [What Post-Processing Steps to Add (and Skip)](#20-what-post-processing-steps-to-add-and-skip)
21. [Sharpening After MFS](#21-sharpening-after-mfs)
22. [Noise Reduction After MFS](#22-noise-reduction-after-mfs)
23. [Color Handling After MFS](#23-color-handling-after-mfs)
24. [Lens Correction and MFS](#24-lens-correction-and-mfs)
25. [Why MFS Changes the Micro-Detail Equation at Zoom](#25-why-mfs-changes-the-micro-detail-equation-at-zoom)
26. [What MFS Does NOT Fix](#26-what-mfs-does-not-fix)
27. [What Origin Camera Should NOT Do](#27-what-origin-camera-should-not-do)
28. [Comparison: Origin Philosophy vs GCam Philosophy](#28-comparison-origin-philosophy-vs-gcam-philosophy)
29. [Prototype Roadmap](#29-prototype-roadmap)
30. [Phase 1: 3-Frame Stacking Prototype](#30-phase-1-3-frame-stacking-prototype)
31. [Phase 2: 5-Frame Weighted Stacking](#31-phase-2-5-frame-weighted-stacking)
32. [Phase 3: Full Adaptive Pipeline](#32-phase-3-full-adaptive-pipeline)
33. [Testing and Validation Strategy](#33-testing-and-validation-strategy)
34. [Sensor-Specific Tuning](#34-sensor-specific-tuning)
35. [Edge Cases and Failure Modes](#35-edge-cases-and-failure-modes)
36. [Glossary](#36-glossary)
37. [References for Further Study](#37-references-for-further-study)

---

## 1. What Multi-Frame Stacking Is

Multi-frame stacking is a computational photography technique where a camera captures several frames of the same scene in rapid succession and combines them into a single output image.

**The core insight:** each individual frame from a camera sensor contains both signal (the actual scene) and noise (random electrical/thermal variation). The signal is deterministic — it represents real photons hitting the sensor. The noise is random — it changes from frame to frame. By capturing multiple frames and merging them, the signal reinforces itself while the noise averages out.

**Why this is not the same as taking one photo:** a single frame is one sample of the scene. Multi-frame stacking is multiple samples, which gives the merge algorithm more information to work with. More information → better output.

**Why this is not the same as HDR:** HDR (high dynamic range) captures frames at different exposure levels to extend dynamic range. MFS captures frames at the same exposure to improve signal quality. They are complementary, not the same thing. Origin Camera explicitly skips HDR but uses MFS.

---

## 2. Why This Matters for Origin Camera

Origin Camera's current pipeline is:

```
Sensor → CameraX ImageCapture → JPEG save
```

There is zero post-processing. The `ImagePostProcessor` interface exists in the codebase but has no implementations. The comment in `CameraXCameraSystem.kt` at the saved-image callback says:

```
/* ZERO post-processing — no ImagePostProcessor invocations */
```

**The problem this creates:**

- Every frame carries the full noise of a single exposure
- Digital zoom amplifies both detail and noise equally
- Fine textures near the sensor's resolving limit get buried in noise
- Stock cameras (GCam, stock OEM apps) use 3–9 frame bursts routinely
- At zoom levels, the difference becomes stark because the stock apps have cleaner signal to work with

**MFS is the single highest-ROI change for Origin Camera** because:

- It preserves the "no fake processing" philosophy — you're combining real photons, not inventing detail
- It directly attacks the zoomed micro-detail problem the user noticed
- It requires no AI models, no HDR tonemapping, no scene detection
- It is a purely signal-processing approach that respects the sensor

---

## 3. How a Single Frame Works (And Where It Falls Short)

### Single capture path:

```
Light → Sensor → Exposure → Readout → ISP → YUV/RAW → JPEG encode → File
```

Each step adds or loses information:

| Stage | What happens | Loss |
|-------|-------------|------|
| **Exposure** | Photons hit photodiodes, create electron charge | Shot noise (quantum randomness of photons) |
| **Readout** | Charge is converted to voltage, then to digital values | Read noise, quantisation noise |
| **ISP** | Demosaic, white balance, colour correction, sharpen, denoise | Tuning tradeoffs |
| **JPEG** | 8-bit quantisation, chroma subsampling | Colour resolution loss |

### Why one frame is not enough:

- **Shot noise** follows a Poisson distribution. Its standard deviation = √(signal). Darker pixels have proportionally more noise.
- **Read noise** is added per-pixel per-read. Each frame pays this cost once.
- **Dark current noise** (thermal electrons) varies per-frame.
- **Quantisation noise** is added when analog values are rounded to digital integers.

All of these are random per frame. None are random per scene. That is the entire basis for stacking.

### Zoomed photography specifically:

When you digitally zoom, you are:

1. Cropping into a smaller region of the sensor
2. That region receives fewer total photons
3. The signal-to-noise ratio drops
4. Fine details (which already sit near the noise floor) become indistinguishable

MFS recovers those details by stacking signal from multiple frames, effectively raising the signal without raising the noise proportionally.

---

## 4. The Multi-Frame Pipeline Explained

### Full pipeline:

```
Light
  ↓
Sensor
  ↓
Exposure (same settings for all frames)
  ↓
Readout (frame 1)
  ↓
Readout (frame 2)
  ↓
Readout (frame 3)
  ↓
...
  ↓
Frame buffer (all frames in memory)
  ↓
Alignment (register frames to a reference)
  ↓
Merge (combine aligned frames)
  ↓
Optional: Light sharpening
  ↓
Optional: Light denoising
  ↓
Colour conversion if needed
  ↓
JPEG/HEIF encode
  ↓
Save
```

### Origin Camera's specific pipeline after MFS:

```
Sensor
  ↓
MFS (alignment + merge)
  ↓
RAW-like output (no HDR, no AI, no beautification)
  ↓
Minimal colour conversion
  ↓
Save
```

**What is intentionally excluded:**

- HDR tonemapping (lifts shadows, flattens contrast)
- AI scene enhancement (invents texture)
- Aggressive sharpening (creates false edges)
- Beauty processing (smooths skin)
- Multi-exposure bracketing (changes exposure per frame)

---

## 5. Frame Count: How Many to Capture

### Recommended counts for Origin Camera:

| Scenario | Frames | Rationale |
|----------|--------|-----------|
| **Prototype / first build** | 3 | Lowest complexity, still shows visible improvement |
| **Normal photo** | 3–5 | Good balance of quality gain vs processing cost |
| **Zoomed photo** | 5 | More data needed to recover fine detail |
| **Low light** | 5–8 | Noise reduction benefits from more samples |
| **Well-lit scene with static subject** | 5 | Maximum quality without motion risk |
| **Moving subject / action** | 3 | Fewer frames = less motion change = easier alignment |

### Why not more than 8:

- **Diminishing returns:** the noise reduction from N frames scales as √N. Going from 1→4 frames cuts noise by 2×. Going from 4→9 frames cuts noise by 1.5×. Going from 9→16 frames cuts noise by 1.33×. The benefit per frame keeps dropping.
- **Motion risk:** more frames = longer capture window = higher chance something moves.
- **Memory pressure:** each 50MP frame is ~100 MB in YUV. Four frames = 400 MB.
- **Processing time:** alignment is O(N×pixels). More frames means the user waits longer.

### The √N rule:

```
Noise reduction factor = 1 / √(N)

N=1  → 1.00× (no reduction)
N=3  → 0.58× (42% noise reduction)
N=5  → 0.45× (55% noise reduction)
N=8  → 0.35× (65% noise reduction)
N=16 → 0.25× (75% noise reduction)
```

The jump from N=1 to N=3 is the biggest single gain. N=5 is significantly better than N=3. Beyond N=8, improvements are small and costs are high.

---

## 6. Timing: The Gap Between Frames

The time between frame captures must be short enough that the scene hasn't changed, but long enough that the sensor can finish one capture and start the next.

### Key constraint: sensor readout speed

Every sensor has a readout time — the time it takes to convert all pixel charges to digital values and transfer them out. During readout, the sensor cannot begin a new exposure.

Typical readout speeds:

| Sensor type | Readout time | Max burst rate |
|-------------|-------------|----------------|
| 12MP (small sensor) | ~15–25 ms | ~40–60 FPS |
| 48–50MP (with fast readout) | ~25–40 ms | ~25–40 FPS |
| 108MP | ~40–80 ms | ~12–25 FPS |
| 200MP | ~60–120 ms | ~8–16 FPS |

### Recommended spacing for Origin Camera:

| Condition | Gap per frame | Total for 5 frames |
|-----------|--------------|-------------------|
| Bright light, fast sensor | 30–50 ms | 150–250 ms |
| Normal indoor light | 50–80 ms | 250–400 ms |
| Low light | 80–120 ms | 400–600 ms |
| Zoom (digital) | 40–80 ms | 200–400 ms |

### Example timeline (5 frames at 40 ms):

```
T=0ms     → capture frame 1 begins
T=exposure→ frame 1 readout begins
T=40ms    → capture frame 2 begins
T=40ms+exp→ frame 2 readout begins
T=80ms    → capture frame 3 begins
T=80ms+exp→ frame 3 readout begins
T=120ms   → capture frame 4 begins
T=120ms+exp→ frame 4 readout begins
T=160ms   → capture frame 5 begins
T=160ms+exp→ frame 5 readout begins
T=200ms+  → all 5 frames in buffer, merge starts
```

### What happens if the gap is too small:

- The sensor may not have finished readout
- The Camera2/CameraX pipeline may drop the request
- Frames may overlap in exposure
- You get fewer frames than requested

### What happens if the gap is too large:

- Lighting may change
- Subject may move
- Hand position may shift
- Alignment becomes harder because frames are less similar

### Real-world minimum from Camera2:

Camera2's `CONTROL_AE_MODE_ON_AUTO_FLASH` and burst capture typically achieve:

```
30–50 FPS on 12MP sensors
15–25 FPS on 48–50MP sensors
8–15 FPS on 108MP sensors
5–10 FPS on 200MP sensors
```

So a 40ms gap at 12MP is feasible. A 40ms gap at 200MP may not be.

### The 10ms question:

Can you capture a frame every 10ms?

- **Theoretical burst rate at 12MP:** some sensors can do 60 FPS → ~16.7ms gap. 10ms is <16.7ms, so no.
- **Sensor limitation:** even ignoring exposure, readout alone takes more than 10ms on most phone sensors.
- **Pipeline bottleneck:** Camera2's capture queue, ISP processing, and buffer management add latency.
- **Verdict:** 10ms is not achievable for full-resolution capture on any current phone sensor. 30ms is the practical floor for 12MP. 50–80ms is the practical floor for 50MP+.

---

## 7. Exposure Strategy for Stacked Capture

For MFS to work correctly, all frames should use identical exposure settings:

| Setting | Should be fixed? | Why |
|---------|-----------------|-----|
| **Exposure time (shutter speed)** | Yes | Different exposures create brightness variation that breaks simple averaging |
| **ISO** | Yes | Different ISOs change noise characteristics frame to frame |
| **Focus position** | Yes | Focus must be locked before burst begins |
| **White balance** | Yes | Locked to prevent colour shifts between frames |
| **Zoom** | Yes | Must not change during capture |

### Exposure lock procedure:

1. Meter the scene normally (auto exposure, auto white balance)
2. Lock all AE/AWB/AF settings
3. Capture the burst with locked settings
4. Unlock after capture completes

### Dealing with changing light during capture:

In rare cases, the scene brightness changes during the burst (cloud moves, lights flicker, etc). A simple check:

```
Compare mean brightness of each frame:
  If max_brightness / min_brightness > 1.3:
    Warn or discard the outlier frame
```

---

## 8. Sensor Resolution and the Megapixel Tradeoff

Higher resolution sensors capture more detail per frame but create practical problems for MFS.

### Raw data per frame at different resolutions:

| Resolution | Pixels | RAW (10-bit, bytes) | YUV420 (bytes) | Single frame cost |
|-----------|--------|---------------------|----------------|-------------------|
| 12MP | 4000×3000 | ~60 MB | ~18 MB | Low |
| 50MP | 8160×6120 | ~250 MB | ~75 MB | Medium |
| 108MP | 12000×9000 | ~540 MB | ~162 MB | High |
| 200MP | 16320×12240 | ~980 MB | ~294 MB | Very high |

### Cost of 5-frame MFS at each resolution:

| Resolution | YUV memory (5 frames) | Processing time estimate |
|-----------|----------------------|------------------------|
| 12MP | ~90 MB | <100ms |
| 50MP | ~375 MB | ~300–500ms |
| 108MP | ~810 MB | ~800ms–1.5s |
| 200MP | ~1.5 GB | ~2–5s |

### Origin Camera's approach:

The app should query `CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE` and `StreamConfigurationMap.getOutputSizes()` to find the maximum available resolution. It should then apply adaptive frame scaling (see next section).

---

## 9. Adaptive Frame Scaling by Sensor Resolution

Instead of a fixed frame count for all devices, the number of frames should scale inversely with sensor resolution. This keeps the total data processed roughly constant.

### The formula:

```
frames = baseFrames × (referenceMP / sensorMP)
```

Where:

- `baseFrames = 16` (reference count for a 12MP sensor in low light)
- `referenceMP = 12`
- `sensorMP` = the actual sensor megapixels

### Applied:

| Sensor | Math | Normal light | Low light |
|--------|------|-------------|-----------|
| 12MP | 16 × (12/12) | 3 | 16 |
| 24MP | 16 × (12/24) | 3 | 8 |
| 50MP | 16 × (12/50) | 3–4 | 4–5 |
| 108MP | 16 × (12/108) | 2–3 | 3 |
| 200MP | 16 × (12/200) | 1–2 | 2 |

### Why this works:

- Higher resolution sensors capture more detail per frame, so fewer frames are needed to match the total information of a lower-resolution sensor burst
- Memory and processing budgets stay roughly constant
- 200MP sensors don't crash the phone

### Per-scenario adaptive logic:

```
fun calculateFrameCount(sensorMP: Float, lightLevel: LightLevel): Int {
    val baseFrames = when (lightLevel) {
        LightLevel.BRIGHT   -> 3
        LightLevel.NORMAL   -> 5
        LightLevel.LOW      -> 16
        LightLevel.VERY_LOW -> 16
    }
    val referenceMP = 12f
    val scaled = (baseFrames * (referenceMP / sensorMP)).roundToInt()
    return scaled.coerceIn(1, 16)
}
```

### Minimum and maximum:

- **Minimum frames:** 1 (effectively no stacking for very high resolution in bright light)
- **Maximum frames:** 16 (low light on lower resolution sensors)

### Low light special case:

In very low light, each individual frame has a very low signal-to-noise ratio. More frames help more. The floor should be higher:

```
fun calculateFrameCountForLowLight(sensorMP: Float): Int {
    // In low light, even high-MP sensors benefit from some stacking
    val baseFrames = 16
    val referenceMP = 12f
    val scaled = (baseFrames * (referenceMP / sensorMP)).roundToInt()
    return scaled.coerceIn(2, 16)
}
```

---

## 10. Alignment: The Hardest Part

Alignment (also called registration) is the process of shifting each frame so that the same scene content occupies the same pixel locations before merging.

### Why alignment is needed:

Even with a tripod-steady hand, there is always sub-pixel movement between frames:

- Hand tremor (~0.5–2 pixels on a phone sensor)
- Breathing moves the whole body
- Finger pressing the shutter shifts the phone
- Imperfect OIS settling

### If you skip alignment:

```
Frame 1:       Frame 2:       Average (no align):
[A][B][C]      [A][B][C]      [A ][B ][C ]
[B][C][D]      [C][B][D]      [BC][BB][CD]
                             → blurred edges
                             → doubled features
                             → less detail, not more
```

### Alignment approaches from simplest to most sophisticated:

| Method | Quality | Speed | Complexity | Notes |
|--------|---------|-------|-----------|-------|
| None | Poor | Instant | None | Only works on a tripod |
| Global translation (2D shift) | Low | Fast | Easy | Corrects x/y shift only |
| Affine (6-parameter) | Medium | Fast | Medium | Corrects shift + rotation + scale |
| Homography (8-parameter) | Medium | Fast | Medium | Corrects perspective too |
| Block-matching | Medium | Medium | Medium | Divides image into blocks, aligns each |
| Optical flow (dense) | High | Slow | High | Per-pixel motion vectors |
| Feature-based (SIFT/ORB) | High | Medium | High | Finds keypoints, matches, warps |

### Recommended for Origin Camera prototype:

**Phase 1:** Global translation using phase correlation or simple block matching.

Translation-only alignment is sufficient to correct hand tremor. For a phone camera, most motion between frames at 40ms gaps is near-global shift.

**Phase 2:** Affine or homography alignment with feature matching (ORB features + RANSAC).

**Phase 3:** Block-matching with motion rejection (ignore blocks that moved too much due to scene motion, not camera motion).

### Phase correlation (simple and effective):

```
For each pair (reference_frame, other_frame):
  1. Convert both to grayscale
  2. Compute FFT of both
  3. Compute cross-power spectrum
  4. Inverse FFT → phase correlation surface
  5. Find peak → shift in pixels
  6. Apply translation to other frame
```

This is available in OpenCV as `phaseCorrelate()` and in various FFT libraries.

### Feature-based alignment:

```
For each frame:
  1. Detect ORB/SIFT features in reference frame
  2. Detect features in target frame
  3. Match features between frames
  4. Find homography matrix with RANSAC (rejects outliers)
  5. Warp target frame to reference frame
```

OpenCV provides all of these functions. The challenge is performance on a phone — feature detection at 50MP is slow. Downscale first, compute alignment at reduced resolution, then apply the warp at full resolution.

### Downscale-align-upscale strategy:

```
For each non-reference frame:
  1. Downscale to 1MP (or even VGA)
  2. Compute alignment at low resolution
  3. Scale alignment parameters back to full resolution
  4. Apply warp to full resolution frame
```

This saves enormous amounts of processing time.

### Motion rejection:

Some parts of the frame may contain moving objects (cars, people, leaves). Those areas should be excluded from alignment and handled differently in the merge stage (see Ghosting section).

---

## 11. Merge Strategies

Once frames are aligned, they must be combined into one output image.

### A. Simple averaging

```
output(x, y) = (frame1(x,y) + frame2(x,y) + ... + frameN(x,y)) / N
```

**Pros:**

- Extremely simple
- Fast
- Optimal noise reduction for static scenes
- No tuning parameters

**Cons:**

- Any misalignment → blur
- Moving objects → ghosting
- Outliers (hot pixels, flare) contaminate the result

**Best for:** Phase 1 prototype, well-aligned frames, static scenes.

### B. Median merge

```
output(x, y) = median(frame1(x,y), frame2(x,y), ..., frameN(x,y))
```

**Pros:**

- Extremely robust to outliers (hot pixels, flare, moving objects)
- No ghosting from objects that move across the frame
- Preserves edges better than averaging

**Cons:**

- Less noise reduction than averaging (for Gaussian noise)
- Needs odd number of frames (3, 5, 7...)
- Slightly slower (needs sorting)

**Best for:** Scenes with small moving elements, when outlier rejection matters more than max noise reduction.

### C. Weighted averaging

```
output(x, y) = Σ(wi × framei(x,y)) / Σ(wi)
```

Where each frame gets a weight `wi` based on quality metrics:

- Sharpness score (higher weight for sharper frames)
- Exposure score (higher weight for properly exposed frames)
- Alignment confidence (higher weight for frames that aligned well)
- Temporal position (higher weight for middle frames, lower for first/last)

**Best for:** Phase 2 and beyond. Gives the best balance of quality and control.

### D. Pixel-wise weighted merge

Instead of one weight per frame, compute a weight per pixel:

```
output(x, y) = Σ(w(i,x,y) × framei(x,y)) / Σ(w(i,x,y))
```

Where each pixel weight depends on:

- Variance across frames (high variance → motion → lower weight)
- Distance from centre of frame (reduce edge artefacts)
- Luminance (brighter pixels have better SNR → higher weight)

**Best for:** Phase 3. The most sophisticated approach but requires per-pixel computation.

### E. Multi-scale fusion

Instead of merging pixels directly, decompose each frame into frequency bands (Laplacian pyramid or wavelet), merge each band separately, then reconstruct:

```
For each frame:
  Build Laplacian pyramid (low frequencies → fine details)
  
For each pyramid level:
  Apply different merge strategy:
    - Low frequencies: average (smooth, colour)
    - High frequencies: max gradient or select sharpest (detail)
    - Mid frequencies: weighted blend

Reconstruct from merged pyramid
```

**Best for:** Advanced pipelines that want the best of both worlds — smooth colour from averaging, crisp detail from selective merging.

---

## 12. Weighting Schemes

### Simple quality-based weighting:

```
weight = sharpnessScore × exposureScore
```

Where:

- `sharpnessScore` = variance of Laplacian (higher = sharper)
- `exposureScore` = 1.0 for well-exposed, lower for under/over

### Temporal weighting (frames near the middle get more weight):

```
weights = [0.15, 0.25, 0.35, 0.25, 0.15]  // 5 frames
weights = [0.20, 0.30, 0.35, 0.15]         // 4 frames (weighted toward early frames because the first frame is the reference)
weights = [0.20, 0.30, 0.50]               // 3 frames
```

The reference frame (usually the first) gets moderate weight. The frames after it, which are temporally close and well-aligned to the reference, get the most weight.

### Adaptive weighting based on alignment:

```
alignmentError = RMS difference between aligned frame and reference
weight = 1.0 / (1.0 + alignmentError × k)
```

Frames that didn't align well automatically contribute less.

### Per-pixel adaptive weighting (the best approach):

```
For each pixel (x,y) in frame i:
  variance = computeVarianceOfPixelAcrossAllFrames(x,y)
  
  If variance is LOW:
    // All frames agree → pixel is static → use all
    weight = 1.0
  
  If variance is HIGH:
    // Frames disagree → motion or misalignment
    // Find the frame closest to median
    distance = abs(framei(x,y) - medianAcrossFrames(x,y))
    weight = exp(-distance² / σ²)
```

This is elegant because it automatically handles:

- Static areas: all frames contribute equally → maximum noise reduction
- Moving areas: only frames with the most representative pixel contribute → minimal ghosting
- Misaligned edges: the same as motion — edges that don't line up get less weight

---

## 13. Ghosting and Motion Handling

Ghosting is the most visible failure mode of multi-frame stacking. It appears as semi-transparent duplicate edges or objects.

### What causes ghosting:

```
Frame 1: [●静止した手]
Frame 2: [●手が動いた]
Frame 3: [●手が動いた]

Average: [●手手●] ← ghosted hand
```

### Detection approaches:

**Approach 1: Frame-to-frame difference threshold**

```
diff = |frame1 - frame2|
if diff(x,y) > threshold:
    mark pixel(x,y) as "motion region"
```

**Approach 2: Variance-based detection**

```
For each pixel across all frames:
    variance = Σ(framei - mean)² / N
    if variance > threshold:
        mark pixel(x,y) as "motion region"
```

**Approach 3: Block-based motion detection**

```
Divide frame into 32×32 blocks
For each block, compute mean and variance
If a block's mean changes significantly across frames:
    mark entire block as "motion region"
```

### Handling motion:

| Strategy | Quality | Complexity | Notes |
|----------|---------|-----------|-------|
| Use fewer frames | Low | None | Captures less data but reduces motion window |
| Reject motion frames entirely | Medium | Low | If one frame has motion, drop it |
| Per-pixel weighted merge with variance | High | Medium | Natural motion handling (see Section 12) |
| Ghost reduction post-processing | High | High | Detect and remove ghost artifacts after merge |
| Selective frame rejection per region | High | Medium | Use the "best" frame for motion regions only |

### The "selective" approach (good for Phase 3):

```
1. Align all frames to reference
2. Detect motion regions (variance across frames)
3. For non-motion regions:
     Weighted average merge (all frames)
4. For motion regions:
     Use only the reference frame (or the sharpest frame)
5. Blend the two regions with a soft transition mask
```

This gives noise reduction in static areas and no ghosting in motion areas.

### Motion at the exposure time level:

If the exposure time is long (e.g., 1/15s in low light), even a single frame can have motion blur. MFS does not fix motion blur within a single frame. It only fixes the variation between frames.

To minimise within-frame motion blur, keep exposure times short:

- **Normal light:** 1/120s or faster
- **Low light:** 1/30s or faster (or use more frames at shorter exposures)
- **Zoomed:** Same as normal — zoom itself doesn't change exposure requirements, but the effective aperture may drop

---

## 14. Memory and Performance Budget

### Temporary memory needed during MFS:

```
total_memory = N × bytes_per_frame + alignment_buffers + merge_buffers + output_buffer
```

### Realistic examples:

| Config | Frames | Format | Bytes/frame | Total | With overhead |
|--------|--------|--------|-------------|-------|--------------|
| 12MP normal | 3 | YUV420 | ~18 MB | ~54 MB | ~100 MB |
| 12MP low light | 16 | YUV420 | ~18 MB | ~288 MB | ~400 MB |
| 50MP normal | 4 | YUV420 | ~75 MB | ~300 MB | ~450 MB |
| 50MP low light | 5 | YUV420 | ~75 MB | ~375 MB | ~500 MB |
| 108MP normal | 3 | YUV420 | ~162 MB | ~486 MB | ~650 MB |
| 200MP normal | 2 | YUV420 | ~294 MB | ~588 MB | ~800 MB |

### Reducing memory pressure:

1. **Stream frames:** Process one frame at a time instead of loading all into memory. Stream the merge by accumulating weighted sums.

2. **Downscale for alignment:** Compute alignment at reduced resolution, apply at full resolution (see Section 10).

3. **Use a frame pool:** Pre-allocate reusable Image buffers to avoid GC churn.

4. **JPEG compression per frame:** If memory is critical, compress each frame to JPEG in flight and decompress during merge (adds CPU cost but reduces memory).

5. **Process on background thread:** Use `HardwareBuffer` and GPU processing where possible.

### Performance budget per frame:

For a responsive camera app:

| Operation | Budget | Notes |
|-----------|--------|-------|
| Capture burst | <500ms total | Otherwise user misses the moment |
| Alignment (all frames) | <200ms | Downscale for alignment keeps this fast |
| Merge | <100ms | Weighted averaging is fast |
| Post-process (sharpen, etc) | <50ms | Light touch only |
| Encode + save | <500ms | JPEG encode is the bottleneck |
| **Total** | **<1.5s** | Acceptable for a non-burst shot |

### Thermal considerations:

- MFS uses more CPU/GPU than single capture
- On a 50MP sensor with 5 frames, the phone will warm up
- Avoid processing more than 1 MFS shot every 5 seconds
- If phone temperature exceeds ~40°C, fall back to single-frame capture

---

## 15. Storage Implications

### Temporary storage:

Merging frames requires temporary files or memory buffers:

- **All-in-memory approach:** frames live in `ImageReader` buffers → faster but higher memory
- **File-backed approach:** frames saved to cache → lower memory but slower I/O

For Origin Camera, start with all-in-memory (simpler), add file-backed fallback later.

### Final output file size:

MFS images at full resolution are large:

| Resolution | Format | Typical size |
|-----------|--------|-------------|
| 12MP | JPEG (95% quality) | ~4–8 MB |
| 50MP | JPEG (95% quality) | ~15–30 MB |
| 108MP | JPEG (95% quality) | ~30–60 MB |
| 200MP | JPEG (95% quality) | ~50–100 MB |

With MFS, the output is cleaner (less noise → better JPEG compression), so file sizes may actually be slightly smaller than a noisy single frame at the same quality setting.

### Auto-delete temporary frames:

Always clean up temporary frame buffers/files after merge completes. Use `try/finally` or `use()` blocks to ensure cleanup even on errors.

---

## 16. The Role of Sensor Readout Speed

Sensor readout speed determines the maximum burst rate and the minimum gap between frames.

### How readout works:

```
Exposure time (all pixels accumulate charge simultaneously)
  ↓
Readout (rows are read sequentially — rolling shutter)
  ↓
Next exposure can begin after the previous readout finishes
```

### Rolling shutter effect:

Since pixel rows are read sequentially, there is a time skew between the top and bottom of the frame. This means:

- Rolling shutter distortion is different in each frame
- Alignment must handle non-uniform shifts between top and bottom
- Very fast motion can make stacking difficult

### What this means for MFS:

- You cannot start frame N+1 until frame N's readout is complete
- The minimum frame gap = exposure time + readout time + pipeline latency
- A sensor with faster readout allows tighter frame spacing → less motion between frames → easier alignment
- The app should query available frame durations from `StreamConfigurationMap.getOutputSizes()` and `HIGH_SPEED_VIDEO_FPS_RANGE` to estimate burst capability

### Making MFS work across different readout speeds:

The gap between frames should be dynamic:

```
gap = exposureTime + readoutTime + 10ms (pipeline margin)
```

For a 50MP sensor with 30ms readout at 1/120s (~8ms exposure):

```
gap = 8ms + 30ms + 10ms = ~48ms
```

For the same sensor in low light at 1/30s (~33ms exposure):

```
gap = 33ms + 30ms + 10ms = ~73ms
```

The app can compute this dynamically from the current exposure settings and a known or estimated readout time.

---

## 17. Camera2 API vs CameraX for MFS

Origin Camera currently uses CameraX exclusively. MFS may require moving closer to Camera2 or using Camera2Interop.

### CameraX limitations for MFS:

| Feature | CameraX support | Notes |
|---------|----------------|-------|
| Burst capture | Not directly supported | `takePicture()` captures one frame at a time |
| Manual frame timing | Not exposed | CameraX manages capture internally |
| RAW capture | Limited | Depends on OEM support |
| YUV access | Via `ImageAnalysis` | Not the same capture path as `ImageCapture` |
| Capture request control | Limited to what Camera2Interop exposes | Cannot set every Camera2 parameter |
| Frame callback per capture | Via `OnImageCapturedCallback` | Single frame, not burst |

### CameraX strategies for MFS:

**Strategy A: Sequential takePicture calls**

```
repeat(N) {
    imageCapture.takePicture(executor, callback)
    Thread.sleep(gap)
}
```

- **Pros:** stays within CameraX, uses existing `ImageCapture` path
- **Cons:** not truly burst — each `takePicture()` goes through the full capture pipeline, gap control is approximate, may not achieve tight spacing

**Strategy B: ImageAnalysis for frame access**

```
Use ImageAnalysis to receive YUV frames at max rate
Buffer N frames from the analysis stream
Trigger capture separately
```

- **Pros:** tight frame control, continuous stream
- **Cons:** `ImageAnalysis` frames are preview-resolution (usually 1080p), not full sensor resolution. Not suitable for MFS at max resolution.

**Strategy C: Camera2Interop with ImageReader**

```
val imageReader = ImageReader.newInstance(width, height, format, N)
val session = camera.createCaptureSession(...)
// Submit N capture requests with repeat
```

- **Pros:** full burst control, max resolution, tight timing
- **Cons:** requires Camera2 API usage, bypasses CameraX convenience

**Strategy D: Camera2 directly for capture path**

Replace `ImageCapture` with a Camera2 capture session for MFS shots. Keep CameraX for preview and video.

- **Pros:** full control over burst, RAW capture, frame timing
- **Cons:** two camera abstractions in one app, added complexity

### Recommended approach:

**Phase 1:** Start with Strategy A (sequential `takePicture`). It will demonstrate the concept and validate the alignment/merge pipeline. If the gap between frames is too large (expected: 100–300ms), accept it for now — the merge will still improve quality.

**Phase 2:** Move to Strategy C or D for tighter burst control (30–80ms gaps).

---

## 18. Burst Capture on Camera2

If moving to Camera2 for the capture path, here is the burst workflow:

### Step 1: Open camera and create session

```kotlin
val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(device: CameraDevice) {
        cameraDevice = device
        val surfaces = listOf(imageReader.surface)
        device.createCaptureSession(surfaces, sessionCallback, handler)
    }
}, handler)
```

### Step 2: Create burst requests

```kotlin
val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
builder.addTarget(imageReader.surface)
// Set all capture parameters:
builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime)
builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso)
builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)
builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
// etc.
val request = builder.build()

// Create a burst of identical requests
val requests = List(frameCount) { request }
```

### Step 3: Submit burst

```kotlin
session.captureBurst(requests, object : CaptureCallback() {
    override fun onCaptureCompleted(session: CaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
        // Frame captured — available in ImageReader queue
    }
}, handler)
```

### Step 4: Read frames from ImageReader

```kotlin
val images = mutableListOf<Image>()
repeat(frameCount) {
    val image = imageReader.acquireLatestImage()
    if (image != null) images.add(image)
}
// Now pass to alignment + merge
```

### Key Camera2 parameters for burst:

| Parameter | Recommended value | Notes |
|-----------|------------------|-------|
| `CONTROL_CAPTURE_INTENT` | `CAPTURE_INTENT_STILL_CAPTURE` | Full quality |
| `CONTROL_AE_MODE` | `AE_MODE_OFF` (manual) or `AE_MODE_ON` (auto-locked) | Must not change between frames |
| `CONTROL_AE_LOCK` | `true` | Locks auto-exposure |
| `CONTROL_AWB_LOCK` | `true` | Locks white balance |
| `EDGE_MODE` | `EDGE_MODE_OFF` | No sharpening from ISP |
| `NOISE_REDUCTION_MODE` | `NOISE_REDUCTION_MODE_OFF` | No ISP denoising |
| `TONEMAP_MODE` | `TONEMAP_MODE_CONTRAST_CURVE` or `..._GAMMA_VALUE` | Minimal tonemapping |

### Burst vs repeated capture:

- `captureBurst()` submits all requests at once. The camera processes them back-to-back as fast as possible.
- Repeated `capture()` calls have more latency between each request.
- **Use `captureBurst()`** for the tightest possible frame spacing.

---

## 19. YUV vs RAW for Multi-Frame Capture

Both YUV and RAW are useful for MFS. Each has tradeoffs.

### YUV (YUV420_888):

| Aspect | Evaluation |
|--------|-----------|
| **Availability** | Supported on all Camera2 devices |
| **Data size** | 1.5 bytes per pixel (~18 MB for 12MP) |
| **Processing** | Already demosaiced — ready to use |
| **Colour** | 8-bit per channel (limited range) |
| **ISP processing** | YUV from Camera2 may have ISP sharpening/denoising applied (varies by OEM) |
| **JPEG path** | CameraX `ImageCapture` gives JPEG only — to get YUV, use `ImageReader` directly |

**Best for:** Phase 1 (simpler processing). Works with OpenCV kernels directly.

### RAW (RAW10/RAW16/SENSOR_RAW):

| Aspect | Evaluation |
|--------|-----------|
| **Availability** | Not all devices support RAW output |
| **Data size** | 2 bytes per pixel (~50 MB for 12MP Bayer RAW) |
| **Processing** | Needs demosaicing — must implement or use a library |
| **Colour** | 10–16 bit per channel (much wider range) |
| **ISP processing** | Zero ISP processing — true sensor data |
| **Metadata** | Has sensor colour matrices, black level, white balance gains |

**Best for:** maintaining the "pure sensor" philosophy (Phase 3 and beyond).

### Origin Camera choice:

**Start with YUV.** It's simpler, universally supported, and still gives the full benefit of MFS. The ISP processing on YUV frames varies by OEM but is usually minimal if `NOISE_REDUCTION_MODE_OFF` and `EDGE_MODE_OFF` are set.

**Move to RAW later** if the goal is to bypass ISP entirely and have complete control over demosaicing and colour.

### Getting YUV frames from Camera2:

```kotlin
val imageReader = ImageReader.newInstance(
    width, height,
    ImageFormat.YUV_420_888,
    frameCount + 2 // buffer queue size
)
```

### Getting RAW frames from Camera2:

```kotlin
val rawSizes = characteristics.get(
    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
)?.getOutputSizes(ImageFormat.RAW_SENSOR)

val imageReader = ImageReader.newInstance(
    rawSizes[0].width, rawSizes[0].height,
    ImageFormat.RAW_SENSOR,
    frameCount + 2
)
```

---

## 20. What Post-Processing Steps to Add (and Skip)

### Origin Camera's philosophy:

> *"Show the closest representation of what the sensor captured, not what an AI thinks the photo should look like."*

### Approved post-MFS steps:

| Step | Reason | When to add |
|------|--------|-------------|
| **Lens correction** | All phone lenses have geometric distortion and vignetting. Correcting this removes lens artefacts without inventing detail. | Phase 1 or 2 |
| **Light sharpening (edge-aware)** | MFS reduces noise, which allows gentle sharpening without amplifying noise. Use a small-radius unsharp mask. | Phase 2 |
| **Colour correction matrix** | Convert sensor-native colour space to sRGB or Display P3. This is not "processing" — it's a mathematical conversion. | Phase 1 |
| **Chromatic aberration correction** | Remove colour fringing at high-contrast edges. | Phase 2 |

### Excluded steps (permanently):

| Step | Reason for exclusion |
|------|---------------------|
| **HDR tonemapping** | Flattens contrast, lifts shadows unnaturally, creates the "processed" look |
| **AI scene enhancement** | Invents texture that wasn't captured |
| **Beauty/skin smoothing** | Destroys natural texture |
| **Heavy sharpening** | Creates overshoot halos, looks artificial |
| **Aggressive denoising** | Smears fine detail — MFS already handles noise |
| **Night mode multi-exposure** | Different philosophy — Origin is not trying to make night look like day |
| **Semantic segmentation** | Applying different processing to sky vs faces vs grass is the opposite of "one honest pipeline" |

---

## 21. Sharpening After MFS

### Why sharpen at all:

- MFS reduces noise but does not increase acutance (edge sharpness)
- The lens and sensor have inherent softness from the optical low-pass filter (OLPF) and diffraction
- A tiny amount of sharpening restores what the optics attenuated

### Rule for Origin Camera:

> Sharpen enough to restore natural edge contrast. Not enough to create halos.

### Unsharp mask:

```
sharpened = original + (original - blurred) × amount
```

Where:

- `blurred` is a Gaussian blur with radius `r`
- `amount` controls intensity (0.3–1.0)
- `r` should be small — 0.5–1.5 pixels

### Origin Camera recommended parameters:

| Setting | Value |
|---------|-------|
| Algorithm | Unsharp mask (edge-aware, not global) |
| Radius | 0.5–1.0 pixels |
| Amount | 0.3–0.6 |
| Threshold | Apply only where |edge| > noise level |

### What to avoid:

| Bad practice | Why |
|-------------|-----|
| Radius > 2 pixels | Creates visible halos |
| Amount > 1.0 | Overshoot -> unnatural edges |
| Applying to high-ISO images | Amplifies any residual noise |
| Global sharpen (no threshold) | Sharpens noise in flat areas |
| Repeated sharpen in different stages | Creates crunchy, artificial texture |

### Edge-aware sharpening (bilateral filter approach):

```
For each pixel:
  if variance in neighbourhood > noise floor:
    apply unsharp mask
  else:
    leave as-is (flat area, nothing to sharpen)
```

---

## 22. Noise Reduction After MFS

### Is NR needed after MFS?

MFS already provides noise reduction proportional to √N. For 3–5 frames:

- 3 frames: 42% noise reduction
- 5 frames: 55% noise reduction

In bright light, this may be sufficient on its own. In low light, a gentle second-stage NR may help.

### If NR is needed:

| Method | Quality | Detail preservation | Speed |
|--------|---------|--------------------|-------|
| Bilateral filter | Medium | Good | Fast |
| Non-local means | High | Excellent | Slow |
| Wavelet thresholding | High | Very good | Medium |
| BM3D | Very high | Excellent | Very slow |

**For Origin Camera:** bilateral filter with a small spatial radius (3–5) and moderate colour radius (20–40). Apply only to frames that still have visible noise after MFS.

### Test to decide if NR is needed:

```
After MFS merge:
  1. Examine flat areas (sky, wall)
  2. If standard deviation > 2-3 digital levels → apply light NR
  3. If standard deviation < 2 → no NR needed
```

---

## 23. Colour Handling After MFS

### Colour processing in the MFS pipeline:

1. **Input:** YUV_420_888 from Camera2 (or JPEG from CameraX)
2. **Alignment:** Convert to grayscale for motion estimation
3. **Merge:** Apply to all YUV planes (Y, U, V separately) or to RGB
4. **Output:** Convert to sRGB/Display P3 for final JPEG

### Colour consistency across frames:

All frames should have identical white balance. If AWB was locked before the burst, all frames will have the same colour. If AWB was running, each frame may have slightly different colour and cannot be directly averaged.

### White balance lock:

```kotlin
// Camera2
captureRequest.set(CaptureRequest.CONTROL_AWB_LOCK, true)

// CameraX via Camera2Interop
val extras = CaptureRequest.Builder().apply {
    set(CaptureRequest.CONTROL_AWB_LOCK, true)
}.build()
imageCapture.captureRequestOptions +=
    Camera2Interop.Extender(imageCapture).setCaptureRequestOptions(...)
```

### Colour conversion path:

```
YUV → (colour correction matrix from sensor) → linear RGB → (tone curve) → sRGB → JPEG
```

For the "no processing" philosophy, use a minimal tone curve — essentially gamma 2.2 or sRGB transfer function, nothing more. No S-curve, no shadow lift, no highlight compression.

---

## 24. Lens Correction and MFS

All phone camera lenses have:

- **Geometric distortion:** straight lines appear curved (barrel or pincushion)
- **Vignetting:** corners are darker than centre
- **Chromatic aberration:** colour fringing at edges
- **Softness:** corners are less sharp than centre (field curvature)

### When to correct:

| Artefact | Correct before MFS? | Correct after MFS? |
|----------|--------------------|--------------------|
| Geometric distortion | Yes — align on corrected frames | No — distort once after merge |
| Vignetting | Optional — flat-field correct each frame | Yes — one correction on merged output |
| Chromatic aberration | Yes — remove per-frame | No — correct once per frame |
| Corner softness | No — this is intent captured | No — this is real lens character |

### Geometric distortion correction:

```kotlin
// OpenCV approach
val cameraMatrix = Mat.eye(3, 3, CvType.CV_64F)
val distCoeffs = Mat(1, 5, CvType.CV_64F)
// Values from camera calibration or manufacturer data

for (frame in frames) {
    val corrected = Mat()
    Imgproc.undistort(frame, corrected, cameraMatrix, distCoeffs)
    correctedFrames.add(corrected)
}
```

### Flat-field correction for vignetting:

```kotlin
// Capture a flat-field image (uniformly lit white surface)
// Divide each captured frame by the flat-field image
// This corrects for both vignetting and pixel non-uniformity

for (frame in frames) {
    corrected = frame / flatFieldImage * mean(flatFieldImage)
}
```

### Calibration data:

Lens calibration parameters can be obtained from:
- `CameraCharacteristics.LENS_INTRINSIC_CALIBRATION`
- `CameraCharacteristics.LENS_DISTORTION`
- Manual calibration using a checkerboard pattern

---

## 25. Why MFS Changes the Micro-Detail Equation at Zoom

When you zoom digitally:

### Without MFS:

```
Single frame at 1×:
  4000×3000 pixels
  Details occupy 20×20 pixels → recognizable

Single frame at 5× zoom (digital crop):
  Crop to 800×600 pixels from centre
  Details occupy 4×4 pixels → barely recognizable
  Noise occupies 2×2 pixels → competes with detail
  Result: mushy, noisy, low-quality
```

### With MFS:

```
5 frames at 1×:
  Each frame has 4000×3000 pixels
  Noise is random per frame
  Detail is consistent across frames

After MFS merge:
  Noise reduced by √5 ≈ 55%
  Detail remains at full strength
  Signal-to-noise ratio improves

At 5× zoom (digital crop of MFS result):
  Detail occupies 4×4 pixels → now visible through noise
  Cleaner image → sharpen can work without amplifying noise
  Result: noticeably cleaner than single-frame zoom
```

### The key insight:

Zoom does not change the physics of stacking. MFS improves the entire image uniformly. The zoomed region benefits because the base image has higher SNR, so when you crop and enlarge, there's less noise to obscure fine detail.

### Resolution limit reminder:

MFS cannot create detail that the sensor did not capture. If a detail is smaller than the pixel pitch (e.g., a thread 0.5 pixels wide at 5× zoom), no amount of stacking will resolve it. MFS only recovers detail that is at or near the noise floor — not detail beyond the diffraction or Nyquist limit.

---

## 26. What MFS Does NOT Fix

| Issue | Why MFS doesn't help | How to fix |
|-------|---------------------|-----------|
| **Motion blur within a single frame** | Each frame is individually blurred | Shorter exposure time, OIS, or faster lens |
| **Out-of-focus areas** | All frames have the same focus | Refocus, or use a smaller aperture (deeper DOF) |
| **Diffraction softening** | Same across all frames | Smaller aperture → worse. Reduce f-number |
| **Lens optical quality** | Fixed per lens | Lens correction (Section 24) |
| **Colour accuracy** | MFS preserves input colour | Better white balance calibration |
| **Dynamic range** | All frames at same exposure | True HDR bracketing (different exposures) — but Origin skips this |
| **Aliasing / moiré** | Each frame has same aliasing | Optical low-pass filter or anti-alias in post |
| **Compression artefacts** | Each JPEG frame has them | Use less compression, or process RAW instead of JPEG |
| **Under-exposed shadows** | Same exposure on all frames | Could increase ISO or exposure time — but this changes the shot |

---

## 27. What Origin Camera Should NOT Do

Based on the Origin Camera philosophy, the following are permanently excluded:

### Things that make photos look "processed":

- ✗ HDR tonemapping (lifts shadows, flattens contrast)
- ✗ Multi-exposure bracketing (changes exposure between frames)
- ✗ AI super-resolution (invents texture ML models hallucinated)
- ✗ Semantic scene enhancement (different treatment for sky, grass, faces)
- ✗ Beauty filters (skin smoothing, eye brightening)
- ✗ Fake bokeh / portrait mode (depth mapping + blur)
- ✗ Saturation boosting (pushing colours beyond reality)
- ✗ Shadow lifting (making night look like day)

### Things that hurt the "honest camera" identity:

- ✗ Applying different processing per photo mode
- ✗ Scene-adaptive tuning that changes the pipeline behaviour
- ✗ Hidden computational passes the user can't control
- ✗ Metadata stripping (keep full EXIF)
- ✗ Overwriting the user's intentionally chosen exposure

### Things that are bad engineering:

- ✗ Hardcoding 50MP assumption (phones vary from 12MP to 200MP)
- ✗ Using a fixed frame count for all sensors (see Section 9)
- ✗ Loading all frames into memory with no fallback (see Section 14)
- ✗ Applying heavy processing without user feedback

---

## 28. Comparison: Origin Philosophy vs GCam Philosophy

| Aspect | GCam | Origin Camera |
|--------|------|---------------|
| **Capture** | 9–15 frames burst | 3–5 frames (adaptive) |
| **Exposure** | Multi-exposure HDR+ | Single exposure, locked |
| **Merge** | Sophisticated multi-frame, multi-scale | Average or weighted average |
| **Tone mapping** | Aggressive HDR tonemapping | Minimal gamma/sRGB curve |
| **Sharpening** | Strong, edge-aware | Light, only to restore optics |
| **Colour** | Google-tuned colour science | Sensor-native, accurate |
| **Denoise** | Heavy, AI-assisted | Only what MFS provides |
| **Detail** | AI-enhanced super-resolution | Only real captured detail |
| **Result** | Polished, social-media-ready | Natural, photographer-oriented |
| **Goal** | Make every photo look great | Show what the sensor captured |

**Origin Camera is not trying to beat GCam at its own game.** The app is choosing a different target: honest sensor output with minimal but smart processing.

---

## 29. Prototype Roadmap

### Phase 1: Proof of concept (3-frame stacking)

**Goal:** Validate the pipeline works and shows visible improvement.

**Duration:** 1–2 weeks.

**Deliverables:**

- [ ] 3-frame burst capture (sequential CameraX takePicture)
- [ ] YUV frame storage in memory
- [ ] Simple alignment (translation-only, phase correlation)
- [ ] Average merge
- [ ] Output saved as JPEG
- [ ] Visual comparison tool (MFS vs single frame)

### Phase 2: Production-ready (5-frame weighted stacking)

**Goal:** Make it fast, robust, and usable for photography.

**Duration:** 3–4 weeks.

**Deliverables:**

- [ ] Camera2 burst capture (30–50ms gap)
- [ ] 5 frames in bright light, dynamic count in low light
- [ ] Feature-based alignment (ORB + homography)
- [ ] Weighted merge (quality-based per frame)
- [ ] Light edge-aware sharpening
- [ ] Lens geometric distortion correction
- [ ] Progress indicator in UI
- [ ] Settings toggle (MFS on/off)

### Phase 3: Adaptive, full-resolution pipeline

**Goal:** Handle all sensors, all light levels, all scenarios.

**Duration:** 4–6 weeks.

**Deliverables:**

- [ ] Dynamic frame count by sensor MP and light level
- [ ] Pixel-wise weighted merge (motion-aware)
- [ ] RAW capture path (optional, for advanced users)
- [ ] Ghost reduction
- [ ] Per-pixel adaptive merge
- [ ] Chromatic aberration correction
- [ ] Colour calibration per sensor
- [ ] Fallback to single frame on failure

---

## 30. Phase 1: 3-Frame Stacking Prototype

### Full implementation plan:

#### Step 1: Capture 3 frames

Using sequential `ImageCapture.takePicture()` calls:

```kotlin
suspend fun captureBurst(): List<ImageProxy> {
    val images = mutableListOf<ImageProxy>()
    val latch = CountDownLatch(3)

    for (i in 0 until 3) {
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    images.add(image)
                    latch.countDown()
                }
                override fun onError(exception: ImageCaptureException) {
                    latch.countDown()
                }
            }
        )
        delay(100) // 100ms gap for phase 1 (will tighten later)
    }

    latch.await(2, TimeUnit.SECONDS)
    return images
}
```

#### Step 2: Convert ImageProxy to ByteArray

```kotlin
fun imageProxyToByteArray(image: ImageProxy): ByteArray {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}
```

#### Step 3: Alignment (translation-only, phase correlation)

Use OpenCV or a simple FFT-based approach:

```kotlin
fun alignFrame(reference: Mat, target: Mat): Mat {
    val refGray = Mat()
    val tgtGray = Mat()
    Imgproc.cvtColor(reference, refGray, Imgproc.COLOR_RGB2GRAY)
    Imgproc.cvtColor(target, tgtGray, Imgproc.COLOR_RGB2GRAY)

    val shift = phaseCorrelate(refGray, tgtGray)
    val translation = Mat(2, 3, CvType.CV_64F).apply {
        put(0, 0, 1.0, 0.0, shift.x)
        put(1, 0, 0.0, 1.0, shift.y)
    }

    val aligned = Mat()
    Imgproc.warpAffine(target, aligned, translation,
        target.size(), Imgproc.INTER_LINEAR)
    return aligned
}
```

#### Step 4: Average merge

```kotlin
fun averageMerge(frames: List<Mat>): Mat {
    val result = Mat.zeros(frames[0].size(), frames[0].type())
    for (frame in frames) {
        Core.add(result, frame, result)
    }
    Core.divide(result, frames.size.toDouble(), result)
    return result
}
```

#### Step 5: Save

```kotlin
fun saveResult(mat: Mat, uri: Uri) {
    val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, bitmap)
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    }
}
```

### Verification:

- Capture same scene with MFS on and off
- Compare zoomed crops side by side
- Measure noise standard deviation in flat areas
- Verify no ghosting in static scenes

---

## 31. Phase 2: 5-Frame Weighted Stacking

### Improvements over Phase 1:

| Aspect | Phase 1 | Phase 2 |
|--------|---------|---------|
| Frame count | 3 | 5 (or dynamic) |
| Capture method | CameraX sequential | Camera2 burst |
| Frame gap | ~100ms | 30–50ms |
| Alignment | Translation (phase correlation) | Homography (ORB + RANSAC) |
| Merge | Average | Weighted by sharpness |
| Sharpening | None | Light unsharp mask |
| Lens correction | None | Geometric distortion |

### Weighted merge implementation:

```kotlin
fun weightedMerge(frames: List<Mat>): Mat {
    val weights = frames.map { frame ->
        val laplacian = Mat()
        Imgproc.Laplacian(frame, laplacian, CvType.CV_64F)
        val mean = Core.mean(laplacian)
        mean.`val`[0] // variance of Laplacian = sharpness score
    }

    // Normalize weights to sum to 1.0
    val total = weights.sum()
    val normWeights = weights.map { it / total }

    // Weighted sum
    val result = Mat.zeros(frames[0].size(), frames[0].type())
    for ((i, frame) in frames.withIndex()) {
        Core.addWeighted(result, 1.0, frame, normWeights[i], 0.0, result)
    }
    return result
}
```

### Homography alignment:

```kotlin
fun homographyAlign(reference: Mat, target: Mat): Mat {
    val refGray = Mat()
    val tgtGray = Mat()
    Imgproc.cvtColor(reference, refGray, Imgproc.COLOR_RGB2GRAY)
    Imgproc.cvtColor(target, tgtGray, Imgproc.COLOR_RGB2GRAY)

    val orb = ORB.create()
    val refKp = MatOfKeyPoint()
    val tgtKp = MatOfKeyPoint()
    val refDesc = Mat()
    val tgtDesc = Mat()

    orb.detectAndCompute(refGray, Mat(), refKp, refDesc)
    orb.detectAndCompute(tgtGray, Mat(), tgtKp, tgtDesc)

    val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
    val matches = MatOfDMatch()
    matcher.match(refDesc, tgtDesc, matches)

    // Filter good matches by distance
    val goodMatches = matches.toList().filter { it.distance < 50 }

    // Compute homography
    val refPts = MatOfPoint2f()
    val tgtPts = MatOfPoint2f()
    // ... convert KeyPoint to Point2f ...

    val homography = Calib3d.findHomography(tgtPts, refPts, Calib3d.RANSAC, 5.0)

    val aligned = Mat()
    Imgproc.warpPerspective(target, aligned, homography, reference.size())
    return aligned
}
```

---

## 32. Phase 3: Full Adaptive Pipeline

### Architecture:

```
CameraSystem (Camera2 capture)
  │
  ├── Light level estimator
  │     ↓
  ├── Frame count calculator (adaptive)
  │     ↓
  ├── Burst capture (N frames, locked settings)
  │     ↓
  ├── Raw frame buffer
  │     ↓
  ├── Alignment (downscale → compute → apply to full res)
  │     ↓
  ├── Per-pixel motion detection
  │     ↓
  ├── Adaptive merge (weighted, motion-aware)
  │     ↓
  ├── Lens correction
  │     ↓
  ├── Light sharpening (edge-aware, thresholded)
  │     ↓
  └── Colour conversion + JPEG save
```

### Adaptive frame count (full implementation):

```kotlin
enum class LightLevel { BRIGHT, NORMAL, LOW, VERY_LOW }

data class MfsConfig(
    val frameCount: Int,
    val frameGapMs: Int,
    val mergeStrategy: MergeStrategy,
    val sharpenStrength: Float
)

fun computeMfsConfig(
    sensorMP: Float,
    lightLevel: LightLevel,
    zoomFactor: Float
): MfsConfig {
    val baseFrames = when (lightLevel) {
        LightLevel.BRIGHT -> 3
        LightLevel.NORMAL -> 5
        LightLevel.LOW -> 8
        LightLevel.VERY_LOW -> 16
    }

    val referenceMP = 12f
    val megaPxScaled = (baseFrames * (referenceMP / sensorMP)).roundToInt()
        .coerceIn(1, 16)

    // Zoom increases needed frames slightly
    val zoomScaled = if (zoomFactor > 2f) {
        (megaPxScaled * 1.5).roundToInt()
    } else {
        megaPxScaled
    }.coerceIn(1, 16)

    val gapMs = when {
        lightLevel == LightLevel.VERY_LOW -> 100
        lightLevel == LightLevel.LOW -> 80
        sensorMP > 50 -> 80
        sensorMP > 20 -> 50
        else -> 40
    }

    return MfsConfig(
        frameCount = zoomScaled,
        frameGapMs = gapMs,
        mergeStrategy = if (zoomScaled >= 5) MergeStrategy.WEIGHTED else MergeStrategy.AVERAGE,
        sharpenStrength = when {
            lightLevel == LightLevel.VERY_LOW -> 0.2f
            zoomFactor > 3f -> 0.5f
            else -> 0.3f
        }
    )
}
```

### Light level estimation:

```kotlin
fun estimateLightLevel(aeResult: CaptureResult): LightLevel {
    val sensitivity = aeResult.get(CaptureResult.SENSOR_SENSITIVITY) ?: 100
    val exposureTime = aeResult.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 1000000L

    // Higher ISO + longer exposure = darker scene
    val effectiveExposure = sensitivity.toFloat() * exposureTime / 1000f

    return when {
        effectiveExposure < 100f -> LightLevel.BRIGHT
        effectiveExposure < 500f -> LightLevel.NORMAL
        effectiveExposure < 2000f -> LightLevel.LOW
        else -> LightLevel.VERY_LOW
    }
}
```

---

## 33. Testing and Validation Strategy

### Unit tests:

- Frame alignment correctness (known shifts)
- Merge algorithms (expected output for known inputs)
- Weight calculation (weights sum to 1.0, frame order correct)
- Frame count calculator (correct values for each sensor MP)

### Integration tests:

- Full pipeline on device (3 frames → aligned → merged → saved)
- Compare file sizes (MFS vs single frame)
- Measure capture time (burst + processing total)

### Quality metrics:

| Metric | What it measures | How to measure |
|--------|-----------------|---------------|
| **SNR improvement** | Noise reduction | Compare std dev in flat region of single vs MFS |
| **MTF50** | Sharpness at 50% contrast | Slanted-edge test chart |
| **Ghosting score** | Visibility of motion artefacts | Visual inspection + edge variance in known motion regions |
| **Alignment error** | How well frames register | RMS difference between reference and aligned frame |
| **Colour accuracy** | ΔE (colour difference) | ColorChecker chart |
| **Processing time** | Pipeline speed | System.nanoTime() before/after |

### Comparison shots:

For every test scene, capture:

1. Single frame (Origin Camera, no processing)
2. MFS 3-frame (Origin Camera)
3. MFS 5-frame (Origin Camera)
4. Stock camera app (same scene)
5. GCam (if available)

Compare at 100% zoom and digital zoom crops.

### Automated test scenes:

| Scene | What it tests |
|-------|--------------|
| Bright outdoor building | Detail, sharpness, colour |
| Indoor with text | Texture preservation |
| Low light bookshelf | Noise reduction, colour accuracy |
| Zoomed fine detail (fabric, leaves) | Micro-detail recovery (primary goal) |
| Moving car (slow) | Ghosting resistance |
| Portrait with hair detail | Edge handling |
| Uniform wall | Noise measurement |

---

## 34. Sensor-Specific Tuning

Every phone sensor behaves differently. Origin Camera should tune per sensor if possible.

### What varies per sensor:

| Parameter | Why it varies |
|-----------|--------------|
| **Readout speed** | Determines minimum frame gap |
| **Noise profile** | Different sensors have different read noise, dark current |
| **Colour filter array** | Bayer pattern (RGGB, RYYB, etc.) affects colour |
| **Lens distortion** | Different lenses have different distortion characteristics |
| **Vignetting** | Varies with lens design |
| **Pixel pitch** | Larger pixels = more light = less noise per pixel |
| **ISO range** | Native ISO, gain stages differ |

### How to discover sensor characteristics at runtime:

```kotlin
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Sensor resolution
val pixelArraySize = characteristics.get(
    CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
)

// Physical size → pixel pitch
val physicalSize = characteristics.get(
    CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
)
val pixelPitch = physicalSize.width / pixelArraySize.width

// Available sensitivity range
val sensitivityRange = characteristics.get(
    CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
)

// Readout speed estimated from available frame durations
val streamConfigMap = characteristics.get(
    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
)
val minFrameDuration = streamConfigMap?.getOutputMinFrameDuration(
    ImageFormat.YUV_420_888,
    Size(pixelArraySize.width, pixelArraySize.height)
)
```

### Storing sensor profiles:

```kotlin
data class SensorProfile(
    val sensorName: String,          // e.g., "SONY IMX989"
    val readoutTimeNs: Long,         // nanoseconds
    val noiseModel: NoiseModel,      // read noise, dark current
    val colourMatrix: FloatArray,    // 3×3 colour correction
    val lensDistortion: FloatArray,  // distortion coefficients
    val optimalSharpening: Float     // per-sensor sharpen amount
)
```

Start with automatic detection and reasonable defaults. Add manual profiles for known sensors as you test on more devices.

---

## 35. Edge Cases and Failure Modes

### What can go wrong and how to handle it:

| Failure mode | Symptom | Detection | Handling |
|-------------|---------|-----------|----------|
| **Alignment fails** | Merged image has double edges | Low feature match count or high homography residual | Fall back to single frame |
| **Motion too large** | Ghosting | High variance across frames in too many pixels | Reduce frame count, use per-pixel weighting |
| **Exposure changes mid-burst** | Frames have different brightness | Mean brightness differs by >30% | Discard outlier frames, re-merge |
| **Camera moves too much** | Large shift between frames | Translation >10% of image size | Discard burst, request re-capture |
| **Subject blinks in portrait** | One frame has eyes closed | Face detection across frames | Exclude blinking frame from merge |
| **Memory pressure** | App crashes on large sensor | `RuntimeException` from ImageReader | Reduce frame count, use file-backed storage |
| **Phone overheats** | Throttling, slow processing | Temperature API | Fall back to single frame until cool |
| **Fast-changing scene** | All frames different | Variance > threshold everywhere | Use single frame with good settings |
| **Flash fires mid-burst** | One frame much brighter | Flash fire detection | Exclude flash frame |
| **Sensor readout fails** | One frame is corrupt | Image quality check (mean, std dev) | Retry capture |

### Fallback chain:

```
Full MFS (N frames)
  → MFS reduced frames (N-2)
    → 3-frame MFS
      → Single frame with light processing
        → Single frame raw output
```

### User-facing fallback:

If MFS detects it cannot produce a good result, save a single frame instead. The user should never see a "worse" image because MFS was attempted.

### Quality gate before saving:

```kotlin
fun shouldUseMfsOutput(merged: Mat, singleFrame: Mat): Boolean {
    val mergedSharpness = computeSharpness(merged)
    val singleSharpness = computeSharpness(singleFrame)
    val mergedNoise = computeNoise(merged)
    val singleNoise = computeNoise(singleFrame)

    return mergedSharpness >= singleSharpness * 0.9 &&
           mergedNoise <= singleNoise * 1.1
}
```

Only save the MFS result if it is measurably at least as good as the single frame.

---

## 36. Glossary

| Term | Definition |
|------|-----------|
| **Acutance** | Perceived edge sharpness. Higher acutance = crisper edges. |
| **Alignment (registration)** | The process of shifting/transforming multiple images so the same scene content overlaps at the same pixel coordinates. |
| **Burst capture** | Taking multiple frames in rapid succession with identical settings. |
| **Demosaicing** | Converting Bayer pattern RAW data (R, G, B pixels sampled at different locations) to full RGB values per pixel. |
| **Ghosting** | A visual artefact where moving objects appear as semi-transparent duplicates in a merged image. |
| **Homography** | An 8-parameter transformation that maps one plane to another, correcting perspective differences between frames. |
| **ISP** | Image Signal Processor — hardware or firmware that processes raw sensor data into viewable images. Usually applies demosaicing, white balance, denoising, sharpening, and colour conversion. |
| **MFS** | Multi-Frame Stacking — the technique of capturing and merging multiple frames. |
| **Noise** | Random variation in pixel values. Types: shot noise (photon counting statistics), read noise (sensor electronics), dark current noise (thermal), quantisation noise (ADC rounding). |
| **OLPF** | Optical Low-Pass Filter — a filter placed in front of the sensor to reduce moiré and aliasing, at the cost of slight sharpness. |
| **Phase correlation** | A frequency-domain method for estimating the translational shift between two images using the cross-power spectrum. |
| **RANSAC** | RANdom SAmple Consensus — an iterative algorithm to estimate model parameters (e.g., homography) robustly in the presence of outliers. |
| **Rolling shutter** | The sensor readout method where rows are exposed and read sequentially, causing geometric distortion of fast-moving objects. |
| **Signal-to-noise ratio (SNR)** | The ratio of meaningful signal to random noise. Higher SNR = cleaner image. |
| **Unsharp mask** | A sharpening technique that adds the difference between the original and a blurred version back to the original. |
| **Vignetting** | Darkening of image corners compared to the centre, caused by the lens optics. |
| **YUV 420** | A colour encoding where the Y (luminance) plane has full resolution, and the U/V (chrominance) planes are subsampled 2× in each direction. |

---

## 37. References for Further Study

### Academic papers:

- *Multi-Image Super-Resolution Using Deep Learning* — if you want to go beyond stacking into true SR
- *A Mathematical Analysis of Burst Photography* — theory of noise reduction through frame averaging
- *Block-Matching and 3D Filtering (BM3D)* — state of the art in image denoising
- *Phase Correlation Based Image Alignment* — foundation for translation-only alignment

### Open-source camera projects:

| Project | What to study |
|---------|--------------|
| **Open Camera** | Camera2 usage, manual controls, RAW capture |
| **GrapheneOS Camera** | Privacy-focused pipeline, minimal processing |
| **Simple Camera** | Clean CameraX implementation |
| **Aperture** (KDE) | Full Camera2 pipeline with control sliders |
| **GCam ports** | Study the burst behaviour and alignment (no source code but observable) |

### Libraries:

| Library | Use |
|---------|-----|
| **OpenCV Android** | Alignment, merge, sharpening, lens correction |
| **AndroidX Camera2** | Burst capture, ImageReader, capture requests |
| **libyuv** (Google) | Fast YUV conversion, scaling, rotation |
| **libultrahdr** (Google) | Ultra HDR gain map encoding (already in the project) |
| **RenderScript / Vulkan** | GPU-accelerated image processing |

### Practical guides:

- *Android Camera2 API: Burst Capture* — developer.android.com
- *OpenCV: Feature Matching + Homography* — OpenCV documentation
- *Computational Photography: Multi-Frame Denoising* — MIT OpenCourseWare
- *Sensor Noise Models for Image Processing* — various sensor datasheets and application notes

---

> *"The best camera app doesn't make the photo look like something it's not. It gives the sensor a voice and stays out of the way."*
>
> — Origin Camera Philosophy
