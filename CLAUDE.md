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

There are two sticks here, one per TV, and one in another household.
`scripts/setup-stick.sh <stick-ip>` sets one up (install, extras, home screen;
safe to repeat), and
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
Settings button opens SimpleTV's Settings screen (the phone settings page's QR
code, and a TV settings button for Android's); holding OK on it for 1.5 s opens
Tailscale, which has no tile so nobody removes it by mistake. The remote's own
settings button still opens Android settings.

On a stick with Tailscale, `setup-stick.sh` sets it as the always-on VPN (lockdown
off) and adds it to the device-idle allowlist; without both, a reboot leaves the
tunnel down (Android starts it, then stops it for being idle). Anything that shows
a phone address uses `localAddress()`, which skips VPNs: with Tailscale up, the
active network is the tunnel, whose 100.x address phones on the Wi-Fi can't reach.
Ports: 8723 photo upload, 8724 sync, 8725 Trivia Night, 8726 Imposter, 8727
settings.

## Households and updates

The household's settings live in `~/.config/simpletv/`: `home.txt` (location,
local news) and `sync_key`. Another household's are in a subfolder
(`~/.config/simpletv/<household>/`), built with `-Phousehold=<household>`;
`BuildConfig.HOUSEHOLD` is the folder name, or `home` for ours. Each
household has its own sync key, so its sticks share only with each other.

