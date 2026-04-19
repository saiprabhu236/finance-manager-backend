package com.sai.finance.finance_manager.orders.dto;

import com.sai.finance.finance_manager.orders.model.OrderCategory;
import com.sai.finance.finance_manager.orders.model.OrderSide;
import com.sai.finance.finance_manager.orders.model.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {

    private Long orderId;
    private String symbol;
    private int quantity;

    private OrderSide side;
    private OrderCategory category;

    private Double limitPrice;
    private Double triggerPrice;

    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}
