package com.sai.finance.finance_manager.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockMetricsDto {
    private String symbol;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private double fiftyTwoWeekHigh;
    private double fiftyTwoWeekLow;
}