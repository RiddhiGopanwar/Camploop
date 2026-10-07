-- =====================================================================
-- Camploop — Buy requests, notifications, private chat, fixed price
-- Run once in: Supabase Dashboard -> SQL Editor
-- (application.properties uses ddl-auto=validate, so these tables MUST
--  exist before you start the Spring Boot app.)
-- Existing tables are NOT altered, except for one trigger on products.
-- =====================================================================

-- ---------- 1. Buy / Request ----------
create table purchase_requests (
  id          bigserial primary key,
  product_id  bigint not null references products(id) on delete cascade,
  buyer_id    uuid   not null references profiles(id)  on delete cascade,
  seller_id   uuid   not null references profiles(id)  on delete cascade,
  status      text   not null default 'PENDING'
              check (status in ('PENDING','SOLD','DECLINED','CANCELLED','CLOSED')),
  created_at  timestamp default now(),
  updated_at  timestamp default now(),
  unique (product_id, buyer_id),          -- one request per buyer per item
  check (buyer_id <> seller_id)           -- can't request your own item
);
create index idx_purchase_requests_seller on purchase_requests(seller_id, status);
create index idx_purchase_requests_buyer  on purchase_requests(buyer_id);

-- ---------- 2. Notifications ----------
create table notifications (
  id            bigserial primary key,
  user_id       uuid not null references profiles(id) on delete cascade,  -- recipient
  type          text not null check (type in ('BUY_REQUEST','NEW_MESSAGE','REQUEST_UPDATE')),
  message       text not null,
  product_id    bigint references products(id) on delete cascade,
  reference_id  bigint,              -- request id or conversation id, depending on type
  is_read       boolean not null default false,
  created_at    timestamp default now()
);
create index idx_notifications_user_unread on notifications(user_id, is_read);

-- ---------- 3. Private chat (one conversation per product + buyer) ----------
create table conversations (
  id          bigserial primary key,
  product_id  bigint not null references products(id) on delete cascade,
  buyer_id    uuid   not null references profiles(id)  on delete cascade,
  seller_id   uuid   not null references profiles(id)  on delete cascade,
  created_at  timestamp default now(),
  unique (product_id, buyer_id),
  check (buyer_id <> seller_id)
);
create index idx_conversations_buyer  on conversations(buyer_id);
create index idx_conversations_seller on conversations(seller_id);

create table messages (
  id               bigserial primary key,
  conversation_id  bigint not null references conversations(id) on delete cascade,
  sender_id        uuid   not null references profiles(id)      on delete cascade,
  body             text   not null check (char_length(body) between 1 and 1000),
  created_at       timestamp default now()
);
create index idx_messages_conversation on messages(conversation_id, id);

-- ---------- 4. Row Level Security ----------
-- All WRITES go through the Spring Boot backend (which connects as the
-- postgres role and bypasses RLS and enforces the business rules).
-- So the only policies needed are SELECT ones: they stop anyone using the
-- public anon key from reading other people's requests / chats directly.
alter table purchase_requests enable row level security;
alter table notifications     enable row level security;
alter table conversations     enable row level security;
alter table messages          enable row level security;

create policy "Buyer or seller can view a request" on purchase_requests
  for select using (auth.uid() = buyer_id or auth.uid() = seller_id);

create policy "Users view their own notifications" on notifications
  for select using (auth.uid() = user_id);

create policy "Only buyer and seller can view a conversation" on conversations
  for select using (auth.uid() = buyer_id or auth.uid() = seller_id);

create policy "Only buyer and seller can read messages" on messages
  for select using (exists (
    select 1 from conversations c
    where c.id = messages.conversation_id
      and (auth.uid() = c.buyer_id or auth.uid() = c.seller_id)
  ));

-- ---------- 5. Fixed price: enforced inside the database itself ----------
-- Even if someone bypasses the API (e.g. calls Supabase directly with the
-- anon key and the existing "update own products" RLS policy), the price of
-- a listing that was published with one can never change afterwards.
create or replace function enforce_fixed_price() returns trigger as $$
begin
  if old.selling_price is not null then
    if new.selling_price is distinct from old.selling_price then
      raise exception 'The selling price is locked once a listing is published';
    end if;
    if new.listing_type is distinct from old.listing_type then
      raise exception 'The listing type cannot be changed once a price is published';
    end if;
  end if;
  return new;
end;
$$ language plpgsql;

create trigger products_fixed_price
  before update on products
  for each row execute function enforce_fixed_price();
