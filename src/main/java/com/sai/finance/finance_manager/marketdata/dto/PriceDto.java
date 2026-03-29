package com.sai.finance.finance_manager.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceDto {
    private String symbol;
    private double currentPrice;
    private double previousClose;
    private double change;
    private double changePercent;
}