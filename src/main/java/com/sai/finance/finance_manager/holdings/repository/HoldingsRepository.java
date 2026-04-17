package com.sai.finance.finance_manager.holdings.repository;

import com.sai.finance.finance_manager.holdings.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingsRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByUserId(String userId);

    Optional<Holding> findByUserIdAndSymbol(String userId, String symbol);

    void deleteByUserIdAndSymbol(String userId, String symbol);
}
