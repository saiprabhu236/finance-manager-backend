# API Documentation  
Finance Manager Backend — Trading Simulation Platform

This document describes all REST and WebSocket APIs exposed by the Finance Manager Backend.  
It is organized according to the **actual user journey**, from authentication → funding → exploring → trading → tracking portfolio → real‑time streaming.

---

# 1. Authentication & User Identity

All authenticated endpoints require:

```
Authorization: Bearer <JWT>
```

## 1.1 Register  
**POST /auth/register**

Registers a new user.

### Request
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "name": "Sai"
}
```

### Response
```json
{
  "message": "Registration successful. OTP sent to email."
}
```

---

## 1.2 Login  
**POST /auth/login**

### Request
```json
{
  "email": "user@example.com",
  "password": "Password123"
}
```

### Response
```json
{
  "accessToken": "<JWT>",
  "refreshToken": "<REFRESH_TOKEN>"
}
```

---

## 1.3 Verify Email OTP  
**POST /auth/verify-otp**

### Request
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

### Response
```json
{ "verified": true }
```

---

## 1.4 Refresh Token  
**POST /auth/refresh-token**

### Request
```json
{
  "refreshToken": "<REFRESH_TOKEN>"
}
```

### Response
```json
{
  "accessToken": "<NEW_JWT>"
}
```

---

## 1.5 Forgot Password Flow  
Endpoints:

- **POST /auth/forgot-password**
- **POST /auth/resend-otp**
- **POST /auth/verify-reset-otp**
- **POST /auth/reset-password**

All accept simple JSON maps like:

```json
{ "email": "user@example.com" }
```

---

## 1.6 JWT Validation Check  
**GET /home**

Returns success if JWT is valid.

---

# 2. Wallet (User Funds Before Trading)

## 2.1 Get Wallet Balance  
**GET /wallet/balance**

### Response
```json
{
  "balance": 50000.00,
  "currency": "INR"
}
```

---

## 2.2 Get All Transactions  
**GET /wallet/transactions**

### Response
```json
[
  {
    "amount": 10000,
    "transactionType": "CREDIT",
    "description": "Added funds",
    "category": "DEPOSIT",
    "referenceId": "TXN123",
    "balanceAfterTransaction": 50000,
    "timestamp": "2025-01-01T10:00:00"
  }
]
```

---

## 2.3 Add Transaction (Credit/Debit)  
**POST /wallet/transaction**

### Request
```json
{
  "amount": 5000,
  "transactionType": "DEBIT",
  "description": "Buy TCS",
  "category": "TRADE",
  "referenceId": "ORDER123"
}
```

---

## 2.4 Filter Transactions  
**POST /wallet/transactions/filter**

Supports pagination + date range + type filter.

---

# 3. Discover Stocks (Explore + Market Data)

# 3.1 Explore Module

## Top Gainers  
**GET /explore/top-gainers?limit=20**

## Top Losers  
**GET /explore/top-losers**

## Most Active  
**GET /explore/most-active**

## Trending  
**GET /explore/trending**

## 52 Week High / Low  
**GET /explore/52week-high**  
**GET /explore/52week-low**

### Sample Response
```json
[
  {
    "symbol": "TCS",
    "price": 4025.5,
    "changePercent": 1.25,
    "volume": 1200000
  }
]
```

---

## Sectors  
**GET /explore/sector**

## Sector Details  
**GET /explore/sector/{sector}**

---

## Themes  
**GET /explore/themes**

## Theme Details  
**GET /explore/themes/{theme}**

---

# 3.2 Market Data

## Search Stocks  
**GET /market/search?query=TCS**

## Current Price  
**GET /market/price?symbol=TCS**

### Response
```json
{
  "symbol": "TCS",
  "price": 4025.5,
  "open": 3980,
  "high": 4050,
  "low": 3975,
  "previousClose": 3990
}
```

---

## Historical Data  
**GET /market/history?symbol=TCS&period=1d**

## Stock Metrics  
**GET /market/metrics?symbol=TCS**

## Market Status  
**GET /market/status**

---

# 4. Watchlist (Pre‑Trade Shortlisting)

## Add to Watchlist  
**POST /watchlist/add?symbol=TCS**

## Remove from Watchlist  
**DELETE /watchlist/remove?symbol=TCS**

## Get Watchlist  
**GET /watchlist?sort=asc**

## Check If In Watchlist  
**GET /watchlist/status?symbol=TCS**

---

# 5. Holdings (What User Already Owns)

## Get Holdings  
**GET /holdings?sort=none**

## Check If Symbol Is In Holdings  
**GET /holdings/status?symbol=TCS**

---

# 6. Orders (Trading)

## 6.1 Place Order  
**POST /orders/place**

### Request
```json
{
  "symbol": "TCS",
  "type": "MARKET",
  "side": "BUY",
  "quantity": 10
}
```

### Response
```json
{
  "orderId": "uuid",
  "symbol": "TCS",
  "side": "BUY",
  "status": "PENDING",
  "quantity": 10
}
```

---

## 6.2 Quick Buy / Sell  
**POST /orders/buy**  
**POST /orders/sell**

Same request body as `/place`, but side is auto‑set.

---

## 6.3 Order History  
**GET /orders/history**

---

# 7. Portfolio (Post‑Trade Analytics)

## Portfolio Summary  
**GET /portfolio**

### Response
```json
{
  "totalValue": 150000,
  "totalPnL": 5000,
  "todayPnL": 1200,
  "holdings": [
    {
      "symbol": "TCS",
      "quantity": 10,
      "avgPrice": 3900,
      "ltp": 4025.5,
      "unrealizedPnL": 1255
    }
  ]
}
```

---

## Portfolio History  
**GET /portfolio/history**

Returns chart points for portfolio value over time.

---

# 8. Real‑Time Streaming (WebSocket)

### WebSocket URL
```
ws://<server>/ws
```

---

## 8.1 Subscribe to Ticks  
**Client → Server**
```json
{
  "action": "subscribe",
  "symbols": ["TCS", "INFY"]
}
```

---

## 8.2 Unsubscribe  
```json
{
  "action": "unsubscribe",
  "symbols": ["TCS"]
}
```

---

## 8.3 Tick Message Schema  
**Server → Client**
```json
{
  "symbol": "TCS",
  "price": 4025.5,
  "change": 35.5,
  "changePercent": 0.89,
  "high": 4050,
  "low": 3975,
  "timestamp": "2025-01-01T10:00:00"
}
```

---

## 8.4 Portfolio Tick Stream  
```json
{
  "timestamp": "2025-01-01T10:00:00",
  "portfolioValue": 150250.75
}
```

---

# 9. Common Error Responses

### 401 Unauthorized
```json
{ "error": "Invalid or expired JWT" }
```

### 400 Bad Request
```json
{ "error": "Insufficient balance" }
```

### 404 Not Found
```json
{ "error": "Symbol not found" }
```

### 429 Too Many Requests
```json
{ "error": "Rate limit exceeded" }
```

---

# End of API Documentation
