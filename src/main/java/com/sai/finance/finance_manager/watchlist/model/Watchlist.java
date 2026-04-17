package com.sai.finance.finance_manager.watchlist.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "watchlist")
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    private String symbol;

    private Double addedPrice;

    private Instant addedAt;

    // getters and setters
}
