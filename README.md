# LeetCode Forcer Android

LeetCode Forcer is an Android app built with Jetpack Compose that turns daily LeetCode progress into a focus gate for selected apps. The app checks a user's LeetCode activity through the LeetCode GraphQL API, stores daily progress locally, and uses an `AccessibilityService` to block apps until the daily goal is met.

## What this project does

The project combines three ideas:

1. A Compose-based control panel for setup and daily status.
2. A LeetCode integration layer that fetches solve data and consistency history.
3. A runtime enforcement layer that decides whether an app should stay open or be pushed away.

In practice, the app lets the user:

- Set a LeetCode username.
- Refresh and cache daily solve status.
- View consistency data as a heatmap.
- Define user whitelist and blacklist package rules.
- Define focus sessions by day and time.
- Use Quick Settings tiles for fast status checks and resets.

## Overall flow

The full app flow is:

1. The user opens the Compose app and saves a LeetCode username.
2. `MainActivity` loads current preferences, rule sets, focus sessions, and LeetCode status.
3. `LeetCodeManager` calls LeetCode's GraphQL API and stores the latest accepted-submission counters in `SharedPreferences`.
4. The app compares current counters with stored counters to decide whether the user has solved problems today.
5. If focus sessions are active, `LeetCodeForcerService` watches foreground app changes through accessibility events.
6. When the user opens an app, the service checks:
   - whether a focus session is active,
   - whether the app is always allowed,
   - whether the app is user-whitelisted or blacklisted,
   - whether today's LeetCode goal has been completed,
   - and whether the app has a stricter 5-problem requirement.
7. If the user has not met the goal, the service sends the user back using global accessibility actions and shows a toast message.
8. Once the daily goal is met, blocked apps become accessible again for that day.

## How the app achieves this

### 1. Compose UI for configuration and visibility

The main screen is implemented in [`app/src/main/java/com/example/leetcodeforcer/MainActivity.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/MainActivity.kt).

It provides:

- Accessibility service status.
- Username setup and editing.
- Manual refresh of LeetCode status.
- Consistency heatmap rendering.
- Whitelist and blacklist package management.
- Focus-session creation and removal.

This screen acts as the control center for the project. It does not do heavy logic itself. Instead, it reads state from managers and writes user intent back into them.

### 2. LeetCode progress detection through GraphQL

LeetCode integration lives in [`app/src/main/java/com/example/leetcodeforcer/LeetCodeManager.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/LeetCodeManager.kt).

This module is responsible for:

- Storing the username.
- Sending GraphQL requests to `https://leetcode.com/graphql`.
- Reading accepted-submission totals and solved counts.
- Fetching consistency calendar data for the heatmap.
- Caching the current UTC date from `https://gettimeapi.dev/v1/time`.
- Persisting daily completion markers in `SharedPreferences`.

The app determines "solved today" by comparing current accepted-submission totals with the previously stored total. If the number increases, the user has made progress today and the app marks the day as completed.

For selected apps, there is also a stricter rule: some packages require at least 5 solved problems in one day before they are allowed.

### 3. Focus scheduling and package rules

Focus rules are managed in [`app/src/main/java/com/example/leetcodeforcer/FocusSettingsManager.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/FocusSettingsManager.kt).

This module stores:

- User whitelist packages.
- User blacklist packages.
- Focus sessions with day and time ranges.
- A built-in always-allowed package list for system-critical or intentionally exempt apps.

The enforcement logic only runs when a focus session is active. If no sessions are configured, the code treats enforcement as active all day. That makes the default behavior strict, while still allowing scheduled focus windows when the user wants more control.

### 4. Accessibility-based enforcement

App blocking is implemented in [`app/src/main/java/com/example/leetcodeforcer/LeetCodeForcerService.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/LeetCodeForcerService.kt).

The service listens for `TYPE_WINDOW_STATE_CHANGED` events, which effectively means it notices when the active window changes. When a package comes to the foreground, the service evaluates the current rules:

- If no focus session is active, it does nothing.
- If the package is blacklisted, it blocks immediately.
- If the daily LeetCode goal is already complete, it allows the app.
- If the package is always allowed or user-whitelisted, it allows the app.
- If the package is in the special "requires 5 problems" set, it checks the stricter completion flag.
- Otherwise, it blocks the app and pushes the user back to home.

