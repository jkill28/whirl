# Silver & ember

Approved light and dark direction, implemented 22 October 2026. This replaces the earlier flat grey/orange treatment in `grey-orange.md`.

## Colour roles

| Role | Light | Dark |
| --- | --- | --- |
| Page canvas | `#ECEFF2` | `#15191F` |
| Reading panel | `#F4F5F7` | `#232A33` |
| Recessed controls / author header | `#E3E7EC` | `#2B3440` |
| Main text | `#252A32` | `#EEF1F4` |
| Metadata | `#58616D` | `#B5BEC9` |
| Divider / outline | `#D4DAE1` | `#3B444F` |
| Orange accent | `#A63D0B` | `#FFAA72` |
| Selected control / badge | `#F4DFD1` | `#493020` |
| Unread row | `#F1ECE8` | `#2E2B29` |

Every light surface is a shade of very light grey. Warm surfaces identify selection and unread activity. Errors retain Material's semantic error colours. Wallpaper colours remain an explicit setting; the unread fill derives from that palette when enabled.

## Components

- Thread lists: one continuous rounded panel per forum, 18dp outer corners, 16dp side gutters, subtle outlines and inset dividers. Rows retain individual lazy items and their original stable keys.
- Forum headings: compact charcoal text with a small orange list icon; forum index and settings headings use a matching orange marker.
- Unread rows: warm fill, semibold title, 3dp leading accent and a softly filled new-post count. The count retains the full accessible label, such as “3 unread posts”.
- Watched filters: native selectable tabs inside a grey rounded track, with a warm selected pill and an unread-thread count.
- Typography: 23sp semibold screen titles, semibold section headings, 16sp thread titles and quieter 12sp metadata, with Android font scaling preserved.
- Posts: individually rounded panels, a deeper author header, and recessed quote blocks with an orange or neutral leading rule. Links, quote navigation and spoilers retain their original interactions.
- Settings and forum index: grouped grey surfaces and restrained native elevation.

Navigation, pagination, original fixture content, watch actions, long-press menus, settings and readable-width limits are preserved. The concept's illustrative activity is not copied into the app.

## Verification

`ANDROID_HOME=/home/jules/Android/sdk ./gradlew lint test assembleDebug` passes with the available Java 17 JDK: 236 tests, no failures, errors or skipped tests. Existing contrast checks cover both themes, all grey elevation roles, unread fills and selected controls at a minimum of 4.5:1. Existing row accessibility tests include custom actions, unread-count speech and 200% font scaling.

The actual debug APK was installed on the local Android 16 emulator (1080 × 2400). The preview uses an invalid API key and repository fixtures in the emulator cache, with networking disabled for the preview package. Screenshots are native `adb exec-out screencap -p` captures, not browser mockups.

| Screen / interaction | Evidence |
| --- | --- |
| Watched All, light | `silver-ember-light.png` |
| Watched All, dark; switched through Settings → Theme | `silver-ember-dark.png` |
| Watched Unread, dark; selected filter changes actual rows | `silver-ember-unread-dark.png` |
| Grouped settings, light | `silver-ember-settings.png` |
| Grouped forum index, dark | `silver-ember-forums.png` |
| Cached thread page 2, dark; post and quote treatment | `silver-ember-thread.png` |

The thread screenshot includes the expected offline-refresh banner while cached posts remain usable. Runtime logs showed no AndroidRuntime errors during these checks. A physical phone, live Whirlpool requests, maximum system text size and landscape were not manually exercised for this change. This is a local debug build; no release was published.

A separate navigation edge case was observed after delivering a thread link to the running app and pressing system Back: the host became blank until the app restarted. Navigation files are unchanged in this design update. The underlying back-stack state was not captured, so its cause remains unconfirmed; the native back callbacks currently ignore a failed `popBackStack()` result. This needs a separate navigation investigation.
