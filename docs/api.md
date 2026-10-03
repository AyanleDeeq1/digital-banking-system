# Digital Banking System — API

The backend exposes a REST API used by the frontend.

The application uses session-based authentication with Spring Security.
After successful authentication, the server maintains the authenticated
session using a `JSESSIONID` cookie.

State-changing requests are protected using CSRF.

---

## Customers

### Register Customer

Registers a new customer.

When registration is completed, the customer's first bank account is
created automatically, together with one debit card linked to that Main Account.
Customer, credentials, account, and card are saved atomically. Card issuance
failure returns `503` using the existing error response (`status`, `massage`)
and rolls back registration. Additional account creation does not issue cards.

#### Request

`POST /api/customers`

**Content-Type:** `application/json`

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

#### Success Response

**Status:** `201 Created`

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

---

### Get Current Customer

Returns the currently authenticated customer.

The customer is identified from the authenticated session. The client
does not need to send a customer ID.

#### Request

`GET /api/customers/me`

The request must include the authenticated session cookie.

#### Success Response

**Status:** `200 OK`

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

---

## Authentication

### Get CSRF Token

Retrieves the CSRF token used for state-changing requests.

#### Request

`GET /api/customers/csrf`

#### Success Response

**Status:** `200 OK`

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "..."
}
```

The returned token is sent using the `X-XSRF-TOKEN` header when making
requests that require CSRF protection.

---

### Login Customer

Authenticates an existing customer using their email and password.

The login flow uses CSRF protection and session-based authentication.

Before sending the login request, the client retrieves a CSRF token from:

`GET /api/customers/csrf`

#### Request

`POST /api/customers/login`

**Content-Type:** `application/json`

**X-XSRF-TOKEN:** `<csrf-token>`

```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

#### Success Response

**Status:** `200 OK`

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

After successful authentication, the server creates an authenticated
session and sends a `JSESSIONID` cookie to the client.

The client includes this session cookie in subsequent authenticated
requests.

#### Login Flow

```text
Client
  |
  | GET /api/customers/csrf
  v
Server
  |
  | CSRF token
  v
Client
  |
  | POST /api/customers/login
  | X-XSRF-TOKEN: <csrf-token>
  | email + password
  v
Server
  |
  | Authenticate customer
  | Create authenticated session
  v
Client
  |
  | JSESSIONID cookie
  v
Authenticated requests
```

---

## Accounts

### Get Customer Accounts

Returns all bank accounts belonging to the currently authenticated
customer.

The customer is identified from the authenticated session. The client
does not send a customer ID.

#### Request

`GET /api/customers/accounts`

The request must include the authenticated session cookie.

#### Success Response

**Status:** `200 OK`

```json
[
  {
    "id": 1,
    "name": "Main Account",
    "accountNumber": "3424-5,8392014756",
    "type": "CHECKING",
    "status": "ACTIVE",
    "balance": 0.00
  }
]
```

If the customer has multiple accounts, each account is returned in the
response array.

Each account includes a numeric `balance` in SEK, calculated as the sum of its
signed ledger entries. Accounts without ledger entries return zero. Balance is
calculated for the authenticated customer's accounts and is not stored on `Account`.

---

### Create Account

Creates an additional bank account for the currently authenticated
customer.

The customer is identified from the authenticated session. The client
does not send a customer ID.

The backend generates the account number and creates the account with
`ACTIVE` status. The response includes a calculated balance of zero because the
new account has no ledger entries.

#### Request

`POST /api/customers/createAccount`

**Content-Type:** `application/json`

**X-XSRF-TOKEN:** `<csrf-token>`

```json
{
  "name": "Savings Account",
  "accountType": "SAVINGS"
}
```

Supported account types are:

- `CHECKING`
- `SAVINGS`

#### Success Response

**Status:** `201 Created`

```json
{
  "id": 2,
  "name": "Savings Account",
  "accountNumber": "3424-5,1059382741",
  "type": "SAVINGS",
  "status": "ACTIVE",
  "balance": 0.00
}
```

---

## Debit Card

`GET /api/customers/card` requires the authenticated session cookie. The server
identifies the customer from the session; no customer or account ID is accepted
for selecting a card. Successful responses use `Cache-Control: no-store`.

**Status:** `200 OK`

```json
{
  "cardNumber": "0000000000000123",
  "lastFour": "0123",
  "cardHolderName": "John Doe",
  "expiryDate": "2029-10-01",
  "cvc2": "007",
  "type": "DEBIT"
}
```

The full card number is returned only to the authenticated owner. Internal IDs are not returned. Card numbers and CVC2
are simulated values; strings preserve leading zeros. Expiry is an ISO date,
displayed as MM/YY in the UI. Existing customers without a card receive `404`
with the existing `status` and `massage` error fields; cards are not backfilled.
Unauthenticated requests are rejected by existing Spring Security (`403`).

