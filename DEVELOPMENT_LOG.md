# Community OS — Development Log
Chronological Record of Verified Implementation Work

Rule: Do not fabricate. Only record what is confirmed by git history, code inspection, or actual test runs.
Format for future entries: Date | Task | Evidence | Result | Commit | Notes

## 2026-10-03 — Phase 0B: Automated Test Baseline Execution with JDK 17 (Retry)

### Objective & Authorization
- **Objective:** Execute the existing automated test suite using the pre-installed JDK 17 to establish a verified green test baseline.
- **Authorization:** Explicitly authorized by the project owner for Phase 0B retry only.

### Initial State
- **Branch:** `main`
- **HEAD Commit:** `0a0e2075b6ddb384275b60c917435aaa4786c3fd`
- **Working Tree:** Clean with respect to tracked files. Preserved 4 untracked files (`Community_OS_Master_Development_Specification_v2.docx`, `DEVELOPMENT_LOG.md`, `DEVELOPMENT_ROADMAP.md`, `PROJECT_TRACKER.md`). Zero staged or modified tracked files.

### Execution Details
- **Working Directory:** `D:\Projects\community helper\android`
- **Process Environment:**
  - `$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"`
  - `$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"`
- **Verified Java Runtime:** `java version "17.0.12" 2024-07-16 LTS`, Java(TM) SE Runtime Environment (build 17.0.12+8-LTS-286)
- **Exact Command:** `.\gradlew.bat test`
- **Exit Code:** `0`
- **Overall Gradle Result:** `BUILD SUCCESSFUL in 2m 22s` (60 actionable tasks: 10 executed, 50 up-to-date)
- **Test Tasks Executed:** `:app:testDebugUnitTest`, `:app:testReleaseUnitTest`, `:app:test`

### Test Metrics & Verified Results
- **Total Test Classes Executed:** 37 classes
- **Total Tests Executed:** 216 tests
- **Total Passed:** 216 tests (100% success rate)
- **Total Failed:** 0 tests
- **Total Skipped / Ignored:** 0 tests
- **Duration:** 34.658s (debug test suite)
- **Test Report Paths:**
  - `android/app/build/reports/tests/testDebugUnitTest/index.html`
  - `android/app/build/reports/tests/testReleaseUnitTest/index.html`
  - `android/app/build/test-results/testDebugUnitTest/`
  - `android/app/build/test-results/testReleaseUnitTest/`

### Test Results by Module Category
1. **`TEST-001` Authentication & Session (4 classes, 23 tests — 100% PASS):**
   - `AuthRepositoryTest`: 13 tests passed
   - `OtpVerificationLogicTest`: 3 tests passed
   - `SplashViewModelTest`: 4 tests passed
   - `SessionManagerTest`: 3 tests passed
2. **`TEST-002` Feature ViewModels & Repositories (22 classes, 149 tests — 100% PASS):**
   - Complaints: `ComplaintsViewModelTest` (4), `CreateComplaintViewModelTest` (4), `ComplaintDetailViewModelTest` (3), `ComplaintRepositoryTest` (9) = 20 tests
   - Visitors: `CreateVisitorViewModelTest` (4), `ResidentVisitorsViewModelTest` (3), `ResidentVisitorDetailViewModelTest` (2), `VisitorRepositoryTest` (11) = 20 tests
   - Marketplace: `MarketplaceListViewModelTest` (6), `CreateEditListingViewModelTest` (9), `ListingDetailViewModelTest` (6), `MarketplaceRepositoryTest` (11), `MarketplaceDaoTest` (8 in package) = 38 tests
   - Maintenance: `MaintenanceViewModelTest` (9), `MaintenanceRepositoryTest` (10), `MaintenanceDaoTest` (7 in package) = 26 tests
   - Profile: `ProfileViewModelTest` (8), `ProfileRepositoryTest` (8) = 16 tests
   - Notices: `NoticesViewModelTest` (5), `NoticeDetailViewModelTest` (4), `NoticeRepositoryTest` (7) = 16 tests
   - Security: `SecurityDashboardViewModelTest` (2), `SecurityVisitorDetailViewModelTest` (4) = 6 tests
   - Home: `HomeViewModelTest` (2), `HomeRepositoryTest` (5) = 7 tests
