# Project Plan

GhostPlay: A gaming activity tracker using Firebase for stats. Tech stack: Kotlin Multiplatform, Compose Multiplatform, Jetpack Compose, KmpTor / Ktor, Firebase Database. User activity and game played time should be stored for future statistics. Keep 4-digit versioning increment (0.0.0.x).

## Project Brief

# GhostPlay Project Brief


GhostPlay is a streamlined gaming activity tracker designed to monitor and store playtime statistics. The application focuses on providing gamers with a clear overview
 of their habits through real-time data synchronization and an adaptive Material 3 interface.

### Features
*   **Session
 Tracking**: Start and stop timers to record precise playtime for individual games.
*   **Gaming Dashboard**: View detailed statistics and historical
 data of gaming sessions.
*   **Firebase Synchronization**: Securely store and retrieve user activity and playtime stats using Firebase
.
*   **Game Library Management**: Add and organize a personalized list of games to track.
*   **Adaptive
 Statistics View**: A responsive UI that provides deep insights into gaming patterns across various device form factors.

### High-Level Technical Stack
*   
**Kotlin**: Primary language for the application logic.
*   **Jetpack Compose**: Modern toolkit for building native Material 3 user
 interfaces.
*   **Jetpack Navigation 3**: State-driven navigation for robust screen transitions and deep linking.
*   **
Compose Material Adaptive**: Implementation of adaptive layouts to support mobile, tablets, and foldables.
*   **Firebase Real
time Database/Firestore**: Cloud-based persistence for user statistics and activity logs.
*   **Ktor**: Lightweight
 networking client for external data requirements.
*   **Kotlin Coroutines**: Asynchronous programming for non-blocking operations.

*
Note: The project follows a 4-digit versioning scheme (e.g., 0.0.0.
1).*

## Implementation Steps

### Task_1_Foundation_and_Library: Set up Firebase integration, Navigation 3 architecture, and the Material 3 theme. Implement the Game Library data layer and UI for managing games.
- **Status:** COMPLETED
- **Updates:** Set up Firebase dependencies, Navigation 3 architecture, and a vibrant Material 3 theme. Implemented the Game Library with Firestore data layer and UI for adding/viewing games. Configured Edge-to-Edge display and 4-digit versioning (0.0.0.1). Created an adaptive icon. App builds successfully.
- **Acceptance Criteria:**
  - Firebase is initialized and connected
  - Navigation 3 host is configured
  - Material 3 theme with vibrant color scheme is applied
  - User can add and view games in the library
  - App builds and runs successfully

### Task_2_Tracking_and_Data: Implement the session tracking logic including a timer for gameplay and data synchronization with Firebase. Create the session history repository.
- **Status:** COMPLETED
- **Updates:** Implemented Session data model and FirebaseSessionRepository. Added a real-time timer for tracking gameplay sessions. Created the Game Details screen to display total playtime and session history. Integrated tracking with the Navigation 3 system. Maintained 0.0.0.1 versioning.
- **Acceptance Criteria:**
  - Start/Stop timer correctly records session duration
  - Sessions are persisted to Firebase/Local DB
  - Playtime statistics are updated in real-time
  - Code follows 4-digit versioning (0.0.0.1)

### Task_3_Adaptive_UI_Stats: Develop the adaptive Dashboard and Statistics screens using Compose Material Adaptive. Integrate charts or summaries to visualize gaming habits.
- **Status:** COMPLETED
- **Updates:** Implemented an adaptive DashboardScreen using NavigationSuiteScaffold for responsive navigation (BottomBar vs NavRail). Integrated statistics engine to calculate total playtime and top games. Seamlessly connected Dashboard and Library within the Navigation 3 system. Used M3 components for a modern look.
- **Acceptance Criteria:**
  - Dashboard displays total playtime and trends
  - UI adapts to compact, medium, and expanded window sizes
  - Smooth transitions between library and stats using Navigation 3
  - M3 components (Cards, TopAppBar) used throughout
- **Duration:** N/A

### Task_4_Final_Polish_Verify: Add an adaptive app icon, refine the 'vibrant' Material 3 theme, and perform a final verification of the application.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Adaptive icon matching GhostPlay brand is created
  - Full Edge-to-Edge display implemented
  - App version is set to 0.0.0.1
  - Final build pass and crash-free execution verified
  - All existing tests pass
- **StartTime:** 2026-06-27 14:01:56 IST

