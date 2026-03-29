package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataPollingService {

    private final MarketDataSubscriptionManager subscriptionManager;
    private final YahooFinanceClient yahooClient;
    private final SnapshotService snapshotService;

    // Stores the latest REAL delayed price from Yahoo (anchor for ticks)
    private final Map<String, Double> basePrices = new ConcurrentHashMap<>();

    /**
     * Poll Yahoo Finance every 30 seconds to refresh REAL delayed prices.
     * These are used as the anchor for synthetic ticks AND to update metrics.
     */
    @Scheduled(fixedRate = 30000)
    public void pollBasePrices() {
        try {
            Set<String> symbols = subscriptionManager.getSubscribedSymbols();

            if (symbols.isEmpty()) {
                return;
            }

            for (String rawSymbol : symbols) {

                // Normalize before polling
                String symbol = subscriptionManager.normalizeSymbol(rawSymbol);

                // Fetch REAL delayed price from Yahoo
                PriceDto price = yahooClient.getCurrentPrice(symbol);

                if (price == null) {
                    log.warn("No price data for {}", symbol);
                    continue;
                }

                double realPrice = price.getCurrentPrice();

                // 1️⃣ Update tick base (used by TickEngine)
                basePrices.put(symbol, realPrice);

                // 2️⃣ Update intraday metrics + real price
                snapshotService.updateRealPrice(symbol, realPrice);
            }

            // 3️⃣ Cleanup unsubscribed symbols
            snapshotService.cleanupUnsubscribed();

        } catch (Exception e) {
            log.error("Error during base price polling", e);
        }
    }

    /**
     * Used by TickBroadcaster to generate synthetic ticks.
     */
    public double getBasePrice(String symbol) {
        return basePrices.getOrDefault(symbol, 0.0);
    }
}