3. **`TEST-003` Room DAOs & Initializer (9 classes, 40 tests — 100% PASS):**
   - `ComplaintDaoTest`: 8 tests passed
   - `MaintenanceDaoTest`: 7 tests passed
   - `MarketplaceDaoTest`: 8 tests passed
   - `NoticeDaoTest`: 6 tests passed
   - `VisitorDaoTest`: 7 tests passed
   - `CommunityDaoTest`: 1 test passed
   - `FlatDaoTest`: 2 tests passed
   - `UserDaoTest`: 4 tests passed
   - `DatabaseInitializerTest`: 10 tests passed
4. **`TEST-004` Database Migrations (1 class, 4 tests — 100% PASS):**
   - `DatabaseMigrationTest`: 4 tests passed (migrations 2→3, 3→4, 4→5, 5→6 verified via PRAGMA column nullability and table structure checks)
   - Note: `MIGRATION_1_2` is defined in DI but not tested in `DatabaseMigrationTest.kt`.

### Repository Safety Confirmation
- **Source & Configuration Changes:** NONE. No application source code, test files, Gradle files, dependencies, manifests, resources, or database files were created, modified, or deleted.
- **Git Commit / Push:** NONE. No git commit, push, reset, or checkout was performed.
- **User Changes Preserved:** All pre-existing untracked and tracking files remain intact.
- **No Fixes Attempted:** No application code was altered.

### Proposed Next Step (Requires Project Owner Approval)
- **Recommendation:** With a verified 100% green test baseline (216/216 passing), proceed to **Phase 2: Critical Fixes & Baseline Hardening** to fix hardcoded `pendingComplaints = 1` in `HomeRepositoryImpl.kt` (connecting to `ComplaintDao`), enforce `AUTH-008` admin approval gate, and add test coverage for `MIGRATION_1_2`.
- **Status:** **PROPOSED — REQUIRES PROJECT OWNER APPROVAL**.

---

## 2026-10-03 — Phase 0: Automated Test Baseline Execution Attempt

### Objective & Authorization
- **Objective:** Execute the existing automated test suite without modifying source code or tests, to establish a verified test baseline.
- **Authorization:** Explicitly authorized by the project owner for Phase 0 only.

### Initial State
- **Branch:** `main`
- **HEAD Commit:** `0a0e2075b6ddb384275b60c917435aaa4786c3fd`
- **Working Tree:** Clean with respect to tracked files. Preserved 4 untracked files (`Community_OS_Master_Development_Specification_v2.docx`, `DEVELOPMENT_LOG.md`, `DEVELOPMENT_ROADMAP.md`, `PROJECT_TRACKER.md`). Zero staged or modified tracked files.

### Execution Details
- **Working Directory:** `D:\Projects\community helper\android`
- **Exact Command:** `.\gradlew.bat test`
- **Exit Code:** `1` (BUILD FAILED in 28s)
- **Overall Gradle Result:** FAILED during Gradle configuration phase before any test tasks were scheduled or executed.

### Test Metrics & Reports
- **Tests Executed:** 0
- **Passed:** 0
- **Failed:** 0
- **Skipped:** 0
- **Test Tasks Executed:** None
- **Test Reports Generated:** None (`android/app/build/reports/tests/` and `android/app/build/test-results/` do not exist).

### Failure Classification & Blocker Details
- **Failure Message:** `FAILURE: Build failed with an exception. * What went wrong: 25.0.3`
- **Classification:** Build Environment / Gradle Configuration Blocker (not a test failure, assertion failure, or code bug).
- **Root Cause:** The default system JVM executing Gradle is OpenJDK 25 (`Temurin-25.0.3+9-LTS`), whereas the project's Android Gradle Plugin version is `8.2.2` (configured in `libs.versions.toml`). AGP 8.2.2 does not recognize or support Java 25 during project configuration, causing Gradle initialization to halt with `25.0.3`.
- **Environment Findings:** A compatible JDK 17 installation was discovered at `C:\Program Files\Java\jdk-17`. However, per strict instructions, no configuration changes, environment alterations, retries, or command substitutions were attempted.

### Repository Safety Confirmation
- **Source & Configuration Changes:** NONE. No application source code, test files, Gradle files, dependencies, manifests, resources, or database files were created, modified, or deleted.
- **Git Commit / Push:** NONE. No git commit, push, reset, or checkout was performed.
- **User Changes Preserved:** All pre-existing untracked and tracking files remain intact.
- **No Fixes Attempted:** In strict compliance with instructions, no test fixes or feature implementation work were attempted.

