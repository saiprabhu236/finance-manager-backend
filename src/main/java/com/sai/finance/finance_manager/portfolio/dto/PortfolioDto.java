package com.sai.finance.finance_manager.portfolio.dto;

import com.sai.finance.finance_manager.holdings.dto.HoldingItemDto;
import lombok.Data;

import java.util.List;

@Data
public class PortfolioDto {

    private double totalInvestment;
    private double currentValue;

    private double totalPnL;
    private double totalPnLPercent;

    private double todayPnL;
    private double todayPnLPercent;

    private List<HoldingItemDto> holdings;
}
