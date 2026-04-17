package com.sai.finance.finance_manager.holdings.dto;

import lombok.Data;
import java.util.List;
import com.sai.finance.finance_manager.watchlist.dto.SparkPointDto;

@Data
public class HoldingItemDto {

    private String symbol;
    private String name;

    private double quantity;
    private double avgPrice;

    private double ltp;
    private double totalInvestment;
    private double currentValue;

    private double pnl;
    private double pnlPercent;

    private List<SparkPointDto> sparkline;
}
