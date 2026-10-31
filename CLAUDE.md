# SimpleTV

A calm, minimal home screen for an onn 4K Google TV stick. It shows the family's
streaming apps as large tiles and opens them; it never plays video itself
(streaming apps are DRM-protected and offer no APIs, so SimpleTV only launches
or deep-links into them).

Users are non-technical. Every screen is calm, clean and obvious: no ads, no
recommendation feeds, no clutter. Every visual state change animates.

## Device

| | |
|---|---|
| Model | onn 4K Streaming Device (2026), codename `wayne` |
| OS | Android TV 14, API 34 — `minSdk = 34` |
| Chip | Realtek RTD1325, 32-bit userspace (`armeabi-v7a`) |
| Memory | ~1.9 GB total, ~650 MB free at idle — keep the app lean |
| Display | 4K panel, UI at 1920×1080, 320 dpi (1 dp = 2 px) |
| Bootloader | Locked; stock OS only |

adb runs over Wi-Fi: `adb connect <stick-ip>:5555`. The stick's sleep timer
is set to Never; in standby it drops off Wi-Fi and adb can't reach it.

There are two sticks, one per TV. `scripts/setup-stick.sh <stick-ip>` sets
one up (install, extras, home screen; safe to repeat), and
`scripts/copy-photos.sh <from-ip> <to-ip>` copies the family photos between
them. Disabling the stock home can knock the stick off Wi-Fi until someone
wakes it, so the script does that last.

## Home screen

SimpleTV is the box's home screen. `MainActivity` has a HOME intent filter,
and the stock home and its setup companion are disabled:

