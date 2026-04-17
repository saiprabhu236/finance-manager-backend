package com.sai.finance.finance_manager.watchlist.repository;

import com.sai.finance.finance_manager.watchlist.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, String> {

    List<Watchlist> findByUserId(String userId);

    Optional<Watchlist> findByUserIdAndSymbol(String userId, String symbol);

    void deleteByUserIdAndSymbol(String userId, String symbol);
}
