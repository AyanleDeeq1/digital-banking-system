# Digital Banking System — Requirements

## Customer and Accounts

- A person can register as a customer of the bank.
- When registration is completed, the customer receives their first bank account.
- A customer can open additional bank accounts.
- A customer can view their bank accounts.
- A customer can view the balance of their accounts.

## Debit Cards

- New registration issues one debit card for the registration-created Main Account.
- Registration, credentials, account, and card creation succeed or roll back together.
- Additional account creation does not issue another card; existing customers are not backfilled.
- Card numbers contain 16 random digits and are unique; CVC2 contains three random digits and is not unique.
- Leading zeros are preserved. Expiry is three calendar years from issuance in Europe/Stockholm.
- An authenticated customer can view only their own card, including the full card number on My Card.
- The My Card sidebar page reuses the existing card design with real API data; Home keeps its sample card.
- Credit cards, card payments, and card management operations remain outside this feature.

## Transactions

- A customer can deposit money into an account.
- A customer can withdraw money from an account.
- A customer can transfer money between accounts.
- Supported transaction types are `DEPOSIT`, `WITHDRAWAL`, and `TRANSFER`.
- Supported transaction statuses are `PENDING`, `COMPLETED`, and `FAILED`.

## Ledger and Balance

- Financial movements are recorded using ledger entries.
- Only successful financial movements affect the ledger.
- An account balance is determined from its ledger entries.
- Ledger entry amounts are signed: positive for incoming money, negative for outgoing money.
- An account with no ledger entries has a zero balance.
- Monetary values use `BigDecimal` and are implicitly SEK in V1.
- Non-zero fractional digits beyond two decimal places are rejected; harmless trailing zeros are accepted.

The current implementation provides transaction/ledger persistence and internal
balance calculation. Deposit, withdrawal, transfer, and API balance exposure are
subsequent implementation steps; the requirements above do not imply those
operations are already available.
