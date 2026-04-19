package com.sai.finance.finance_manager.orders.repository;

import com.sai.finance.finance_manager.orders.model.Order;
import com.sai.finance.finance_manager.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(String userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    default List<Order> findPendingOrders() {
        return findByStatus(OrderStatus.PENDING);
    }

    default List<Order> findTriggeredOrders() {
        return findByStatus(OrderStatus.TRIGGERED);
    }

}
