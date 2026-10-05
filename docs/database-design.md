# Database Design

The database design represents the current persistence model of the Digital Banking System.

It contains the `Customer`, `Account`, `PasswordCredential`, `Transaction`, and `LedgerEntry` tables and shows their primary keys, foreign keys, and relationships.

The design will evolve as new requirements are introduced.

## Database Diagram

![Database Diagram](../images/database-design.png)

## Debit Card Persistence

The `card` table supplements the diagram above:

- `id`: generated primary key
- `card_number`: required unique `VARCHAR(16)`
- `cvc2`: required `VARCHAR(3)`, not unique
- `expiry_date`: required `DATE`
- `account_id`: required unique foreign key to `account`
- `pin`: required `VARCHAR(255)` holding the encrypted PIN envelope

Each card belongs directly to one account. Customer ownership is obtained through
the account; `card` has no `customer_id` column. The unique account foreign key
allows at most one card per account. The application issues one card per customer
only at registration, linked to the Main Account. Registration saves customer,
credentials, account, and card in one transaction; additional accounts receive no card.
Legacy accounts without cards are not automatically backfilled. Number generation
tries at most ten candidates when known collisions occur. A concurrent database
uniqueness failure aborts registration; it is not retried in the failed transaction.
These are simulated card details, stored as strings without a production card
encryption system. Hibernate value/error logging is disabled to avoid disclosing
card details in bind or duplicate-key messages.

## Transaction and Ledger Persistence

The logical `Transaction` in the diagram maps to `bank_transaction`:

- `id`: generated primary key
- `type`, `status`: string enum values
- `amount`: required `DECIMAL(19,2)`, positive transaction amount
- `created_at`: required timestamp mapped from Java `Instant`

`LedgerEntry` maps to `ledger_entry`:

- `id`: generated primary key
- `amount`: required signed `DECIMAL(19,2)`
- `account_id`: required foreign key to `account`
- `transaction_id`: required foreign key to `bank_transaction`

A transaction can have zero or more ledger entries. Relationships do not cascade
deletion of accounts or transactions from ledger entries.

Java monetary values use `BigDecimal`. Constructors normalize harmless trailing
zeros to scale 2 and reject values requiring rounding or exceeding the column's
17 integer digits. This is a storage constraint, not a business spending limit.

Account balances are computed using `SUM(amount)`, with no rows interpreted as
zero. Grouped queries calculate balances for several account IDs together.
An aggregate balance is not restricted to the precision of an individual entry.

Flyway `db/migration/V1__initial_schema.sql` creates all six entity tables from an
empty MySQL 8.4 database. It uses BIGINT AUTO_INCREMENT IDs, native ENUM columns,
DECIMAL(19,2) amounts, DATETIME(6) transaction time, and InnoDB foreign keys.
Hibernate uses `validate` in the local profile, `update` in production, and
`create-drop` in tests. Deployment configuration is unchanged. V1 is an initial
schema, not an upgrade script for existing databases; already-applied migrations
must not be rewritten in a deployed environment.

## Transfer Persistence and Concurrency

A successful transfer saves one completed transaction and two equal, opposite signed
ledger entries atomically. The service uses READ_COMMITTED isolation and pessimistic
write locks on both Account rows, acquired by ascending account ID in separate queries.
The source balance is read only after both locks are held. All future financial writers
must follow the same account-lock protocol to preserve this concurrency guarantee.

Account-number storage remains unchanged. Current V1 recipient input is ten digits;
the backend performs an exact lookup of `3424-5,` plus those digits, matching the
existing generator and unique full account number. No suffix or fuzzy lookup is used.
## ATM Persistence and PIN Encryption

The existing `card.pin` column now contains a versioned `v1:` Base64 envelope with
12-byte nonce and authenticated AES-256-GCM ciphertext; plaintext PINs are never saved
by new card issuance. Cards reference only accounts, and no balance column is added.
`CardPinMigration` transactionally encrypts legacy four-digit values at startup without
changing their PINs, leaves encrypted values unchanged, and verifies the configured key
can decrypt existing values. Startup fails for malformed data or the wrong key.

`CARD_PIN_ENCRYPTION_KEY` must be a Base64-encoded, random 32-byte secret, retained across
restarts and backed up securely. It is supplied through environment/local secret
configuration, never committed with source code. The test profile uses a separate
non-secret fixture key. This is a targeted legacy PIN conversion within the existing
Hibernate schema-management setup, not a new migration framework.

Deposits and withdrawals each persist one completed transaction and one signed entry
in a READ_COMMITTED transaction. Both lock the Account row with the existing
pessimistic write-lock query, coordinating with transfers. Withdrawal reads the balance
only after acquiring the lock. Any financial write failure rolls back both records.
