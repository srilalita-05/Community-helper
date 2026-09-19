# Community OS — Communities & Flats

This document details the community, block/section, and flat data currently bundled with the Community OS Android application.

- **Developer-Managed Seed Data:** The data documented here represents initial development and demonstration seed data bundled with the application.
- **Runtime Persistence:** The runtime application accesses community and flat data from the local Room database (`CommunityDatabase`) after database initialization.
- **Source of Truth:** [`android/app/src/main/assets/community_data.json`](app/src/main/assets/community_data.json) serves as the developer-managed source of truth for the bundled seed data.
- **Scope:** This seed catalog is intentionally concise for local testing and demonstration purposes; it is not intended to represent an exhaustive real-world community directory.

---

## Current Communities

### Orchard Heights Apartments

- **Address:** 42 Greenfield Boulevard
- **City:** Bengaluru
- **Blocks/Sections:** 4 blocks represented in seed data

#### Block A

| Flat | Floor |
|---|---:|
| A-101 | 1 |
| A-102 | 1 |
| A-201 | 2 |

#### Block B

| Flat | Floor |
|---|---:|
| B-301 | 3 |
| B-304 | 3 |

#### Block C

| Flat | Floor |
|---|---:|
| C-501 | 5 |
| C-502 | 5 |

#### Block D

| Flat | Floor |
|---|---:|
| D-101 | 1 |
| D-102 | 1 |

---

### Palm Meadows Villa

- **Address:** 10 Sunrise Avenue
- **City:** Hyderabad
- **Blocks/Sections:** 2 villas represented in seed data

#### Villa 1

| Flat | Floor |
|---|---:|
| V-101 | 1 |

#### Villa 2

| Flat | Floor |
|---|---:|
| V-102 | 1 |

---

## Seed Data Summary

| Community | Blocks/Sections | Flats |
|---|---:|---:|
| Orchard Heights Apartments | 4 | 9 |
| Palm Meadows Villa | 2 | 2 |
| **Total** | **6** | **11** |

---

## Source of Truth

The developer-managed file [`android/app/src/main/assets/community_data.json`](app/src/main/assets/community_data.json) is the definitive source of truth for the initial bundled community, block, and flat seed data.

### Runtime Architecture Flow

```text
community_data.json (assets)
  → DatabaseInitializer (JSON parsing & idempotent seeding)
    → Room Database (CommunityDao & FlatDao)
      → Application Repositories & UI
```

- When the application launches, `DatabaseInitializer` reads `community_data.json` from assets and populates `CommunityEntity` and `FlatEntity` tables idempotently if records do not already exist.
- Application UI screens and feature repositories query Room DAOs (`CommunityDao`, `FlatDao`) directly.
- The UI layer and feature repositories must never read or parse `community_data.json` directly.
