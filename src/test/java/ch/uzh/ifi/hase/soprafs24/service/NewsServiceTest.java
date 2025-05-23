package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

        @Mock
        private NewsRepository newsRepository;

        @Mock
        private HttpClient mockHttpClient;

        @Mock
        private HttpResponse<String> mockHttpResponse;

        private NewsService newsService;
        private final ObjectMapper objectMapper = new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        private static final String TEST_API_KEY = "TEST_API_KEY_VALID";
        private static final String DEMO_API_KEY = "demo";
        private static final DateTimeFormatter AV_API_TIME_PUBLISHED_FORMAT = DateTimeFormatter
                        .ofPattern("yyyyMMdd'T'HHmmss");

        @BeforeEach
        void setUp() {
                newsService = new NewsService(newsRepository, TEST_API_KEY);
                ReflectionTestUtils.setField(newsService, "httpClient", mockHttpClient);
                InMemoryGameRegistry.clear();
        }

        @AfterEach
        void tearDown() {
                InMemoryGameRegistry.clear();
                ExecutorService executor = (ExecutorService) ReflectionTestUtils.getField(newsService,
                                "newsFetchExecutor");
                if (executor != null && !executor.isShutdown()) {
                        executor.shutdownNow();
                        try {
                                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                                        System.err.println("NewsFetchExecutor did not terminate in @AfterEach");
                                }
                        } catch (InterruptedException e) {
                                executor.shutdownNow();
                                Thread.currentThread().interrupt();
                        }
                }
        }

        private String createNewsResponseJson(List<AlphaVantageNewsApiPojos.FeedItem> feedItems)
                        throws JsonProcessingException {
                AlphaVantageNewsApiPojos.AlphaVantageNewsResponse response = new AlphaVantageNewsApiPojos.AlphaVantageNewsResponse();
                response.feed = feedItems;
                return objectMapper.writeValueAsString(response);
        }

        private String createErrorResponseJson(String errorMessage) throws JsonProcessingException {
                AlphaVantageNewsApiPojos.AlphaVantageNewsResponse response = new AlphaVantageNewsApiPojos.AlphaVantageNewsResponse();
                response.errorMessage = errorMessage;
                return objectMapper.writeValueAsString(response);
        }

        private String createInformationResponseJson(String informationMessage) throws JsonProcessingException {
                AlphaVantageNewsApiPojos.AlphaVantageNewsResponse response = new AlphaVantageNewsApiPojos.AlphaVantageNewsResponse();
                response.information = informationMessage;
                return objectMapper.writeValueAsString(response);
        }

        @Test
        void fetchAndSaveNewsForTickers_validInput_fetchesAndSavesNewsSuccessfully()
                        throws IOException, InterruptedException {
                String ticker = "AAPL";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);
                AlphaVantageNewsApiPojos.FeedItem feedItem = new AlphaVantageNewsApiPojos.FeedItem();
                feedItem.title = "Apple News";
                feedItem.url = "http://example.com/aaplnews";
                feedItem.summary = "Summary for Apple.";
                feedItem.timePublished = gameStartDate.atTime(10, 0, 0).format(AV_API_TIME_PUBLISHED_FORMAT);
                feedItem.overallSentimentScore = 0.6;
                feedItem.overallSentimentLabel = "Bullish";
                feedItem.source = "NewsSource";
                feedItem.sourceDomain = "newssource.com";
                feedItem.bannerImage = "http://example.com/image.png";
                AlphaVantageNewsApiPojos.TickerSentimentPojo tickerSentiment = new AlphaVantageNewsApiPojos.TickerSentimentPojo();
                tickerSentiment.ticker = ticker;
                tickerSentiment.tickerSentimentScore = "0.7";
                tickerSentiment.relevanceScore = "0.9";
                tickerSentiment.tickerSentimentLabel = "Very Bullish";
                feedItem.tickerSentiment = Collections.singletonList(tickerSentiment);
                AlphaVantageNewsApiPojos.TopicPojo topicPojo = new AlphaVantageNewsApiPojos.TopicPojo();
                topicPojo.topic = "Technology";
                topicPojo.relevanceScore = "0.9";
                feedItem.topics = Collections.singletonList(topicPojo);
                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createNewsResponseJson(Collections.singletonList(feedItem)));
                when(newsRepository.findByUrl(feedItem.url)).thenReturn(Optional.empty());
                when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
                verify(newsRepository, times(1)).save(newsCaptor.capture());
                News savedNews = newsCaptor.getValue();
                assertEquals(feedItem.title, savedNews.getTitle());
                assertEquals(feedItem.url, savedNews.getUrl());
                assertEquals(feedItem.summary, savedNews.getSummary());
                assertEquals(LocalDateTime.parse(feedItem.timePublished, AV_API_TIME_PUBLISHED_FORMAT),
                                savedNews.getPublishedTime());
                assertEquals(feedItem.overallSentimentScore, savedNews.getOverallSentimentScore());
                assertEquals(feedItem.overallSentimentLabel, savedNews.getOverallSentimentLabel());
                assertEquals(feedItem.source, savedNews.getSource());
                assertEquals(feedItem.sourceDomain, savedNews.getSourceDomain());
                assertEquals(feedItem.bannerImage, savedNews.getBannerImage());
                assertNotNull(savedNews.getApiTickerSentimentJson());
                assertNotNull(savedNews.getApiTopicRelevanceJson());
                List<AlphaVantageNewsApiPojos.TickerSentimentPojo> savedTickerSentiments = objectMapper
                                .readValue(savedNews.getApiTickerSentimentJson(), new TypeReference<>() {
                                });
                assertEquals(1, savedTickerSentiments.size());
                assertEquals(ticker, savedTickerSentiments.get(0).ticker);
                List<AlphaVantageNewsApiPojos.TopicPojo> savedTopics = objectMapper
                                .readValue(savedNews.getApiTopicRelevanceJson(), new TypeReference<>() {
                                });
                assertEquals(1, savedTopics.size());
                assertEquals("Technology", savedTopics.get(0).topic);
                verify(mockHttpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
        }

        @Test
        void fetchAndSaveNewsForTickers_invalidApiKey_skipsNewsFetch() throws IOException, InterruptedException {
                ReflectionTestUtils.setField(newsService, "API_KEY", DEMO_API_KEY);
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList("AAPL"), LocalDate.now(),
                                LocalDate.now().plusDays(1));
                verify(mockHttpClient, never()).send(any(), any());
                verify(newsRepository, never()).save(any());
                ReflectionTestUtils.setField(newsService, "API_KEY", null);
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList("AAPL"), LocalDate.now(),
                                LocalDate.now().plusDays(1));
                verify(mockHttpClient, never()).send(any(), any());
                verify(newsRepository, never()).save(any());
        }

        @Test
        void fetchAndSaveNewsForTickers_emptyTickersList_skipsNewsFetch() throws IOException, InterruptedException {
                newsService.fetchAndSaveNewsForTickers(Collections.emptyList(), LocalDate.now(),
                                LocalDate.now().plusDays(1));
                verify(mockHttpClient, never()).send(any(), any());
        }

        @Test
        void fetchAndSaveNewsForTickers_nullParameters_skipsNewsFetch() throws IOException, InterruptedException {
                newsService.fetchAndSaveNewsForTickers(null, LocalDate.now(), LocalDate.now().plusDays(1));
                verify(mockHttpClient, never()).send(any(), any());
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList("AAPL"), null,
                                LocalDate.now().plusDays(1));
                verify(mockHttpClient, never()).send(any(), any());
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList("AAPL"), LocalDate.now(), null);
                verify(mockHttpClient, never()).send(any(), any());
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_apiReturnsErrorStatusCode_logsErrorAndSavesNoNews()
                        throws IOException, InterruptedException {
                String ticker = "FAIL";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);
                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(500);
                when(mockHttpResponse.body()).thenReturn("Internal Server Error");
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                verify(newsRepository, never()).save(any(News.class));
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_apiReturnsErrorMessageInBody_logsErrorAndSavesNoNews()
                        throws IOException, InterruptedException {
                String ticker = "ERR";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);

                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createErrorResponseJson("Invalid API call"));
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                verify(newsRepository, never()).save(any(News.class));
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_apiReturnsRateLimitMessage_logsWarningAndSavesNoNews()
                        throws IOException, InterruptedException {
                String ticker = "RATE";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);

                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createInformationResponseJson(
                                "Thank you for using Alpha Vantage! Our standard API call frequency is ..."));
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                verify(newsRepository, never()).save(any(News.class));
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_apiReturnsNoFeedItems_logsInfoAndSavesNoNews()
                        throws IOException, InterruptedException {
                String ticker = "EMPTY";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);

                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createNewsResponseJson(Collections.emptyList()));
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                verify(newsRepository, never()).save(any(News.class));
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_newsArticleAlreadyExists_skipsSavingDuplicate()
                        throws IOException, InterruptedException {
                String ticker = "DUPE";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);

                AlphaVantageNewsApiPojos.FeedItem feedItem = new AlphaVantageNewsApiPojos.FeedItem();
                feedItem.url = "http://example.com/dupenews";
                feedItem.title = "Dupe News";
                feedItem.timePublished = gameStartDate.atStartOfDay().format(AV_API_TIME_PUBLISHED_FORMAT);
                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createNewsResponseJson(Collections.singletonList(feedItem)));
                when(newsRepository.findByUrl(feedItem.url)).thenReturn(Optional.of(new News()));
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                verify(newsRepository, never()).save(any(News.class));
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_newsArticleWithNullUrl_isSkipped()
                        throws IOException, InterruptedException {
                String ticker = "NULLURL";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);
                AlphaVantageNewsApiPojos.FeedItem feedItem = new AlphaVantageNewsApiPojos.FeedItem();
                feedItem.url = null;
                feedItem.title = "News with null URL";
                feedItem.timePublished = gameStartDate.atStartOfDay().format(AV_API_TIME_PUBLISHED_FORMAT);
                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createNewsResponseJson(Collections.singletonList(feedItem)));
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                verify(newsRepository, never()).save(any(News.class));
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_newsArticleWithInvalidPublishedTime_isSkipped()
                        throws IOException, InterruptedException {
                String ticker = "BADTIME";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);
                AlphaVantageNewsApiPojos.FeedItem feedItem = new AlphaVantageNewsApiPojos.FeedItem();
                feedItem.url = "http://example.com/badtime";
                feedItem.title = "News with bad time";
                feedItem.timePublished = "INVALID_TIME_FORMAT";
                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createNewsResponseJson(Collections.singletonList(feedItem)));
                when(newsRepository.findByUrl(feedItem.url)).thenReturn(Optional.empty());
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                verify(newsRepository, never()).save(any(News.class));
        }

        @Test
        void fetchAndSaveNewsForSingleTicker_titleAndSourceLengthLimits_areApplied()
                        throws IOException, InterruptedException {
                String ticker = "LNGSTR";
                LocalDate gameStartDate = LocalDate.of(2023, 1, 1);
                LocalDate gameEndDate = LocalDate.of(2023, 1, 2);
                AlphaVantageNewsApiPojos.FeedItem feedItem = new AlphaVantageNewsApiPojos.FeedItem();
                feedItem.url = "http://example.com/longstrings";
                feedItem.title = "a".repeat(600);
                feedItem.source = "b".repeat(300);
                feedItem.sourceDomain = "c".repeat(300);
                feedItem.summary = "Short summary";
                feedItem.timePublished = gameStartDate.atStartOfDay().format(AV_API_TIME_PUBLISHED_FORMAT);
                when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                                .thenReturn(mockHttpResponse);
                when(mockHttpResponse.statusCode()).thenReturn(200);
                when(mockHttpResponse.body()).thenReturn(createNewsResponseJson(Collections.singletonList(feedItem)));
                when(newsRepository.findByUrl(feedItem.url)).thenReturn(Optional.empty());
                when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));
                newsService.fetchAndSaveNewsForTickers(Collections.singletonList(ticker), gameStartDate, gameEndDate);
                ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
                verify(newsRepository, times(1)).save(newsCaptor.capture());
                News savedNews = newsCaptor.getValue();
                assertEquals(510, savedNews.getTitle().length());
                assertEquals(250, savedNews.getSource().length());
                assertEquals(250, savedNews.getSourceDomain().length());
        }

        @Test
        void getNewsForGame_validGameIdAndData_returnsRelevantNewsDTOs() throws JsonProcessingException {
                Long gameId = 1L;
                LocalDate startDate = LocalDate.of(2023, 1, 1);
                LocalDate endDate = LocalDate.of(2023, 1, 2);
                LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline = new LinkedHashMap<>();
                stockTimeline.put(startDate, Map.of("AAPL", 150.0, "MSFT", 200.0));
                stockTimeline.put(endDate, Map.of("AAPL", 151.0, "MSFT", 201.0));
                GameManager gameManager = new GameManager(gameId, stockTimeline, 60);
                InMemoryGameRegistry.registerGame(gameId, gameManager);
                News newsAAPL = new News();
                newsAAPL.setId(10L);
                newsAAPL.setTitle("AAPL News");
                newsAAPL.setUrl("url.aapl");
                newsAAPL.setPublishedTime(startDate.atTime(10, 0));
                AlphaVantageNewsApiPojos.TickerSentimentPojo sentimentAAPLPojo = new AlphaVantageNewsApiPojos.TickerSentimentPojo();
                sentimentAAPLPojo.ticker = "AAPL";
                sentimentAAPLPojo.tickerSentimentScore = "0.8";
                sentimentAAPLPojo.relevanceScore = "0.9";
                sentimentAAPLPojo.tickerSentimentLabel = "Bullish";
                newsAAPL.setApiTickerSentimentJson(
                                objectMapper.writeValueAsString(Collections.singletonList(sentimentAAPLPojo)));
                News newsMSFT = new News();
                newsMSFT.setId(11L);
                newsMSFT.setTitle("MSFT News Relevant");
                newsMSFT.setUrl("url.msft");
                newsMSFT.setPublishedTime(startDate.atTime(11, 0));
                AlphaVantageNewsApiPojos.TickerSentimentPojo sentimentMSFTPojo = new AlphaVantageNewsApiPojos.TickerSentimentPojo();
                sentimentMSFTPojo.ticker = "MSFT";
                sentimentMSFTPojo.tickerSentimentScore = "0.1";
                sentimentMSFTPojo.relevanceScore = "0.5";
                sentimentMSFTPojo.tickerSentimentLabel = "Neutral";
                newsMSFT.setApiTickerSentimentJson(
                                objectMapper.writeValueAsString(Collections.singletonList(sentimentMSFTPojo)));
                News newsGOOG = new News();
                newsGOOG.setId(12L);
                newsGOOG.setTitle("GOOG News Irrelevant");
                newsGOOG.setUrl("url.goog");
                newsGOOG.setPublishedTime(startDate.atTime(12, 0));
                AlphaVantageNewsApiPojos.TickerSentimentPojo sentimentGOOGPojo = new AlphaVantageNewsApiPojos.TickerSentimentPojo();
                sentimentGOOGPojo.ticker = "GOOG";
                sentimentGOOGPojo.tickerSentimentScore = "0.1";
                sentimentGOOGPojo.relevanceScore = "0.5";
                sentimentGOOGPojo.tickerSentimentLabel = "Neutral";
                newsGOOG.setApiTickerSentimentJson(
                                objectMapper.writeValueAsString(Collections.singletonList(sentimentGOOGPojo)));
                when(newsRepository.findByPublishedTimeBetweenOrderByPublishedTimeDesc(
                                eq(startDate.atStartOfDay()), eq(endDate.atTime(23, 59, 59))))
                                .thenReturn(Arrays.asList(newsGOOG, newsMSFT, newsAAPL));
                List<NewsDTO> dtos = newsService.getNewsForGame(gameId);
                assertEquals(2, dtos.size());
                assertEquals("MSFT News Relevant", dtos.get(0).getTitle());
                assertEquals("AAPL News", dtos.get(1).getTitle());
                assertEquals(1, dtos.get(0).getTickerSentiments().size());
                assertEquals("MSFT", (dtos.get(0).getTickerSentiments().get(0)).get("ticker"));
        }

        @Test
        void getNewsForGame_gameWithEmptyStockTimeline_returnsEmptyList() {
                Long gameId = 2L;
                GameManager gameManager = new GameManager(gameId, new LinkedHashMap<>(), 60);
                InMemoryGameRegistry.registerGame(gameId, gameManager);
                List<NewsDTO> dtos = newsService.getNewsForGame(gameId);
                assertTrue(dtos.isEmpty());
        }

        @Test
        void getNewsForGame_gameWithNullStockTimeline_returnsEmptyList() {
                Long gameId = 3L;
                GameManager gameManager = mock(GameManager.class);
                when(gameManager.getStockTimeline()).thenReturn(null);
                InMemoryGameRegistry.registerGame(gameId, gameManager);
                List<NewsDTO> dtos = newsService.getNewsForGame(gameId);
                assertTrue(dtos.isEmpty());
        }

        @Test
        void getNewsForGame_gameWithNoTickersInTimeline_returnsEmptyList() {
                Long gameId = 3L;
                LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline = new LinkedHashMap<>();
                stockTimeline.put(LocalDate.now(), Collections.emptyMap());
                GameManager gameManager = new GameManager(gameId, stockTimeline, 60);
                InMemoryGameRegistry.registerGame(gameId, gameManager);
                List<NewsDTO> dtos = newsService.getNewsForGame(gameId);
                assertTrue(dtos.isEmpty());
        }

        @Test
        void getNewsForGame_newsItemWithNoMatchingTickerSentiment_isFilteredOut() throws JsonProcessingException {
                Long gameId = 4L;
                LocalDate date = LocalDate.of(2023, 1, 1);
                LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline = new LinkedHashMap<>();
                stockTimeline.put(date, Map.of("ONLY_THIS", 100.0));
                GameManager gameManager = new GameManager(gameId, stockTimeline, 60);
                InMemoryGameRegistry.registerGame(gameId, gameManager);
                News newsOtherTicker = new News();
                newsOtherTicker.setId(20L);
                newsOtherTicker.setPublishedTime(date.atStartOfDay());
                AlphaVantageNewsApiPojos.TickerSentimentPojo sentimentPojo = new AlphaVantageNewsApiPojos.TickerSentimentPojo();
                sentimentPojo.ticker = "OTHER";
                sentimentPojo.tickerSentimentScore = "0.5";
                sentimentPojo.relevanceScore = "0.5";
                sentimentPojo.tickerSentimentLabel = "Neutral";
                newsOtherTicker
                                .setApiTickerSentimentJson(objectMapper
                                                .writeValueAsString(Collections.singletonList(sentimentPojo)));
                when(newsRepository.findByPublishedTimeBetweenOrderByPublishedTimeDesc(any(), any()))
                                .thenReturn(Collections.singletonList(newsOtherTicker));
                List<NewsDTO> dtos = newsService.getNewsForGame(gameId);
                assertTrue(dtos.isEmpty());
        }

        @Test
        void getNewsForGame_newsItemWithNullTickerSentimentJson_isFilteredOut() {
                Long gameId = 5L;
                LocalDate date = LocalDate.of(2023, 1, 1);
                LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline = new LinkedHashMap<>();
                stockTimeline.put(date, Map.of("AAPL", 100.0));
                GameManager gameManager = new GameManager(gameId, stockTimeline, 60);
                InMemoryGameRegistry.registerGame(gameId, gameManager);
                News newsNullSentiment = new News();
                newsNullSentiment.setId(21L);
                newsNullSentiment.setPublishedTime(date.atStartOfDay());
                newsNullSentiment.setApiTickerSentimentJson(null);
                when(newsRepository.findByPublishedTimeBetweenOrderByPublishedTimeDesc(any(), any()))
                                .thenReturn(Collections.singletonList(newsNullSentiment));
                List<NewsDTO> dtos = newsService.getNewsForGame(gameId);
                assertTrue(dtos.isEmpty());
        }

        @Test
        void getNewsForGame_newsItemWithMalformedTickerSentimentJson_logsErrorAndFiltersOut() {
                Long gameId = 6L;
                LocalDate date = LocalDate.of(2023, 1, 1);
                LinkedHashMap<LocalDate, Map<String, Double>> stockTimeline = new LinkedHashMap<>();
                stockTimeline.put(date, Map.of("AAPL", 100.0));
                GameManager gameManager = new GameManager(gameId, stockTimeline, 60);
                InMemoryGameRegistry.registerGame(gameId, gameManager);
                News newsMalformed = new News();
                newsMalformed.setId(22L);
                newsMalformed.setPublishedTime(date.atStartOfDay());
                newsMalformed.setApiTickerSentimentJson("this is not json");
                when(newsRepository.findByPublishedTimeBetweenOrderByPublishedTimeDesc(any(), any()))
                                .thenReturn(Collections.singletonList(newsMalformed));
                List<NewsDTO> dtos = newsService.getNewsForGame(gameId);
                assertTrue(dtos.isEmpty());
        }

        @Test
        void convertToDTO_withValidTickerSentiment_parsesCorrectly() throws JsonProcessingException {
                News news = new News();
                news.setId(1L);
                news.setTitle("Test News");
                AlphaVantageNewsApiPojos.TickerSentimentPojo sentimentPojo = new AlphaVantageNewsApiPojos.TickerSentimentPojo();
                sentimentPojo.ticker = "TEST";
                sentimentPojo.tickerSentimentScore = "0.123";
                sentimentPojo.relevanceScore = "0.456";
                sentimentPojo.tickerSentimentLabel = "TestLabel";
                news.setApiTickerSentimentJson(
                                objectMapper.writeValueAsString(Collections.singletonList(sentimentPojo)));
                NewsDTO dto = ReflectionTestUtils.invokeMethod(newsService, "convertToDTO", news);
                assertNotNull(dto.getTickerSentiments());
                assertEquals(1, dto.getTickerSentiments().size());
                Map<String, Object> sentimentMap = dto.getTickerSentiments().get(0);
                assertEquals("TEST", sentimentMap.get("ticker"));
                assertEquals(0.123, sentimentMap.get("sentimentScore"));
                assertEquals(0.456, sentimentMap.get("relevanceScore"));
                assertEquals("TestLabel", sentimentMap.get("sentimentLabel"));
        }

        @Test
        void convertToDTO_withUnparseableScoresInTickerSentiment_handlesGracefully() throws JsonProcessingException {
                News news = new News();
                news.setId(2L);
                AlphaVantageNewsApiPojos.TickerSentimentPojo sentimentPojo = new AlphaVantageNewsApiPojos.TickerSentimentPojo();
                sentimentPojo.ticker = "BAD";
                sentimentPojo.tickerSentimentScore = "not-a-double";
                sentimentPojo.relevanceScore = "also-not-a-double";
                sentimentPojo.tickerSentimentLabel = "BadLabel";
                news.setApiTickerSentimentJson(
                                objectMapper.writeValueAsString(Collections.singletonList(sentimentPojo)));
                NewsDTO dto = ReflectionTestUtils.invokeMethod(newsService, "convertToDTO", news);
                assertNotNull(dto.getTickerSentiments());
                assertEquals(1, dto.getTickerSentiments().size());
                Map<String, Object> sentimentMap = dto.getTickerSentiments().get(0);
                assertEquals("N/A", sentimentMap.get("sentimentScore"));
                assertEquals("N/A", sentimentMap.get("relevanceScore"));
        }

        @Test
        void convertToDTO_withNullOrEmptyTickerSentimentJson_returnsEmptyListForSentiments() {
                News news = new News();
                news.setId(3L);
                news.setApiTickerSentimentJson(null);
                NewsDTO dto1 = ReflectionTestUtils.invokeMethod(newsService, "convertToDTO", news);
                assertTrue(dto1.getTickerSentiments().isEmpty());
                news.setApiTickerSentimentJson("");
                NewsDTO dto2 = ReflectionTestUtils.invokeMethod(newsService, "convertToDTO", news);
                assertTrue(dto2.getTickerSentiments().isEmpty());
        }
}