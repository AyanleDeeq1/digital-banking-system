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
Deposit, withdrawal, transfer, and transaction-history endpoints are not implemented.
