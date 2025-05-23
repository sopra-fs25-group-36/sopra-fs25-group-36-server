package ch.uzh.ifi.hase.soprafs24.service.dto.alphavantage;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AlphaVantageNewsApiPojos {

    public static class AlphaVantageNewsResponse {
        @JsonProperty("feed")
        public List<FeedItem> feed;

        @JsonProperty("items")
        public String items;

        @JsonProperty("sentiment_score_definition")
        public String sentimentScoreDefinition;

        @JsonProperty("relevance_score_definition")
        public String relevanceScoreDefinition;

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
        public String timePublished;

        @JsonProperty("authors")
        public List<String> authors;

        @JsonProperty("summary")
        public String summary;

        @JsonProperty("banner_image")
        public String bannerImage;

        @JsonProperty("source")
        public String source;

        @JsonProperty("category_within_source")
        public String categoryWithinSource;

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
        public String relevanceScore;
    }

    public static class TickerSentimentPojo {
        @JsonProperty("ticker")
        public String ticker;

        @JsonProperty("relevance_score")
        public String relevanceScore;

        @JsonProperty("ticker_sentiment_score")
        public String tickerSentimentScore;

        @JsonProperty("ticker_sentiment_label")
        public String tickerSentimentLabel;
    }
}