### Proposed Next Step (Requires Project Owner Approval)
- **Proposal:** Authorize running `.\gradlew.bat test` with `JAVA_HOME` pointing to the existing compatible JDK 17 (`C:\Program Files\Java\jdk-17`) so the 28 test files can execute and record the verified test baseline.
- **Status:** **PROPOSED — REQUIRES PROJECT OWNER APPROVAL**.

---

## 2026-10-03 — Specification Reconciliation and Tracking Document Validation Pass

### Summary of Actions
- Performed an exhaustive, documentation-only verification pass across:
  - `project description.docx` (v1 original product vision — binary parsed via PowerShell XML extraction)
  - `Community_OS_Master_Development_Specification_v2.docx` (v2 living development specification — parsed via PowerShell XML extraction)
  - `README.md`
  - Complete Android codebase under `android/app/src/main/` and `android/app/src/test/`
  - Git repository state at HEAD `0a0e207`
- Reconciled status taxonomy and synchronized `PROJECT_TRACKER.md`, `DEVELOPMENT_ROADMAP.md`, and `DEVELOPMENT_LOG.md`.
- Identified and corrected previous audit claims regarding database configuration, migration tests, and storage mechanisms.

---

### Key Technical Verifications & Corrected Claims

#### 1. Room Database Configuration & Destructive Migration Status
- **Corrected Claim:** The initial audit stated that Room migrations were used *without* `fallbackToDestructiveMigration()`.
- **Verified Code Reality:** In `android/app/src/main/java/com/communityos/data/local/di/DatabaseModule.kt` (line 267–269):
  ```kotlin
  .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
  .fallbackToDestructiveMigration()
  .build()
  ```
  `.fallbackToDestructiveMigration()` is **ACTIVE**. If a migration path is not explicitly matched or if schema divergence occurs, Room will drop all tables destructively.
- **Database Schema Export:** In `CommunityDatabase.kt` line 38: `exportSchema = false`. Room schema json is not exported into git.
- **Registered Migrations:** 5 migrations registered in DI: `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5`, `MIGRATION_5_6`.
- **Migration Test Discrepancy:** In `DatabaseMigrationTest.kt`, only 4 test methods exist (testing 2→3, 3→4, 4→5, 5→6). Migration 1→2 is implemented in DI but has no automated test in `DatabaseMigrationTest.kt`.
- **Entities (9):** `UserEntity`, `CommunityEntity`, `FlatEntity`, `NoticeEntity`, `ComplaintEntity`, `VisitorEntity`, `MarketplaceListingEntity`, `MaintenanceBillEntity`, `MaintenancePaymentEntity`.
- **DAOs (8):** `UserDao`, `CommunityDao`, `FlatDao`, `NoticeDao`, `ComplaintDao`, `VisitorDao`, `MarketplaceDao`, `MaintenanceDao`.
- **Type Converters:** `RoomConverters` handles `UserRole`, `ComplaintStatus`, `VisitorStatus`, `MarketplaceCategory`, `ListingStatus`, `BillStatus`, `PaymentMethod`.

#### 2. Session Persistence Mechanism
- **Verified Code Reality:** `android/app/src/main/java/com/communityos/data/local/session/SessionManager.kt` uses Jetpack Preferences DataStore:
  ```kotlin
  val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")
  ```
- **EncryptedSharedPreferences:** Search confirms zero occurrences in the codebase. Sensitive tokens are not currently encrypted on disk.

#### 3. Profile Architecture & Screen Placement
- **Verified Code Reality:** `ProfileScreen.kt` resides in package `com.communityos.home.ui.ProfileScreen.kt`.
- Domain use cases (`GetProfileUseCase`, `UpdateProfileUseCase`), repository (`ProfileRepositoryImpl`), and `ProfileViewModel` are in `com.communityos.profile.*`. Package `com.communityos.profile.ui` does not exist.

#### 4. Pre-Packaged Assets & Seeding
- `community_data.json` (2.3 KB): Contains 2 communities ("Orchard Heights Apartments" with 4 blocks and 8 flats; "Palm Meadows Villa" with 2 villas and 2 flats).
- `notice_data.json` (2.0 KB): Contains 4 pre-seeded notices.
- Maintenance bills are seeded directly in code via `DatabaseInitializer.kt` (lines 67–137), not via external JSON.

#### 5. User Approval Enforcement
- `UserEntity.isApproved` exists in the Room schema (default 0), but is never read or checked during login, session restore, or screen routing. Any user can select Admin or Security Guard role during onboarding and immediately enter the corresponding dashboard.

