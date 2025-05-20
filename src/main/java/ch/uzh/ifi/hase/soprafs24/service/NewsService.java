package ch.uzh.ifi.hase.soprafs24.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy; // For @PreDestroy

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.uzh.ifi.hase.soprafs24.entity.News;
import ch.uzh.ifi.hase.soprafs24.game.GameManager;
import ch.uzh.ifi.hase.soprafs24.game.InMemoryGameRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.NewsRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.NewsDTO;
import ch.uzh.ifi.hase.soprafs24.service.dto.alphavantage.AlphaVantageNewsApiPojos;

@Service
@Transactional
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private final NewsRepository newsRepository;
    private final String API_KEY;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService newsFetchExecutor;

    // For Premium API Key (75 calls/minute ~ 0.8s/call).
    // No artificial delay needed if API respects concurrency from premium key.
    private static final long API_CALL_DELAY_MILLISECONDS = 0; // Set to 0 for premium if no bursting issues

    private static final DateTimeFormatter AV_API_TIME_PUBLISHED_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter AV_API_TIME_PARAM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
    private static final String ALPHA_VANTAGE_BASE_URL = "https://www.alphavantage.co/query";

    public NewsService(NewsRepository newsRepository, @Value("${ALPHAVANTAGE_API_KEY}") String apiKey) {
        this.newsRepository = newsRepository;
        this.API_KEY = apiKey;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // Adjust pool size based on typical number of tickers per game and API
        // tolerance
        this.newsFetchExecutor = Executors
                .newFixedThreadPool(Math.min(10, Runtime.getRuntime().availableProcessors() * 2));
        log.info("NewsService initialized with a thread pool size of {}",
                ((java.util.concurrent.ThreadPoolExecutor) newsFetchExecutor).getCorePoolSize());

    }

    public void fetchAndSaveNewsForTickers(List<String> tickers, LocalDate gameStartDate, LocalDate gameEndDate) {
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equalsIgnoreCase("YOUR_API_KEY_HERE")
                || API_KEY.equalsIgnoreCase("demo")) {
            log.warn("Alpha Vantage API key is not configured or is a placeholder/demo. Skipping news fetch.");
            return;
        }
        if (tickers == null || tickers.isEmpty() || gameStartDate == null || gameEndDate == null) {
            log.warn("Cannot fetch news. Tickers, startDate, or endDate is null/empty.");
            return;
        }

        String firstTicker = tickers.isEmpty() ? "N/A" : tickers.get(0);
        String lastTicker = tickers.isEmpty() ? "N/A" : tickers.get(tickers.size() - 1);

        log.info("Starting ASYNCHRONOUS news fetch for {} tickers (e.g., {} to {}), from {} to {}.",
                tickers.size(), firstTicker, lastTicker, gameStartDate, gameEndDate);

        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (String ticker : tickers) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try {
                    if (API_CALL_DELAY_MILLISECONDS > 0) {
                        Thread.sleep(API_CALL_DELAY_MILLISECONDS);
                    }
                    // Pass 1 to save only the latest news article per ticker
                    return fetchAndSaveNewsForSingleTicker(ticker, gameStartDate, gameEndDate, 1);
                } catch (InterruptedException e) {
                    log.warn("News fetching task interrupted for ticker {}.", ticker, e);
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                    return 0;
                } catch (IOException e) {
                    log.error("IOException during news fetch for ticker {}: {}", ticker, e.getMessage(), e);
                    return 0;
                } catch (Exception e) { // Catch any other unexpected runtime exceptions from the async task
                    log.error("Unexpected exception during news fetch for ticker {}: {}", ticker, e.getMessage(), e);
                    return 0;
                }
            }, newsFetchExecutor);
            futures.add(future);
        }

        // Wait for all asynchronous tasks to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Exception occurred while waiting for all news fetch tasks to complete: {}", e.getMessage(), e);
        }

        int totalNewsSavedThisRun = 0;
        for (CompletableFuture<Integer> future : futures) {
            try {
                // Check if future completed normally before calling get()
                if (!future.isCompletedExceptionally() && !future.isCancelled()) {
                    totalNewsSavedThisRun += future.get();
                } else if (future.isCancelled()) {
                    log.warn("A news fetch task was cancelled.");
                }
            } catch (Exception e) { // Catches ExecutionException or InterruptedException from future.get()
                log.error("Error retrieving result from an asynchronous news fetch task: {}", e.getMessage(), e);
            }
        }

        log.info("Completed ASYNCHRONOUS news fetch run. Total new articles saved in this run: {}",
                totalNewsSavedThisRun);
    }

    // Modified to accept and use numberOfArticlesToSave
    private int fetchAndSaveNewsForSingleTicker(String ticker, LocalDate gameStartDate, LocalDate gameEndDate,
            int numberOfArticlesToSave) throws IOException, InterruptedException {
        String timeFrom = gameStartDate.atStartOfDay().format(AV_API_TIME_PARAM_FORMAT);
        String timeTo = gameEndDate.atTime(23, 59).format(AV_API_TIME_PARAM_FORMAT);

        // Request a slightly larger limit from API than strictly needed, in case the
        // very first few are filtered out
        // or to give some buffer. The API might still send its default (e.g., 50) if it
        // ignores small limits.
        // Our code will then pick the top 'numberOfArticlesToSave' from what's
        // received.
        int requestLimit = Math.max(numberOfArticlesToSave, 5);
        // If you are absolutely sure API respects limit=1 and you only want 1, you can
        // set requestLimit = numberOfArticlesToSave

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_BASE_URL)
                .queryParam("function", "NEWS_SENTIMENT")
                .queryParam("apikey", API_KEY)
                .queryParam("tickers", ticker)
                .queryParam("time_from", timeFrom)
                .queryParam("time_to", timeTo)
                .queryParam("sort", "EARLIEST") // Crucial for getting the "EARLIEST" news first
                .queryParam("limit", requestLimit);

        URI uri = uriBuilder.build().toUri();
        log.debug("Constructed Alpha Vantage URL for ticker {}: {}", ticker, uri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error(
                    "Alpha Vantage News API request FAILED for ticker {}. Status: {}, URL: {}, Body (first 500 chars): {}",
                    ticker, response.statusCode(), uri,
                    response.body().substring(0, Math.min(response.body().length(), 500)));
            return 0;
        }

        String jsonResponse = response.body();
        AlphaVantageNewsApiPojos.AlphaVantageNewsResponse newsApiResponse;

        try {
            newsApiResponse = objectMapper.readValue(jsonResponse,
                    AlphaVantageNewsApiPojos.AlphaVantageNewsResponse.class);
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to parse JSON response for ticker {}. URL: {}. Response (first 500 chars): '{}'. Error: {}",
                    ticker, uri, jsonResponse.substring(0, Math.min(jsonResponse.length(), 500)), e.getMessage());
            return 0;
        }

        if (newsApiResponse.errorMessage != null && !newsApiResponse.errorMessage.isEmpty()) {
            log.error("AlphaVantage News API returned an error for ticker {}. URL: {}. Error: {}", ticker, uri,
                    newsApiResponse.errorMessage);
            return 0;
        }

        if (newsApiResponse.information != null &&
                (newsApiResponse.information.toLowerCase().contains("api call frequency") ||
                        newsApiResponse.information.toLowerCase().contains("thank you for using alpha vantage"))) {
            log.warn(
                    "AlphaVantage API message for ticker {}. URL: {}. Message: '{}'. This might indicate a rate limit or API key issue.",
                    ticker, uri, newsApiResponse.information);
            return 0;
        }

        if (newsApiResponse.feed == null || newsApiResponse.feed.isEmpty()) {
            log.info("No news items returned from AlphaVantage for ticker: {}, Date Range: {} to {}. URL: {}", ticker,
                    timeFrom, timeTo, uri);
            return 0;
        }

        log.info(
                "Received {} news items from AlphaVantage for ticker {} (requested limit: {}). Will process up to {} item(s).",
                newsApiResponse.feed.size(), ticker, requestLimit, numberOfArticlesToSave);

        int newNewsSavedForThisTicker = 0;
        for (int i = 0; i < newsApiResponse.feed.size() && i < numberOfArticlesToSave; i++) {
            AlphaVantageNewsApiPojos.FeedItem item = newsApiResponse.feed.get(i);

            if (item.url == null || item.url.isEmpty()) {
                log.warn("Skipping news item with null or empty URL for ticker {}. Title: {}", ticker, item.title);
                continue;
            }
            if (newsRepository.findByUrl(item.url).isPresent()) {
                log.trace("News item with URL {} already exists. Skipping for ticker {}.", item.url, ticker);
                continue;
            }

            News news = new News();
            news.setTitle(item.title != null ? item.title.substring(0, Math.min(item.title.length(), 510)) : "N/A");
            news.setUrl(item.url);
            news.setSummary(item.summary != null ? item.summary : "N/A");
            news.setBannerImage(item.bannerImage);
            news.setSource(item.source != null ? item.source.substring(0, Math.min(item.source.length(), 250)) : "N/A");
            news.setSourceDomain(item.sourceDomain != null
                    ? item.sourceDomain.substring(0, Math.min(item.sourceDomain.length(), 250))
                    : "N/A");

            try {
                if (item.timePublished == null || item.timePublished.isEmpty()) {
                    log.warn("Published time is null or empty for news item with URL {}. Skipping.", item.url);
                    continue;
                }
                news.setPublishedTime(LocalDateTime.parse(item.timePublished, AV_API_TIME_PUBLISHED_FORMAT));
            } catch (DateTimeParseException e) {
                log.warn(
                        "Could not parse published time string: '{}' for ticker {}. URL: {}. Skipping news item. Error: {}",
                        item.timePublished, ticker, item.url, e.getMessage());
                continue;
            }
            news.setOverallSentimentScore(item.overallSentimentScore);
            news.setOverallSentimentLabel(item.overallSentimentLabel);

            try {
                if (item.tickerSentiment != null && !item.tickerSentiment.isEmpty()) {
                    news.setApiTickerSentimentJson(objectMapper.writeValueAsString(item.tickerSentiment));
                }
                if (item.topics != null && !item.topics.isEmpty()) {
                    news.setApiTopicRelevanceJson(objectMapper.writeValueAsString(item.topics));
                }
            } catch (JsonProcessingException e) {
                log.error("Error serializing ticker/topic sentiments to JSON for URL {} (ticker {}): {}", item.url,
                        ticker, e.getMessage());
            }
            newsRepository.save(news);
            newNewsSavedForThisTicker++;
        }

        if (newNewsSavedForThisTicker > 0) {
            log.info("Successfully saved {} new news articles for ticker {}.", newNewsSavedForThisTicker, ticker);
        } else if (!newsApiResponse.feed.isEmpty() && numberOfArticlesToSave > 0) { // Check if we wanted to save but
                                                                                    // didn't
            log.info(
                    "Received news for ticker {} but did not save any (e.g., all duplicates, or issues with the first {} item(s)).",
                    ticker, numberOfArticlesToSave);
        }
        return newNewsSavedForThisTicker;
    }

    public List<NewsDTO> getNewsForGame(Long gameId) {
        GameManager gameManager = InMemoryGameRegistry.getGame(gameId);
        if (gameManager == null) {
            log.warn("Game not found with ID: {} while trying to get news.", gameId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found: " + gameId);
        }

        LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline = gameManager.getStockTimeline();
        if (stockTimeline == null || stockTimeline.isEmpty()) {
            log.warn("Game {} has a null or empty stock timeline. Cannot determine date range or tickers for news.",
                    gameId);
            return Collections.emptyList();
        }

        List<LocalDate> gameDates = new ArrayList<>(stockTimeline.keySet());
        if (gameDates.isEmpty()) {
            log.warn("Game {} stock timeline keyset is empty. Cannot determine date range.", gameId);
            return Collections.emptyList();
        }
        LocalDate gameStartDate = gameDates.get(0);
        LocalDate gameEndDate = gameDates.get(gameDates.size() - 1);

        Set<String> gameTickers = stockTimeline.values().stream()
                .filter(java.util.Objects::nonNull)
                .flatMap(dailyPrices -> dailyPrices.keySet().stream().filter(java.util.Objects::nonNull)) // Also filter
                                                                                                          // null
                                                                                                          // symbols
                .collect(Collectors.toSet());

        if (gameTickers.isEmpty()) {
            log.warn("Game {} has no tickers in its timeline after processing. Cannot filter news by ticker.", gameId);
            return Collections.emptyList();
        }

        LocalDateTime startDateTime = gameStartDate.atStartOfDay();
        LocalDateTime endDateTime = gameEndDate.atTime(23, 59, 59);

        log.debug("Querying news from DB for game {} between {} and {}", gameId, startDateTime, endDateTime);
        List<News> newsItems = newsRepository.findByPublishedTimeBetweenOrderByPublishedTimeDesc(startDateTime,
                endDateTime);
        log.info("Fetched {} news items from DB for game {} date range ({} to {}). Now filtering for game tickers: {}",
                newsItems.size(), gameId, startDateTime, endDateTime, gameTickers);

        List<NewsDTO> relevantNewsDTOs = newsItems.stream()
                .filter(news -> isNewsRelevantToGameTickers(news, gameTickers))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.info("Found {} relevant news items for game {} after ticker filtering.", relevantNewsDTOs.size(), gameId);
        return relevantNewsDTOs;
    }

    private boolean isNewsRelevantToGameTickers(News news, Set<String> gameTickers) {
        if (news.getApiTickerSentimentJson() == null || news.getApiTickerSentimentJson().isEmpty()) {
            return false;
        }
        try {
            TypeReference<List<AlphaVantageNewsApiPojos.TickerSentimentPojo>> typeRef = new TypeReference<>() {
            };
            List<AlphaVantageNewsApiPojos.TickerSentimentPojo> tickerSentiments = objectMapper
                    .readValue(news.getApiTickerSentimentJson(), typeRef);
            for (AlphaVantageNewsApiPojos.TickerSentimentPojo ts : tickerSentiments) {
                if (ts.ticker != null && gameTickers.contains(ts.ticker)) {
                    return true;
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing ticker sentiment JSON for news ID {}: {}. JSON (first 200 chars): '{}'",
                    news.getId(), e.getMessage(), news.getApiTickerSentimentJson().substring(0,
                            Math.min(news.getApiTickerSentimentJson().length(), 200)));
        }
        return false;
    }

    private NewsDTO convertToDTO(News news) {
        NewsDTO dto = new NewsDTO();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setUrl(news.getUrl());
        dto.setSummary(news.getSummary());
        dto.setBannerImage(news.getBannerImage());
        dto.setSource(news.getSource());
        dto.setSourceDomain(news.getSourceDomain());
        dto.setPublishedTime(news.getPublishedTime());
        dto.setOverallSentimentScore(news.getOverallSentimentScore());
        dto.setOverallSentimentLabel(news.getOverallSentimentLabel());

        if (news.getApiTickerSentimentJson() != null && !news.getApiTickerSentimentJson().isEmpty()) {
            try {
                TypeReference<List<AlphaVantageNewsApiPojos.TickerSentimentPojo>> typeRef = new TypeReference<>() {
                };
                List<AlphaVantageNewsApiPojos.TickerSentimentPojo> rawTickerSentiments = objectMapper
                        .readValue(news.getApiTickerSentimentJson(), typeRef);

                List<Map<String, Object>> simplifiedTickerSentiments = rawTickerSentiments.stream()
                        .map(ts -> {
                            Double sentimentScore = null;
                            try {
                                if (ts.tickerSentimentScore != null && !ts.tickerSentimentScore.isBlank()) {
                                    sentimentScore = Double.parseDouble(ts.tickerSentimentScore);
                                }
                            } catch (NumberFormatException e) {
                                log.warn(
                                        "Could not parse tickerSentimentScore '{}' for ticker {} in newsId {}. URL: {}",
                                        ts.tickerSentimentScore, ts.ticker, news.getId(), news.getUrl());
                            }
                            Double relevanceScore = null;
                            try {
                                if (ts.relevanceScore != null && !ts.relevanceScore.isBlank()) {
                                    relevanceScore = Double.parseDouble(ts.relevanceScore);
                                }
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse relevanceScore '{}' for ticker {} in newsId {}. URL: {}",
                                        ts.relevanceScore, ts.ticker, news.getId(), news.getUrl());
                            }
                            return Map.of(
                                    "ticker", (Object) (ts.ticker != null ? ts.ticker : "N/A"),
                                    "relevanceScore", relevanceScore != null ? (Object) relevanceScore : "N/A",
                                    "sentimentScore", sentimentScore != null ? (Object) sentimentScore : "N/A",
                                    "sentimentLabel",
                                    (Object) (ts.tickerSentimentLabel != null ? ts.tickerSentimentLabel : "N/A"));
                        })
                        .collect(Collectors.toList());
                dto.setTickerSentiments(simplifiedTickerSentiments);
            } catch (IOException e) {
                log.error("Error parsing ticker sentiments for DTO from news ID {}. URL: {}. Error: {}",
                        news.getId(), news.getUrl(), e.getMessage());
                dto.setTickerSentiments(Collections.emptyList());
            }
        } else {
            dto.setTickerSentiments(Collections.emptyList());
        }
        return dto;
    }

    @PreDestroy
    public void shutdownExecutor() {
        log.info("Shutting down NewsFetchExecutor...");
        newsFetchExecutor.shutdown();
        try {
            if (!newsFetchExecutor.awaitTermination(30, TimeUnit.SECONDS)) { // Wait 30s
                log.warn("NewsFetchExecutor did not terminate in 30s, forcing shutdown.");
                newsFetchExecutor.shutdownNow();
                if (!newsFetchExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("NewsFetchExecutor did not terminate even after shutdownNow().");
                }
            } else {
                log.info("NewsFetchExecutor shut down cleanly.");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted during NewsFetchExecutor shutdown, forcing shutdownNow().");
            newsFetchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}