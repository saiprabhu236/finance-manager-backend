package com.sai.finance.finance_manager.orders.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String symbol;

    private int quantity;

    // BUY or SELL
    @Enumerated(EnumType.STRING)
    private OrderSide side;

    // MARKET, LIMIT, SL, SL_M
    @Enumerated(EnumType.STRING)
    private OrderCategory category;

    // For LIMIT orders
    private Double limitPrice;

    // For SL / SL-M orders
    private Double triggerPrice;

    // PENDING, TRIGGERED, EXECUTED, CANCELLED, REJECTED
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}

