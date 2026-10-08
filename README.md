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
  ClearChain.Tests/           xUnit tests

Mobile/
  app/src/main/java/com/clearchain/app/
    data/         Room entities and DAOs, Retrofit APIs and DTOs, repositories
    domain/       Models, repository interfaces, use cases
    presentation/ Compose screens, ViewModels, navigation, shared components
    ui/theme/     Colors, typography, spacing tokens
    util/         Shared helpers
  app/src/test/   JVM unit tests
```

## Getting started

### Backend

```bash
cd Backend/ClearChain.API
cp .env.example .env   # then fill in the values
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

`Backend/ClearChain.API/.env.example` lists every variable the API reads. Copy it to
`.env` next to it and fill in the values:

| Variables | Used for | If missing |
| --- | --- | --- |
| `DATABASE_URL` | PostgreSQL connection | API won't start |
| `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` | Image and document storage | API won't start |
| `JWT_SECRET_KEY`, `JWT_ISSUER`, `JWT_AUDIENCE` | Signing sign-in tokens | API won't start |
| `JWT_EXPIRY_MINUTES`, `REFRESH_TOKEN_EXPIRY_DAYS` | Token lifetimes | Defaults to 60 minutes and 7 days |
| `AZURE_VISION_ENDPOINT`, `AZURE_VISION_KEY` | Food image analysis | API won't start |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM` | Verification and password-reset emails | Emails are skipped (logged as a warning) |
| `ALLOWED_ORIGINS` | CORS for browser clients (comma-separated) | Only `http://localhost:3000` is allowed |

Push notifications read `firebase-adminsdk.json` from the API's working directory rather
than an environment variable. Without it, push is disabled and notifications still arrive
over SignalR.

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
| `/api/auth` | Register, login, refresh, logout, email verification, password change and reset |
| `/api/organizations` | Profiles, verification documents, public profiles, stats |
| `/api/listings` | Clearance listing CRUD, geospatial search, archive/restore |
| `/api/pickuprequests` | Request lifecycle: create, approve, reject, ready, picked up |
| `/api/cart` | NGO cart and grouped checkout |
| `/api/inventory` | NGO inventory tracking and distribution |
| `/api/savedlistings` | Saved/bookmarked listings |
| `/api/messages` | Per-pickup conversations |
| `/api/notifications` | Notification inbox |
| `/api/reviews` | Post-pickup reviews |
| `/api/disputes` | Disputes on completed pickups |
| `/api/imageanalysis` | AI food image analysis and upload |
| `/api/admin` | Verification queue, statistics, transactions, disputes |

Full request and response schemas are published at `/swagger` when the API is running.

## Database

PostgreSQL via Supabase, with Supabase Storage for images and documents.
Table names are lowercase; 15 tables are mapped:

`organizations`, `clearancelistings`, `listinggroups`, `pickuprequests`,
`pickuprequestitems`, `carts`, `cartitems`, `inventory`, `savedlistings`,
`notifications`, `messages`, `reviews`, `disputes`, `fcmtokens`,
`refreshtokens`.

Schema changes are applied through EF Core migrations in
`Backend/ClearChain.Infrastructure/Migrations`. The directory starts empty on a fresh
checkout — the prior incremental migrations were squashed, so generate the baseline
before first use:

```bash
cd Backend
dotnet ef migrations add InitialCreate --project ClearChain.Infrastructure --startup-project ClearChain.API
dotnet ef database update --project ClearChain.Infrastructure --startup-project ClearChain.API
```

A database that already carries this schema must be stamped rather than migrated, or
EF will try to create tables that exist. After generating the migration above, record
it as applied instead of running `database update`:

```sql
CREATE TABLE IF NOT EXISTS "__EFMigrationsHistory" (
    "MigrationId"    character varying(150) NOT NULL,
    "ProductVersion" character varying(32)  NOT NULL,
    CONSTRAINT "PK___EFMigrationsHistory" PRIMARY KEY ("MigrationId")
);

INSERT INTO "__EFMigrationsHistory" ("MigrationId", "ProductVersion")
VALUES ('<the generated migration id>', '8.0.11')
ON CONFLICT ("MigrationId") DO NOTHING;
```

Drop any rows left over from the earlier incremental migrations at the same time —
they name migrations that no longer exist.

For a later schema change, add a new migration on top of the baseline rather than
editing it:

```bash
dotnet ef migrations add <Name> --project ClearChain.Infrastructure --startup-project ClearChain.API
```

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
cd Backend && dotnet test
cd Mobile  && ./gradlew test
```

## License

MIT
