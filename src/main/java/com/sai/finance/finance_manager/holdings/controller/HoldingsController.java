package com.sai.finance.finance_manager.holdings.controller;

import com.sai.finance.finance_manager.holdings.dto.HoldingItemDto;
import com.sai.finance.finance_manager.holdings.service.HoldingsService;
import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.repository.UserRepository;
import com.sai.finance.finance_manager.service.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/holdings")
@RequiredArgsConstructor
public class HoldingsController {

    private final HoldingsService holdingsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping
    public List<HoldingItemDto> getHoldings(@RequestHeader("Authorization") String authHeader,
                                            @RequestParam(defaultValue = "none") String sort) {

        String userId = extractUserId(authHeader);
        return holdingsService.getHoldings(userId, sort);
    }

    @GetMapping("/status")
    public boolean isInHoldings(@RequestHeader("Authorization") String authHeader,
                                @RequestParam String symbol) {

        String userId = extractUserId(authHeader);
        return holdingsService.isInHoldings(userId, symbol);
    }

    // ⭐ EXACT SAME LOGIC AS WATCHLIST CONTROLLER
    private String extractUserId(String authHeader) {

        String token = authHeader.substring(7); // remove "Bearer "
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId().toString();
    }
}

