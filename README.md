# Camploop — Campus Marketplace

Camploop is a campus-exclusive marketplace for college students. Every student
has **one account** and can both buy and sell there are no separate
buyer/seller roles.

## Features

- **Accounts:** sign up, log in and log out with Supabase Auth; a profile (name, college, photo) is created automatically on first login.
- **Listings:** sell, exchange or donate an item with photos, category, condition, original/selling price and a pickup location.
- **Marketplace:** keyword search, category/condition/price filters and sorting. Cards show a savings badge when a listing has both an original and a selling price.
- **Wishlist and recently viewed:** save items for later (stored in the database); recently viewed items are tracked in the browser.
- **Buy / Request:** a **Buy** button (**Request** for Exchange and Donate listings) on every listing. The seller is notified, and can mark the item sold, decline, or chat with the buyer. Buyers can cancel a pending request.
- **Contact Seller:** a small private chat between only the buyer and the seller of an item, for agreeing a time and place to meet.
- **Fixed price:** once a listing is published with a selling price, the price cannot be changed — enforced by the backend and by a database trigger.
- **Notifications:** an unread badge beside your name for new requests, new chat messages and request updates (sold / declined).
- **Profile dashboard:** My Listings, Wishlist, Requests (received and sent), Messages, Sold Items and Recently Viewed.
- **Reports:** flag a listing for review.

## Tech stack

- **Frontend:** plain HTML, CSS and JavaScript, with `supabase-js` (loaded via CDN) for auth and image storage
- **Backend:** Java 17, Spring Boot 3.3, Spring Data JPA / Hibernate, REST APIs. This is where search and filtering, the savings calculation, buy-request rules, chat access control, the fixed-price rule and ownership checks live
- **Database, Auth, Storage:** Supabase (managed Postgres, Supabase Auth and Supabase Storage)

### How the pieces fit together

- The **frontend** calls Supabase directly (via `supabase-js`) for sign up, log in, log out, and uploading product images to Supabase Storage.
- Every request to the **Spring Boot backend** (`/api/**`) carries the current Supabase session's JWT in an `Authorization: Bearer <token>` header. `SupabaseJwtFilter` verifies the token's signature against your project's JWT secret and resolves it to a `CurrentUser` (id + email). That is how the backend knows who is asking, without ever seeing a password.
- The backend connects to the **same** Supabase Postgres database over plain JDBC (Spring Data JPA). Rules such as "you can only edit your own listing", "only the seller can mark a request sold" and "only the buyer and seller can read a chat" are enforced in the service layer.
- **Row Level Security** in Postgres is a second line of defense at the database layer itself.

## Project structure

```
camploop/
├── pom.xml
├── sql/
│   └── 002_buy_chat_pricelock.sql      → requests, notifications, chat and price-lock SQL
├── src/main/java/com/camploop/
│   ├── CamploopApplication.java
│   ├── model/          → Profile, Product, ProductImage, Wishlist, Report,
│   │                     PurchaseRequest, Notification, Conversation, Message (+ enums/)
│   ├── repository/     → Spring Data JPA repositories
│   ├── service/        → ProfileService, ProductService, WishlistService, ReportService,
│   │                     PurchaseRequestService, ChatService, NotificationService
│   ├── controller/     → Profile, Product, Wishlist, Report,
│   │                     PurchaseRequest, Chat and Notification controllers
│   ├── dto/            → request/response DTOs
│   ├── config/         → SupabaseProperties, SupabaseJwtFilter, AuthContext, CORS config
│   └── exception/      → ApiException + a global @RestControllerAdvice handler
└── src/main/resources/
    ├── application.properties
    └── static/         → the entire frontend
        ├── index.html, marketplace.html, product.html, sell.html, profile.html
        ├── login.html, signup.html
        ├── css/style.css
        └── js/ (supabase-config.js, auth.js, api.js, main.js, chat.js)
```

## Setup

### 1. Create your Supabase project

