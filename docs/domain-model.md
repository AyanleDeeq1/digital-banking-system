# Domain Model

The domain model represents the current core concepts of the Digital Banking System and their relationships.

![Domain Model](../images/domain-model.png)

## Design Decisions

### Password Credentials

`PasswordCredential` is modeled separately from `Customer` to separate authentication data from customer information.

A `PasswordCredential` belongs exclusively to a `Customer` and follows the customer's lifecycle.

### Account Types

An `Account` has an `AccountType` that describes the purpose of the account.

The system currently supports two account types:

- `Checking` — intended for everyday banking activities.
- `Savings` — intended for saving money.

An account also has a user-friendly name, allowing a customer to distinguish between multiple accounts.

### Account Status

An `Account` has an `AccountStatus` that represents its current state.

The system currently supports three account statuses:

- `Active` — the account is active and can be used normally.
- `Frozen` — the account is temporarily restricted.
- `Closed` — the account is no longer active.

### Account Balance

Balance is not stored directly on `Account`. It will be calculated from the account's transaction history when transaction functionality is introduced.