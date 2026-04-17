package com.sai.finance.finance_manager.watchlist.controller;

import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.repository.UserRepository;
import com.sai.finance.finance_manager.service.JwtService;
import com.sai.finance.finance_manager.watchlist.dto.WatchlistItemDto;
import com.sai.finance.finance_manager.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final WatchlistService watchlistService;

    public WatchlistController(JwtService jwtService,
                               UserRepository userRepository,
                               WatchlistService watchlistService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.watchlistService = watchlistService;
    }

    private String getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId().toString();
    }

    @PostMapping("/add")
    public void add(@RequestHeader("Authorization") String authHeader,
                    @RequestParam String symbol) {
        String userId = getUserIdFromToken(authHeader);
        watchlistService.addToWatchlist(userId, symbol);
    }

    @DeleteMapping("/remove")
    public void remove(@RequestHeader("Authorization") String authHeader,
                       @RequestParam String symbol) {
        String userId = getUserIdFromToken(authHeader);
        watchlistService.removeFromWatchlist(userId, symbol);
    }

    @GetMapping
    public List<WatchlistItemDto> getWatchlist(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(name = "sort", defaultValue = "none") String sort
    ) {
        String userId = getUserIdFromToken(authHeader);
        return watchlistService.getWatchlist(userId, sort);
    }


    @GetMapping("/status")
    public boolean status(@RequestHeader("Authorization") String authHeader,
                          @RequestParam String symbol) {
        String userId = getUserIdFromToken(authHeader);
        return watchlistService.isInWatchlist(userId, symbol);
    }
}


/**
 * GET /watchlist?sort=ltp
 * GET /watchlist?sort=percent
 * GET /watchlist?sort=gain
 * GET /watchlist?sort=name
 * GET /watchlist?sort=added
 */
