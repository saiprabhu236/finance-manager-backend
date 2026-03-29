package com.sai.finance.finance_manager.marketdata.controller;

import com.sai.finance.finance_manager.marketdata.dto.CandleDto;
import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.dto.SearchResultDto;
import com.sai.finance.finance_manager.marketdata.dto.StockMetricsDto;
import com.sai.finance.finance_manager.marketdata.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    // ---------------------------------------------------------
    // 1. SEARCH STOCKS
    // ---------------------------------------------------------
    @GetMapping("/search")
    public List<SearchResultDto> search(@RequestParam String query) {
        return marketDataService.searchStocks(query);
    }

    // ---------------------------------------------------------
    // 2. GET CURRENT PRICE
    // ---------------------------------------------------------
    @GetMapping("/price")
    public PriceDto getCurrentPrice(@RequestParam String symbol) {
        return marketDataService.getCurrentPrice(symbol);
    }

    // ---------------------------------------------------------
    // 3. GET HISTORICAL DATA
    // ---------------------------------------------------------
    @GetMapping("/history")
    public List<CandleDto> getHistoricalData(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1d") String period
    ) {
        return marketDataService.getHistoricalData(symbol, period);
    }

    // ---------------------------------------------------------
    // 4. GET STOCK METRICS (OHLC, 52W HIGH/LOW)
    // ---------------------------------------------------------
    @GetMapping("/metrics")
    public StockMetricsDto getStockMetrics(@RequestParam String symbol) {
        return marketDataService.getStockMetrics(symbol);
    }
    // ---------------------------------------------------------
    // 1. SUBSCRIBE TO A SYMBOL
    // ---------------------------------------------------------
    @PostMapping("/subscribe/{symbol}")
    public String subscribe(@PathVariable String symbol) {
        marketDataService.subscribe(symbol);
        return "Subscribed to " + symbol;
    }

    // ---------------------------------------------------------
    // 2. UNSUBSCRIBE FROM A SYMBOL
    // ---------------------------------------------------------
    @PostMapping("/unsubscribe/{symbol}")
    public String unsubscribe(@PathVariable String symbol) {
        marketDataService.unsubscribe(symbol);
        return "Unsubscribed from " + symbol;
    }

    // ---------------------------------------------------------
    // 3. LIST ACTIVE SUBSCRIPTIONS
    // ---------------------------------------------------------
    @GetMapping("/subscriptions")
    public Object getSubscriptions() {
        return marketDataService.getActiveSubscriptions();
    }

}