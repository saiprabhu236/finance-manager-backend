package com.sai.finance.finance_manager.holdings.repository;

import com.sai.finance.finance_manager.holdings.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface HoldingsRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByUserId(String userId);

    Optional<Holding> findByUserIdAndSymbol(String userId, String symbol);
    @Query("SELECT DISTINCT h.symbol FROM Holding h")
    Set<String> findAllSymbolsOwnedByAnyUser();


    void deleteByUserIdAndSymbol(String userId, String symbol);
}