## Current API Summary

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/customers` | Register a customer |
| `GET` | `/api/customers/csrf` | Retrieve a CSRF token |
| `POST` | `/api/customers/login` | Authenticate a customer |
| `GET` | `/api/customers/me` | Get the authenticated customer |
| `GET` | `/api/customers/accounts` | Get the authenticated customer's accounts |
| `GET` | `/api/customers/card` | Get the authenticated customer's debit card |
| `POST` | `/api/customers/createAccount` | Create an additional account |

---

## Transactions

Transaction and ledger persistence and balance calculation are implemented.
The existing account listing and creation responses expose calculated balances.
Simulated ATM deposits and withdrawals are implemented below. Transaction history is not implemented.

### Transfer Money

`POST /api/customers/accounts/{sourceAccountId}/transfers`

Requires the authenticated session and the existing CSRF header. No customer ID is
accepted; the source must belong to the authenticated customer.

```json
{
  "destinationAccountNumber": "1234567890",
  "amount": 500.00
}
```

The destination may be an own account or another URBank customer's account. Enter
only ten digits, preserving leading zeros. The backend resolves the current V1 fixed
URBank prefix `3424-5,` by exact account-number lookup; full stored numbers remain
unchanged. Both accounts must be ACTIVE and different. Amount must be positive,
fit DECIMAL(19,2), and require no rounding. Trailing zeros are accepted. Currency is
implicitly SEK. The source needs sufficient ledger-derived balance; no overdraft.

**201 Created**

```json
{
  "id": 123,
  "type": "TRANSFER",
  "amount": 500.00,
  "createdAt": "2026-10-02T10:00:00Z",
  "status": "COMPLETED"
}
```

One transaction creates a source entry of -500.00 and destination entry of +500.00.
Ledger details are not returned. Validation failures persist no financial records.
The entire movement is atomic, with both accounts locked in ascending ID order and
the balance checked afterward under READ_COMMITTED isolation.

Errors retain `status`, `massage`, and optional `fieldErrors`: malformed or invalid
input and same-account requests return 400; missing accounts return 404; foreign
source ownership returns 403; inactive accounts or insufficient funds return 409. Transfer lock or write failures return a safe 503 response.
Authentication and CSRF failures are rejected by the existing security filter (403).

The Dashboard Transfer Money action opens My Accounts (account selection) or Another
Account (ten-digit recipient input). Successful transfers re-fetch account data;
React never authoritatively adjusts balances. The combined Deposit / Withdraw action opens the simulated ATM.
## Card PIN Reveal

`POST /api/customers/card/pin` requires session authentication and CSRF. Request:
`{"password":"<application password>"}`. The server verifies the current customer's
application password using BCrypt and returns `200`, `Cache-Control: no-store`, and
`{"pin":"0123"}` with the actual four-digit PIN. This explicit password-protected
reveal is the only API that exposes the actual PIN; the ordinary card endpoint never
returns PIN/ciphertext. The stored encrypted envelope is never exposed. Wrong password
returns `401` through the existing error convention. No customer or card ID is accepted.
My Card hides the displayed PIN after 30 seconds, and clears submitted password state.

## Simulated ATM Channel

All endpoints require the existing authenticated session, `credentials: "include"`,
and the current CSRF header. The customer/card is resolved from the principal.

| Method | Endpoint | Body | Success |
|---|---|---|---|
| POST | `/api/customers/atm/pin` | `{"pin":"0123"}` | 204; verifies card PIN in this HTTP session |
| POST | `/api/customers/atm/eject` | None | 204; clears PIN verification |
| POST | `/api/customers/atm/accounts/{accountId}/deposits` | `{"amount":500.00}` | 201; completed DEPOSIT |
| POST | `/api/customers/atm/accounts/{accountId}/withdrawals` | `{"amount":500.00}` | 201; completed WITHDRAWAL |

PIN verification returns `401` for an incorrect PIN and clears any previous ATM
verification. Missing card returns `404`. Deposits and withdrawals require a verified
card in this session matching the current principal; unverified requests return `403`.

Only ACTIVE accounts owned by the current customer may be selected, including accounts
other than the card's linked Main Account. Missing account returns `404`, foreign
account `403`, inactive account or insufficient funds `409`, invalid monetary input
`400`, and handled lock/write failures `503`. Errors retain `status`, `massage`, and
optional `fieldErrors`. Authentication/CSRF rejection remains the existing `403`.

Both operations return the existing transaction response shape:

```json
{"id":123,"type":"DEPOSIT","amount":500.00,"createdAt":"2026-10-02T10:00:00Z","status":"COMPLETED"}
```

Withdrawal uses `WITHDRAWAL` in the same shape. A deposit adds one +500.00 entry;
a withdrawal adds one -500.00 entry. Transactions store a positive amount. Failed
operations persist no transaction/entry, and balance stays ledger-derived. Amounts
follow MonetaryAmount rules, are positive SEK values, and may contain harmless trailing
zeros. Withdrawal allows exact-balance spending and rejects overdraft.

The Dashboard links to one `/deposit-withdraw` React page. Insert/PIN/menu/operation/result
states change the live overlay on the unchanged ATM image. Success re-fetches account
data before displaying the new balance. Eject clears the server verification and UI
inputs. No physical cash inventory, banknote denominations, transaction history, PIN
reset/change, external bank, fee, receipt, or ATM-location endpoint is introduced.
### Logout Customer

`POST /api/customers/logout` uses the current session and CSRF header. Success returns
`204 No Content` without a redirect. Spring Security clears authentication, invalidates
the HTTP session (including ATM PIN verification), and deletes the JSESSIONID cookie.
GET cannot perform logout. Missing/invalid CSRF returns 403 and preserves the session.
Header and sidebar share the App logout handler, disable submission while pending,
show failures inline, and clear React customer state only after confirmed success.
Success navigates Home and fetches a fresh anonymous CSRF token for subsequent login
or registration. Authenticated account access is denied after logout.