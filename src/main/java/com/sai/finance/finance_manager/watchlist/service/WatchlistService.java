package com.sai.finance.finance_manager.watchlist.service;

import com.sai.finance.finance_manager.marketdata.dto.CandleDto;
import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.model.NseSymbol;
import com.sai.finance.finance_manager.marketdata.service.MarketDataService;
import com.sai.finance.finance_manager.marketdata.service.NseSymbolStore;
import com.sai.finance.finance_manager.watchlist.dto.SparkPointDto;
import com.sai.finance.finance_manager.watchlist.dto.WatchlistItemDto;
import com.sai.finance.finance_manager.watchlist.model.Watchlist;
import com.sai.finance.finance_manager.watchlist.repository.WatchlistRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final MarketDataService marketDataService;
    private final NseSymbolStore nseSymbolStore;

    @Transactional
    public void addToWatchlist(String userId, String symbol) {

        boolean exists = watchlistRepository
                .findByUserIdAndSymbol(userId, symbol)
                .isPresent();

        if (exists) return;

        PriceDto snapshot = marketDataService.getCurrentPrice(symbol);

        Watchlist entry = new Watchlist();
        entry.setUserId(userId);
        entry.setSymbol(symbol);
        entry.setAddedPrice(snapshot.getCurrentPrice());
        entry.setAddedAt(Instant.now());

        watchlistRepository.save(entry);
    }

    @Transactional
    public void removeFromWatchlist(String userId, String symbol) {
        watchlistRepository.deleteByUserIdAndSymbol(userId, symbol);
    }

    // ⭐ UPDATED METHOD — now supports sorting
    @Transactional(readOnly = true)
    public List<WatchlistItemDto> getWatchlist(String userId, String sort) {

        List<WatchlistItemDto> list = watchlistRepository.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        switch (sort.toLowerCase()) {

            case "ltp":
                list.sort(Comparator.comparing(WatchlistItemDto::getLtp).reversed());
                break;

            case "percent":
                list.sort(Comparator.comparing(WatchlistItemDto::getPercentToday).reversed());
                break;

            case "gain":
                list.sort(Comparator.comparing(WatchlistItemDto::getGainSinceAdded).reversed());
                break;

            case "name":
                list.sort(Comparator.comparing(WatchlistItemDto::getName));
                break;

            case "added":
                list.sort(Comparator.comparing(WatchlistItemDto::getAddedPrice));
                break;

            default:
                // no sorting
                break;
        }

        return list;
    }

    private WatchlistItemDto toDto(Watchlist entry) {

        String symbol = entry.getSymbol();

        PriceDto price = marketDataService.getCurrentPrice(symbol);
        List<CandleDto> candles = marketDataService.getHistoricalData(symbol, "1d");

        double currentPrice = price.getCurrentPrice();
        double movementSinceAdded = currentPrice - entry.getAddedPrice();

        String name = nseSymbolStore.getAll()
                .stream()
                .filter(s -> s.symbol().equalsIgnoreCase(symbol))
                .map(NseSymbol::name)
                .findFirst()
                .orElse(symbol);

        WatchlistItemDto dto = new WatchlistItemDto();
        dto.setSymbol(symbol);
        dto.setName(name);
        dto.setLtp(currentPrice);
        dto.setChangeToday(price.getChange());
        dto.setPercentToday(price.getChangePercent());
        dto.setAddedPrice(entry.getAddedPrice());
        dto.setGainSinceAdded(movementSinceAdded);

        dto.setSparkline(
                candles.stream()
                        .map(c -> new SparkPointDto(c.getTimestamp(), c.getClose()))
                        .collect(Collectors.toList())
        );

        return dto;
    }

    public boolean isInWatchlist(String userId, String symbol) {
        return watchlistRepository.findByUserIdAndSymbol(userId, symbol).isPresent();
    }
}
