package com.sai.finance.finance_manager.orders.service;

import com.sai.finance.finance_manager.orders.model.Order;
import com.sai.finance.finance_manager.orders.model.OrderStatus;
import com.sai.finance.finance_manager.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PendingOrderService {

    private final OrderRepository orderRepository;

    public List<Order> getPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING);
    }

    public List<Order> getTriggeredOrders() {
        return orderRepository.findByStatus(OrderStatus.TRIGGERED);
    }

    public void markAsTriggered(Order order) {
        order.setStatus(OrderStatus.TRIGGERED);
        orderRepository.save(order);
    }

    public void markAsExecuted(Order order) {
        order.setStatus(OrderStatus.EXECUTED);
        orderRepository.save(order);
    }

    public void markAsCancelled(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}
