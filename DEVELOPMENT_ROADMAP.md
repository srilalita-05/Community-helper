# Community OS — Development Roadmap
**Proposed Order — Not Yet Approved. Await Project Owner Explicit Approval Before Beginning Any Phase.**

> Last Updated: 2026-10-03
> Based on: Code audit (HEAD: 0a0e2075), `project description.docx` (v1 original product vision), and `Community_OS_Master_Development_Specification_v2.docx` (v2 living development specification).
> Feature IDs cross-reference `PROJECT_TRACKER.md`.

---

## Roadmap Overview

```
Phase 0 (PROPOSED - NEXT): Establish verified test baseline (./gradlew test)
Phase 1 (COMPLETED):       Core local persistence and baseline resident workflows (HEAD: 0a0e207)
Phase 2 (PROPOSED):        Test baseline validation + critical fixes & database hardening
Phase 3 (PROPOSED):        Admin dashboard & operational resident workflows (Notices, Complaints, Dues, Contacts)
Phase 4 (PROPOSED):        Community social & interaction modules (Feed, Clubs, Events, Skill Sharing, Reviews)
Phase 5 (PROPOSED):        Backend transition, real auth & real-time notifications (Spring Boot, FCM, QR passes)
Phase 6 (PROPOSED):        Payment gateway, media storage & multi-community scaling
Phase 7 (PROPOSED):        AI, analytics, accessibility & release hardening
```

---

## Phase 0 — Test Baseline Verification
**Status: PROPOSED — RECOMMENDED FIRST STEP (Awaiting Approval)**
**Objective:** Establish a verified green automated test baseline before introducing any code modifications.

### Feature IDs
- `TEST-001`, `TEST-002`, `TEST-003`, `TEST-004` (28 existing test files across Auth, ViewModels, Room DAOs, and Migrations)

### Prerequisites
- None (Robolectric tests run locally on the JVM; no Android emulator or device required)

### Tasks
1. Execute `./gradlew test` from the `android/` directory.
2. Record execution metrics: total tests run, passed, failed, and skipped.
3. Diagnose any failing tests resulting from test-harness configuration or environment issues.
4. Record verified test results in `DEVELOPMENT_LOG.md`.
5. Do NOT modify any production application code during this verification phase.

### Deliverables
- Verified `./gradlew test` pass/fail report recorded in `DEVELOPMENT_LOG.md`.
- Updated test status in `PROJECT_TRACKER.md` (`TEST-001` through `TEST-004`).

### Exit Conditions
- All existing tests execute cleanly or have documented failure diagnoses.
- Explicit approval from project owner to begin Phase 2.

---

## Phase 1 — Core Architecture & Baseline Workflows
**Status: COMPLETED (Verified committed on main through HEAD 0a0e207)**
**Objective:** Clean Architecture foundation, local Room persistence, session management, and baseline resident features.

### Committed Features
- `AUTH-001`, `AUTH-002`, `AUTH-004`, `AUTH-005`, `AUTH-007`: Splash, onboarding, registration, community selection, and logout.
- `AUTH-003`: Mock OTP verification (`123456`, `000000`) with simulated latency.
- `AUTH-006`: Flat and role selection (resident, guard, admin).
- `VIS-001`, `VIS-002`, `VIS-003`: Resident pre-approval, visitor list, and security gate processing dashboard.
- `COMP-001`, `COMP-002`: Resident complaint creation and list/details tracking.
- `NOTICE-001`: Resident notice browsing and detail view (seeded from `notice_data.json`).
- `PROF-001`: Resident profile view and editing (`com.communityos.home.ui.ProfileScreen.kt`).
- `MKT-001`, `MKT-002`, `MKT-003`: Marketplace browse, create/edit, and detail view.
- `MAINT-001`, `MAINT-002`: Maintenance dues view and simulated payment recording.
- Local Persistence: Room database version 6 with 9 entities, 8 DAOs, type converters, and 5 registered migrations.
- Architecture: Hilt dependency injection, MVI with StateFlow, Clean Architecture with UseCases and Repositories.

