package com.sai.finance.finance_manager.marketdata.model;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SymbolState {

    private double lastRealPrice;
    private double lastTickPrice;
    private long lastRealUpdateTime;

    // Intraday metrics
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume; // simulated or 0

    // 52-week metrics (optional, simulated or fetched once per day)
    private double fiftyTwoWeekHigh;
    private double fiftyTwoWeekLow;

    // For daily reset
    private LocalDate lastResetDate = LocalDate.now();
}