Go to [supabase.com](https://supabase.com), create a new project, and note down:

- **Project Settings → API:** Project URL and the `anon` public key
- **Project Settings → API → JWT Settings:** JWT Secret
- **Project Settings → Database → Connection string:** host and password

### 2. Create the database tables

Open **SQL Editor** in Supabase and run both parts below, in this order. The app
uses `spring.jpa.hibernate.ddl-auto=validate`, so it will not start until every
table exists.

#### 2a. Core tables — profiles, products, images, wishlists, reports

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

#### 2b. Requests, notifications, chat and fixed price

Open **`sql/002_buy_chat_pricelock.sql`** from this repository, paste the whole
file into the SQL Editor and run it. It creates:

| Table | Purpose |
|---|---|
| `purchase_requests` | One request per buyer per product. Status: `PENDING`, `SOLD`, `DECLINED`, `CANCELLED`, `CLOSED`. |
| `notifications` | Per-user notifications (`BUY_REQUEST`, `NEW_MESSAGE`, `REQUEST_UPDATE`) with an unread flag. |
| `conversations` | One private chat per (product, buyer), linking the buyer and the seller. |
| `messages` | Chat messages (1–1000 characters) belonging to a conversation. |

It also:

- enables **Row Level Security** on all four tables, so only the people involved can read a request or chat. All writes go through the backend, which applies the business rules;
- creates the **`products_fixed_price` trigger**, which rejects any update to `selling_price` or `listing_type` on a listing that was published with a price.

### 3. Create the Storage bucket for product images

In **Storage → New bucket**, create a bucket named `product-images` and make it
**public**, so listing photos can be displayed without a signed URL. Then add
policies allowing signed-in users to upload and everyone to read:

```sql
create policy "Authenticated users can upload product images"
on storage.objects for insert
with check (bucket_id = 'product-images' and auth.role() = 'authenticated');

create policy "Product images are publicly readable"
on storage.objects for select
using (bucket_id = 'product-images');
```

### 4. Configure the app

**Frontend** — edit `src/main/resources/static/js/supabase-config.js`:

```js
const SUPABASE_URL = 'https://your-project-ref.supabase.co';
const SUPABASE_ANON_KEY = 'your-anon-key';
```

**Backend** — edit `src/main/resources/application.properties`. Prefer
environment variables so secrets never reach GitHub:

```properties
spring.datasource.url=jdbc:postgresql://db.your-project-ref.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
supabase.jwt-secret=${SUPABASE_JWT_SECRET}
```

> ⚠️ **Never commit real passwords, JWT secrets or service-role keys.** If any
> were ever pushed to a public repository, rotate them in Supabase and keep
> `application.properties` (or a local override file) out of version control.

### 5. Run it

Requires **Java 17+** and **Maven**.

```bash
mvn clean spring-boot:run
```

Open **http://localhost:8081** (the port set by `server.port` in
`application.properties`). Sign up (Supabase sends a confirmation email if your
project has that turned on), log in, and the homepage, marketplace, sell flow
and profile dashboard are all served from there.

## How it works

### Listings and search

Sellers create a listing with photos (uploaded straight to Supabase Storage),
a category, condition, listing type (Sell, Exchange or Donate), optional
original and selling prices, and a pickup location. Only `AVAILABLE` listings
appear in the marketplace. Marking a listing `SOLD` or `UNAVAILABLE` removes it
from search but keeps it in the seller's history. The marketplace supports
keyword search, category, condition and price filters, and sorting.

### Buy / Request

1. A logged-in student clicks **Buy** (Sell listings) or **Request** (Exchange and Donate listings) on a card or the product page.
2. The seller receives a notification ("*Name* is interested in *Item*") and sees the request under **Profile → Requests**.
3. The seller can:
   - **Mark as sold** — the listing becomes `SOLD` and leaves the marketplace, this buyer's request becomes `SOLD`, every other pending request becomes `CLOSED`, and all affected buyers are notified.
   - **Decline** — the buyer is notified.
   - **Chat** — opens the private chat with that buyer.
4. The buyer sees the status of every request they have sent under **Profile → Requests**, and can cancel a pending one.

Rules enforced by the backend: you cannot request your own listing, you cannot
request an item that is not `AVAILABLE`, and you get one request per item (a
cancelled request can be re-opened).

### Contact Seller (private chat)

- **Contact Seller** on a product page opens a chat popup (`js/chat.js`). The conversation is created on first use and is unique per (product, buyer).
- Only the buyer and the seller of that product can read or send messages; anyone else gets a "not found". A seller can only open a chat with a student who has already requested or messaged them about that item.
- New messages are fetched every 4 seconds while the popup is open. The recipient gets one unread notification per conversation (not one per message), cleared when they open the chat.
- All your chats are listed under **Profile → Messages**.

### Fixed price

