# SKILLS.md — Gayadi-Android Agent Guide

## Project Overview

**GAYADI** is an adaptive group travel assistant app. It recommends itineraries and routes based on traveler personality, weather, crowd levels, and transit changes across three phases: pre-trip, during-trip, and post-trip.

This repository contains the **Android client** built with **Jetpack Compose + Kotlin**.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.1+ |
| UI | Jetpack Compose (Material 3) |
| Navigation | Navigation Compose |
| Image Loading | Coil Compose |
| Build | Gradle 8.x + Kotlin DSL |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Java | 17 |

## Project Structure

```
app/src/main/java/com/gayadi/android/
├── MainActivity.kt              # Entry point
├── navigation/
│   ├── Routes.kt                # Route constants
│   └── NavGraph.kt              # NavHost with all screens
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # Design tokens
│   │   ├── Theme.kt             # Material3 theme
│   │   └── Type.kt              # Typography
│   ├── components/
│   │   └── BottomNavBar.kt      # Shared bottom navigation
│   └── screens/
│       ├── LoginScreen.kt       # 01 — OAuth login
│       ├── BasicInfoScreen.kt   # 12 — Name & intro input
│       ├── SurveyScreen.kt      # 11 — Personality survey
│       ├── SurveyResultScreen.kt# 03 — Personality result
│       ├── FriendAddScreen.kt   # 02 — Add travel mates
│       ├── PlaceSearchScreen.kt # 04 — Place search
│       ├── PlaceDetailScreen.kt # 05 — Place detail + reviews
│       ├── MyTripScreen.kt      # 06 — Trip checklist
│       ├── RealtimeHomeScreen.kt# 07 — Realtime home + 08 reschedule sheet
│       ├── MyPageScreen.kt      # 09 — Profile
│       ├── SettingsScreen.kt    # 10 — Settings & logout
│       ├── NoticeListScreen.kt  # 13 — Update notices
│       ├── NoticeDetailScreen.kt# 14 — Notice detail
│       └── InquiryScreen.kt     # 15 — Support inquiry form
```

## Screen Flow

```
Login → BasicInfo → Survey → SurveyResult → RealtimeHome
                                                    ↕
                                              MyTrip / MyPage
                                                    ↓
                                              Settings → NoticeList → NoticeDetail
                                                       → Inquiry
                                              PlaceSearch → PlaceDetail
                                              FriendAdd
```

## Design System

### Colors (see `ui/theme/Color.kt`)
- **Primary Blue** `#5B9BD5` — main CTA buttons
- **Kakao Yellow** `#FEE500` — Kakao login button
- **Surface Light** `#F5F7FA` — card backgrounds
- **Text Primary** `#1C1C1E` — headings
- **Text Secondary** `#8E8E93` — subtitles

### Tag Colors
Each personality/crowd tag uses a pastel background + saturated text pair (e.g., `TagPink` / `TagPinkText`).

### Typography
Uses `GayadiTypography` — Korean-optimized scale from `headlineLarge` (28sp) to `labelSmall` (10sp).

## Coding Conventions

1. **One screen per file** in `ui/screens/`.
2. **Stateless composables** — screens receive callbacks via parameters; no ViewModel yet (MVP phase).
3. **Mock data inline** — each screen holds its own `val mockXxx = ...` for preview and demo.
4. **Korean strings** — all user-facing text is in Korean, hardcoded for now.
5. **No external resources** — use emoji or Compose-drawn placeholders for images/icons until assets are added.
6. **Material 3 components** — prefer `Button`, `Card`, `TextField`, `Chip` from M3.
7. **Preview functions** — every screen has a `@Preview` composable.

## Backend Integration (Future)

The companion server (`Gayadi-Server`) exposes REST APIs at `/api/v1/...`. When integrating:
- Use Retrofit + kotlinx.serialization or Moshi
- Auth token via Kakao/Google OAuth → store in DataStore
- Base URL configurable via BuildConfig

## How to Build

```bash
# Requires JDK 17 + Android SDK
./gradlew assembleDebug
```

Or open in Android Studio — Gradle wrapper will be auto-generated on first sync.

## Agent Workflow

When modifying this project:
1. Read the target screen file first to understand current state.
2. Follow existing patterns (mock data, callback params, Preview).
3. Use colors from `Color.kt` — do not hardcode hex values in screens.
4. Add new routes to `Routes.kt` and `NavGraph.kt`.
5. Run `./gradlew assembleDebug` if Gradle wrapper is available.
6. Commit with conventional format: `feat(screen): description` or `fix(screen): description`.
