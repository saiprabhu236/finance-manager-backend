package com.sai.finance.finance_manager.holdings.service;

import com.sai.finance.finance_manager.holdings.dto.HoldingItemDto;
import com.sai.finance.finance_manager.holdings.model.Holding;
import com.sai.finance.finance_manager.holdings.repository.HoldingsRepository;
import com.sai.finance.finance_manager.marketdata.dto.CandleDto;
import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.service.MarketDataService;
import com.sai.finance.finance_manager.marketdata.service.NseSymbolStore;
import com.sai.finance.finance_manager.watchlist.dto.SparkPointDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HoldingsService {

    private final HoldingsRepository holdingsRepository;
    private final MarketDataService marketDataService;
    private final NseSymbolStore nseSymbolStore;

    // ============================================================
    // INTERNAL METHODS (CALLED BY ORDERS MODULE ONLY)
    // ============================================================

    @Transactional
    public void addOrUpdateHolding(String userId, String symbol, double buyQty, double buyPrice) {

        Holding holding = holdingsRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseGet(() -> {
                    Holding h = new Holding();
                    h.setUserId(userId);
                    h.setSymbol(symbol);
                    h.setAddedAt(Instant.now());
                    h.setQuantity(0);
                    h.setAvgPrice(0);
                    return h;
                });

        double existingQty = holding.getQuantity();
        double existingAvg = holding.getAvgPrice();

        double newTotalQty = existingQty + buyQty;
        double newTotalCost = existingQty * existingAvg + buyQty * buyPrice;
        double newAvgPrice = newTotalQty == 0 ? 0 : newTotalCost / newTotalQty;

        holding.setQuantity(newTotalQty);
        holding.setAvgPrice(newAvgPrice);

        holdingsRepository.save(holding);
    }

    @Transactional
    public void reduceOrRemoveHolding(String userId, String symbol, double sellQty) {

        Optional<Holding> opt = holdingsRepository.findByUserIdAndSymbol(userId, symbol);
        if (opt.isEmpty()) return;

        Holding holding = opt.get();
        double newQty = holding.getQuantity() - sellQty;

        if (newQty <= 0) {
            holdingsRepository.delete(holding);
        } else {
            holding.setQuantity(newQty);
            holdingsRepository.save(holding);
        }
    }

    // ============================================================
    // PUBLIC METHODS (FRONTEND READ-ONLY)
    // ============================================================

    @Transactional(readOnly = true)
    public List<HoldingItemDto> getHoldings(String userId, String sort) {

        List<HoldingItemDto> list = holdingsRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return applySorting(list, sort);
    }

    @Transactional(readOnly = true)
    public boolean isInHoldings(String userId, String symbol) {
        return holdingsRepository.findByUserIdAndSymbol(userId, symbol).isPresent();
    }

    // ============================================================
    // DTO MAPPING (REUSES WATCHLIST SPARKLINE LOGIC)
    // ============================================================

    private HoldingItemDto toDto(Holding holding) {

        String symbol = holding.getSymbol();

        PriceDto price = marketDataService.getCurrentPrice(symbol);
        List<CandleDto> candles = marketDataService.getHistoricalData(symbol, "1d");

        double ltp = price.getCurrentPrice();
        double quantity = holding.getQuantity();
        double avgPrice = holding.getAvgPrice();

        double totalInvestment = quantity * avgPrice;
        double currentValue = quantity * ltp;
        double pnl = currentValue - totalInvestment;
        double pnlPercent = totalInvestment == 0 ? 0 : (pnl / totalInvestment) * 100.0;

        String name = nseSymbolStore.getAll()
                .stream()
                .filter(s -> s.symbol().equalsIgnoreCase(symbol))
                .map(s -> s.name())
                .findFirst()
                .orElse(symbol);

        HoldingItemDto dto = new HoldingItemDto();
        dto.setSymbol(symbol);
        dto.setName(name);

        dto.setQuantity(quantity);
        dto.setAvgPrice(avgPrice);

        dto.setLtp(ltp);
        dto.setTotalInvestment(totalInvestment);
        dto.setCurrentValue(currentValue);

        dto.setPnl(pnl);
        dto.setPnlPercent(pnlPercent);

        // ⭐ EXACT SAME SPARKLINE LOGIC AS WATCHLIST
        dto.setSparkline(
                candles.stream()
                        .map(c -> new SparkPointDto(c.getTimestamp(), c.getClose()))
                        .collect(Collectors.toList())
        );

        return dto;
    }

    // ============================================================
    // SORTING
    // ============================================================

    private List<HoldingItemDto> applySorting(List<HoldingItemDto> list, String sort) {

        if (sort == null) return list;

        switch (sort.toLowerCase()) {

            case "pnl":
                list.sort(Comparator.comparing(HoldingItemDto::getPnl).reversed());
                break;

            case "value":
                list.sort(Comparator.comparing(HoldingItemDto::getCurrentValue).reversed());
                break;

            case "name":
                list.sort(Comparator.comparing(HoldingItemDto::getName));
                break;

            default:
                // no sorting
                break;
        }

        return list;
    }

    @Transactional(readOnly = true)
    public double getHoldingQuantity(String userId, String symbol) {
        return holdingsRepository.findByUserIdAndSymbol(userId, symbol)
                .map(Holding::getQuantity)
                .orElse(0.0);
    }

    public Set<String> getAllOwnedSymbols() {
        return holdingsRepository.findAllSymbolsOwnedByAnyUser();
    }


}
