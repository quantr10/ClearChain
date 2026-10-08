# ClearChain

![.NET 8](https://img.shields.io/badge/.NET-8.0-512BD4?logo=dotnet&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-3FCF8E?logo=supabase&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

Surplus food clearance platform connecting grocery stores with NGOs to reduce food waste.

Grocery stores post surplus stock as clearance listings. NGOs browse nearby listings, check
out a cart of items as pickup requests, collect the goods and track them in their own
inventory. Admins verify organizations, resolve disputes and monitor the platform.

The repository holds two applications:

| Application | Folder | Description |
| --- | --- | --- |
| API | [`Backend/`](Backend) | ASP.NET Core Web API with SignalR and Hangfire |
| Android app | [`Mobile/`](Mobile) | Kotlin / Jetpack Compose client for all three roles |

## Table of Contents

- [Background](#background)
  - [Features](#features)
  - [Built with](#built-with)
- [Install](#install)
  - [Dependencies](#dependencies)
  - [Steps](#steps)
- [Usage](#usage)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [API](#api)
- [Maintainers](#maintainers)
- [Contributing](#contributing)
- [License](#license)

## Background

Grocery stores regularly hold stock that is still good but won't sell in time, while NGOs
need food to distribute. ClearChain gives them a shared place to meet: surplus becomes
listings that local NGOs can claim. Each pickup is tracked from request to collection,
and the goods carry on into the NGO's inventory until they are distributed. Organizations
are verified by an admin before they can trade, so both sides know who they are dealing
with.

### Features

**Grocery stores**
- Create listings from a photo; Azure Computer Vision suggests the title, category,
  an estimated expiry date and a quality grade
- Edit, archive, restore and adjust the quantity of listings
- Approve, reject and mark pickup requests ready
- Export listings and requests as CSV
- Dashboard with today's summary, activity and analytics

**NGOs**
- Browse listings with filters and a distance radius around a chosen location
- Save listings, build a cart and check out several items as grouped pickup requests
- Confirm a pickup with a proof photo, which moves the items into inventory
- Track inventory, mark items distributed, and see what is expiring

**Admins**
- Verification queue for new organizations, with approve/reject and a reason
- Platform statistics, alerts and the full list of transactions (exportable as CSV)
- Dispute review and resolution, with both parties notified

**All roles**
- Per-pickup messaging, post-pickup reviews and disputes on completed pickups
- Realtime updates over SignalR, push notifications over Firebase, and a notification inbox
- Public organization profiles with ratings
- Light/dark theme and English/Vietnamese, switchable in Settings

### Built with

**Backend**

| Concern | Technology |
| --- | --- |
| Framework | .NET 8, ASP.NET Core Web API, Swagger (Swashbuckle) |
| Data | Entity Framework Core 8, PostgreSQL on Supabase, Supabase Storage |
| Auth | JWT access tokens, rotating refresh tokens, BCrypt |
| Realtime | SignalR |
| Background jobs | Hangfire with PostgreSQL storage |
| Integrations | Azure Computer Vision, FirebaseAdmin (push), MailKit (email) |
| Logging and config | Serilog (console and rolling file), DotNetEnv |

**Mobile**

| Concern | Technology |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM with `data` / `domain` / `presentation` layers, Hilt |
| Networking | Retrofit, OkHttp, kotlinx.serialization, SignalR Java client |
| Storage | Room (offline cache), DataStore (preferences) |
| Platform | Firebase Cloud Messaging, Google Maps Compose, Play Services location |
| Images | Coil |

The app supports Android 8.0 (API 26) and up, compiles against API 36, targets API 34 and
builds with JDK 17.

## Install

### Dependencies

- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- [Android Studio](https://developer.android.com/studio) with JDK 17 and the Android SDK (API 36)
- A [Supabase](https://supabase.com) project (PostgreSQL database and Storage)
- An [Azure Computer Vision](https://azure.microsoft.com/products/ai-services/ai-vision) resource
- A [Firebase](https://console.firebase.google.com) project with an Android app registered
  as `com.clearchain.app`
- A Google Maps API key with the Maps SDK for Android enabled
- An SMTP account for sending email (optional for local development)

### Steps

**1. Clone the repository**

```bash
git clone https://github.com/quantr10/ClearChain.git
cd ClearChain
```

**2. Create the Supabase Storage buckets**

In **Storage**, create these buckets and make them **public**. The API returns public URLs
to the files it uploads.

| Bucket | Holds | Limits enforced by the API |
| --- | --- | --- |
| `avatars` | Profile pictures | Images, 5 MB |
| `documents` | Verification documents | Images, PDF or Word, 10 MB |
| `food-images` | Listing photos | Images, 5 MB |
| `pickup-proofs` | Pickup confirmation photos | Images, 5 MB |
| `disputes` | Dispute evidence | Images, 5 MB |

If you change a bucket's limits in Supabase, update
[`StorageBucketPolicy.cs`](Backend/ClearChain.API/Common/StorageBucketPolicy.cs) to match.

**3. Configure the backend**

```bash
cd Backend/ClearChain.API
cp .env.example .env
```

Fill in `.env` (see [Configuration](#configuration)). For push notifications, place your
Firebase service-account key in the same folder as `firebase-adminsdk.json`.

**4. Create the database schema**

The repository does not include migrations. From `Backend/ClearChain.API`, generate a
baseline and apply it to an empty database:

```bash
dotnet tool install --global dotnet-ef --version 8.0.11   # once
dotnet ef migrations add InitialCreate --project ../ClearChain.Infrastructure
dotnet ef database update --project ../ClearChain.Infrastructure
```

The `ef` commands start the API to read its configuration, which is why they run from the
folder that holds `.env`. Hangfire creates its own tables in a `hangfire` schema the first
time the API starts.

**5. Set up the Android app**

1. Download `google-services.json` for `com.clearchain.app` from the Firebase console and
   place it at `Mobile/app/google-services.json`. The build fails without it.
2. Add your values to `Mobile/local.properties` (see [Configuration](#configuration)).
3. Open the `Mobile` folder in Android Studio and sync Gradle.

**6. Create the first admin account**

The app can only register grocery and NGO accounts. After [starting the API and the app](#usage),
register an account in the app, verify its email, then promote it in the Supabase SQL
editor:

```sql
UPDATE organizations
SET type = 'admin', verified = true, verificationstatus = 'approved'
WHERE email = 'you@example.com';
```

Sign out and back in so the new role is in your token. Admins skip onboarding and land on
the admin dashboard.

## Usage

Start the API, then build and install the app on a running emulator or connected device:

```bash
# API on http://localhost:5000
cd Backend/ClearChain.API
dotnet run

# Android app
cd Mobile
./gradlew installDebug
```

The API checks its required variables at startup and stops with a message listing any that
are missing. To confirm it is up:

```bash
curl http://localhost:5000/
curl http://localhost:5000/api/health/jobs
```

### A pickup, end to end

1. **Grocery** creates a listing from a photo and publishes it.
2. **NGO** finds it in Browse, adds it to the cart and checks out, which creates a pickup
   request per item.
3. **Grocery** approves the request, then marks it ready.
4. **NGO** collects the goods and confirms the pickup with a proof photo. The items move
   into the NGO's inventory.
5. Either side can leave a review, or open a dispute that an admin reviews and resolves.

New organizations first confirm their email with a 6-digit code, complete the onboarding
profile and upload a verification document. Until an admin approves them they can browse,
but creating listings, requesting pickups and checking out are blocked.

### Developer endpoints

| URL | What it shows |
| --- | --- |
| `GET /` | API status |
| `GET /api/health/signalr` | Hub routes |
| `GET /api/health/jobs` | Background job schedule |
| `/swagger` | Interactive API docs (only when `ASPNETCORE_ENVIRONMENT=Development`) |
| `/hangfire?access_token=<token>` | Hangfire dashboard (admins only) |

The Hangfire dashboard takes the access token in the URL because a browser tab can't send
an `Authorization` header.

Serilog writes to the console and to a daily file in `logs/` next to the built API (for
example `bin/Debug/net8.0/logs/`), keeping 30 days.

## Configuration

### Backend

The file is `Backend/ClearChain.API/.env`, and
[`.env.example`](Backend/ClearChain.API/.env.example) lists every variable. Environment
variables set in the shell or by a host work too and take precedence.

| Variables | Used for | If missing |
| --- | --- | --- |
| `DATABASE_URL` | PostgreSQL connection (also used by Hangfire) | API won't start |
| `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` | File storage | API won't start |
| `JWT_SECRET_KEY`, `JWT_ISSUER`, `JWT_AUDIENCE` | Signing sign-in tokens | API won't start |
| `JWT_EXPIRY_MINUTES`, `REFRESH_TOKEN_EXPIRY_DAYS` | Token lifetimes | Defaults to 60 minutes and 7 days |
| `AZURE_VISION_ENDPOINT`, `AZURE_VISION_KEY` | Food image analysis | API won't start |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM` | Verification and password-reset emails | Emails are skipped and a warning is logged |
| `ALLOWED_ORIGINS` | CORS for browser clients, comma-separated | Only `http://localhost:3000` is allowed |
| `ASPNETCORE_ENVIRONMENT` | `Development` enables Swagger | Production behavior, no Swagger |

Notes:
- `JWT_SECRET_KEY` must be at least 32 characters.
- `firebase-adminsdk.json` is optional. Without it, push is disabled and notifications
  still arrive over SignalR and in the inbox.
- Without SMTP, nobody receives a verification code. For local development, mark an account
  verified directly: `UPDATE organizations SET emailverified = true WHERE email = '...';`

### Android

The file is `Mobile/local.properties`:

```properties
sdk.dir=path/to/your/Android/sdk
API_BASE_URL=http://10.0.2.2:5000/api/
MAPS_API_KEY=your_google_maps_key
```

| Key | Used for |
| --- | --- |
| `sdk.dir` | Android SDK location (Android Studio writes this) |
| `API_BASE_URL` | API address ending in `/api/`; defaults to `http://10.0.2.2:5000/api/`, the emulator's address for your computer |
| `MAPS_API_KEY` | Google Maps key for the location picker and maps |

`API_BASE_URL` is the only place the server address is set. The app derives the SignalR hub
root from it by dropping the `/api` path, and adds a missing trailing slash.

### Physical devices

The network security config allows plain HTTP only to `10.0.2.2` and `localhost`. The
simplest setup is to forward the port over USB and point the app at `localhost`:

```bash
adb reverse tcp:5000 tcp:5000
```

```properties
API_BASE_URL=http://localhost:5000/api/
```

To reach the API over Wi-Fi by IP instead, serve it over HTTPS or add that IP to
[`network_security_config.xml`](Mobile/app/src/main/res/xml/network_security_config.xml).

## Architecture

### Project structure

```
ClearChain/
├── Backend/
│   ├── ClearChain.sln
│   ├── ClearChain.API/              ASP.NET Core host
│   │   ├── Controllers/             One controller per /api route group
│   │   ├── DTOs/                    Request and response types, grouped by feature
│   │   ├── Services/                Business logic, storage, email, push, image analysis
│   │   ├── Hubs/                    SignalR hubs
│   │   ├── Jobs/                    Hangfire recurring jobs
│   │   ├── Middleware/              Error handling, Hangfire auth, verified-org filter
│   │   ├── Common/                  Shared helpers and policies
│   │   ├── Program.cs               Startup, DI, auth, CORS, jobs, hub routes
│   │   └── .env.example             Every configuration variable the API reads
│   ├── ClearChain.Domain/           Entities and enums
│   └── ClearChain.Infrastructure/   ApplicationDbContext (EF Core mapping)
└── Mobile/
    ├── app/src/main/java/com/clearchain/app/
    │   ├── data/                    Room, Retrofit APIs and DTOs, SignalR client, repositories
    │   ├── domain/                  Models, repository interfaces, use cases
    │   ├── presentation/            Compose screens and ViewModels per feature, navigation
    │   ├── ui/theme/                Colors, typography, spacing tokens
    │   └── util/                    Shared helpers
    ├── app/src/main/res/            Resources; strings in values/ (en) and values-vi/ (vi)
    └── gradle/libs.versions.toml    Dependency versions
```

### Authentication

- Sign-in returns a 60-minute access token and a refresh token. The refresh token rotates
  on every use, and the app refreshes silently when a request gets a 401.
- Five wrong passwords lock an account for 15 minutes.
- Email verification and password reset both use 6-digit emailed codes. A code lasts 15
  minutes and is cancelled after five wrong tries.
- Resetting a password signs out every device.

### Verification gate

Endpoints that create activity (listings, pickup requests, checkout) carry
`[RequireVerifiedOrganization]` and refuse organizations an admin hasn't approved.

### Realtime and offline

The API pushes changes to the app over these SignalR hubs. Data marked as cached is kept in
Room, so its screens still show it when the network is unavailable.

| Hub | Who can connect | Events | Cached offline |
| --- | --- | --- | --- |
| `/hubs/pickuprequests` | Any signed-in user | `PickupRequestCreated`, `PickupRequestStatusChanged`, `PickupRequestUpdated`, `PickupRequestCancelled` | Yes |
| `/hubs/listings` | Any signed-in user | `ListingCreated`, `ListingUpdated`, `ListingDeleted`, `ListingQuantityChanged` | Yes |
| `/hubs/inventory` | Any signed-in user | `InventoryItemAdded`, `InventoryItemDistributed`, `InventoryItemExpired` | Yes |
| `/hubs/notifications` | Any signed-in user | `NotificationReceived` | Yes |
| `/hubs/admin` | Admins only | `NewOrganizationRegistered`, `TransactionCompleted`, `StatsUpdated`, `SystemAlert` | No |

Clients pass the access token as an `access_token` query parameter.

### Background jobs

Hangfire runs these recurring jobs (all times UTC):

| Job | Schedule | What it does |
| --- | --- | --- |
| `check-expiring-listings` | Daily 00:00 | Warn groceries about listings expiring tomorrow |
| `check-expired-listings` | Daily 00:05 | Mark listings expired and notify |
| `check-expiring-inventory` | Daily 00:10 | Warn NGOs about inventory expiring within 2 days |
| `check-expired-inventory` | Daily 00:15 | Mark inventory expired and notify |
| `expire-stale-pickup-requests` | Daily 00:20 | Cancel pending requests past their pickup date and release the stock |
| `cleanup-refresh-tokens` | Daily 01:00 | Delete revoked or expired refresh tokens older than 30 days |
| `cleanup-old-notifications` | Daily 01:10 | Delete read notifications after 30 days, all after 90 |
| `prune-stale-fcm-tokens` | Sundays 01:20 | Drop device tokens not re-registered in 60 days |
| `broadcast-platform-stats` | Hourly | Push fresh statistics to the admin dashboard |

## API

Endpoints are grouped by controller under `/api`. Full request and response schemas are in
Swagger at `/swagger` when running in Development.

| Route | Purpose |
| --- | --- |
| `/api/auth` | Register, email verification, login, refresh, logout, password change and reset, account deletion, device token |
| `/api/organizations` | Own profile, avatar and verification document, stats and activity, public profiles and reputation |
| `/api/listings` | Listing CRUD, quantity, archive/restore, search by category and distance |
| `/api/cart` | NGO cart and checkout, which creates the pickup requests |
| `/api/pickuprequests` | Request lifecycle: approve, mark ready, confirm pickup with photo, cancel or reject |
| `/api/inventory` | NGO inventory, distribution, expiry update |
| `/api/savedlistings` | Saved listings |
| `/api/messages` | Per-pickup conversations |
| `/api/notifications` | Notification inbox |
| `/api/reviews` | Post-pickup reviews |
| `/api/disputes` | Open disputes on completed pickups; admin review and resolution |
| `/api/imageanalysis` | Food image analysis and upload |
| `/api/admin` | Organization verification, statistics, alerts, all pickup requests |
| `/api/health/*` | SignalR and job status |

SignalR hubs are listed under [Realtime and offline](#realtime-and-offline).

## Maintainers

[@quantr10](https://github.com/quantr10)

## Contributing

Questions and bug reports go in [GitHub Issues](https://github.com/quantr10/ClearChain/issues).

Pull requests are welcome. Before opening one:

1. Branch from `main`.
2. Describe what changed and why. If the change touches the database schema, include the
   migration or the SQL to apply.

## License

[MIT](https://opensource.org/licenses/MIT) © 2026 Quan Tran
