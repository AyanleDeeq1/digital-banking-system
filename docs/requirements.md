# Digital Banking System — Requirements

## Customer and Accounts

- A person can register as a customer of the bank.
- When registration is completed, the customer receives their first bank account.
- A customer can open additional bank accounts.
- A customer can view their bank accounts.
- A customer can view the balance of their accounts.

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