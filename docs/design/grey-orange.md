# Grey and orange theme

Reference: `grey-orange-concept.png` (built-in Image Gen). Preserve the native Android app, screen hierarchy, copy, navigation, list density, Material icons, accessibility and behaviour. The concept guides palette and surface treatment; retain the actual pagination controls and fixture data rather than the concept's invented page tabs, chevrons and example metadata.

## Tokens and components

- Light: background #F5F5F5, surface #FAFAFA, primary text #1B1B1B, secondary text #595959, accent #AD3F00, accent container #FFE2CC.
- Dark: background #121212, surface #181818, primary text #F2F2F2, secondary text #BDBDBD, accent #FF9B59, accent container #48250F.
- Elevation uses neutral grey steps, with no Material default purple or wallpaper tint by default. Explicit wallpaper colour opt-in remains available.
- Preserve Material typography, shapes, spacing, icon geometry and touch targets. No new raster UI assets, cards or navigation.
- Active page controls, buttons, links, favourite stars and drawer selection use orange. Metadata, separators and secondary quotes use grey. Error states retain semantic red.
- Launcher: existing speech-bubble spiral geometry, orange strokes on graphite. Splash: matching graphite.

## Verification

Use native Android build/tests and emulator screenshots. Browser testing does not apply to this Kotlin/Compose app. Compare layout, typography, palette, icons, copy and list density against the concept and upstream screenshots. Light/dark, large text and the optional wallpaper setting need device checks.

## Verification result — 22 October 2026

`./gradlew lint test assembleDebug` passed: 236 tests, zero failures. Added contrast checks cover normal/accent text across elevated surfaces in both modes (at least 4.5:1). The first check caught insufficient light accent contrast; #AD3F00 fixes it. The settings regression test now checks that wallpaper colours are opt-in.

Native Android 16 emulator, 1080 × 2400: launched the actual debug APK, seeded only the emulator's cache from the repository's API fixtures, with an invalid preview key and networking disabled. No production account or live server changes. Switched Unread → All, opened the drawer, navigated to Settings, selected Dark in the real theme chooser, and returned to the list. Screenshots use `adb exec-out screencap -p`. Browser/IAB testing is inapplicable to a native Android app. Screenshots and the concept were inspected with `view_image`.

| Comparison | Evidence and result |
| --- | --- |
| Palette | `light-preview.png` and `dark-preview.png`: neutral grey elevations and orange active tabs/headers; no fallback purple or wallpaper tint. |
| Text | Existing Roboto/Material typography, title hierarchy and metadata sizes retained. Neutral secondary text stays readable. |
| Layout | Full-width list rows and dividers retained; no cards or replacement navigation. Native viewport verified at 1080 × 2400. |
| Icons | Existing Material glyphs and touch targets retained. `drawer-preview.png` shows orange selection with grey inactive entries. |
| Copy | Source diff adds no visible app copy. Original labels, titles and fixture content retained; no invented concept metadata copied. |
| Interaction | Unread/All changes actual rows and indicator. Drawer opens Settings. Theme chooser changes the entire screen immediately. |
| Unread state | The default red Material badge was replaced with primary/onPrimary orange so it represents unread activity consistently. Errors retain red. |

The implementation follows the concept's grey/orange visual direction, with intentional deviations to preserve the existing app: actual navigation/pagination, original typography/list density, and real fixture content. The light accent is slightly darker for contrast. The two-phone concept is an art-direction reference rather than a pixel-matched native screenshot. No remaining theme mismatch was observed on the inspected surfaces. Live Whirlpool requests, a physical phone, landscape and maximum text-size manual checks were not performed. Historical upstream store screenshots/PNG listing assets remain the original release records; the icon generator and its HTML sources now use the new palette. No release was published.

Concept generated with the built-in Image Gen tool. Brief: preserve Whirl's native forum list and Material controls; render light/dark grey surfaces with orange active controls, neutral metadata, and no new navigation or cards.
