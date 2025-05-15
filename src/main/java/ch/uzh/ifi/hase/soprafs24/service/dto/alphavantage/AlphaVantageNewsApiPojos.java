package ch.uzh.ifi.hase.soprafs24.service.dto.alphavantage; // Suggested package

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Container for the POJOs if kept in one file, or make them top-level classes.
public class AlphaVantageNewsApiPojos {

    public static class AlphaVantageNewsResponse {
        @JsonProperty("feed")
        public List<FeedItem> feed;

        @JsonProperty("items")
        public String items; // API returns this as a string e.g., "50"

        @JsonProperty("sentiment_score_definition")
        public String sentimentScoreDefinition;

        @JsonProperty("relevance_score_definition")
        public String relevanceScoreDefinition;

        // For handling API call issues (e.g., rate limits, errors)
        @JsonProperty("Information")
        public String information;

        @JsonProperty("Error Message")
        public String errorMessage;
    }

    public static class FeedItem {
        @JsonProperty("title")
        public String title;

        @JsonProperty("url")
        public String url;

        @JsonProperty("time_published")
        public String timePublished; // Format: YYYYMMDDTHHMMSS

        @JsonProperty("authors")
        public List<String> authors;

        @JsonProperty("summary")
        public String summary;

        @JsonProperty("banner_image")
        public String bannerImage;

        @JsonProperty("source")
        public String source;

        @JsonProperty("category_within_source")
        public String categoryWithinSource; // Might be useful later

        @JsonProperty("source_domain")
        public String sourceDomain;

        @JsonProperty("topics")
        public List<TopicPojo> topics;

        @JsonProperty("overall_sentiment_score")
        public Double overallSentimentScore;

        @JsonProperty("overall_sentiment_label")
        public String overallSentimentLabel;

        @JsonProperty("ticker_sentiment")
        public List<TickerSentimentPojo> tickerSentiment;
    }

    public static class TopicPojo {
        @JsonProperty("topic")
        public String topic;

        @JsonProperty("relevance_score")
        public String relevanceScore; // API returns as string e.g., "0.158 topics"
    }

    public static class TickerSentimentPojo {
        @JsonProperty("ticker")
        public String ticker;

        @JsonProperty("relevance_score")
        public String relevanceScore; // API returns as string

        @JsonProperty("ticker_sentiment_score")
        public String tickerSentimentScore; // API returns as string

        @JsonProperty("ticker_sentiment_label")
        public String tickerSentimentLabel;
    }
}