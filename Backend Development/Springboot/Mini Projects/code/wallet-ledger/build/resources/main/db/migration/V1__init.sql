create table wallets (
  id uuid primary key,
  owner_name varchar(120) not null,
  currency varchar(3) not null,
  status varchar(30) not null
);

create table ledger_transactions (
  id uuid primary key,
  idempotency_key varchar(120) not null unique,
  reference_type varchar(40) not null,
  status varchar(30) not null,
  created_at timestamptz not null
);

create table ledger_entries (
  id uuid primary key,
  transaction_id uuid not null references ledger_transactions(id),
  wallet_id uuid not null references wallets(id),
  entry_type varchar(20) not null,
  amount numeric(19,2) not null,
  currency varchar(3) not null,
  created_at timestamptz not null
);

create index ix_ledger_entries_wallet on ledger_entries(wallet_id);
create index ix_ledger_entries_transaction on ledger_entries(transaction_id);
