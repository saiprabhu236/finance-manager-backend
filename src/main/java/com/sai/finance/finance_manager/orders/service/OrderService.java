package com.sai.finance.finance_manager.orders.service;

import com.sai.finance.finance_manager.holdings.service.HoldingsService;
import com.sai.finance.finance_manager.marketdata.service.MarketDataService;
import com.sai.finance.finance_manager.marketdata.service.MarketDataSubscriptionManager;
import com.sai.finance.finance_manager.orders.dto.OrderResponse;
import com.sai.finance.finance_manager.orders.dto.PlaceOrderRequest;
import com.sai.finance.finance_manager.orders.model.*;
import com.sai.finance.finance_manager.orders.repository.OrderRepository;
import com.sai.finance.finance_manager.wallet.dto.WalletTransactionRequest;
import com.sai.finance.finance_manager.wallet.model.TransactionType;
import com.sai.finance.finance_manager.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final HoldingsService holdingsService;
    private final MarketDataService marketDataService;
    private final WalletService walletService;
    private final MarketDataSubscriptionManager marketDataSubscriptionManager;


    public OrderResponse placeOrder(String userId, PlaceOrderRequest req) {

        validateOrder(req);

        // SELL validation
        if (req.getSide() == OrderSide.SELL) {
            validateSellHoldings(userId, req);
        }

        Long userIdLong = Long.valueOf(userId);

        // BUY funds validation
        if (req.getSide() == OrderSide.BUY) {
            validateFundsForBuy(userIdLong, req);
        }

        // Create order
        Order order = Order.builder()
                .userId(userId)
                .symbol(req.getSymbol())
                .quantity(req.getQuantity())
                .side(req.getSide())
                .category(req.getCategory())
                .limitPrice(req.getLimitPrice())
                .triggerPrice(req.getTriggerPrice())
                .status(determineInitialStatus(req))
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);

        // MARKET orders execute instantly
        if (order.getCategory() == OrderCategory.MARKET) {
            executeMarketOrder(order, userIdLong);
        }

        return toResponse(order);
    }

    private void validateOrder(PlaceOrderRequest req) {

        if (req.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        if (req.getCategory() == OrderCategory.LIMIT && req.getLimitPrice() == null) {
            throw new RuntimeException("Limit price required for LIMIT order");
        }

        if ((req.getCategory() == OrderCategory.SL || req.getCategory() == OrderCategory.SL_M)
                && req.getTriggerPrice() == null) {
            throw new RuntimeException("Trigger price required for SL / SL-M order");
        }
    }

    private void validateSellHoldings(String userId, PlaceOrderRequest req) {

        double availableQty = holdingsService.getHoldingQuantity(userId, req.getSymbol());

        if (availableQty == 0) {
            throw new RuntimeException("Cannot place SELL order: no holdings for " + req.getSymbol());
        }

        if (availableQty < req.getQuantity()) {
            throw new RuntimeException("Cannot sell more than you own. Available: "
                    + availableQty + ", Requested: " + req.getQuantity());
        }
    }

    private void validateFundsForBuy(Long userId, PlaceOrderRequest req) {

        double priceForCheck;

        if (req.getCategory() == OrderCategory.MARKET) {
            priceForCheck = marketDataService.getLtp(req.getSymbol());
        } else {
            priceForCheck = req.getLimitPrice();
        }

        double required = priceForCheck * req.getQuantity();
        BigDecimal balance = walletService.getCurrentBalance(userId);

        if (balance.compareTo(BigDecimal.valueOf(required)) < 0) {
            throw new RuntimeException("Insufficient funds. Required: " + required +
                    ", Available: " + balance);
        }
    }

    private OrderStatus determineInitialStatus(PlaceOrderRequest req) {

        if (req.getCategory() == OrderCategory.MARKET) {
            return OrderStatus.EXECUTED;
        }

        return OrderStatus.PENDING;
    }

    private void executeMarketOrder(Order order, Long userId) {

        double ltp = marketDataService.getLtp(order.getSymbol());
        double qty = order.getQuantity();
        double tradeValue = ltp * qty;

        WalletTransactionRequest txnReq = new WalletTransactionRequest();
        txnReq.setAmount(BigDecimal.valueOf(tradeValue));
        txnReq.setCategory("ORDER");

        String symbol = order.getSymbol();
        String userIdStr = order.getUserId();

        if (order.getSide() == OrderSide.BUY) {

            txnReq.setTransactionType(TransactionType.DEBIT);
            txnReq.setDescription("BUY " + symbol);

            walletService.addTransaction(userId, txnReq);
            holdingsService.addOrUpdateHolding(userIdStr, symbol, qty, ltp);

            // 🔔 Subscribe after BUY
            marketDataSubscriptionManager.subscribe(symbol);

        } else {

            txnReq.setTransactionType(TransactionType.CREDIT);
            txnReq.setDescription("SELL " + symbol);

            walletService.addTransaction(userId, txnReq);
            holdingsService.reduceOrRemoveHolding(userIdStr, symbol, qty);

            // 🔔 Unsubscribe only if user no longer holds the stock
            double remainingQty = holdingsService.getHoldingQuantity(userIdStr, symbol);
            if (remainingQty <= 0) {
                marketDataSubscriptionManager.unsubscribe(symbol);
            }
        }

        order.setExecutedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.EXECUTED);
        orderRepository.save(order);
    }


    public List<OrderResponse> getOrderHistory(String userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .symbol(order.getSymbol())
                .quantity(order.getQuantity())
                .side(order.getSide())
                .category(order.getCategory())
                .limitPrice(order.getLimitPrice())
                .triggerPrice(order.getTriggerPrice())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .executedAt(order.getExecutedAt())
                .build();
    }
}
