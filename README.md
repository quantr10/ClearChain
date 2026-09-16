# ClearChain

Surplus food clearance platform connecting grocery stores with NGOs to reduce food waste.

Grocery stores post surplus stock as clearance listings; NGOs browse, request a pickup,
collect the goods and track them through their own inventory. Admins verify organizations
and monitor the platform.

## Tech stack

**Backend — .NET 8**

- ASP.NET Core Web API
- Entity Framework Core on PostgreSQL (Supabase)
- JWT authentication with refresh tokens, BCrypt password hashing
- SignalR for realtime updates
- Hangfire for background jobs
- Serilog for logging
- Azure Computer Vision for food image analysis
- FirebaseAdmin for push notifications
- MailKit for transactional email

**Mobile — Kotlin / Android**

- Jetpack Compose
- MVVM + clean architecture (`data` / `domain` / `presentation`)
- Hilt dependency injection
- Retrofit + OkHttp
- Room for offline cache
- Firebase Cloud Messaging
- Google Maps Compose
- English and Vietnamese localization

## Repository layout

```
Backend/
  ClearChain.API/             Controllers, DTOs, services, hubs, jobs, middleware
  ClearChain.Domain/          Entities, enums, constants
  ClearChain.Infrastructure/  DbContext and EF Core migrations
  ClearChain.Tests/           xUnit test project (no tests written yet)

Mobile/
  app/src/main/java/com/clearchain/app/
    data/         Room entities and DAOs, Retrofit APIs and DTOs, repositories
    domain/       Models, repository interfaces, use cases
    presentation/ Compose screens, ViewModels, navigation, shared components
    ui/theme/     Colors, typography, spacing tokens
    util/         Shared helpers
```

## Getting started

### Backend

```bash
cd Backend/ClearChain.API
# create a .env file with the variables listed below
dotnet restore
dotnet run
```

The API listens on `http://localhost:5000`, with Swagger at `/swagger` and the
Hangfire dashboard at `/hangfire` (admin role required).

### Android

Open the `Mobile` folder in Android Studio, add your values to `local.properties`,
sync Gradle and run. From the command line:

```bash
cd Mobile
./gradlew assembleDebug
```

## Configuration

### Backend (`.env`)

```env
DATABASE_URL=your_postgresql_connection_string
SUPABASE_URL=your_supabase_url
SUPABASE_ANON_KEY=your_anon_key
SUPABASE_SERVICE_KEY=your_service_key
JWT_SECRET_KEY=your_jwt_secret
JWT_ISSUER=clearchain-api
JWT_AUDIENCE=clearchain-mobile
JWT_EXPIRY_MINUTES=60
REFRESH_TOKEN_EXPIRY_DAYS=7
```

### Android (`local.properties`)

```properties
sdk.dir=your_android_sdk_path
API_BASE_URL=http://10.0.2.2:5000/api/
MAPS_API_KEY=your_google_maps_key
```

`API_BASE_URL` is the only place the server address is set. `Constants` derives
the Retrofit base URL from it and, by dropping the `/api` path, the root the
SignalR hubs are served from — so pointing the app at a device on the LAN or a
staging server means editing this line and nothing else. A missing trailing
slash is added automatically.

The app does not talk to Supabase directly; the API handles storage.

## API surface

Endpoints are grouped by controller under `/api`:

| Route | Purpose |
| --- | --- |
| `/api/auth` | Register, login, refresh, logout, email verification, password change |
| `/api/organizations` | Profiles, verification documents, public profiles, stats |
| `/api/listings` | Clearance listing CRUD, geospatial search, archive/restore |
| `/api/pickuprequests` | Request lifecycle: create, approve, reject, ready, picked up |
| `/api/cart` | NGO cart and grouped checkout |
| `/api/inventory` | NGO inventory tracking and distribution |
| `/api/savedlistings` | Saved/bookmarked listings |
| `/api/messages` | Per-pickup conversations |
| `/api/notifications` | Notification inbox |
| `/api/reviews` | Post-pickup reviews |
| `/api/reports` | Content reports |
| `/api/disputes` | Disputes on completed pickups |
| `/api/imageanalysis` | AI food image analysis and upload |
| `/api/admin` | Verification queue, statistics, transactions, disputes |

Full request and response schemas are published at `/swagger` when the API is running.

## Database

PostgreSQL via Supabase, with Supabase Storage for images and documents.
Table names are lowercase; 17 tables are mapped:

`organizations`, `clearancelistings`, `listinggroups`, `pickuprequests`,
`pickuprequestitems`, `carts`, `cartitems`, `inventory`, `savedlistings`,
`notifications`, `messages`, `reviews`, `reports`, `disputes`,
`foodimageanalyses`, `fcmtokens`, `refreshtokens`.

Schema changes are applied through EF Core migrations in
`Backend/ClearChain.Infrastructure/Migrations`.

## User roles

1. **Grocery** — create and manage listings, handle incoming pickup requests
2. **NGO** — browse listings, request pickups, manage received inventory
3. **Admin** — verify organizations, review disputes, monitor platform statistics

## Code style

`.editorconfig` at the repository root carries the formatting and naming rules for
both stacks: 4-space indentation, UTF-8, a 120-column guide, file-scoped C#
namespaces, and star imports for Kotlin packages contributing five or more symbols.

`.gitattributes` pins line endings to LF so a Windows checkout does not put the
formatters and the repository at odds.

Format before committing:

```bash
cd Backend && dotnet format whitespace ClearChain.sln
cd Mobile  && ./gradlew spotlessApply
```

`./gradlew spotlessCheck` and `dotnet format whitespace ClearChain.sln
--verify-no-changes` both pass on a clean tree, so either can gate CI.

Spotless runs ktlint. Four standard rules are switched off where they would
contradict the conventions above: star imports, PascalCase `@Composable`
functions, several composables per file, and trailing comments on DTO fields.
Line length is a guide, not a gate: ktlint can fail a long line but cannot wrap
one, and a number of Compose argument lists run well past 120 columns.

## Testing

```bash
cd Backend && dotnet test     # xUnit project is set up but currently has no tests
cd Mobile  && ./gradlew test  # only the generated example tests exist
```

## License

MIT
