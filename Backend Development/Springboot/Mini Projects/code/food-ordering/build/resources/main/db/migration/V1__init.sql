create table app_users (
  id uuid primary key,
  email varchar(120) not null unique,
  password_hash varchar(120) not null,
  role varchar(30) not null
);

create table restaurants (
  id uuid primary key,
  name varchar(120) not null,
  open boolean not null
);

create table menu_items (
  id uuid primary key,
  restaurant_id uuid not null references restaurants(id),
  name varchar(120) not null,
  price_cents bigint not null,
  active boolean not null
);

create table cart_items (
  id uuid primary key,
  user_id uuid not null references app_users(id),
  restaurant_id uuid not null references restaurants(id),
  menu_item_id uuid not null references menu_items(id),
  quantity integer not null,
  unique(user_id, menu_item_id)
);

create table food_orders (
  id uuid primary key,
  user_id uuid not null references app_users(id),
  restaurant_id uuid not null references restaurants(id),
  idempotency_key varchar(120) not null unique,
  status varchar(40) not null,
  subtotal_cents bigint not null,
  delivery_fee_cents bigint not null,
  total_cents bigint not null,
  created_at timestamptz not null
);

create table food_order_items (
  id uuid primary key,
  order_id uuid not null references food_orders(id),
  menu_item_id uuid not null references menu_items(id),
  name varchar(120) not null,
  quantity integer not null,
  price_cents bigint not null
);

insert into restaurants(id, name, open) values
  ('30000000-0000-0000-0000-000000000001', 'Kotlin Kitchen', true);

insert into menu_items(id, restaurant_id, name, price_cents, active) values
  ('31000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'Spring Boot Biryani', 22000, true),
  ('31000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'Coroutine Coffee', 9000, true);
