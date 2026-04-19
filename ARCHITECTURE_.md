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
    Client["Client Web/Mobile"] -->|HTTP (REST)| API["Spring Boot REST Controllers"]
    Client -->|WebSocket| WS["WebSocket Endpoint"]

    API --> SVC["Service Layer"]
    WS --> RT["Real-Time Tick Stream"]

    SVC --> MD["Market Data Services"]
    SVC --> ORD["Orders & Trading Engine"]
    SVC --> WAL["Wallet Service"]
    SVC --> HLD["Holdings Service"]
    SVC --> PRT["Portfolio Service"]
    SVC --> EXP["Explore Service"]

    MD --> YF["YahooFinanceClient"]
    YF -->|HTTP| Yahoo["Yahoo Finance API"]

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

---

## 4. Market data architecture

### 4.1 Explore mini‑universe (500 stocks)

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

### 4.2 WebSocket architecture

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

---

## 5. Trading engine architecture

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

---

### 5.2 Matching engine

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

---

## 6. Security & rate limiting architecture

```mermaid
flowchart LR
    REQ["Incoming HTTP Request"] --> RL["RateLimitFilter (Bucket4j)"]
    RL -->|Allowed| JWT["JwtFilter"]
    RL -->|Rejected| ERR["429 Too Many Requests"]

    JWT --> SEC["Spring Security Context"]
    SEC --> CTRL["Controller"]
```

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

- Real NSE market data  
- Virtual money  
- Real‑time tick streaming  
- A realistic trading engine  
- PostgreSQL persistence  
- JWT security + rate limiting  

It is designed to be scalable, maintainable, and production‑grade.

