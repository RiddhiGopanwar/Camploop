# Camploop — Campus Marketplace (Version 1, Supabase-backed)

Camploop is a campus-exclusive marketplace for college students. Every student
has **one account** and can both buy and sell — there are no separate
buyer/seller roles.

## Tech stack

- **Frontend:** Plain HTML, CSS, JavaScript + `supabase-js` (loaded via CDN) for auth/storage
- **Backend:** Java 17, Spring Boot 3.3, Spring Data JPA / Hibernate, REST APIs — this is where search/filter, savings calculation, and ownership rules actually live
- **Database, Auth, Storage:** Supabase (managed Postgres + Supabase Auth + Supabase Storage)

### How the pieces fit together

- The **frontend** calls Supabase directly (via `supabase-js`) for sign up, log in,
  log out, and uploading product images to Supabase Storage.
- Every request to the **Spring Boot backend** (`/api/**`) carries the current
  Supabase session's JWT in an `Authorization: Bearer <token>` header.
  `SupabaseJwtFilter` verifies that token's signature against your project's
  JWT secret and resolves it to a `CurrentUser` (id + email) — that's how the
  backend knows who's asking, without ever seeing a password.
- The backend connects to the **same** Supabase Postgres database over plain
  JDBC (Spring Data JPA), and enforces things like "you can only edit your own
  listing" in `ProductService`. **Row Level Security** in Postgres (SQL below)
  is a second line of defense at the database layer itself.

## Project structure

```
camploop/
├── pom.xml
├── src/main/java/com/camploop/
│   ├── CamploopApplication.java
│   ├── model/          → Profile, Product, ProductImage, Wishlist, Report (+ enums/)
│   ├── repository/     → Spring Data JPA repositories
│   ├── service/        → ProfileService, ProductService, WishlistService, ReportService
│   ├── controller/     → ProfileController, ProductController, WishlistController, ReportController
│   ├── dto/             → request/response DTOs
│   ├── config/           → SupabaseProperties, SupabaseJwtFilter, AuthContext, CORS config
│   └── exception/         → ApiException + a global @RestControllerAdvice handler
└── src/main/resources/
    ├── application.properties
    └── static/            → the entire frontend
        ├── index.html, marketplace.html, product.html, sell.html, profile.html
        ├── login.html, signup.html
        ├── css/style.css
        └── js/ (supabase-config.js, auth.js, api.js, main.js)
```

## 1. Create your Supabase project

