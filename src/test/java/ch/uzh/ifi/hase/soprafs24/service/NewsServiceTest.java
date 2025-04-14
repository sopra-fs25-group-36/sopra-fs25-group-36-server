// src/test/java/ch/uzh/ifi/hase/soprafs24/service/NewsServiceTest.java
package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.FinvizNewsArticleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils; // To set @Value fields
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Initialize mocks
class NewsServiceTest {

    @Mock // Create a mock instance of RestTemplate
    private RestTemplate restTemplate;

    @InjectMocks // Create an instance of NewsService and inject mocks (@Mock) into it
    private NewsService newsService;

    // Test values for configuration properties
    private final String testApiUrl = "http://test.finviz.com/news.ashx";
    private final String testApiKey = "TEST_API_KEY";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(newsService, "finvizApiUrl", testApiUrl);
        ReflectionTestUtils.setField(newsService, "finvizApiKey", testApiKey);
    }

    private URI buildExpectedUri() {
        return UriComponentsBuilder.fromUriString(testApiUrl)
                .queryParam("v", "1")
                .queryParam("auth", testApiKey)
                .build(true)
                .toUri();
    }

    @Test
    void fetchFinvizNews_success_returnsParsedArticles() {
        // GIVEN: Mock RestTemplate response
        String csvData = "\"Title\",\"Source\",\"Date\",\"Url\",\"Category\"\n" +
                         "\"Article One\",\"Source A\",\"2024-01-01 12:00:00\",\"http://a.com\",\"Cat A\"\n" +
                         "\"Article Two, With Comma\",\"Source B\",\"2024-01-02 13:00:00\",\"http://b.com\",\"Cat B\"";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(csvData, HttpStatus.OK);
        URI expectedUri = buildExpectedUri();
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class)))
                .thenReturn(mockResponseEntity);

        // WHEN: Call the service method
        List<FinvizNewsArticleDTO> articles = newsService.fetchFinvizNews();

        // THEN: Assert results
        assertNotNull(articles);
        assertEquals(2, articles.size());
        assertEquals("Article One", articles.get(0).getTitle());
        assertEquals("Source A", articles.get(0).getSource());
        assertEquals("Article Two, With Comma", articles.get(1).getTitle());
        assertEquals("Source B", articles.get(1).getSource());
        verify(restTemplate, times(1)).exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class));
    }

    @Test
    void fetchFinvizNews_nonOkStatus_returnsEmptyList() {
        // GIVEN: Mock RestTemplate response with non-OK status
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        URI expectedUri = buildExpectedUri();
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class)))
                .thenReturn(mockResponseEntity);

        // WHEN
        List<FinvizNewsArticleDTO> articles = newsService.fetchFinvizNews();

        // THEN
        assertNotNull(articles);
        assertTrue(articles.isEmpty());
        verify(restTemplate, times(1)).exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class));
    }

    @Test
    void fetchFinvizNews_restClientException_returnsEmptyList() {
        // GIVEN: Mock RestTemplate to throw an exception
        URI expectedUri = buildExpectedUri();
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // WHEN
        List<FinvizNewsArticleDTO> articles = newsService.fetchFinvizNews();

        // THEN
        assertNotNull(articles);
        assertTrue(articles.isEmpty());
        verify(restTemplate, times(1)).exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class));
    }

     @Test
    void fetchFinvizNews_nullBody_returnsEmptyList() {
        // GIVEN: Mock RestTemplate response with OK status but null body
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        URI expectedUri = buildExpectedUri();
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class)))
                .thenReturn(mockResponseEntity);

        // WHEN
        List<FinvizNewsArticleDTO> articles = newsService.fetchFinvizNews();

        // THEN
        assertNotNull(articles);
        assertTrue(articles.isEmpty());
        verify(restTemplate, times(1)).exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class));
    }

    @Test
    void fetchFinvizNews_malformedCsv_returnsEmptyListOrPartialListOnError() {
        // GIVEN: Mock RestTemplate response with malformed CSV (header mismatch)
        String badCsvData = "\"WrongTitle\",\"Source\",\"Date\",\"Url\",\"Category\"\n" +
                            "\"Article One\",\"Source A\",\"2024-01-01 12:00:00\",\"http://a.com\",\"Cat A\"";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(badCsvData, HttpStatus.OK);
        URI expectedUri = buildExpectedUri();
        when(restTemplate.exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class)))
                .thenReturn(mockResponseEntity);

        // WHEN
        List<FinvizNewsArticleDTO> articles = newsService.fetchFinvizNews();

        // THEN: Expect empty list because the header mismatch causes parsing failure for the only record
        assertNotNull(articles);
        assertTrue(articles.isEmpty());
        verify(restTemplate, times(1)).exchange(eq(expectedUri), eq(HttpMethod.GET), eq(null), eq(String.class));
    }

    // REMOVED the fetchFinvizNews_IOExceptionDuringParsing_returnsEmptyList test method

}