```bash
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

`pm enable <package>` on the same two packages restores the stock home. An
OTA update may re-enable them. Back on the home screen does nothing, and the
Settings button opens Android settings.

## Remote buttons

The stock home handled the remote's extra buttons, so `RemoteButtons` (an
accessibility service) gives them their jobs, from anywhere, even over a
playing app. It is enabled once over adb:

```bash
adb shell settings put secure enabled_accessibility_services com.redmesastudios.simpletv/.RemoteButtons
adb shell settings put secure accessibility_enabled 1
```

| Button | Key code | Job |
|---|---|---|
| YouTube / Netflix / Disney+ / Paramount+ | BUTTON_3 / 4 / 9 / 10 | open that app (asks first over another app) |
| Free TV | FEATURED_APP_1 | family photo screensaver (asks first over another app) |
| ★ Star | MACRO_1 | SimpleTV search |
| 👤 Person | PROFILE_SWITCH | SimpleTV Continue Watching |
| ⚙ Settings | NOTIFICATION | Android settings |
| Channel ▲ / ▼ | CHANNEL_UP / DOWN | play / pause the playing app |
| TV | GUIDE (or TV) | info panel over the playing app |

Over a playing app, the app buttons show "Switch to …?" (an accessibility
overlay): OK or the same button switches, Back keeps watching, it closes by
itself after 6 seconds. From SimpleTV's own screens they switch straight away.
The TV button's info panel shows the show's title, year, kind, TMDB score and
description when SimpleTV started it in that app (`NowPlaying`, a file the
service reads across processes), otherwise "Watching <app>"; always the time,
date and weather. The service forgets the started show when its app leaves the
front, when Back is pressed inside the app (to its menus or another show), or
after 4 hours. TV or Back closes it; it closes by
itself after 10 seconds.
Every other key passes through. The service runs in its own process
(`:buttons`): every remote press waits on it, and on SimpleTV's busy UI thread
a delayed key release turned one press into several. Keys sent with
`adb shell input` skip the service, so test buttons with the real remote.
Never `am force-stop` SimpleTV (or `am start -S` it): Android turns a
force-stopped app's accessibility service off, and every remote button goes
dead until the two commands above are run again. Installing over it is fine.
Voice search uses the remote's mic: `adb shell pm grant
com.redmesastudios.simpletv android.permission.RECORD_AUDIO`, or the prompt on
first use.

## Build and run

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest assembleDebug
adb -s <stick-ip>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

The emulator `SimpleTV_GoogleTV_API36` (Google TV image, 2 GB RAM) is for
checks away from the stick; it runs API 36 and arm64, so the stick is the
source of truth.

## Code map

- `MainActivity.kt` — switches between the home screen, search, Continue
  Watching and a folder; the Home button always returns to the home screen,
  the remote's Star and Person buttons arrive as intents, and the show guide
  refreshes on resume.
- `HomeScreen.kt` — header (greeting, date, clock, Search, Settings), app banner
  tiles in pages of ten (two rows of five), and the At a glance cards along the
  bottom, all over the fall scene. A page with more apps after it shows nine
  and a More apps tile, which slides the next page in; Back slides back. Apps
  with a folder open it; the rest launch directly. Focus starts on the
  top-left app of the first page, and every Home press returns it there.
- `Glance.kt` / `GlanceCards.kt` — the At a glance cards: weather, news count and
  markets, refreshed at most every 15 minutes while the home screen is up.
  Each card opens its channel page.
- `ChannelData.kt` / `ChannelScreens.kt` — the channel pages: Weather (now, NWS
  alerts, 6 days), News (SignalPost's top and world stories from the last 24
  hours and local stories from the last 3 days, by newsroom count; OK opens
  a story in full, Back returns to it in the list) and
  Markets (S&P 500, Dow, Nasdaq, then the family's own stocks three to a row,
  each with the day's chart against the previous close; "Edit your stocks"
  types the symbol list, kept on the box; OK opens a market's detail page:
  today's range, open, previous close, volume, 52-week range, performance
  over 1 week to 1 year, and a year's chart, from Yahoo's chart endpoint —
  market cap and P/E need Yahoo's cookie-gated quote API, so they're left out). Each refreshes every 10 minutes
  while open; Back returns to its card.
- `InfoPanel.kt` — the TV button's panel (see Remote buttons).
- `ScreensaverScreen.kt` / `Photos.kt` / `PhotoUploadServer.kt` — the family photo
  screensaver (Free TV button): a slow crossfading slideshow with a clock.
  Landscape photos fill the screen and drift; portrait and square ones show
  whole, full height, over a blurred copy of themselves. Left/Right step,
  Up/Down change the time per photo (5 s to 2 min, kept on the box), OK opens
  "Add family photos", Back leaves. "Add family
  photos" runs a small web server on the home network only while it's showing,
  with a QR code and a fresh four-digit code; phones pick photos, the page
  shrinks them, and the stick decodes (JPEG/PNG/HEIC), turns upright and
  resizes each to 1920×1080 in `files/photos`. The page also lists and removes
  photos. Photos never leave the stick and never go in the repo. The slideshow
  keeps the screen on so the idle screensaver stays away.
- `PhotoDream.kt` — the same slideshow as the TV's idle screensaver (a
  DreamService hosting Compose). Chosen once over adb:
  `adb shell settings put secure screensaver_components com.redmesastudios.simpletv/.PhotoDream`;
  `settings delete secure screensaver_components` restores Google TV's own.
  Dreams can't be started from adb without root, so test by letting the TV idle.
- `Holidays.kt` / `HalloweenScene.kt` — holiday decorations over the scene, each
  on for a stretch of days around its holiday and gone afterwards (Halloween:
  Oct 24 – Nov 1: harvest moon, bats, a graveyard, a hay bale with a black
  cat, corn stalks, jack-o'-lanterns, a ghost, and a skeleton and a mummy
  walking by now and then). They sit where the home screen leaves room: the
  sky above the apps and the hillside between the apps and the glance cards.
- `WatchTime.kt` — tile order: most-watched over the last 30 days first,
  re-sorted at most once a day. Needs usage access, granted once over adb:
  `adb shell appops set com.redmesastudios.simpletv GET_USAGE_STATS allow`.
  Without it the curated order stays.
- `FolderScreen.kt` — a service's folder: a large "Open <app>" button with the
  app's logo at top left, focused when the folder opens (it bounces once), the
  focused show's artwork and details below it; rows below: Continue Watching, Just Added, New Season, New
  Season Coming, Top 10 Shows and Movies ("Popular" outside Netflix). YouTube's
  folder has Continue Watching, New From Your Subscriptions and Popular on
  YouTube, with a sign-in code in place of the details until the TV is signed in.
- `YouTube.kt` — YouTube sign-in (Google's TV device-code flow, refresh token
  kept on the box), the Data API client, and the hourly cached rows
  (`files/youtube.json`). Shorts are left out.
- `SearchScreen.kt` — search by typing or voice: shows, movies and people, each
  title marked with where it streams (including services not on this TV).
- `ContinueWatchingScreen.kt` — everything started from SimpleTV, one row per
  app in home-screen order.
- `RemoteButtons.kt` — the remote's extra buttons (see Remote buttons).
- `Catalog.kt` / `ShowGuides.kt` / `CatalogStore.kt` — row building, the
  TMDB / Watchmode / Netflix Top 10 clients, and the once-a-day cached guide
  (`files/catalog.json`).
- `ShowLinks.kt` — opens a show in its app (Netflix Top 10 by Netflix ID,
  YouTube by video link, everything else by Watchmode's web link, cached per
  show), falling back to the app itself; records Continue Watching.
- `AppArt.kt` — each app's own TV banner (its logo artwork), decoded at tile
  size; apps without one get their icon and name on a warm color.
- `FallScene.kt` / `FallingLeaves.kt` — the backdrop (hills, edge trees, leaf
  pile) and the falling leaves. Leaf motion is pure math in design px
  (1920×1080). The leaves step about 30 times a second and stop while another
  app is in front; that keeps SimpleTV near 40% of one core on the stick.
- `StreamingApps.kt` — the curated app list in home-screen order; apps that
  aren't installed are skipped. Launches via the leanback launch intent.
- `Greeting.kt` — greeting by hour.
- `ui/theme/` — Fall palette and Manrope type (bundled variable font in
  `res/font`, OFL licence in `FONT-LICENSE-OFL.txt`). Text sizes are the design's
  px values halved.

Plain Compose foundation only (no tv-material). Unit tests in `app/src/test`.

## Design

The design canvas lives at https://claude.ai/artifact/BnhjnTjfFvcc1RH6VxxBWk.
"Fall — Home, app banners" is the built home screen; "Fall — Netflix folder",
"Fall — Weather channel" and "News card states" are the approved designs for
the folders and info channels.

## Data sources

Keys and household settings live outside the repo, in the developer's
`~/.config`, and the build bakes them into `BuildConfig` (README lists them): TMDB in
`~/.config/tmdb/{read_token,api_key}`, Watchmode in
`~/.config/watchmode/api_key` (free plan: 2,500 credits a month, web links
only), and the YouTube OAuth client (`client_id=` / `client_secret=` lines) in
`~/.config/youtube/credentials.txt`.
The YouTube client is a "TVs and Limited Input devices" client in the Google
Cloud project SimpleTV, published to production but unverified (scope
`youtube.readonly`), so sign-ins don't expire weekly; approving the code shows
Google's "unverified app" warning once. The SignalPost API key is in
`~/.config/signalpost/api_key`. `~/.config/simpletv/home.txt` holds
`weather_point` (latitude,longitude), `local_news_market` and
`local_news_name`; a blank one turns its feature off with a note on screen
(`Home` in `Glance.kt`). Netflix Top 10 comes from
Netflix's own weekly pages (`tudum/top10/united-states` and `/tv`); other
services get "Popular Shows/Movies" from TMDB. Watchmode's release feed ignores
its service filter and caps at 250 entries, so SimpleTV pulls it in 10-day
spans (about 9 credits a day) and splits it by service; some services carry
two Watchmode IDs (Peacock: 388 for releases, 389 for links). Its free
"leaving soon" feed only covers the next 10 departures across all services, so
there is no Leaving Soon row. Weather is the National Weather Service's hourly
forecast for `weather_point`. The news card counts SignalPost stories first
seen in the last 6 hours (overnight before 10 AM). Markets are the S&P 500,
Dow and Nasdaq from Yahoo Finance's public chart endpoint (no key), against
the previous close.

## Rules

- Debloating uses `pm uninstall --user 0` (reversible with
  `cmd package install-existing <pkg>`). Never remove Google services, the
  Play Store, `com.google.android.katniss` (voice remote),
  `com.netflix.tokenmanager`, or `com.google.android.tungsten.setupwraith`
  (it stays installed, only disabled).
- Reference launchers (FLauncher, FlauncherL, android/tv-samples) are for
  ideas only — never copy their code.