Go to [supabase.com](https://supabase.com), create a new project, and note down
(from **Project Settings → API**):
- Project URL
- `anon` public key
- **Project Settings → API → JWT Settings → JWT Secret**
- **Project Settings → Database → Connection string** (host/password)

## 2. Run this SQL (Supabase SQL Editor)

This creates the five tables from the spec, wires up Row Level Security so
students can only touch their own data, and lets Supabase Auth's `auth.users`
back the `profiles` table.

```sql
create table profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null,
  email text not null unique,
  college text,
  profile_image text,
  created_at timestamp default now()
);

create table products (
  id bigserial primary key,
  seller_id uuid not null references profiles(id) on delete cascade,
  name text not null,
  description text not null,
  category text not null,
  original_price numeric(10,2),
  selling_price numeric(10,2),
  condition text not null,
  listing_type text not null,
  status text not null default 'AVAILABLE',
  pickup_location text,
  created_at timestamp default now()
);

create table product_images (
  id bigserial primary key,
  product_id bigint not null references products(id) on delete cascade,
  image_url text not null,
  sort_order int default 0
);

create table wishlists (
  id bigserial primary key,
  user_id uuid not null references profiles(id) on delete cascade,
  product_id bigint not null references products(id) on delete cascade,
  created_at timestamp default now(),
  unique (user_id, product_id)
);

create table reports (
  id bigserial primary key,
  reporter_id uuid not null references profiles(id) on delete cascade,
  product_id bigint not null references products(id) on delete cascade,
  reason text not null,
  created_at timestamp default now()
);

-- Row Level Security
alter table profiles enable row level security;
alter table products enable row level security;
alter table product_images enable row level security;
alter table wishlists enable row level security;
alter table reports enable row level security;

-- Profiles: anyone can view (marketplace shows seller names), only the owner can edit
create policy "Profiles are viewable by everyone" on profiles for select using (true);
create policy "Users can update their own profile" on profiles for update using (auth.uid() = id);
create policy "Users can insert their own profile" on profiles for insert with check (auth.uid() = id);

-- Products: anyone can view, only the owner can insert/update/delete
create policy "Products are viewable by everyone" on products for select using (true);
create policy "Users can insert their own products" on products for insert with check (auth.uid() = seller_id);
create policy "Users can update their own products" on products for update using (auth.uid() = seller_id);
create policy "Users can delete their own products" on products for delete using (auth.uid() = seller_id);

-- Product images: anyone can view; only the owning seller can add/remove
create policy "Product images are viewable by everyone" on product_images for select using (true);
create policy "Sellers manage their own product images" on product_images for all
  using (exists (select 1 from products p where p.id = product_id and p.seller_id = auth.uid()));

-- Wishlists: strictly private to each user
create policy "Users manage their own wishlist" on wishlists for all using (auth.uid() = user_id);

-- Reports: anyone logged in can create; only the reporter can view their own reports
create policy "Users can create reports" on reports for insert with check (auth.uid() = reporter_id);
create policy "Users can view their own reports" on reports for select using (auth.uid() = reporter_id);
```

> Note: `spring.jpa.hibernate.ddl-auto=update` in `application.properties` will
> also happily create/adjust these tables for you on first run if you'd rather
> skip writing SQL by hand — just be aware Hibernate won't set up the RLS
> policies above, so run at least the `create policy` statements yourself
> either way.

## 3. Create the Storage bucket for product images

In **Storage → New bucket**, create a bucket named `product-images` and make
it **public** (so listing photos can be displayed without a signed URL). Then
add a policy allowing authenticated users to upload into their own folder:

```sql
create policy "Authenticated users can upload product images"
on storage.objects for insert
with check (bucket_id = 'product-images' and auth.role() = 'authenticated');

create policy "Product images are publicly readable"
on storage.objects for select
using (bucket_id = 'product-images');
```

## 4. Configure the app

**Frontend** — edit `src/main/resources/static/js/supabase-config.js`:
```js
const SUPABASE_URL = 'https://your-project-ref.supabase.co';
const SUPABASE_ANON_KEY = 'your-anon-key';
```

**Backend** — edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://db.your-project-ref.supabase.co:5432/postgres
spring.datasource.password=your-db-password
supabase.jwt-secret=your-jwt-secret
```

## 5. Run it

```bash
mvn spring-boot:run
```
Open **http://localhost:8080**. Sign up (Supabase will send a confirmation
email if your project has that turned on), log in, and the homepage,
marketplace, sell flow, and profile dashboard are all served from there.

## API overview

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/profiles/me` | Current student's profile (auto-created on first call) |
| PUT | `/api/profiles/me` | Update your name/college/profile image |
| GET | `/api/profiles/{id}` | Public view of any student (name, college, listing counts) |
| GET | `/api/products/recent` | Recent listings (homepage preview) |
| GET | `/api/products?keyword=&category=&condition=&minPrice=&maxPrice=&sort=` | Search/filter/sort |
| GET | `/api/products/{id}` | Product detail |
| GET | `/api/products/mine` | My own listings, all statuses (auth required) |
| POST | `/api/products` | Create a listing (auth required) |
| PUT | `/api/products/{id}` | Edit a listing you own |
| DELETE | `/api/products/{id}` | Delete a listing you own |
| PATCH | `/api/products/{id}/status?status=SOLD` | Mark SOLD / UNAVAILABLE — drops out of search, stays in history |
| GET | `/api/wishlist` | My wishlist (auth required) |
| POST | `/api/wishlist/{productId}` | Add to wishlist |
| DELETE | `/api/wishlist/{productId}` | Remove from wishlist |
| POST | `/api/reports/{productId}` | Report a listing (auth required, body: `{ "reason": "..." }`) |

Auth itself (`signUp` / `signInWithPassword` / `signOut`) is called directly
against Supabase from the frontend — see `js/auth.js` — not through this
backend.

## What's intentionally out of scope for V1

Per the brief: online payment, online delivery, AI recommendations/pricing,
fraud detection, an admin moderation dashboard, buyer-seller chat,
ratings/reviews, and college/ID verification. Reports are stored but not yet
reviewed by anyone — that's the admin dashboard's job in V2. "Recently
viewed" is tracked client-side (localStorage) since there's no purchase/order
concept yet to hang a server-side history off of; a real "purchase history"
would need a transactions table, which wasn't in the V1 schema — flagging
this in case you want it added.

## Design notes

The frontend keeps its "campus corkboard / notebook margin" identity — grid
paper backgrounds, pinned index-card product tiles, marker-coral CTAs, a
locker-tag category grid — from the original build. Product cards now show a
savings badge when a listing has both an original and selling price, and
carry a small pickup-location line when the seller specified one.
