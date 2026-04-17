package com.sai.finance.finance_manager.watchlist.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
public class WatchlistItemDto {

    private String symbol;
    private String name;

    private double ltp;
    private double changeToday;
    private double percentToday;

    private double addedPrice;
    private double gainSinceAdded;

    private List<SparkPointDto> sparkline;

    // getters and setters
}
