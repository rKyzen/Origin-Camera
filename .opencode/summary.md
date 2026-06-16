# Origin Camera Project Summary

## Goal
Build and polish a custom camera UI for Origin Camera with smooth zoom slider, device level indicator, wake lock, toolbar consistency, dynamic lens labels, live camera preview in filters, and volume-button zoom.

## Constraints & Preferences
- Match strict dark theme across all screens (black bg, `#CC1C1C1C` cards, shared `BottomToolbar`)
- Level indicator must NOT spin 360° or flicker; show small arrows for correction direction
- Toolbar must be in the same position on all screens (now at bottom center)
- Lens label must show actual focal length (base × zoom ratio)
- Filters screen must show the *live* camera preview (not a placeholder), with the same corner brackets + level indicator as the camera viewfinder
- Volume buttons control zoom, not capture or navigation
- Gallery/filters open as overlays on top of the camera feed (not separate navigation destinations)

## Progress
### Done
- **Toolbar clickable callbacks**: `BottomToolbar`, `PreviewLayout`, `LayoutWrapper`, `ContentScreen`, `PreviewScreen` all accept `onGalleryClick`/`onFiltersClick`/`onSettingsClick`. Piped through `PreviewNavigation`.
- **Volume buttons → zoom**: Volume up/down step zoom by 0.15× via `Modifier.onKeyEvent` + `nativeKeyEvent`. Removed old Activity `onKeyDown` for VOLUME_UP → settings. Cleaned up `onVolumeUpPressed` plumbing from `MainActivity`, `JcaApp`, and navigation.
- **Routes added**: `GALLERY_ROUTE` and `FILTERS_ROUTE` in Routes.kt; composable destinations wired in `JcaApp.kt` with sample `FilterItem` data.
- **FiltersScreen rewritten** to match reference image:
  - Viewfinder shrunk to 280dp height, `Color.Black` background (no green placeholder)
  - Reuses `CornerBrackets` + `LevelIndicator` from `CaptureLayout` (made public)
  - Accepts `viewfinder` composable parameter for live camera feed
  - Filter cards: `#1C1C1C` background, 100dp height, 90dp thumbnails, 17sp title / 13sp subtitle
  - "Add Color Filter" button: white pill, black text, full width
- **Overlay approach**: Gallery and filters now open as transparent overlays inside `ContentScreen` (not navigation). Camera `viewfinderContent` composable extracted and passed directly to `FiltersScreen`, keeping the camera feed visible underneath the overlay.
- **`CornerBrackets` and `LevelIndicator` made public** in `CaptureLayout.kt` for reuse in `FiltersScreen`.
- **Toolbar position consistent**: Camera toolbar moved to `Alignment.BottomCenter` (was `TopCenter`), matching filters screen toolbar position at bottom.

### In Progress
- (none)

### Blocked
- (none)

## Key Decisions
- **Overlay over navigation**: Gallery/filters rendered as overlays in `ContentScreen` rather than separate navigation destinations. This keeps the camera controller running and allows the live `PreviewDisplay` to be passed to `FiltersScreen` as a composable parameter.
- **Volume zoom via Activity + Compose**: Removed Activity-level `onKeyDown` for VOLUME_UP, used `Modifier.onKeyEvent` + `nativeKeyEvent` to intercept both volume keys at the Compose level before `CaptureButtonComponents` unhandled listener fires.
- **`BottomToolbar` bottom-aligned on all screens**: Changed camera screen from `Alignment.TopCenter` to `Alignment.BottomCenter` so the toolbar doesn't jump when toggling between camera and filter overlays.
- **`rawOrientationDegreesFlow` for level indicator**: Separate flow avoids breaking camera rotation snapping logic; two `OrientationEventListener` instances are acceptable.
- **Dynamic long focal length (buildString)**: Lens label now shows `roundToInt(baseFocalLength * currentZoom)` instead of static "13 MM" / "23 MM".

## Next Steps
- Replace placeholder thumbnail colors in FiltersScreen with actual cached camera frames
- Add actual photo capture and storage for GalleryScreen
- Consider fetching real `LENS_INFO_AVAILABLE_FOCAL_LENGTHS` from `Camera2CameraInfo` for more accurate focal length display

## Critical Context
- Build command: `./gradlew :ui:components:capture:compileStableDebugKotlin` and `./gradlew :feature:preview:compileStableDebugKotlin`
- OrientationEventListener gives 0-359°, ORIENTATION_UNKNOWN = -1 (device flat)
- Compose BOM `2025.10.01`, Kotlin 2.2.0
- `Modifier.onKeyEvent` + `nativeKeyEvent` used for volume zoom (Compose `KeyEvent.type`/`KeyEvent.key` are NOT available in this version — use `event.nativeKeyEvent` instead)
- `CornerBrackets` and `LevelIndicator` are now public in `CaptureLayout.kt`
- `rawOrientationDegreesFlow` is in `com.google.jetpackcamera.ui.components.capture` package
- `ContentScreen` wraps `LayoutWrapper` + overlay `when` block in a `Box(modifier = modifier)` — need to ensure the closing `}` for Box is present before ContentScreen's closing `}` (fixed a missing brace)

## Relevant Files
- `ui/components/capture/src/main/java/com/google/jetpackcamera/ui/components/capture/CaptureLayout.kt`: Main layout — `PreviewLayout`, `LevelIndicator` (public), `HorizontalZoomSlider`, `BottomToolbar`, `CornerBrackets` (public), `BracketColor` (public)
- `ui/components/capture/src/main/java/com/google/jetpackcamera/ui/components/capture/FiltersScreen.kt`: Filters overlay — shrunken viewfinder with live camera preview, filter cards, "Add Color Filter" button, reusable `BottomToolbar`
- `ui/components/capture/src/main/java/com/google/jetpackcamera/ui/components/capture/GalleryScreen.kt`: Gallery overlay — 3-column photo grid
- `feature/preview/src/main/java/com/google/jetpackcamera/feature/preview/PreviewScreen.kt`: Overlay logic in `ContentScreen` (`overlayScreen` state, `viewfinderContent` extraction, `FiltersScreen`/`GalleryScreen` composable overlays), volume key zoom in `LayoutWrapper`
- `feature/preview/src/main/java/com/google/jetpackcamera/feature/preview/navigation/PreviewNavigation.kt`: Added `onNavigateToGallery`/`onNavigateToFilters` parameters
- `app/src/main/java/com/google/jetpackcamera/MainActivity.kt`: Removed `onKeyDown` for VOLUME_UP → settings, removed `onVolumeUpPressedListener`
- `app/src/main/java/com/google/jetpackcamera/ui/JcaApp.kt`: Removed `onVolumeUpPressed` plumbing, added `GALLERY_ROUTE`/`FILTERS_ROUTE` composable destinations with slide-from-bottom transitions
- `app/src/main/java/com/google/jetpackcamera/ui/Routes.kt`: Added `GALLERY_ROUTE` and `FILTERS_ROUTE` constants
