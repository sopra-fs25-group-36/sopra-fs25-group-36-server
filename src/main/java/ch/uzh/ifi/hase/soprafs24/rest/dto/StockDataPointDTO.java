package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDate;

public class StockDataPointDTO {

    private String symbol;
    private LocalDate date;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Long volume;

    public StockDataPointDTO() {}

    public StockDataPointDTO(String symbol, LocalDate date, Double open, Double high, Double low, Double close, Long volume) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getHigh() {
        return high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Double getLow() {
        return low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }
}