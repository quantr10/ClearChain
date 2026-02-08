# 🍎 ClearChain



Surplus food clearance platform connecting grocery stores with NGOs to reduce food waste.



## 📱 Project Overview



**ClearChain** helps grocery stores donate surplus food to NGOs instead of throwing it away.



### Tech Stack



**Backend (.NET 8)**

- ASP.NET Core Web API

- PostgreSQL (Supabase)

- JWT Authentication

- BCrypt password hashing

- SignalR (realtime)

- Hangfire (background jobs)



**Mobile (Kotlin)**

- Jetpack Compose

- MVVM + Clean Architecture

- Hilt DI

- Retrofit

- Room (offline-first)

- Firebase Cloud Messaging



**Database**

- Supabase PostgreSQL

- Supabase Storage (images)

- 7 tables (lowercase naming)



## 🚀 Features



### Week 1-2 (Completed)

- ✅ Project setup

- ✅ Database schema

- ✅ Authentication API (register, login, refresh)

- ✅ Organization management

- ✅ Admin verification system

- ✅ Error handling middleware

- ✅ Audit logging



### Upcoming

- 🔄 Listing CRUD (groceries)

- 🔄 Pickup request flow

- 🔄 Inventory management (NGOs)

- 🔄 Android app UI

- 🔄 Real-time updates

- 🔄 Push notifications



## 📦 Installation



### Backend

```bash

cd Backend/ClearChain.API


# Create .env file (copy from .env.example)

# Add your Supabase credentials



dotnet restore

dotnet build

dotnet run

```



**API runs on:** http://localhost:5000



**Swagger:** http://localhost:5000/swagger



### Android



Open `Mobile/ClearChain` in Android Studio



Update `local.properties` with your configuration



Sync Gradle and run



## 🗄️ Database Schema



**Tables:**

- `organizations` - Users (grocery, ngo, admin)

- `clearancelistings` - Food listings

- `pickuprequests` - Pickup requests

- `pickuprequestlistings` - Junction table

- `distributeditems` - NGO inventory

- `refreshtokens` - JWT refresh tokens

- `auditlogs` - Action tracking



## 🔐 Authentication



**JWT-based authentication**



**Endpoints:**

- `POST /api/auth/register` - Register organization

- `POST /api/auth/login` - Login

- `POST /api/auth/refresh` - Refresh access token

- `POST /api/auth/logout` - Logout

- `GET /api/auth/me` - Get current user

- `POST /api/auth/change-password` - Change password



## 👤 User Roles



1. **Grocery/Mall** - Create listings, manage pickups

2. **NGO** - Browse listings, request pickups, manage inventory

3. **Admin** - Verify organizations, monitor system



## 📝 Environment Variables



### Backend (.env)

```env

DATABASE\_URL=your\_postgresql\_connection\_string

SUPABASE\_URL=your\_supabase\_url

SUPABASE\_ANON\_KEY=your\_anon\_key

SUPABASE\_SERVICE\_KEY=your\_service\_key

JWT\_SECRET\_KEY=your\_jwt\_secret

JWT\_ISSUER=clearchain-api

JWT\_AUDIENCE=clearchain-mobile

JWT\_EXPIRY\_MINUTES=60

REFRESH\_TOKEN\_EXPIRY\_DAYS=7

```



### Android (local.properties)

```properties

sdk.dir=your\_android\_sdk\_path

API\_BASE\_URL=http://10.0.2.2:5000/api

SUPABASE\_URL=your\_supabase\_url

SUPABASE\_ANON\_KEY=your\_anon\_key

```



## 🛠️ Development Timeline



**Month 1 (Week 1-4):** MVP Core

- Week 1: Setup \& Infrastructure ✅

- Week 2: Authentication ✅

- Week 3-4: Core Features (in progress)



**Month 2 (Week 5-8):** Advanced Features

**Month 3 (Week 9-12):** Polish \& Production



## 📄 API Documentation



Full API documentation available at `/swagger` when running the backend.



## 🧪 Testing



**Backend:**

```bash

cd Backend

dotnet test

```



**Android:**

```bash

./gradlew test

```



## 📞 Support



For issues or questions, please open an issue on GitHub.



## 📜 License



MIT License



---



\*\*Status:\*\* 🟢 Active Development (Week 2 - Day 5 Complete)