### Remaining Gaps from Phase 1
- `HomeRepositoryImpl.kt` hardcodes `pendingComplaints = 1`.
- `UserEntity.isApproved` is never enforced on login or navigation.
- `photoUri` in `VisitorEntity` and `imageUri` in `MarketplaceListingEntity` are not populated.
- Admin dashboard, Feed, Events, Clubs, Analytics, and Notifications show static placeholders.

---

## Phase 2 — Critical Fixes & Baseline Hardening
**Status: PROPOSED**
**Objective:** Resolve known discrepancies and harden local persistence before expanding feature scope.

### Feature IDs
- Fix hardcoded `pendingComplaints = 1` in `HomeRepositoryImpl.kt` (query real count from `ComplaintDao`).
- `AUTH-008`: Admin approval gate enforcement (hold unapproved residents at pending screen until approved).
- `SEC-005`: Database hardening — add test coverage for `MIGRATION_1_2` in `DatabaseMigrationTest.kt`, review `exportSchema = true`, and plan safe migration strategy.
- Add unit tests verifying `HomeRepositoryImpl` live complaint counting.

### Prerequisites
- Phase 0 complete (verified green test baseline on record).
- Architecture alignment on `AUTH-008` approval workflow UX.

### Deliverables
- Live complaint count reflecting actual Room records on resident dashboard.
- Approval gate preventing unapproved users from accessing resident/admin features.
- 100% test pass rate recorded in `DEVELOPMENT_LOG.md`.

### Exit Conditions
- `./gradlew test` passes cleanly.
- Project owner approves transition to Phase 3.

---

## Phase 3 — Admin Dashboard & Operational Resident Workflows
**Status: PROPOSED**
**Objective:** Replace static admin placeholder with a functional management console and close operational gaps.

### Feature IDs
- `ADMIN-001`: Admin Dashboard master interface (resident list, stats overview, module routing).
- `AUTH-008`: Admin resident approval / rejection interface.
- `COMP-003`: Admin complaint triaging, status updates (`SUBMITTED` -> `IN_PROGRESS` -> `RESOLVED` -> `CLOSED`), and priority assignment.
- `NOTICE-002`: Admin notice creation, scheduling, and local community broadcast.
- `MAINT-003`: Admin maintenance bill generation across flats and collection tracking.
- `MAINT-004`: Maintenance payment receipts & history export for residents.
- `EMER-003`: Society emergency contact directory (security gate, estate manager, local police, fire, hospital).

### Prerequisites
- Phase 2 complete.
- Design approval for Admin navigation structure (dedicated graph vs. role-conditional screens).

### Deliverables
- Working admin workflows for residents, complaints, notices, and billing.
- Downloadable/viewable maintenance receipts for residents.
- Offline emergency contact directory accessible from home dashboard.

---

## Phase 4 — Community Social & Interaction Modules
**Status: PROPOSED**
**Objective:** Implement resident social engagement, interest groups, event participation, and peer collaboration.

### Feature IDs
- `FEED-001`: Community Feed UI and post creation (replaces `FeedPlaceholder.kt`).
- `FEED-002`: Feed interactions (polls, media attachments, like counters, comment threads).
- `CLUB-001`, `CLUB-002`: Club directory, membership join/leave, discussion boards, and club announcements.
- `EVENT-001`: Event listing, calendar view, and event creation (replaces `EventsPlaceholder.kt`).
- `EVENT-002`: Event RSVP, attendance tracking, and volunteer drive sign-ups.
- `SKILL-001`: Dedicated Skill Sharing showcase (coding mentorship, music classes, yoga, chess coaching).
- `SKILL-002`: Skill session scheduling and collaboration requests.
- `MKT-004`: Ratings and reviews (1-5 stars + textual reviews) for marketplace sellers and service providers.

### Prerequisites
- Phase 3 complete.
- Room database schema migration (version 7) to add Feed, Club, Event, Skill, and Review entities.

---

## Phase 5 — Backend Transition, Real Auth & Real-Time Communications
**Status: PROPOSED**
**Objective:** Transition from local-only simulated data to connected backend architecture and real-time alerts.