#### 6. Dashboard Hardcoded Metrics
- `HomeRepositoryImpl.kt` line 50 hardcodes `pendingComplaints = 1` as a literal integer. It does not query `ComplaintDao`.

---

### Specification Reconciliation (v1 vs. v2 vs. Implementation)

| Module / Requirement | Present in v1 Docx | Present in v2 Docx | Codebase State | Status in Tracker |
|---|---|---|---|---|
| Splash & Session Restore | Yes | Yes | Implemented with DataStore | COMPLETE (`AUTH-001`) |
| Phone OTP Login | Yes | Yes | Mock OTP (`123456`, `000000`) | PARTIAL (`AUTH-003`) |
| Admin Approval Gate | Yes | Yes | `isApproved` column exists, not enforced | NOT STARTED / BLOCKED (`AUTH-008`) |
| Extended Roles (Volunteers, Organizers, Providers) | Yes | Yes | Only Resident, Guard, Admin exist | NOT STARTED (`ROLE-001`) |
| Community Feed MVP | Yes | Yes | `FeedPlaceholder.kt` only | FOUNDATION (`FEED-001`) |
| Feed Polls, Media & Interactions | Yes | Yes | None | NOT STARTED (`FEED-002`) |
| Clubs & Communities MVP | Yes | Yes | `ClubDetailsScreen.kt` static box | FOUNDATION (`CLUB-001`) |
| Club Directory & Discussion Boards | Yes | Yes | None | NOT STARTED (`CLUB-002`) |
| Visitor Pre-Approval & Processing | Yes | Yes | Implemented in Room + UI | COMPLETE (`VIS-001..003`) |
| Security Visitor Photo Capture | Yes | Yes | `photoUri` column exists; permanently null | NOT STARTED (`VIS-004`) |
| Real-time Resident Approval Push | Yes | Yes | None (no FCM) | NOT STARTED / BLOCKED (`VIS-005`) |
| QR-Based Visitor Entry Passes | Yes | Yes | None | NOT STARTED (`VIS-006`) |
| Delivery Workflow & Photo Auto-Deletion | Yes | Yes | None | NOT STARTED (`VIS-007`) |
| Resident Complaints MVP | Yes | Yes | Implemented in Room + UI | COMPLETE (`COMP-001..002`) |
| Admin Complaint Triage & Assignment | Yes | Yes | None | NOT STARTED (`COMP-003`) |
| AI Complaint Categorization | Yes | Yes | None | NOT STARTED / FUTURE (`COMP-004`) |
| Event Management MVP | Yes | Yes | `EventsPlaceholder.kt` only | FOUNDATION (`EVENT-001`) |
| Event RSVP, Attendance & Volunteers | Yes | Yes | None | NOT STARTED (`EVENT-002`) |
| Marketplace MVP | Yes | Yes | Implemented in Room + UI | COMPLETE (`MKT-001..003`) |
| Marketplace Ratings & Reviews | Yes | Yes | None | NOT STARTED (`MKT-004`) |
| Dedicated Skill Sharing Module | Yes | Yes | None (Marketplace "Services" is not Skill Sharing) | NOT STARTED (`SKILL-001..002`) |
| Emergency Panic SOS | Yes | Yes | UI banner only; repo does `delay(800)` | PARTIAL (`EMER-001`) |
| Emergency Contacts Directory | Yes | Yes | None | NOT STARTED (`EMER-003`) |
| Maintenance Dues View | Yes | Yes | Dynamic Room query | COMPLETE (`MAINT-001`) |
| Resident Bill Payment | Yes | Yes | Simulated UPI record in Room | PARTIAL (`MAINT-002`) |
| Admin Dues Generation & Statistics | Yes | Yes | None | NOT STARTED (`MAINT-003`) |
| Maintenance Receipts & History Export | Yes | Yes | None | NOT STARTED (`MAINT-004`) |
| Real Payment Gateway (Razorpay/Stripe) | Yes | Yes | None | NOT STARTED / DEFERRED (`MAINT-005`) |
| Push Notifications (FCM) | Yes | Yes | `NotificationsPlaceholder.kt` only | FOUNDATION (`NOTIF-001`) |
| In-App Notification Center | Yes | Yes | None | NOT STARTED (`NOTIF-002`) |
| Email Notifications | Yes | Yes | None | NOT STARTED (`NOTIF-003`) |
| Community Analytics Dashboard | Yes | Yes | `AnalyticsPlaceholder.kt` only | FOUNDATION (`ANAL-001`) |
| AI Community Health & Recommendations | Yes | Yes | None | NOT STARTED / FUTURE (`ANAL-002`) |
| Admin Dashboard Console | Yes | Yes | `AdminDashboardScreen.kt` 36-line static text | FOUNDATION (`ADMIN-001`) |
| Spring Boot Backend & PostgreSQL | Yes | Yes | `backend/` empty; `ApiService.kt` empty | NOT STARTED / DEFERRED (`BACK-001..002`) |

