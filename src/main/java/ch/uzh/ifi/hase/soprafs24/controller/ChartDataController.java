package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.rest.dto.StockDataPointDTO;
import ch.uzh.ifi.hase.soprafs24.service.ChartDataService;

@RestController
@RequestMapping("/api/charts") 
public class ChartDataController {

    private final ChartDataService chartDataService;

    @Autowired
    public ChartDataController(ChartDataService chartDataService) {
        this.chartDataService = chartDataService;
    }


    @PostMapping("/fetch-daily")
    @ResponseStatus(HttpStatus.ACCEPTED) 
    public ResponseEntity<String> triggerDailyDataFetch() {

        try {
             chartDataService.fetchAndStoreDailyDataForAllSymbols();
             return ResponseEntity.accepted().body("Daily chart data fetching process started successfully.");
        } catch (Exception e) {

             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body("Failed to start data fetching: " + e.getMessage());
        }
    }


    @GetMapping("/{symbol}/daily")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<StockDataPointDTO>> getDailyChartData(@PathVariable String symbol) {
        List<StockDataPointDTO> chartData = chartDataService.getDailyChartData(symbol.toUpperCase()); 
        if (chartData.isEmpty()) {

             return ResponseEntity.ok(chartData);

        }
        return ResponseEntity.ok(chartData);
    }
}