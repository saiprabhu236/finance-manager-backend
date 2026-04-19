package com.sai.finance.finance_manager.orders.controller;

import com.sai.finance.finance_manager.orders.dto.PlaceOrderRequest;
import com.sai.finance.finance_manager.orders.dto.OrderResponse;
import com.sai.finance.finance_manager.orders.service.OrderService;
import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.repository.UserRepository;
import com.sai.finance.finance_manager.service.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/place")
    public OrderResponse placeOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PlaceOrderRequest request) {

        String userId = extractUserId(authHeader);
        return orderService.placeOrder(userId, request);
    }

    @PostMapping("/buy")
    public OrderResponse buy(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PlaceOrderRequest request) {

        String userId = extractUserId(authHeader);
        request.setSide(com.sai.finance.finance_manager.orders.model.OrderSide.BUY);
        return orderService.placeOrder(userId, request);
    }

    @PostMapping("/sell")
    public OrderResponse sell(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PlaceOrderRequest request) {

        String userId = extractUserId(authHeader);
        request.setSide(com.sai.finance.finance_manager.orders.model.OrderSide.SELL);
        return orderService.placeOrder(userId, request);
    }

    @GetMapping("/history")
    public List<OrderResponse> getHistory(
            @RequestHeader("Authorization") String authHeader) {

        String userId = extractUserId(authHeader);
        return orderService.getOrderHistory(userId);
    }

    private String extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId().toString();
    }
}