---

## 2026-10-03 — Initial Audit and Tracker Setup

### Baseline Established
- Audited By: Automated code inspection (all source files read directly)
- Branch: `main`
- HEAD: `0a0e2075b6ddb384275b60c917435aaa4786c3fd`
- Remote Status: `origin/main` — up to date
- Untracked Files: `Community_OS_Master_Development_Specification_v2.docx` (present on disk, not committed to git)

### Tracking Files Created
- `PROJECT_TRACKER.md` — feature master checklist with verified statuses
- `DEVELOPMENT_ROADMAP.md` — dependency-aware proposed implementation sequence
- `DEVELOPMENT_LOG.md` — this file

---

## Git Commit History (Full — Recovered from git log)

All commits are on the `main` branch. Listed oldest-to-newest.

| Num | Short Hash | Full Hash | Subject |
|---|---|---|---|
| 1 | c67c5ee | c67c5ee8e3216288bb44f2046f963aa7bea6f6a9 | first commit |
| 2 | 911097f | 911097fcd4b8b836c2a3dc6681f4bd9a6cc89460 | Improve README.md formatting, relative links, and Mermaid diagrams |
| 3 | 00e53b8 | 00e53b8d92ce6ecb858334f369da46c815ceb466 | updated readme |
| 4 | abf664e | abf664e28698ea19ebeb297d4c85d41147fbdf54 | feat: add local persistence and fix OTP input |
| 5 | 13373a7 | 13373a7e2c8f1f84faed50568d1ee40e23471d60 | feat: connect auth and home to local persistence |
| 6 | 7337e1b | 7337e1b7783b2433f056a31f09bb92abfe25cbb3 | feat: move community seed data to json |
| 7 | ed8ee15 | ed8ee1546065f5c10bb67cbddc624c9c897a5978 | feat: add session-aware app startup |
| 8 | d146b1f | d146b1f89f17fa710e6019bf581a22d7462e1877 | feat: add logout and session lifecycle |
| 9 | 2469135 | 2469135660ae66d9c72456ddae25efa9fff24796 | feat: add resident profile management |
| 10 | 2fb6add | 2fb6add619d5c5df71faa68fea35f6cd6d2d83af | feat: add resident notices |
| 11 | 0c7e813 | 0c7e81354641d612972d1a19ae37885f24131129 | docs: add community and flat documentation |
| 12 | af5f846 | af5f84625e06963698cf9d3630373a10c90b18e2 | feat: add resident complaints MVP |
| 13 | df49b00 | df49b00adf68dcb15fdf3dffcbf81b95ee3271c0 | feat: add resident visitor management and security workflow |
| 14 | 5802b45 | 5802b456ec4c224eea533d470325bfaf2082e9db | feat: add resident marketplace MVP |
| 15 | 0a0e207 | 0a0e2075b6ddb384275b60c917435aaa4786c3fd | feat: add resident maintenance dues and billing |

---

## Architecture Decisions Recorded (Inferred from Code)

### Decision 1: Local-First Architecture with Room
All active data is persisted in Room (SQLite). No remote API calls are executed.
Retrofit and OkHttp dependencies exist in `build.gradle.kts` but `ApiService.kt` has zero methods.
`SessionManager` uses Jetpack Preferences DataStore for session persistence.
Status: In effect through HEAD `0a0e207`.

### Decision 2: Versioned Room Migrations with Destructive Fallback
Database version = 6.
Explicit migrations registered: `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`, `MIGRATION_4_5`, `MIGRATION_5_6`.
`.fallbackToDestructiveMigration()` is explicitly active in `DatabaseModule.kt`.
`DatabaseMigrationTest.kt` verifies migrations 2→3, 3→4, 4→5, 5→6. Migration 1→2 lacks an automated test.
`exportSchema = false` in `CommunityDatabase.kt`.
Status: In effect. Production hardening will require removing fallback to destructive migration and enabling schema export.

