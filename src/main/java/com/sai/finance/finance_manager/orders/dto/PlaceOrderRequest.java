package com.sai.finance.finance_manager.orders.dto;

import com.sai.finance.finance_manager.orders.model.OrderCategory;
import com.sai.finance.finance_manager.orders.model.OrderSide;
import lombok.Data;

@Data
public class PlaceOrderRequest {

    private String symbol;
    private int quantity;

    private OrderSide side;          // BUY / SELL
    private OrderCategory category;  // MARKET / LIMIT / SL / SL_M

    private Double limitPrice;       // For LIMIT orders
    private Double triggerPrice;     // For SL / SL-M orders
}
