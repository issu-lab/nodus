# Development and publication artwork

## Android build

The current source version is **0.1.21**, package `com.nodus.launcher`, minimum
SDK 26, target/compile SDK 34. Use JDK 17 and Android Build-Tools 34.0.0.
The Gradle wrapper selects Gradle 8.4. Build and inspect with:

```sh
./gradlew :app:assembleDebug :app:lintDebug
python3 tests/run_notification_tests.py
python3 tests/check_localization.py
```

The Python runner uses the already cached Kotlin 1.9.0 compiler; first build the
Android project to populate the cache. For a separate cache:

```sh
python3 tests/run_notification_tests.py --gradle-cache /path/to/gradle/caches
```

It runs 14 notification, 20 home/calendar/layout, 27 home interaction/music/destination and
9 app drawer policy and 7 battery reading checks
without an Android runtime.
They cover stale notifications, reply/removal ordering, failures, app selection,
limits, usage events, reserved gesture edges, swipe direction/thresholds, music
metadata cleanup, fixed placement, compact spacing, widget dismissal/return,
continuous drawer progress, release distance/velocity and calendar boundaries in three time
zones. They do not test PendingIntent, RemoteInput, permissions or touch gestures
on Android.

The last implementation check passed debug assembly and Lint (zero errors,
4 existing resource/icon warnings (offline check)) in an isolated build copy. The local debug APK in `dist/` has a
SHA-256 companion file and is excluded from source commits. The home refinements
produce a separate 0.1.18 debug APK; the previous APK remains available.

## Architecture

| Area | Responsibility |
|---|---|
| `ui/home/` | Home screen, app refresh and suggestions |
| `ui/home/HomeHeader.kt` | Shared real/sample header, independent sizes and classic/super compact arrangements and proportional single-line fitting |
| `home/HomePolicy.kt` | Pure rules for app selection, limits, usage events, calendar and placement |
| `home/HomeInteractionPolicy.kt` | Swipe geometry and music metadata cleanup |
| `home/AppDrawerPolicy.kt` | Continuous progress and density-independent distance/velocity decisions |
| `ui/home/HomeGestures.kt` | Upward drag tracking, velocity, cancellation and reserved system edges |
| `ui/home/AppDrawerState.kt` | Frame-synchronous drag updates and interruptible spring settling |
| `ui/home/AppDrawer.kt` | Sliding overlay with a lazy app list; home remains mounted below it |
| `ui/home/FixedHomeLayout.kt` | One-pass measurement, vertical repositioning and proportional overflow fit |
| `ui/home/DismissibleHomeWidget.kt` | Horizontal widget dismissal, independent of hub/app drawer navigation |
| `ui/common/ContentGrid.kt` | Shared 24 dp leading edge including app dots/icons; 32 dp app label lane |
| `ui/home/MusicWidget.kt` | Bounded metadata fields and accessible vector controls |
| `ui/common/StatusBarVisibility.kt` | Shared persisted status-bar behavior, reapplied on resume/focus |
| `ui/settings/` and `util/Prefs.kt` | Settings, favorite ordering, DataStore persistence and atomic limits |
| `ui/theme/` and `ui/common/` | Shared fonts, text sizes and components |
| `ui/welcome/` | Welcome/About content, installed version and approved logo |
| `ui/hub/HubWidgetsViewModel.kt` | Music/calendar observers scoped to the visible hub and selected destinations |
| `ui/hub/HubWidgetCards.kt` | Actual media/calendar widgets, permission action and temporary dismissal |
| `widgets/BatteryReading.kt` | Validated broadcast/direct-capacity fallback and retention of the last valid reading |
| `widgets/DeviceWidgets.kt` | Battery, cellular signal, media sessions, calendar and weather |
| `service/NodusNotificationListener.kt` | Android notification snapshots and destination filters through StateFlow |
| `notifications/NotificationActions.kt` | Actions checked against the current notification revision |
| `util/RecentApps.kt` | Seven-day opening counts and daily local cache |
| `data/AppRepository.kt` | Launchable apps and their launch targets |

The app panel uses normalized progress. Pointer moves update its drag state directly;
only release/Back launches an animation. Translation is read in the graphics layer,
so each animation frame does not rebuild the app list. The panel stays composed behind the full-size opaque, input-owning home surface
while closed (z = -1, semantics cleared). This warms the first lazy viewport.
While visible (z = 1), a shield blocks touches/accessibility on the underlying home. The list resets on close rather than on the first pointer move. Back retargets
from the current position, and cancelled/multitouch opening drags settle closed.
The lazy list hands unconsumed downward user drag at its top to the drawer via
nested scrolling; inertial list motion cannot start a close. Reversal stays with
the panel until release, with separate closing distance/velocity thresholds.

The hub's real widgets use a separate lifecycle-scoped DeviceWidgets instance with
`inHub = true`: this uses hub destinations and never starts location, cellular or
battery observers. Calendar access is required for event widgets on either surface.
Widget sections and Android notification rows share one scrolling list but remain
distinct; counts and clear-all still apply only to active Android notifications.