### Decision 3: Mock OTP (No Real SMS)
OTP verification accepts hardcoded values: `123456` or `000000`.
Implemented with `delay(1000)` to simulate network latency.
Status: Active in all builds. Must be replaced with Firebase Phone Auth in Phase 5.

### Decision 4: Simulated Payment
Payment flow creates local Room records with `PaymentMethod.UPI_SIMULATED` and a simulated transaction reference.
No external payment gateway SDK is integrated.
Status: Active. Must be replaced with Razorpay / UPI Intent in Phase 6.

### Decision 5: MVI + Clean Architecture
UI -> ViewModel (StateFlow) -> UseCase -> Repository (interface) -> Impl (Room).
Hilt DI for all injection. `@HiltViewModel` on all ViewModels.
Modular navigation: `authGraph` and `homeGraph` extension functions on `NavGraphBuilder`.
Status: Consistent pattern across all implemented modules.

### Decision 6: JSON-Seeded Community and Notice Data
`community_data.json` and `notice_data.json` in `app/src/main/assets/`.
`DatabaseInitializer` loads these idempotently on first app launch.
Maintenance seed data is hardcoded in `DatabaseInitializer.kt` (lines 67–137).
Status: In effect.

---

## Test Suite Status (As of 2026-10-03 Audit)

| Category | File Count | Run Verified | Notes |
|---|---|---|---|
| Auth / Session | 4 files | UNVERIFIED | Robolectric-based JVM tests |
| Feature ViewModels | 14 files | UNVERIFIED | Robolectric-based JVM tests |
| Room DAOs | 9 files | UNVERIFIED | Robolectric-based in-memory SQLite |
| DB Migration | 1 file | UNVERIFIED | Tests migrations 2→3, 3→4, 4→5, 5→6 (1,038 lines) |
| Instrumentation | 0 files | N/A | No `androidTest/` directory |
| **Total** | **28 files** | **None verified** | Baseline execution pending Phase 0 approval |

---

## Room Database Schema (Version 6)

| Table | Entity | Key Columns |
|---|---|---|
| `users` | `UserEntity` | `id`, `phoneNumber`, `name`, `email`, `role`, `communityId`, `flatId`, `isApproved`, `createdAt` |
| `communities` | `CommunityEntity` | `id`, `name`, `address`, `city` |
| `flats` | `FlatEntity` | `id`, `communityId`, `block`, `flatNumber`, `floor` |
| `notices` | `NoticeEntity` | `id`, `communityId`, `title`, `content`, `createdAt`, `updatedAt` |
| `complaints` | `ComplaintEntity` | `id`, `residentId`, `communityId`, `flatId`, `category`, `description`, `status`, `createdAt`, `updatedAt` |
| `visitors` | `VisitorEntity` | `id`, `residentId`, `communityId`, `flatId`, `name`, `phoneNumber`, `purpose`, `vehicleNumber`, `scheduledArrivalDate`, `status`, `photoUri`, `checkInTime`, `checkOutTime`, `verifiedBySecurityId`, `createdAt`, `updatedAt` |
| `marketplace_listings` | `MarketplaceListingEntity` | `id`, `residentId`, `communityId`, `title`, `description`, `category`, `price`, `contactPhone`, `status`, `imageUri`, `createdAt`, `updatedAt` |
| `maintenance_bills` | `MaintenanceBillEntity` | `id`, `flatId`, `communityId`, `title`, `period`, `amount`, `dueDate`, `status`, `createdAt`, `updatedAt` |
| `maintenance_payments` | `MaintenancePaymentEntity` | `id`, `billId`, `flatId`, `residentId`, `amountPaid`, `paymentMethod`, `transactionRef`, `paymentDate` |

---

## Build Configuration (HEAD 0a0e207)

| Setting | Value |
|---|---|
| Kotlin | 1.9.22 |
| Compose Compiler Extension | 1.5.8 |
| Compile SDK | 34 |
| Target SDK | 34 |
| Min SDK | 26 (Android 8.0 Oreo) |
| Version Code | 1 |
| Version Name | 1.0 |
| `isMinifyEnabled` release | `false` (ProGuard / R8 disabled) |
| Room Version | 2.6.1 |
| Hilt Version | 2.50 |
| Retrofit Version | 2.9.0 |
| DataStore Version | 1.0.0 |
| Robolectric Version | 4.11.1 |