create table parking_spots (
  id uuid primary key,
  code varchar(40) not null unique,
  type varchar(30) not null,
  status varchar(30) not null,
  version bigint not null default 0
);

create table parking_tickets (
  id uuid primary key,
  vehicle_number varchar(30) not null,
  vehicle_type varchar(30) not null,
  spot_id uuid not null references parking_spots(id),
  status varchar(30) not null,
  entry_time timestamptz not null,
  exit_time timestamptz,
  fee_cents bigint,
  version bigint not null default 0
);

create unique index ux_active_ticket_vehicle
  on parking_tickets(vehicle_number)
  where status in ('ACTIVE', 'PAYMENT_PENDING');

insert into parking_spots(id, code, type, status, version) values
  ('00000000-0000-0000-0000-000000000101', 'F1-B-001', 'BIKE', 'AVAILABLE', 0),
  ('00000000-0000-0000-0000-000000000102', 'F1-C-001', 'COMPACT', 'AVAILABLE', 0),
  ('00000000-0000-0000-0000-000000000103', 'F1-C-002', 'COMPACT', 'AVAILABLE', 0),
  ('00000000-0000-0000-0000-000000000104', 'F1-L-001', 'LARGE', 'AVAILABLE', 0),
  ('00000000-0000-0000-0000-000000000105', 'F1-EV-001', 'EV_CHARGING', 'AVAILABLE', 0);
