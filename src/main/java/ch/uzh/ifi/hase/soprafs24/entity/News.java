package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "NEWS_ARTICLE")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, unique = true, length = 1024)
    private String url;

    @Lob
    @Column(nullable = false)
    private String summary;

    @Column(length = 1024)
    private String bannerImage;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String sourceDomain;

    @Column(nullable = false)
    private LocalDateTime publishedTime;

    @Column
    private Double overallSentimentScore;

    @Column
    private String overallSentimentLabel;

    @Lob
    @Column
    private String apiTickerSentimentJson;

    @Lob
    @Column
    private String apiTopicRelevanceJson;

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

    public String getApiTickerSentimentJson() {
        return apiTickerSentimentJson;
    }

    public void setApiTickerSentimentJson(String apiTickerSentimentJson) {
        this.apiTickerSentimentJson = apiTickerSentimentJson;
    }

    public String getApiTopicRelevanceJson() {
        return apiTopicRelevanceJson;
    }

    public void setApiTopicRelevanceJson(String apiTopicRelevanceJson) {
        this.apiTopicRelevanceJson = apiTopicRelevanceJson;
    }
}