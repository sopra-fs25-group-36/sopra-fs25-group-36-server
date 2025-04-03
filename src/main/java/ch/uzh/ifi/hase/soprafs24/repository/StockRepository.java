package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // All stock records for a given symbol
    List<Stock> findBySymbol(String symbol);

    // A specific day's record for a stock symbol
    List<Stock> findBySymbolAndDate(String symbol, LocalDate date);

    // Records for all stocks on a given date
    List<Stock> findAllByDate(LocalDate date);

    // Get all prices for a symbol in a time range
    List<Stock> findBySymbolAndDateBetween(String symbol, LocalDate start, LocalDate end);

    // Method to load random stock, but only dates with 10 days of consecutive day data after it
    @Query(value = """
    SELECT date
    FROM (
        SELECT DISTINCT date
        FROM stock
        WHERE date <= (SELECT MAX(date) - INTERVAL '9 days' FROM stock)
    ) AS valid_dates
    ORDER BY RANDOM()
    LIMIT 1
""", nativeQuery = true)
    LocalDate findRandomStartDateWith10Days();
    @Query(value = """
    SELECT * FROM stock 
    WHERE date IN (
        SELECT DISTINCT date 
        FROM stock 
        WHERE date >= :startDate 
        ORDER BY date ASC 
        LIMIT 10
    )
    ORDER BY date ASC
""", nativeQuery = true)
    List<Stock> findStocksForTenDays(@Param("startDate") LocalDate startDate);


    @Query(value = "SELECT * FROM stock WHERE symbol = :symbol ORDER BY date LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Stock> findBySymbolWithLimitOffset(@Param("symbol") String symbol, @Param("offset") int offset,
            @Param("limit") int limit);
}
