package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.holdings.service.HoldingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSubscriptionLoader {

    private final HoldingsService holdingsService;
    private final MarketDataSubscriptionManager subscriptionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void loadSubscriptionsOnStartup() {

        log.info("Loading subscriptions from holdings...");

        // Get all symbols owned by all users
        Set<String> ownedSymbols = holdingsService.getAllOwnedSymbols();

        for (String symbol : ownedSymbols) {
            subscriptionManager.subscribe(symbol);
            log.info("Auto-subscribed on startup: {}", symbol);
        }

        log.info("Startup subscription load complete. Total symbols: {}", ownedSymbols.size());
    }
}
