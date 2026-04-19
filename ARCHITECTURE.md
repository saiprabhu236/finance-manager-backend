# ARCHITECTURE.md  
Finance Manager Backend — Trading Simulation Platform

---

## 1. Introduction

This document describes the **system architecture** of the Finance Manager Backend — a **paper‑trading platform** that uses **real NSE market data (15‑minute delayed)**, **virtual money**, and a **simulated trading engine**.

The goals of this architecture are:

- To simulate a **real brokerage‑style trading backend**
- To handle **real‑time market data** efficiently
- To maintain **clean separation of concerns**
- To be **scalable, maintainable, and extensible**
- To showcase **production‑grade backend engineering skills**

---

## 2. High‑level system architecture

```mermaid
flowchart LR
    Client["Client Web/Mobile"] -->|"HTTP (REST)"| API["Spring Boot REST Controllers"]
    Client -->|"WebSocket"| WS["WebSocket Endpoint"]

    API --> SVC["Service Layer"]
    WS --> RT["Real-Time Tick Stream"]

    SVC --> MD["Market Data Services"]
    SVC --> ORD["Orders & Trading Engine"]
    SVC --> WAL["Wallet Service"]
    SVC --> HLD["Holdings Service"]
    SVC --> PRT["Portfolio Service"]
    SVC --> EXP["Explore Service"]

    MD --> YF["YahooFinanceClient"]
    YF -->|"HTTP"| Yahoo["Yahoo Finance API"]

    SVC --> REPO["JPA Repositories"]
    REPO --> DB["PostgreSQL"]

    subgraph RealTime
        MD --> SNAP["SnapshotService"]
        SNAP --> TICK["TickEngine"]
        TICK --> RT
        TICK --> EXP
    end
```


---

## 3. Layered architecture

```mermaid
flowchart TB
    PL["Presentation Layer (Controllers, WebSocket)"] --> SL["Service Layer (Business Logic)"]
    SL --> DL["Domain Layer (Entities, DTOs, Domain Models)"]
    DL --> PERS["Data Layer (JPA Repositories, PostgreSQL)"]
    SL --> INT["Integration Layer (Yahoo Client, Schedulers, TickEngine, WebSocket Broadcaster, RateLimitFilter)"]
```
### 3.1 Presentation layer

- **REST Controllers** expose endpoints for:
  - Auth, OTP, JWT
  - Wallet
  - Orders
  - Holdings
  - Portfolio
  - Market data
  - Explore
- **WebSocket handlers** manage:
  - Client subscriptions to tick streams
  - Broadcasting of real‑time tick updates

### 3.2 Service layer

- Encapsulates **business logic** and workflows:
  - Order validation and placement
  - Matching engine orchestration
  - Wallet debit/credit logic
  - Holdings and portfolio calculations
  - Explore rankings (gainers/losers, sectors, themes)
  - Market data aggregation

### 3.3 Domain layer

- **Entities**: JPA entities mapped to PostgreSQL tables  
- **DTOs**: Request/response models for APIs and WebSocket messages  
- **Domain models**: Internal representations for ticks, snapshots, metrics, etc.

### 3.4 Data layer

- **JPA repositories** for all entities:
  - Users, OTP, refresh tokens
  - Wallet, wallet transactions
  - Orders
  - Holdings
- **PostgreSQL** as the only database engine

### 3.5 Integration layer

- **YahooFinanceClient** for external market data  
- **Schedulers** for:
  - Yahoo polling (mini‑universe)
  - Matching engine  
- **TickEngine** for synthetic ticks  
- **WebSocket broadcaster** for real‑time streaming  
- **RateLimitFilter** (Bucket4j) for global rate limiting  


---

## 4. Market data architecture

Market data is the backbone of the system. It is split into:

- **Real price fetcher** (MarketDataService + YahooFinanceClient)  
- **Explore mini‑universe** (500 stocks, scheduled polling)  
- **TickEngine** (synthetic live movement)  
- **SnapshotService** (single source of truth for OHLC + change%)  
- **WebSocket broadcasting** (real‑time ticks to clients)  

### 4.1 Real price fetcher (MarketDataService + YahooFinanceClient)

The `YahooFinanceClient` is responsible for fetching **real NSE prices** from Yahoo Finance, with **15‑minute delayed data** (as per Yahoo’s behavior). It provides:

- **Current price** (`PriceDto`)
- **Historical candles** (`CandleDto`)
- **Stock metrics** (`StockMetricsDto`)

It uses **in‑memory caches** with TTL to reduce external calls and handle transient failures.

Example (from `YahooFinanceClient`):

