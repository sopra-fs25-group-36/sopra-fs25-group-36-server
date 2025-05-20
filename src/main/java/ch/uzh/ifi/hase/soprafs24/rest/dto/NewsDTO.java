package ch.uzh.ifi.hase.soprafs24.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
// Remove: import com.crazzyghost.alphavantage.newssentiment.TickerSentiment; // Not used
// Remove: import com.fasterxml.jackson.core.type.TypeReference; // Not used here
// Remove: import com.fasterxml.jackson.databind.ObjectMapper; // Not used here
// Remove: import java.io.IOException; // Not used here
import java.time.LocalDateTime;
// Remove: import java.util.Collections; // Not used here
import java.util.List;
import java.util.Map;
// Remove: import java.util.stream.Collectors; // Not used here

public class NewsDTO {

    private Long id;
    private String title;
    private String url;
    private String summary;
    private String bannerImage;
    private String source;
    private String sourceDomain;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedTime;
    private Double overallSentimentScore;
    private String overallSentimentLabel;
    private List<Map<String, Object>> tickerSentiments; // Simplified for DTO

    // Getters and Setters (no changes)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceDomain() {
        return sourceDomain;
    }

    public void setSourceDomain(String sourceDomain) {
        this.sourceDomain = sourceDomain;
    }

    public LocalDateTime getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(LocalDateTime publishedTime) {
        this.publishedTime = publishedTime;
    }

    public Double getOverallSentimentScore() {
        return overallSentimentScore;
    }

    public void setOverallSentimentScore(Double overallSentimentScore) {
        this.overallSentimentScore = overallSentimentScore;
    }

    public String getOverallSentimentLabel() {
        return overallSentimentLabel;
    }

    public void setOverallSentimentLabel(String overallSentimentLabel) {
        this.overallSentimentLabel = overallSentimentLabel;
    }

    public List<Map<String, Object>> getTickerSentiments() {
        return tickerSentiments;
    }

    public void setTickerSentiments(List<Map<String, Object>> tickerSentiments) {
        this.tickerSentiments = tickerSentiments;
    }

    // Removed static parseTickerSentiments method as NewsService's convertToDTO now
    // handles this.
}