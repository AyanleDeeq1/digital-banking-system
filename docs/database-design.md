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
- `customer_id`: required unique foreign key to `customer`
- `account_id`: required unique foreign key to `account`

Each card belongs to one customer and that customer's registration-created
Main Account. Existing customers/accounts can have no card. Registration saves
customer, credentials, account, and card in one transaction. Number generation
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

Schema management remains Hibernate `ddl-auto: update` for the application and
`create-drop` for tests. No migration strategy has been introduced. Financial
operation services, account locking, and HTTP endpoints are not part of this
foundational implementation step.
