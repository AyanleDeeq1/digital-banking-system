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
- Each new customer receives only one debit card, linked to the Main Account created at registration. Additional accounts do not receive cards.
- A card belongs only to its account; customer ownership is resolved through that account.
- Existing accounts without cards are not automatically backfilled.
- Card numbers contain 16 random digits and are unique; CVC2 contains three random digits and is not unique.
- Leading zeros are preserved. Expiry is three calendar years from issuance in Europe/Stockholm.
- An authenticated customer can view only their registration-issued card, including the full card number on My Card.
- The My Card sidebar page reuses the existing card design with real API data; Home keeps its sample card.
- Credit cards, card payments, and card management operations remain outside this feature.

## Transactions

- Customers can deposit money through the simulated URBank ATM.
- Customers can withdraw money through the simulated URBank ATM without overdrawing their accounts.
- An authenticated customer can initiate internal SEK transfers between their own accounts or to another URBank customer account.
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

Transfers require an owned source account and ACTIVE source and destination accounts.
Source and destination must differ. The positive amount follows the existing monetary
precision rules. Sufficient ledger-derived funds are required; exact-balance transfers
are allowed and overdraft is rejected. Failed validation persists no financial records.

For URBank V1, recipients are entered as ten digits. The backend resolves them using
the existing fixed clearing prefix; stored numbers such as `3424-5,1234567890` remain
unchanged. The Dashboard has one Deposit / Withdraw action linking to the simulated ATM.
## Simulated URBank ATM

- One `/deposit-withdraw` page uses the existing ATM image, with live screen controls.
- Insert the customer's existing debit card and verify its four-digit PIN on the backend before cash operations.
- Registration generates a random four-digit PIN, preserving leading zeros. PINs are encrypted at rest.
- My Card can show the actual PIN only after verifying the owner's application password; PIN display hides after 30 seconds.
- Deposits and withdrawals accept only ACTIVE accounts owned by the session customer, including additional accounts.
- Deposit creates one COMPLETED DEPOSIT and one positive ledger entry; withdrawal creates one COMPLETED WITHDRAWAL and one negative entry.
- Amounts follow existing SEK monetary rules. Exact-balance withdrawal is allowed; overdraft is rejected.
- Cash operations are atomic, lock the affected account, and re-fetch authoritative balances after success.
- Eject Card clears ATM verification from the server session and clears sensitive form state.
- This simulation does not model physical cash inventory, denominations, fees, receipts, or PIN reset/change workflows.
