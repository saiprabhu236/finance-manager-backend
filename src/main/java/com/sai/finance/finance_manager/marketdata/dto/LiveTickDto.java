package com.sai.finance.finance_manager.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LiveTickDto {
    private String symbol;
    private double lastTradedPrice;
    private double bidPrice;
    private double askPrice;
    private long volume;
    private long timestamp;
}