This is the key mechanism that turns solve progress into real device friction.

### 5. Quick Settings tiles for fast control

The project also exposes Quick Settings tiles:

- [`app/src/main/java/com/example/leetcodeforcer/LeetCodeTileService.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/LeetCodeTileService.kt)
  Refreshes LeetCode status and shows whether the device is effectively locked or unlocked.
- [`app/src/main/java/com/example/leetcodeforcer/LeetcodeTileService2.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/LeetcodeTileService2.kt)
  Shows broader status, including focus-session and accessibility-service state.
- [`app/src/main/java/com/example/leetcodeforcer/LeetcodeResetTile.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/LeetcodeResetTile.kt)
  Clears the normal daily completion marker.
- [`app/src/main/java/com/example/leetcodeforcer/AlarmyDataTile.kt`](/c:/Users/profe/Downloads/jetpackComposePortfiolio/LeetcodeForcerAndroid-main%20(2)/LeetcodeForcerAndroid-main/app/src/main/java/com/example/leetcodeforcer/AlarmyDataTile.kt)
  Shows whether a focus session is active and how many sessions exist.

These tiles reduce friction for daily use. The user does not need to open the full app just to refresh or check status.

## Architecture summary

The project is small, but the architecture is cleanly split:

- UI layer: Compose screens and widgets in `MainActivity.kt`
- domain/data layer: `LeetCodeManager.kt`
- rule engine and persistence: `FocusSettingsManager.kt`
- enforcement/runtime layer: `LeetCodeForcerService.kt`
- shortcut surfaces: Quick Settings tile services

The main implementation choice is to keep persistence simple with `SharedPreferences` and JSON arrays instead of introducing Room or a larger data stack. That makes the app lightweight and easy to reason about.

## Why this design works

This project works because each responsibility is isolated:

- The UI is only responsible for input and visibility.
- LeetCode syncing is centralized in one manager.
- Focus and package rules are centralized in one settings manager.
- Runtime app blocking is handled by the accessibility service.

That separation makes the behavior predictable:

- status can be refreshed independently,
- rules can be changed without touching enforcement code,
- and enforcement only depends on a small number of shared states.

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Android Accessibility Service
- Android Quick Settings Tile Service
- `SharedPreferences` for persistence
- LeetCode GraphQL API

## Project structure

```text
app/src/main/java/com/example/leetcodeforcer/
|- MainActivity.kt              // Compose UI and setup flow
|- LeetCodeManager.kt           // LeetCode API calls and daily status logic
|- FocusSettingsManager.kt      // whitelist, blacklist, and focus session storage
|- LeetCodeForcerService.kt     // runtime blocking service
|- LeetCodeTileService.kt       // quick refresh tile
|- LeetcodeTileService2.kt      // status/info tile
|- LeetcodeResetTile.kt         // reset tile
|- AlarmyDataTile.kt            // focus-session tile
```

## Setup

### Requirements

- Android Studio
- Android SDK with `minSdk 24`
- Internet access for LeetCode and time API requests

### Run locally

1. Open the project in Android Studio.
2. Sync Gradle.
3. Build and install the app on a real device or emulator.
4. Enable the accessibility service for the app in Android settings.
5. Open the app UI and save a LeetCode username.
6. Add the Quick Settings tiles if you want fast access to status tools.

## Important implementation notes

- The app depends on the accepted-submission count increasing, not on parsing individual submissions.
- The current date is cached from an external time API to reduce local clock drift issues.
- If no focus sessions are configured, enforcement is treated as always active.
- Some packages are intentionally always allowed so the user cannot lock themselves out of basic device control.
- There is a package-specific 5-problem rule currently applied to `com.tencent.ig`.

## Current limitations

- The app uses `SharedPreferences`, so there is no advanced migration or relational storage model.
- Enforcement is based on accessibility events, which is practical but depends on Android accessibility behavior.
- Network failures can prevent the app from refreshing status.
- The manifest currently has the launcher intent filter for `MainActivity` commented out, so app launch behavior may need adjustment depending on how you install and open it.

## Build

Use:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```