```java
public PriceDto getCurrentPrice(String rawSymbol) {
    String symbol = SymbolMapper.toYahooSymbol(rawSymbol);
    validateSymbol(symbol);

    try {
        // CACHE (TTL = 5 seconds)
        CachedPrice cached = priceCache.get(symbol);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < 5000) {
            return cached.price;
        }

        // DIRECT FETCH
        PriceDto dto = fetchPrice(symbol);

        // CACHE RESULT
        priceCache.put(symbol, new CachedPrice(dto, System.currentTimeMillis()));

        return dto;

    } catch (Exception ex) {
        log.error("Error fetching price for {}", rawSymbol, ex);

        // FALLBACK
        CachedPrice cached = priceCache.get(symbol);
        if (cached != null) {
            return cached.price;
        }

        return new PriceDto(rawSymbol, 0, 0, 0, 0);
    }
}
```

This service is used by:

- Watchlist  
- Holdings  
- Portfolio  
- Stock detail pages  
- Charts and metrics views

### 4.2 Explore mini‑universe (500 stocks)

The Explore module maintains a **mini‑universe of 500 NSE stocks**. It is optimized for:

- Top gainers / losers  
- Sector movers  
- Themes (EV, Infra, Make in India, etc.)  
- Trending / most active  

**Key design:**

- 500 symbols loaded from a universe configuration (e.g., `universe.json`)  
- Scheduler runs every **120ms**  
- Each tick:
  - Picks **1 symbol**
  - Fetches its latest price from Yahoo
  - Updates in‑memory state
- Over ~60 seconds, all 500 symbols are refreshed  
- Synthetic ticks are then generated for live movement  

```mermaid
flowchart LR
    subgraph Universe
        U["Universe (500 NSE Symbols)"]
    end

    SCHED["@Scheduled(120ms) Yahoo Polling"] -->|1 symbol per cycle| YF["YahooFinanceClient"]
    YF --> ST["SymbolStateStore (in-memory)"]

    ST --> SNAP["SnapshotService"]
    SNAP --> EXP["ExploreService"]

    EXP --> EXP_API["ExploreController (Gainers/Losers/Sectors/Themes)"]
```

---

### 4.3 WebSocket architecture

The WebSocket layer streams **real‑time ticks** to subscribed clients.

```mermaid
flowchart LR
    TICK["TickEngine"] --> SNAP["SnapshotService"]
    SNAP --> BROAD["TickBroadcaster"]

    subgraph WebSocketLayer
        SUB["Subscription Manager"] --> SESS["Active Sessions"]
        BROAD --> SESS
    end

    ClientA["Client A"] -->|Subscribe| SUB
    ClientB["Client B"] -->|Subscribe| SUB
```

- Clients subscribe to specific symbols or groups  
- Subscription manager tracks per‑session subscriptions  
- Broadcaster pushes tick messages only to interested sessions  
- Avoids duplicate sessions and memory leaks 

---

### 4.4 SnapshotService

`SnapshotService` maintains **derived metrics** for each symbol:

- OHLC (open, high, low, close)  
- Change and changePercent  
- Volume (if applicable)  
- Last tick time  

It acts as the **single source of truth** for:

- Explore rankings  
- Watchlist snapshots  
- Portfolio valuations

---

## 5. Trading engine architecture
The trading engine simulates a **real brokerage order lifecycle** with:

- Market, Limit, SL, SL‑M orders  
- Pending queue  
- Matching engine (scheduled)  
- Wallet integration  
- Holdings integration  
- Portfolio impact  

### 5.1 Order placement flow

```mermaid
flowchart LR
    Client --> ORD_API["OrdersController"]
    ORD_API --> ORD_SVC["OrdersService"]
    ORD_SVC --> VAL["Validation"]
    VAL --> SAVE["Save Order (PENDING)"]
    SAVE --> QUEUE["Pending Orders Queue"]
    QUEUE --> MATCH["Matching Engine (@Scheduled)"]
```

```java
public OrderDto placeOrder(PlaceOrderRequest req, User user) {
    validateOrder(req, user);

    Order order = orderMapper.toEntity(req, user);
    order.setStatus(OrderStatus.PENDING);
    orderRepository.save(order);

    pendingOrderQueue.add(order.getId());
    return orderMapper.toDto(order);
}
```
---

### 5.2 Matching engine

The **Matching Engine** runs every second and processes pending orders:

- Reads pending orders  
- Fetches current tickPrice for the symbol  
- Applies execution rules based on order type:

  - **Market**: execute at current tickPrice  
  - **Limit**: execute if price crosses limit condition  
  - **SL / SL‑M**: execute when trigger price is hit  

```mermaid
flowchart TB
    MATCH_SCHED["@Scheduled(1s) matchOrders()"] --> LOAD["Load Pending Orders"]
    LOAD --> LOOP["For each Order"]
    LOOP --> PRICE["Get tickPrice from SnapshotService"]
    PRICE --> DECIDE{"Execution Condition Met?"}
    DECIDE -->|No| SKIP["Keep Pending"]
    DECIDE -->|Yes| EXEC["Execute Order"]
    EXEC --> WAL["Update Wallet"]
    EXEC --> HLD["Update Holdings"]
    EXEC --> STATUS["Update Order Status (FILLED / PARTIAL)"]
```

