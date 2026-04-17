package com.sai.finance.finance_manager.portfolio.service;

import com.sai.finance.finance_manager.holdings.dto.HoldingItemDto;
import com.sai.finance.finance_manager.holdings.service.HoldingsService;
import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.service.MarketDataService;
import com.sai.finance.finance_manager.portfolio.dto.PortfolioDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingsService holdingsService;
    private final MarketDataService marketDataService;

    @Transactional(readOnly = true)
    public PortfolioDto getPortfolio(String userId) {

        // 1️⃣ Fetch holdings
        List<HoldingItemDto> holdings = holdingsService.getHoldings(userId, "none");

        // If no holdings → return zeroed portfolio
        if (holdings.isEmpty()) {
            PortfolioDto dto = new PortfolioDto();
            dto.setTotalInvestment(0);
            dto.setCurrentValue(0);
            dto.setTotalPnL(0);
            dto.setTotalPnLPercent(0);
            dto.setTodayPnL(0);
            dto.setTodayPnLPercent(0);
            dto.setHoldings(List.of());
            return dto;
        }

        double totalInvestment = 0;
        double currentValue = 0;
        double todayPnL = 0;

        // 2️⃣ Compute totals + todayPnL
        for (HoldingItemDto h : holdings) {

            double qty = h.getQuantity();
            double avg = h.getAvgPrice();
            double ltp = h.getLtp();

            totalInvestment += qty * avg;
            currentValue += qty * ltp;

            // Fetch yesterday close for today PnL
            PriceDto price = marketDataService.getCurrentPrice(h.getSymbol());
            double yesterdayClose = price.getPreviousClose();

            todayPnL += qty * (ltp - yesterdayClose);
        }

        double totalPnL = currentValue - totalInvestment;
        double totalPnLPercent = totalInvestment == 0 ? 0 : (totalPnL / totalInvestment) * 100.0;
        double todayPnLPercent = totalInvestment == 0 ? 0 : (todayPnL / totalInvestment) * 100.0;

        // 3️⃣ Build final DTO
        PortfolioDto dto = new PortfolioDto();
        dto.setTotalInvestment(totalInvestment);
        dto.setCurrentValue(currentValue);

        dto.setTotalPnL(totalPnL);
        dto.setTotalPnLPercent(totalPnLPercent);

        dto.setTodayPnL(todayPnL);
        dto.setTodayPnLPercent(todayPnLPercent);

        dto.setHoldings(holdings);

        return dto;
    }
}
