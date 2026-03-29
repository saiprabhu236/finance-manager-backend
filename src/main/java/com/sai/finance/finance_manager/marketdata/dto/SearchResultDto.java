package com.sai.finance.finance_manager.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDto {
    private String symbol;
    private String name;
    private String exchange;
    private String type;

    public SearchResultDto(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
        this.exchange = "NSE";
        this.type = "EQUITY";
    }
}