- `ProductService.update` rejects any edit that changes the selling price, or switches a priced listing away from the Sell type, with HTTP 409 and a clear message. Other fields such as the description and condition remain editable.
- The `products_fixed_price` database trigger enforces the same rule, so it cannot be bypassed by calling Supabase directly with the anon key.
- Listings that never had a price (Exchange and Donate) have nothing to lock.

### Notifications

New requests, new chat messages and request updates create notifications. The
count of unread ones appears as a badge beside your name in the navbar and is
cleared when you open the Requests tab or the relevant chat.

## API overview

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/profiles/me` | Current student's profile (auto-created on first call) |
| PUT | `/api/profiles/me` | Update your name, college or profile image |
| GET | `/api/profiles/{id}` | Public view of any student (name, college, listing counts) |
| GET | `/api/products/recent` | Recent listings (homepage preview) |
| GET | `/api/products?keyword=&category=&condition=&minPrice=&maxPrice=&sort=` | Search, filter and sort |
| GET | `/api/products/{id}` | Product detail |
| GET | `/api/products/mine` | My own listings, all statuses (auth required) |
| POST | `/api/products` | Create a listing (auth required) |
| PUT | `/api/products/{id}` | Edit a listing you own (the selling price is locked once published) |
| DELETE | `/api/products/{id}` | Delete a listing you own |
| PATCH | `/api/products/{id}/status?status=SOLD` | Mark SOLD or UNAVAILABLE; drops out of search and closes pending requests |
| GET | `/api/wishlist` | My wishlist (auth required) |
| POST | `/api/wishlist/{productId}` | Add to wishlist |
| DELETE | `/api/wishlist/{productId}` | Remove from wishlist |
| POST | `/api/reports/{productId}` | Report a listing (auth required, body: `{ "reason": "..." }`) |
| POST | `/api/requests/{productId}` | Buy / Request an item |
| GET | `/api/requests/received` | Requests on my listings (seller inbox) |
| GET | `/api/requests/sent` | Requests I have sent |
| GET | `/api/requests/product/{productId}/mine` | My request for one product (204 if none) |
| POST | `/api/requests/{id}/mark-sold` | Seller: mark sold to this buyer |
| POST | `/api/requests/{id}/decline` | Seller: decline a request |
| POST | `/api/requests/{id}/cancel` | Buyer: cancel a pending request |
| POST | `/api/conversations` | Open or create a chat (body: `{ "productId": 1, "buyerId": null }`; sellers pass `buyerId`) |
| GET | `/api/conversations` | My chats, newest activity first |
| GET | `/api/conversations/{id}/messages?after=` | Messages in a chat (participants only); `after` is the last message id already seen |
| POST | `/api/conversations/{id}/messages` | Send a message (body: `{ "body": "..." }`, max 1000 characters) |
| GET | `/api/notifications` | My latest 30 notifications |
| GET | `/api/notifications/unread-count` | Unread count (navbar badge) |
| POST | `/api/notifications/read?type=` | Mark notifications read (optionally only one type) |

Auth itself (`signUp`, `signInWithPassword`, `signOut`) is called directly
against Supabase from the frontend — see `js/auth.js` — not through this
backend.

## Trying it out locally

Use two accounts, with the second in a private or incognito window.

1. **Seller:** sign up, go to **Sell**, and list an item with a selling price.
2. **Buyer:** open the marketplace and click **Buy**; the button changes to "✓ Request sent". Open the item, click **Contact Seller**, and send a message.
3. **Seller:** a badge appears by your name. **Profile → Requests** shows the request and **Profile → Messages** shows the chat. Click **Mark as sold** and the item leaves the marketplace.
4. **Price lock:** in the Supabase SQL Editor run `update products set selling_price = 1 where selling_price is not null;`. It must fail with *"The selling price is locked once a listing is published"*.

## Not included yet

Online payments, delivery, AI recommendations or pricing, fraud detection, an
admin moderation dashboard, ratings and reviews, college or ID verification, and
real-time (websocket) chat. Chat currently polls every 4 seconds. Reports are
stored but not yet reviewed by anyone, and "Recently viewed" is tracked in the
browser (localStorage) rather than the database.

## Design notes

The frontend has a "campus corkboard / notebook margin" look: grid-paper
backgrounds, pinned index-card product tiles, marker-coral buttons and a
locker-tag category grid. Product cards show a savings badge when a listing has
both an original and a selling price, a small pickup-location line when the
seller specified one, and a Buy / Request button. The navbar badge, the Requests
and Messages tabs and the chat popup reuse the same colors, borders and
typography.