`scripts/release.sh` runs the tests, builds every household (ours last, so
`app/build` holds ours) with one `versionCode` (minutes since 1970; a plain
build stamps the current minute) and posts them with a `latest.txt` as a
release on the private repo `dbarkman/SimpleTV-builds`. `Updater.kt` checks
it every 6 hours while the home screen is up, reading it with
`BuildConfig.UPDATE_TOKEN` (a read-only token in
`~/.config/simpletv/update_token`), and installs a newer build of its own
household over itself once the APK's SHA-256 matches. Installing needs
`appops set com.redmesastudios.simpletv REQUEST_INSTALL_PACKAGES allow`
(`setup-stick.sh` does it). Android installs without a prompt only when
SimpleTV is its own installer of record, so every hand install uses
`adb install -r -i com.redmesastudios.simpletv`; a plain `adb install`
leaves no installer and the next update shows an Update prompt once (after
that SimpleTV is the installer). Builds are signed with this Mac's debug
keystore, `~/.android/debug.keystore`; losing it means a fresh install on
every stick. The other household's stick is reached over Tailscale when it
needs hands-on fixing.

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
after 4 hours. The full description scrolls inside the panel with Up and
Down, with a ▼ while there's more below; each scroll keeps the panel up
another 10 seconds. TV or Back closes it; it closes by itself after 10
seconds.
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
adb -s <stick-ip>:5555 install -r -i com.redmesastudios.simpletv app/build/outputs/apk/debug/app-debug.apk
```

The emulator `SimpleTV_GoogleTV_API36` (Google TV image, 2 GB RAM) is for
checks away from the stick; it runs API 36 and arm64, so the stick is the
source of truth.

## Code map

- `MainActivity.kt` — switches between the home screen, search, Continue
  Watching and a folder; the Home button always returns to the home screen,
  the remote's Star and Person buttons arrive as intents, and the show guide
  refreshes on resume.
- `Updater.kt` — the self-updater (see Households and updates) and
  `UpdateResult`, which shows Android's install prompt if it asks.
- `HomeScreen.kt` — header (greeting, date, clock, Get Apps (the Play Store), Movies, Search, Settings), app banner
  tiles in pages of ten (two rows of five), and the At a glance cards along the
  bottom, all over the fall scene. A page with more apps after it shows nine
  and a More apps tile, which slides the next page in; Back slides back. Apps
  with a folder open it; the rest launch directly. Focus starts on the
  top-left app of the first page, and every Home press returns it there.
- `RemoveApp.kt` — holding OK on an app tile for 1.5 s shows "Remove <app> from
  this TV?" (Cancel selected). The prompt takes no focus: the home screen steers
  it with Left/Right/OK/Back and swallows the held OK's release. Remove hands
  over to Android's uninstall prompt; apps that came with the TV open their
  settings page instead, where they can be turned off.
- `Glance.kt` / `GlanceCards.kt` — the At a glance cards: weather, news count and
  markets, refreshed at most every 15 minutes while the home screen is up.
  Each card opens its channel page; the household can hide any of them.
- `HouseSettings.kt` / `SettingsServer.kt` / `SettingsScreen.kt` — the
  household's settings: birthdays and anniversaries, hidden At a glance cards
  and holiday packs. They're records in the shared box (collection `house`),
  so they spread between the household's sticks with Sync, newest change per
  record winning; a removal is a record too. `birthdays.txt` seeds the list:
  each line becomes a record with a fixed id dated 0, added only where that id
  is new, so phone edits stick and lines added later still arrive. The
  Settings screen runs a small web server (only while it shows) with a QR code
  and a four-digit code; the phone page edits everything and each change
  saves at once.
- `ChannelData.kt` / `ChannelScreens.kt` — the channel pages: Weather (now, NWS
  alerts, 6 days; Down selects a day and OK opens it: the forecaster's words for
  the day and night, rain chance, wind and every third hour's temperature,
  with ◀ ▶ stepping to the day before and after), News (SignalPost's top and world stories from the last 24
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
- `Sync.kt` — the household's sticks share Continue Watching (`WatchHistory`,
  each entry with its `watchedAt`) and the family photos. Each runs a small
  server on port 8724 while SimpleTV runs, announces it over NSD
  (`_simpletv._tcp`) and pulls from every other stick: on start, every 10
  minutes, on resume, and when another stick pokes it after a change. Removed
  photos leave a note (`files/photos_removed.json`) so removals spread too.
  Requests carry `BuildConfig.SYNC_KEY` (from `~/.config/simpletv/sync_key`);
  without a key, sync is off. Requests use a plain socket, since Android blocks
  cleartext `HttpURLConnection`. App order stays per stick.
- `SharedRecords.kt` — the Game Bridge: the household's shared box for Red Mesa Studios
  games on the stick (Trivia Night, Imposter; they share `players` and keep their own
  results): collections of JSON records, each with an `id` and an `at`
  time, one file each in `files/shared`. An app POSTs a collection to
  `127.0.0.1:8724/sync/shared/<name>` with the sync key and gets back the
  merged copy (newest `at` per id wins); `GET /sync/shared` hands every
  collection to the other sticks on each pull. SimpleTV never reads the
  records themselves.
- `PhotoDream.kt` — the same slideshow as the TV's idle screensaver (a
  DreamService hosting Compose). Chosen once over adb:
  `adb shell settings put secure screensaver_components com.redmesastudios.simpletv/.PhotoDream`;
  `settings delete secure screensaver_components` restores Google TV's own.
  Dreams can't be started from adb without root, so test by letting the TV idle.
- `HolidayCalendar.kt` — which holiday shows on a date. Holidays come in packs
  (US National, Christian, World, Fun days, Seasons, Jewish, Muslim, Indian);
  `holiday_packs=` in home.txt picks them (blank: all but the last three).
  Each holiday has its stretch of days; where two overlap the shorter wins its
  days, and the Seasons pack's first-day-of-a-season days show only when
  nothing else does. Only holidays in `DrawnHolidays` count, so
  a holiday without a scene never blanks out one with a scene. Easter is
  computed, Hebrew dates use Calendrical Calculations arithmetic, Islamic
  dates java.time's Umm al-Qura `HijrahDate`, seasons Meeus's equinox
  formulas; Lunar New Year and the Hindu dates are tables, to extend as years
  are published. Tests check them against the macOS calendars.
- `FamilyDays.kt` — birthdays and anniversaries (from `HouseSettings`; the
  household's `birthdays.txt`, never in the repo, seeds them). On the day the
  greeting becomes "Happy birthday, <name>!" and the scene celebrates instead of
  any holiday.
- `Holidays.kt` — the decorations' motion, pure math in design px.
- `NewYearScene.kt` / `EpiphanyScene.kt` / `MlkDayScene.kt` / `PopcornDayScene.kt` /
  `GroundhogDayScene.kt` / `LunarNewYearScene.kt` / `PizzaDayScene.kt` /
  `AshWednesdayScene.kt` / `ValentinesScene.kt` / `GameDayScene.kt` /
  `PresidentsDayScene.kt` / `PiDayScene.kt` / `StPatricksScene.kt` /
  `SpringDayScene.kt` / `AprilFoolsScene.kt` / `HalloweenScene.kt` /
  `ElectionScene.kt` / `VeteransDayScene.kt` / `ThanksgivingScene.kt` /
  `ChristmasScene.kt` / `CelebrationScene.kt` — the drawn scenes. New Year's (Dec 31 –
  Jan 1; "Happy New Year!"): a marquee sign with the coming year in chasing lights,
  fireworks over the hill and up between the greeting and the clock, and balloon
  bunches. MLK Day ("Honoring Dr. King"): a sun rising behind the hill, two lines of
  people of every color holding hands with a ripple through their hands, and a dove
  with an olive sprig crossing now and then. Epiphany (Jan 6; "Happy Epiphany"): a
  guiding star with a beam of light and three wise men on camels walking across. Popcorn
  Day (Jan 19; "Happy Popcorn Day!"): a striped bucket with kernels popping out, smaller
  buckets and ADMIT ONE tickets. Groundhog Day ("Happy Groundhog Day!"): a low sun, a
  groundhog that pops out, looks around, sees its shadow and ducks back, and a "6 MORE
  WEEKS?" sign. Lunar New Year ("Happy Lunar New Year!"): red lanterns, an upside-down
  福 on a red diamond, plum branches and a paper dragon weaving across the sky.
  Valentine's (Feb 7–14; "Happy Valentine's Day!" on the 14th only): a heart garland,
  hearts rising off the hill, a mailbox with a letter, roses, a "BE MINE" candy heart
  and two lovebirds leaning in. Presidents' Day ("Happy Presidents' Day"): a log cabin
  with chimney smoke, a cherry tree, a waving flag with small flags, and a tricorn and a
  stovepipe hat on a bench. Pizza Day (Feb 9; "Happy Pizza Day!"): a steaming pepperoni
  pizza on a board whose slice lifts out with stretching cheese, pizza boxes and a
  parmesan shaker. Ash Wednesday (no greeting): a quiet white chapel with lit windows and
  a swinging bell, bare shrubs and candles. Pi Day (Mar 14; "Happy Pi Day!"): a pie with a
  π in its crust, the digits of pi scrolling along a ribbon, and a compass drawing circles
  on a chalkboard. St. Patrick's (Mar 14–17; "Happy St. Patrick's Day!" on the 17th only;
  its own all-green backdrop with nothing drifting, whatever the season): a rainbow down
  to a pot of gold, a shamrock garland, shamrocks in the grass and a green hat on a stump.
  First day of spring ("Hello, spring!"): tulips and daffodils, a birdhouse with a robin
  popping out, and butterflies. April Fools ("Happy April Fools' Day!"): a jack-in-the-box
  that cranks and pops, an upside-down APRIL FOOLS! sign, a banana peel and a whoopee
  cushion. Game day (the second Sunday of February,
  `Holiday.SuperBowl`; "Happy game day!"; no team or league marks): a football kicked
  from its tee through goalposts now and then, a scoreboard whose home score goes up 3
  per kick, a "GO TEAM!" pennant and a bowl of chips. Halloween (Oct 24 – Nov 1):
  harvest moon, bats, graveyard, hay bale with a black cat, corn stalks,
  jack-o'-lanterns, a ghost, a skeleton and a mummy walking by. Election Day
  (Nov 2 through Election Day): bunting, a waving flag, a ballot box, a VOTE
  sign, balloons and Uncle Sam marching by. Thanksgiving (day after Election
  Day through Thanksgiving): corn stalks, a hay bale with pumpkins and
  gourds, a cornucopia, and a turkey strutting by that stops to fan its
  tail. Veterans Day (Nov 11, inside Thanksgiving's stretch; the greeting
  says "Thank you, veterans"): a tall waving flag, rows of small flags along the
  hill, a wreath on an easel, twinkling stars, and a bald eagle gliding across
  now and then. Christmas (Saturday after Thanksgiving through Dec 30, always on the
  winter backdrop): twinkling string lights, a decorated tree with a glowing
  star and presents, a waving snowman, and Santa's sleigh crossing under the
  lights now and then. Celebration: bunting, rising balloons (hearts for an
  anniversary), a cake with flickering candles and presents.
- `AppArt.kt` — each app's own TV banner (its logo artwork), decoded at tile
  size; apps without one get their icon and name on a warm color.
- `HomeScene.kt` / `FallingLeaves.kt` — the backdrop behind every screen
  (hills, edge trees, something drifting down) in the season's colors:
  winter snow with snow on the trees, spring blossoms and petals, summer
  greens and dandelion seeds, fall leaves with a leaf pile. Motion is pure math
  in design px (1920×1080). It steps about 30 times a second and stops while
  another app is in front; that keeps SimpleTV near 40% of one core on the
  stick. To see a date's scene ahead of time:
  `adb shell am start -n com.redmesastudios.simpletv/.MainActivity --es scene_date 2026-12-25`
  (Home goes back to today).
- `ServiceDirectory.kt` — TMDB's and Watchmode's US service lists (cached a
  week in `files/services.json`). An app SimpleTV wasn't told about is matched
  to its service by name (`matchService`: "ESPN" takes ESPN and ESPN Plus,
  never "… Amazon Channel" resales); one that matches both lists gets a folder
  like the known services. The catalog rebuilds early when a newly matched
  service has no rows yet, and never keeps a guide that came back all empty.
- `StreamingApps.kt` — every installed TV app gets a tile (`homeApps`): the
  known services first, in their curated order with their guide IDs, then the
  rest by name. Settings, the Play Store (Get Apps) and SimpleTV itself get no
  tile. Launches via the leanback launch intent.
- `Greeting.kt` — greeting by hour, and the words some holidays use instead
  (`holidayGreeting`, which takes the date for Valentine's and St. Patrick's).
- `ui/theme/` — Fall palette and Manrope type (bundled variable font in
  `res/font`, OFL licence in `FONT-LICENSE-OFL.txt`). Text sizes are the design's
  px values halved.

Plain Compose foundation only (no tv-material). Unit tests in `app/src/test`.

## Design

The design canvas lives at https://claude.ai/artifact/BnhjnTjfFvcc1RH6VxxBWk.
"Fall — Home, app banners" is the built home screen; "Fall — Netflix folder",
"Fall — Weather channel" and "News card states" are the approved designs for
the folders and info channels. "App icon and logo" holds the icon: direction A,
Horizon (a sun over fall hills in a TV, on clay `#C8643B`). `docs/logo.svg` is
its source; the launcher icon is an adaptive icon (`mipmap-anydpi/ic_launcher.xml`
with `drawable/ic_launcher_foreground.xml`), and the TV banner
(`drawable-xhdpi/banner.png`, 640×360) and `docs/banner.png` are the icon plus the
Manrope "Simple**TV**" wordmark, rendered from HTML with headless Chrome.

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
`~/.config/signalpost/api_key`, and OMDb's (an `API_KEY=` line) in
`~/.config/omdb/api_key`. `~/.config/simpletv/home.txt` holds
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

- Debloating uses `pm uninstall --user 0`. For apps that came with the stick
  it's reversible with `cmd package install-existing <pkg>`; apps in
  `pm list packages -3` (Play-installed, such as Philo or Tubi) are removed
  outright and come back only from the Play Store, so ask before removing one.
  Never remove Google services, the
  Play Store, `com.google.android.katniss` (voice remote),
  `com.netflix.tokenmanager`, or `com.google.android.tungsten.setupwraith`
  (it stays installed, only disabled).
- Reference launchers (FLauncher, FlauncherL, android/tv-samples) are for
  ideas only — never copy their code.
