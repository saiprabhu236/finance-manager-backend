package com.sai.finance.finance_manager.portfolio.controller;

import com.sai.finance.finance_manager.explore.dto.ChartPointDto;
import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.portfolio.dto.PortfolioDto;
import com.sai.finance.finance_manager.portfolio.service.PortfolioService;
import com.sai.finance.finance_manager.portfolio.service.PortfolioTickEngine;
import com.sai.finance.finance_manager.repository.UserRepository;
import com.sai.finance.finance_manager.service.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioTickEngine portfolioTickEngine;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @GetMapping
    public PortfolioDto getPortfolio(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return portfolioService.getPortfolio(userId);
    }

    @GetMapping("/history")
    public List<ChartPointDto> getPortfolioHistory(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return portfolioTickEngine.getPortfolioHistory(userId);
    }

    private String extractUserId(String authHeader) {
        String token = authHeader.substring(7); // remove "Bearer "
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getId().toString();
    }
}
