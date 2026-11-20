# EUEE Mastery — Offline-First Native Android Application

**EUEE Mastery** is a complete, 100% offline-first native Android educational ecosystem built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room (SQLite)**, **DataStore**, **WorkManager**, and native Android OS integrations for **Ethiopian University Entrance Examination (EUEE)** preparation across **Mathematics, Physics, Chemistry, Biology, English, and Aptitude (Grades 9–12)**.

---

## Core Capabilities

1. **Dashboard & Telemetry**: Tracks total/attempted/correct/incorrect questions, accuracy %, mastery status, spaced-repetition due queue, study streak, daily/weekly activity chart, resumable study sessions & mock exams, today's tasks, upcoming alarms, and weakest topics.
2. **Complete Subject Library**: Hierarchical organization (`Subject → Grade (9–12) → Unit → Topic → Questions`) supporting custom subjects, units, topics, and question banks.
3. **10 Study Modes**: Practice, Active Recall, Flashcards, Multiple Choice, Timed Examination, Review, Smart Study, Random Practice, Weak Topics, and Rapid Revision — all saving progress automatically.
4. **Spaced Repetition Engine (`SpacedRepetitionEngine`)**: SM-2 adaptation (`NEW → LEARNING → REVIEWING → MASTERED`) requiring multi-review streaks and interval thresholds (no single-answer auto-mastery), configurable daily limits, and automatic mistake logging into the Notebook.
5. **Notebook & Flashcards**: Rich note editor, search, subject filtering, pinning, automatic mistake review logs, and one-tap Note-to-Flashcard conversion.
6. **Mock Examination Simulator**: Custom exam builder, full-screen distraction-free interface, persistent `InProgressExamEntity` recovery across app restarts, countdown timer, question navigator grid, flag-for-review, unanswered warnings, auto-submit on expiry, and detailed topic-level exam reports.
7. **Daily Study Planner**: Daily and weekly schedules linked to subjects/units/topics with priorities and daily goal tracking.
8. **Native Android Alarm System (`AlarmManager`)**: Repeating weekday masks, one-time alarms, subject/category alarms, Snooze & Dismiss actions, vibration, `BootReceiver` reboot rescheduling, alarm history, and transparent `SCHEDULE_EXACT_ALARM` permission handling with honest fallback notice.
9. **Focus Timer & Native Android DND (`NotificationManager`)**: Pomodoro & custom timers, foreground service countdown notification, Quick Settings Tile (`FocusQuickSettingsTileService`), and explicit user-consented Notification Policy Access that saves and restores the previous interruption filter.
10. **Student Essentials Hub**: EUEE exam countdown & auto-paced daily targets, exam-hall two-pass time budget helper, interactive `/600` score planner with live Room accuracy sync, Grades 9–12 syllabus completion tracker, exam-day pouch & OMR bubble checklists, and high-yield pocket cheat sheets for all 6 subjects.
11. **21 Offline Scientific & Mathematical Tools**: Scientific expression calculator, graph plotter, linear/quadratic/2×2 simultaneous equation solvers, unit converter, scientific notation, percentage, geometry, trigonometry, logarithms, 2×2 matrix analyzer, statistics calculator, periodic table, molar-mass calculator, rational Gaussian chemical equation balancer, and Math/Physics/Chemistry/Biology/English/Aptitude references.
12. **Question Studio, Import Center & Export Center**: Paginated browsing (`50` per page) for 5,000+ question banks, bulk edit/delete, SAF JSON/CSV/TXT import with duplicate detection & validation report, full JSON backup & restore, printable PDF exam sheets (`PdfDocument`), PNG question cards, and native Android Share Sheet (`FileProvider`).
13. **Android-Native Extras**: Home-screen widgets (`DailyProgressWidgetProvider`, `TodayTasksWidgetProvider`), App Shortcuts, Text-to-Speech with speed controls, Speech Recognition intent (delegated to system speech service), Haptic Feedback, and Keep-Screen-Awake controls.

---

## Project Package Architecture (`app/src/main/java/com/example/`)

- `app/` — `EueeMasteryApp`, `AppContainer`
- `core/` — `Models`, `Components`, `SpeechHelper`, `HapticHelper`
- `database/` — `Entities`, `Daos`, `EueeDatabase`, `SeedDataProvider`
- `data/` — `EueeRepository`, `PreferencesRepository`
- `domain/` — `SpacedRepetitionEngine`, `AnswerValidator`
- `navigation/` — `Routes`, `EueeAppShell`
- `dashboard/`, `essentials/`, `study/`, `questions/`, `exams/`, `planner/`, `alarms/`, `focus/`, `notifications/`, `notebook/`, `analytics/`, `tools/`, `imports/`, `exports/`, `settings/`, `widgets/`

---

## Built APK Output
- A pre-built Debug APK is saved in `apk/EUEE-Mastery-debug.apk` for direct download.

---

## Setup & Offline Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or CLI Gradle with Android SDK Platform 36 (`compileSdk = 36`, `minSdk = 26`).
- JDK 11 or higher.

### Build & Run Tests
```bash
# Run all local JVM unit tests & Robolectric UI/Database integration tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Build Release APK (requires KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD env vars)
gradle :app:assembleRelease
```

### Offline Operation
After Gradle dependencies are cached, the application requires **no internet connection, no Firebase project, and no cloud accounts**. All data is stored locally in Room (`euee_mastery.db`) and Preferences DataStore (`euee_preferences`).

---

## Android Permissions Used

- `POST_NOTIFICATIONS` — Study reminders, alarm alerts, and persistent focus timer notification (Android 13+).
- `SCHEDULE_EXACT_ALARM` — Exact alarm scheduling via `AlarmManager` (falls back honestly to inexact windows if denied).
- `ACCESS_NOTIFICATION_POLICY` — Optional Do Not Disturb integration during Focus sessions (explicitly granted by user in Android Settings; restores previous DND filter on completion).
- `RECEIVE_BOOT_COMPLETED` — Reschedules enabled alarms after device restart.
- `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` — Haptics, alarms, and focus countdown timer.
- **No broad storage permissions** (`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`) are requested; file operations use the zero-permission Android Storage Access Framework (`OpenDocument` / `CreateDocument`).
