## Customers

### Register Customer

Registers a new customer. A first bank account is created automatically
when the customer is registered.

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

### Login Customer

Authenticates an existing customer using their email and password.

The login flow uses CSRF protection and session-based authentication.

#### Get CSRF Token

Before sending the login request, the client retrieves a CSRF token.

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

The returned token is sent in the `X-XSRF-TOKEN` header when making the
login request.

#### Login Request

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

The client includes the session cookie in subsequent requests that
require authentication.

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