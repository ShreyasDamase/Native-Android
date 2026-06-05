create table events (
  id uuid primary key,
  title varchar(120) not null,
  starts_at timestamptz not null
);

create table seats (
  id uuid primary key,
  event_id uuid not null references events(id),
  seat_code varchar(20) not null,
  category varchar(30) not null,
  price_cents bigint not null,
  status varchar(30) not null,
  version bigint not null default 0,
  unique(event_id, seat_code)
);

create table seat_holds (
  id uuid primary key,
  event_id uuid not null references events(id),
  status varchar(30) not null,
  expires_at timestamptz not null,
  created_at timestamptz not null
);

create table seat_hold_items (
  id uuid primary key,
  hold_id uuid not null references seat_holds(id),
  seat_id uuid not null references seats(id),
  unique(hold_id, seat_id)
);

create table bookings (
  id uuid primary key,
  hold_id uuid not null references seat_holds(id),
  idempotency_key varchar(120) not null unique,
  status varchar(30) not null,
  total_cents bigint not null,
  created_at timestamptz not null
);

insert into events(id, title, starts_at) values
  ('10000000-0000-0000-0000-000000000001', 'KotlinConf Movie Night', now() + interval '7 days');

insert into seats(id, event_id, seat_code, category, price_cents, status, version) values
  ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'A1', 'GOLD', 45000, 'AVAILABLE', 0),
  ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'A2', 'GOLD', 45000, 'AVAILABLE', 0),
  ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'B1', 'SILVER', 25000, 'AVAILABLE', 0);
