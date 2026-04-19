package com.sai.finance.finance_manager.orders.engine;

import com.sai.finance.finance_manager.holdings.service.HoldingsService;
import com.sai.finance.finance_manager.marketdata.service.MarketDataService;
import com.sai.finance.finance_manager.marketdata.service.MarketDataSubscriptionManager;
import com.sai.finance.finance_manager.orders.model.*;
import com.sai.finance.finance_manager.orders.repository.OrderRepository;
import com.sai.finance.finance_manager.orders.service.PendingOrderService;
import com.sai.finance.finance_manager.wallet.dto.WalletTransactionRequest;
import com.sai.finance.finance_manager.wallet.model.TransactionType;
import com.sai.finance.finance_manager.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMatchingEngine {

    private final PendingOrderService pendingOrderService;
    private final OrderRepository orderRepository;
    private final MarketDataService marketDataService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    private final MarketDataSubscriptionManager marketDataSubscriptionManager;


    @Scheduled(fixedRate = 1000)
    public void matchOrders() {

        List<Order> pendingOrders = pendingOrderService.getPendingOrders();
        List<Order> triggeredOrders = pendingOrderService.getTriggeredOrders();

        handleTriggers(pendingOrders);

        handleExecutions(triggeredOrders);
        handleExecutions(pendingOrders);
    }

    private void handleTriggers(List<Order> pendingOrders) {
        for (Order order : pendingOrders) {

            if (order.getCategory() == OrderCategory.SL ||
                    order.getCategory() == OrderCategory.SL_M) {

                double ltp = marketDataService.getLtp(order.getSymbol());

                if (isTriggerHit(order, ltp)) {
                    order.setStatus(OrderStatus.TRIGGERED);
                    orderRepository.save(order);
                }
            }
        }
    }

    private boolean isTriggerHit(Order order, double ltp) {

        if (order.getSide() == OrderSide.SELL) {
            return ltp <= order.getTriggerPrice();
        } else {
            return ltp >= order.getTriggerPrice();
        }
    }

    private void handleExecutions(List<Order> orders) {
        for (Order order : orders) {

            double ltp = marketDataService.getLtp(order.getSymbol());

            if (shouldExecute(order, ltp)) {
                executeOrder(order, ltp);
            }
        }
    }

    private boolean shouldExecute(Order order, double ltp) {

        // SL-M executes immediately after trigger
        if (order.getStatus() == OrderStatus.TRIGGERED &&
                order.getCategory() == OrderCategory.SL_M) {
            return true;
        }

        // SL behaves like LIMIT after trigger
        if (order.getStatus() == OrderStatus.TRIGGERED &&
                order.getCategory() == OrderCategory.SL) {
            return isLimitConditionMet(order, ltp);
        }

        // LIMIT orders
        if (order.getStatus() == OrderStatus.PENDING &&
                order.getCategory() == OrderCategory.LIMIT) {
            return isLimitConditionMet(order, ltp);
        }

        return false;
    }

    private boolean isLimitConditionMet(Order order, double ltp) {

        if (order.getSide() == OrderSide.BUY) {
            return ltp <= order.getLimitPrice();
        } else {
            return ltp >= order.getLimitPrice();
        }
    }

    private void executeOrder(Order order, double executionPrice) {

        String userIdStr = order.getUserId();
        Long userId = Long.valueOf(userIdStr);
        String symbol = order.getSymbol();
        double qty = order.getQuantity();
        double tradeValue = executionPrice * qty;

        WalletTransactionRequest txnReq = new WalletTransactionRequest();
        txnReq.setAmount(BigDecimal.valueOf(tradeValue));
        txnReq.setCategory("ORDER");

        if (order.getSide() == OrderSide.BUY) {

            txnReq.setTransactionType(TransactionType.DEBIT);
            txnReq.setDescription("BUY " + symbol);

            walletService.addTransaction(userId, txnReq);
            holdingsService.addOrUpdateHolding(userIdStr, symbol, qty, executionPrice);

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

}
