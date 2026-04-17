package com.sai.finance.finance_manager.portfolio.service;

import com.sai.finance.finance_manager.holdings.dto.HoldingItemDto;
import com.sai.finance.finance_manager.holdings.service.HoldingsService;
import com.sai.finance.finance_manager.explore.dto.ChartPointDto;
import com.sai.finance.finance_manager.marketdata.service.MarketStatusService;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PortfolioTickEngine {

    private final HoldingsService holdingsService;
    private final MarketStatusService marketStatusService;

    // userId → 1D portfolio graph
    private final Map<String, List<ChartPointDto>> portfolioGraph1D = new ConcurrentHashMap<>();

    private LocalDate lastTradingDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    // Called by controller when user opens portfolio screen
    public List<ChartPointDto> getPortfolioHistory(String userId) {
        portfolioGraph1D.computeIfAbsent(userId, this::initGraphForUser);
        return portfolioGraph1D.get(userId);
    }

    private List<ChartPointDto> initGraphForUser(String userId) {
        double currentValue = computeCurrentPortfolioValue(userId);
        long now = System.currentTimeMillis();

        List<ChartPointDto> list = new ArrayList<>();
        list.add(new ChartPointDto(now, currentValue));
        return list;
    }

    // 🔁 Runs every 1 second — synthetic portfolio ticks
    @Scheduled(fixedRate = 1000)
    public void generatePortfolioTicks() {

        // 1️⃣ Only during market hours
        if (!marketStatusService.isMarketOpenNow()) {
            return;
        }

        // 2️⃣ Reset graph on new trading day
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        if (!today.equals(lastTradingDate)) {
            portfolioGraph1D.clear();
            lastTradingDate = today;
            return;
        }

        // 3️⃣ Update portfolio graph for each active user
        for (String userId : portfolioGraph1D.keySet()) {

            double currentValue = computeCurrentPortfolioValue(userId);
            long now = System.currentTimeMillis();

            List<ChartPointDto> graph = portfolioGraph1D.get(userId);
            if (graph == null) continue;

            // If user has no holdings → flat 0 graph
            if (currentValue == 0) {
                graph.clear();
                graph.add(new ChartPointDto(now, 0));
                continue;
            }

            // Append synthetic tick
            graph.add(new ChartPointDto(now, currentValue));

            // Keep last 300 points
            if (graph.size() > 300) {
                graph.remove(0);
            }
        }
    }

    private double computeCurrentPortfolioValue(String userId) {
        List<HoldingItemDto> holdings = holdingsService.getHoldings(userId, "none");

        double total = 0.0;
        for (HoldingItemDto h : holdings) {
            total += h.getQuantity() * h.getLtp();  // uses synthetic tickPrice
        }
        return total;
    }
}
