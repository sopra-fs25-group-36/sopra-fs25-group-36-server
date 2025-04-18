package ch.uzh.ifi.hase.soprafs24.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crazzyghost.alphavantage.AlphaVantage;
import com.crazzyghost.alphavantage.Config;
import com.crazzyghost.alphavantage.parameters.OutputSize;
import com.crazzyghost.alphavantage.timeseries.response.StockUnit;
import com.crazzyghost.alphavantage.timeseries.response.TimeSeriesResponse;

import ch.uzh.ifi.hase.soprafs24.entity.StockDataPoint;
import ch.uzh.ifi.hase.soprafs24.repository.StockDataPointRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StockDataPointDTO;

@Service
@Transactional 
public class ChartDataService {

    private final Logger log = LoggerFactory.getLogger(ChartDataService.class);
    private final StockDataPointRepository stockDataPointRepository;
    private final String apiKey;


    private static final List<String> CHART_SYMBOLS = List.of(
            "AAPL", "TSLA", "AMZN", "MSFT", "NVDA", "GOOG", "INTC", "NFLX", "AMD", 
            "XOM", "CVX",       
            "JPM", "GS",        
            "PFE", "JNJ",       
            "PG"               

    );

    @Autowired
    public ChartDataService(@Qualifier("stockDataPointRepository") StockDataPointRepository stockDataPointRepository,
                             @Value("${ALPHAVANTAGE_API_KEY}") String apiKey) {
        this.stockDataPointRepository = stockDataPointRepository;
        this.apiKey = apiKey;
    }
    public void fetchAndStoreDailyDataForAllSymbols() {
        log.info("Starting daily data fetch for {} symbols...", CHART_SYMBOLS.size());

        Config cfg = Config.builder()
                .key(apiKey)
                .timeOut(10)
                .build();
        AlphaVantage.api().init(cfg);

        int symbolCount = 0;
        for (String symbol : CHART_SYMBOLS) {
            symbolCount++;
            log.info("Fetching daily data for symbol: {} ({}/{})", symbol, symbolCount, CHART_SYMBOLS.size());
            try {
                TimeSeriesResponse response = AlphaVantage.api()
                        .timeSeries()
                        .daily()
                        .forSymbol(symbol)
                        .outputSize(OutputSize.FULL) // Or OutputSize.COMPACT for only the last 100 days
                        .fetchSync(); 

                if (response.getErrorMessage() != null) {
                    log.error("Alpha Vantage API error for symbol {}: {}", symbol, response.getErrorMessage());
                    addApiDelay(symbolCount, CHART_SYMBOLS.size());
                    continue; 
                }

                List<StockUnit> stockUnits = response.getStockUnits();
                if (stockUnits == null || stockUnits.isEmpty()) {
                    log.warn("No stock units returned for symbol: {}", symbol);
                    addApiDelay(symbolCount, CHART_SYMBOLS.size());
                    continue;
                }

                int newRecords = 0;
                for (StockUnit unit : stockUnits) {
                    LocalDate date = LocalDate.parse(unit.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);

                    if (!stockDataPointRepository.existsBySymbolAndDate(symbol, date)) {
                        StockDataPoint dataPoint = new StockDataPoint();
                        dataPoint.setSymbol(symbol);
                        dataPoint.setDate(date);
                        dataPoint.setOpen(unit.getOpen());
                        dataPoint.setHigh(unit.getHigh());
                        dataPoint.setLow(unit.getLow());
                        dataPoint.setClose(unit.getClose());
                        dataPoint.setVolume(unit.getVolume());

                        stockDataPointRepository.save(dataPoint);
                        newRecords++;
                    }
                }
                log.info("Saved {} new daily records for symbol: {}", newRecords, symbol);

               addApiDelay(symbolCount, CHART_SYMBOLS.size());


            } catch (Exception e) { 
                log.error("Failed to fetch or process data for symbol {}: {}", symbol, e.getMessage(), e);
                addApiDelay(symbolCount, CHART_SYMBOLS.size());
            }
        }
        log.info("Finished daily data fetch.");
    }

    private void addApiDelay(int currentSymbolIndex, int totalSymbols) {
         if (currentSymbolIndex < totalSymbols) {
             try {
                 long delaySeconds = 15;
                 log.debug("Waiting {} seconds before next API call...", delaySeconds);
                 TimeUnit.SECONDS.sleep(delaySeconds);
             } catch (InterruptedException ie) {
                 log.warn("API delay interrupted.", ie);
                 Thread.currentThread().interrupt();
             }
         }
    }


    /**
     * Retrieves stored daily data points for a specific symbol.
     *
     * @param symbol The stock symbol.
     * @return A list of DTOs suitable for charting.
     */
    public List<StockDataPointDTO> getDailyChartData(String symbol) {
        log.debug("Retrieving daily chart data for symbol: {}", symbol);
        List<StockDataPoint> dataPoints = stockDataPointRepository.findBySymbolOrderByDateAsc(symbol);
        return dataPoints.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private StockDataPointDTO convertToDto(StockDataPoint entity) {
        return new StockDataPointDTO(
                entity.getSymbol(),
                entity.getDate(),
                entity.getOpen(), 
                entity.getHigh(), 
                entity.getLow(),  
                entity.getClose(),
                entity.getVolume()
        );
    }
}