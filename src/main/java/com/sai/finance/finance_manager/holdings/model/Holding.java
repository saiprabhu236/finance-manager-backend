package com.sai.finance.finance_manager.holdings.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "holdings")
@Data
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String symbol;

    private double quantity;
    private double avgPrice;

    private Instant addedAt;
}