Pseudo‑code:

```java
@Scheduled(fixedRate = 1000)
public void matchOrders() {
    List<Order> pending = orderRepository.findAllPending();

    for (Order order : pending) {
        double ltp = snapshotService.getLastPrice(order.getSymbol());

        if (!shouldExecute(order, ltp)) {
            continue;
        }

        executeOrder(order, ltp);
    }
}
```

### 5.3 Wallet integration

- **BUY**:
  - Debit wallet balance by `executedQty * executedPrice`
  - Create wallet transaction entry  
- **SELL**:
  - Credit wallet balance  
  - Create wallet transaction entry  

Wallet is modeled as:

- **Wallet**: current balance (derived from transactions)  
- **WalletTransaction**: immutable ledger of all debits/credits  

### 5.4 Holdings integration

On execution:

- For **BUY**:
  - If holding exists:
    - Update quantity  
    - Recalculate weighted average price  
  - Else:
    - Create new holding  
- For **SELL**:
  - Reduce quantity  
  - If quantity becomes zero → delete holding  

Holdings store:

- Symbol  
- Quantity  
- Average buy price  

### 5.5 Portfolio calculation

Portfolio is computed **on the fly** using:

- Holdings (quantity, avgPrice)  
- Current tickPrice (from SnapshotService or MarketDataService)  
- Previous close (for today’s P&L)

For each holding:

- **Current value** = `qty * tickPrice`  
- **Cost** = `qty * avgPrice`  
- **Unrealized P&L** = `currentValue - cost`  
- **Today’s P&L** = `qty * (tickPrice - previousClose)`  

Portfolio aggregates:

- Total portfolio value  
- Total unrealized P&L  
- Total today’s P&L  
- Per‑stock breakdown  

---

## 6. Security & rate limiting architecture

Security is handled via:

- **JWT authentication**  
- **Spring Security filters**  
- **Global rate limiting** using Bucket4j  

```mermaid
flowchart LR
    REQ["Incoming HTTP Request"] --> RL["RateLimitFilter (Bucket4j)"]
    RL -->|Allowed| JWT["JwtFilter"]
    RL -->|Rejected| ERR["429 Too Many Requests"]

    JWT --> SEC["Spring Security Context"]
    SEC --> CTRL["Controller"]
```

### 6.1 JWT authentication

- Access tokens are validated on each request  
- User identity is loaded into the security context  
- Protected routes:
  - `/wallet/**`
  - `/orders/**`
  - `/holdings/**`
  - `/portfolio/**`
  - `/market/**`
  - `/explore/**` (if required)

### 6.2 Rate limiting (Bucket4j)

- **Global IP‑based rate limiting**  
- Example: 20 requests/min (configurable)  
- Applied **before** JWT filter to protect:
  - OTP endpoints  
  - Auth endpoints  
  - Market data endpoints  
  - Orders and trading endpoints 

---

## 7. Database architecture (PostgreSQL)

```mermaid
erDiagram
    USERS {
        UUID id
        string email
        string password_hash
        timestamp created_at
    }

    OTP {
        UUID id
        string email
        string code
        timestamp expires_at
    }

    REFRESH_TOKENS {
        UUID id
        UUID user_id
        string token
        timestamp expires_at
    }

    WALLETS {
        UUID id
        UUID user_id
        numeric balance
    }

    WALLET_TRANSACTIONS {
        UUID id
        UUID wallet_id
        numeric amount
        string type
        timestamp created_at
    }

    ORDERS {
        UUID id
        UUID user_id
        string symbol
        string side
        string type
        numeric price
        numeric trigger_price
        numeric quantity
        string status
        timestamp created_at
    }

    HOLDINGS {
        UUID id
        UUID user_id
        string symbol
        numeric quantity
        numeric avg_price
    }

    USERS ||--o{ WALLETS : owns
    WALLETS ||--o{ WALLET_TRANSACTIONS : has
    USERS ||--o{ ORDERS : places
    USERS ||--o{ HOLDINGS : holds
    USERS ||--o{ REFRESH_TOKENS : has
```

---

## 8. Scalability & future enhancements

- Horizontal scaling  
- Redis caching  
- Kafka event‑driven architecture  
- Microservices split (Auth, Market Data, Trading, Wallet, Portfolio)

---

## 9. Conclusion

This architecture combines:

- **Real NSE market data (15‑minute delayed)**  
- **Virtual money and simulated execution**  
- **Clean layered design**  
- **Real‑time tick streaming**  
- **A realistic trading engine**  
- **PostgreSQL‑backed persistence**  
- **JWT security and rate limiting**  

It is designed to be:

- **Understandable** for new developers  
- **Impressive** for hiring managers and senior engineers  
- **Extensible** for future features  
- **Robust** enough to resemble a real fintech backend.

This document, together with `README.md` and API documentation, presents the Finance Manager Backend as a **serious, production‑style trading simulation platform** built with **professional backend engineering practices**.
```