### Feature IDs
- `BACK-001`: Spring Boot REST API scaffolding in `backend/` with PostgreSQL schema.
- `BACK-002`: Android Retrofit integration (`ApiService.kt`) and offline-first Room synchronization.
- `AUTH-009` / `INT-001`: Real Firebase Phone Auth with SMS delivery and OTP rate limiting.
- `NOTIF-001` / `INT-002`: Firebase Cloud Messaging (FCM) integration for real-time device push.
- `NOTIF-002`: In-app notification center with notification history.
- `NOTIF-003`: Email notification integration for official notices and receipts.
- `VIS-004`: Security gate camera capture for visitor photographs.
- `VIS-005`: Real-time push notification to resident when visitor arrives at gate.
- `VIS-006`: QR-based visitor entry passes with WhatsApp/SMS sharing.
- `VIS-007`: Delivery partner workflow and automated visitor photo lifecycle/cleanup.
- `EMER-002`: Real emergency SOS dispatch alerting gate security, estate admins, and designated contacts.

### Prerequisites
- Phase 4 complete.
- Backend hosting provisioned (PostgreSQL + Spring Boot).
- Firebase project configured with Phone Auth and FCM credentials.

---

## Phase 6 — Payment Gateway, Media Storage & Multi-Community Scaling
**Status: PROPOSED**
**Objective:** Commercial readiness, cloud media persistence, and multi-tenant scaling.

### Feature IDs
- `MAINT-005` / `INT-004`: Real payment gateway integration (Razorpay / UPI Intent / Stripe) replacing simulated payments.
- `INT-003`: Cloudinary or AWS S3 integration for persistent visitor photos, marketplace images, and profile pictures.
- `MULTI-001`: Active community switcher for residents or admins with multiple properties.
- `ROLE-001`: Extended role support (Service Providers, Event Organizers, Volunteers).
- `TEST-005`: Automated instrumentation / UI tests (Compose UI tests / Espresso).

### Prerequisites
- Phase 5 complete.
- Merchant payment gateway and cloud storage API credentials provisioned.

---

## Phase 7 — AI, Analytics, Accessibility & Release Hardening
**Status: PROPOSED**
**Objective:** Intelligent community features, comprehensive analytics, accessibility compliance, and release build security.

### Feature IDs
- `ANAL-001`: Community Analytics Dashboard (active users, collection trends, complaint turnaround).
- `ANAL-002`: AI-driven community insights, event recommendations, and community health scoring.
- `COMP-004`: AI automated complaint categorization and priority triage.
- `SEC-002`: TalkBack accessibility audit and content description compliance.
- `SEC-003`: Release build hardening (`isMinifyEnabled = true`, ProGuard/R8 rules).
- `SEC-004`: Secrets management audit (remove hardcoded test keys).
- Production Release APK / AAB signing and verification.

---

## Architectural Dependency Graph

```
Phase 0: TEST BASELINE (TEST-001..004)
    │
    ▼
Phase 2: HARDENING (Complaint live count, AUTH-008 Gate, Migration tests)
    │
    ▼
Phase 3: ADMIN & OPERATIONS (ADMIN-001, COMP-003, NOTICE-002, MAINT-003/004, EMER-003)
    │
    ▼
Phase 4: SOCIAL & INTERACTION (FEED-001/002, CLUB-001/002, EVENT-001/002, SKILL-001/002, MKT-004)
    │
    ▼
Phase 5: BACKEND & COMMS (BACK-001/002, Firebase Auth, FCM, VIS-004..007, EMER-002)
    │
    ▼
Phase 6: PAYMENTS & SCALE (Razorpay, Cloudinary, MULTI-001, ROLE-001, TEST-005)
    │
    ▼
Phase 7: AI, ANALYTICS & RELEASE (ANAL-001/002, COMP-004, ProGuard, a11y)
```

---

## Project Policy & Next Action

1. **Roadmap Authority:** All phases beyond Phase 1 are PROPOSED recommendations. The project owner reserves the right to modify priorities or phase boundaries.
2. **Current Immediate Recommendation:** Approve Phase 0 execution (`./gradlew test`) to record the verified test baseline before modifying code.
3. **Strict Stopping Rule:** No feature code or Gradle build task will be executed until the project owner provides explicit written authorization.
