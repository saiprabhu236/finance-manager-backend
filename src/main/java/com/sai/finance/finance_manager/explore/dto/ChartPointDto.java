package com.sai.finance.finance_manager.explore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartPointDto {
    private long timestamp;   // epoch millis
    private double value;     // normalized index value
}
