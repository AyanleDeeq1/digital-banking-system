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