Paused music additionally requires an active media notification from the player's
package. The listener exposes only an unfiltered in-memory set of media packages,
so home/hub filters cannot suppress this evidence. Visible widget observers react
to evidence changes and discard callbacks after stop. Playing sessions remain
eligible without a notification; removing a task from Recents is not treated as
proof that Android stopped its media service.

The hub has no independent notification archive. Stable keys and retained
expansion state limit jumps while reading. Draft state uses a revision hash
instead of retaining the notification's original body. Sending and removing are
separate outcomes, so a failed removal after a successful send does not invite
a duplicate reply. Home observers and location requests stop when home is hidden.

The complete behavior reference is [BEHAVIOR.md](BEHAVIOR.md).
Historical prototype notes are retained only in the original working copy and
excluded from the clean publication export. They do not define an additional
backlog. The README, behavior reference and roadmap take precedence.

## Artwork

The repository banner and social preview reuse the shared iSSU layout:

| Asset | Size | Editable source |
|---|---|---|
| `assets/nodus-banner.png` | 5120 × 1280, RGB | `source/nodus-banner.svg` |
| `assets/nodus-social-preview.png` | 1280 × 640, RGB | `source/nodus-social-preview.svg` |

The approved N remains uncut and rounded. The project accent is `#F800FF`;
the shared iSSU wordmark and corner arcs retain their original colors.
The wordmark preserves the approved `noduS` case and tightened S spacing.
SVG text is outlined from Exo 2 Bold 700 and Inter Semibold 600. The SVGs are
self-contained; installed fonts are not needed to view them.

The banner preserves title baseline `(1650, 685)`, subtitle baseline
`(1650, 835)`, sizes 250/88, icon centers `(4200/4480/4760, 590)` and the two
140-pixel corner arcs. The three symbols represent Android, the hub and live
updates. The shared badge is copied unchanged.

To rebuild from the project directory, use Python 3.10+ and Node.js 20+:

```sh
python3 -m venv /tmp/nodus-artwork-venv
/tmp/nodus-artwork-venv/bin/pip install -r source/requirements.txt
npm install --prefix source
/tmp/nodus-artwork-venv/bin/python source/build_graphics.py
node source/render-assets.cjs
```

Only this project's SVGs and PNGs are written. Shared branding masters are not
edited. Font license copies are in `licenses/`; artwork tool dependencies are
development-only. Verify PNG sizes, the shared geometry, links and notices after
rebuilding.

## Future GitHub publication

The public repository is https://github.com/issu-lab/nodus . Experimental builds
are distributed through GitHub prereleases. The canonical footer uses the real
raw GitHub URL, preserving width 480 and the Open Homelab link.

Suggested description: “Minimal Android launcher with a notification hub.”
Suggested topics: `android`, `launcher`, `kotlin`, `jetpack-compose`, `notifications`.
Upload `assets/nodus-social-preview.png` through GitHub Settings → General →
Social preview; GitHub does not select that image automatically from the files.

This working copy currently belongs to a larger local workspace Git repository.
It also contains previously tracked build artifacts. `.gitignore` does not
untrack them. A future standalone repository must be populated from a clean,
reviewed source export, excluding caches, build outputs, `local.properties`,
debug APKs and unrelated workspace history. Also exclude local unversioned duplicate
source copies: build validation uses the versioned source set and intentional
new files, not every file found in the working directory. No existing artifacts are deleted
or untracked by this presentation update.

The first prerelease is `v0.1.18-beta.1`. Its downloaded ZIP and extracted APK
were checked against the local files and SHA-256 manifest. README and badge bytes
were also verified through GitHub. Device screenshots, phone validation and visual
GitHub desktop/mobile render checks remain in the [roadmap](ROADMAP.md).

## Prepare the GitHub candidate without changing the app

Stage or commit the publication metadata first, then run:

```sh
python3 tools/prepare_github.py --repository issu-lab/nodus --output /tmp/nodus-github-candidate
```

`issu-lab/nodus` is the public repository.
The output directory must be new. The tool performs no network writes and never
initializes or pushes Git. It copies only allowed tracked source files, omitting
build/cache directories, local SDK settings, signing keys, legacy notes and
unversioned duplicates. It preserves the source and existing APK byte for byte;
only the exported README footer is adjusted to the proposed repository's raw URL.

The beta ZIP packages the existing APK with installation notes, MIT and complete
font notices. `manifest.json` records source hashes and `SHA256SUMS.txt` records
archive/APK hashes. Neither the APK nor the ZIP belongs in source commits.

Before publishing, review the exported tree, create a separate repository with
new history, upload a marked prerelease and verify downloaded asset hashes.
Preserve the APK signing identity for updates. Do not generate a new CI debug
key for each release; a later production signing transition needs an explicit
migration plan. The current app, minimum Android version and signing keys are
unchanged by this preparation.

## Settings localization

English defaults live in `values/ui_strings.xml`; Italian overrides are in
`values-it/ui_strings.xml`. Android resolves the locale when activities are
created. No manual language preference or forced process locale is used.
Dynamic settings callbacks use their current activity context; plural messages
use Android quantity resources. Brand names and stored preference/route keys
are not translated. `tests/check_localization.py` validates locale parity,
format placeholders and source resource references without an Android runtime.
Settings and their shared welcome/header/music components are localized; other
home/hub command labels remain Italian in this version.
