package ch.uzh.ifi.hase.soprafs24.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ch.uzh.ifi.hase.soprafs24.rest.dto.NewsDTO;
import ch.uzh.ifi.hase.soprafs24.service.NewsService;

@WebMvcTest(NewsController.class)
class NewsControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsService newsService;

        @Test
        void getGameNews_success() throws Exception {
                Long gameId = 1L;
                NewsDTO newsDTO1 = new NewsDTO();
                newsDTO1.setTitle("Apple Stock Rises");
                newsDTO1.setSummary("Apple stock shows positive growth");
                newsDTO1.setUrl("https://example.com/news1");
                newsDTO1.setPublishedTime(java.time.LocalDateTime.parse("2024-01-15T10:00:00"));
                NewsDTO newsDTO2 = new NewsDTO();
                newsDTO2.setTitle("Microsoft Announces New Product");
                newsDTO2.setSummary("Microsoft unveils innovative technology");
                newsDTO2.setUrl("https://example.com/news2");
                newsDTO2.setPublishedTime(java.time.LocalDateTime.parse("2024-01-16T14:30:00"));
                List<NewsDTO> mockNews = Arrays.asList(newsDTO1, newsDTO2);
                when(newsService.getNewsForGame(gameId)).thenReturn(mockNews);
                mockMvc.perform(get("/api/news/{gameId}", gameId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].title").value("Apple Stock Rises"))
                                .andExpect(jsonPath("$[0].summary").value("Apple stock shows positive growth"))
                                .andExpect(jsonPath("$[1].title").value("Microsoft Announces New Product"))
                                .andExpect(jsonPath("$[1].summary").value("Microsoft unveils innovative technology"));
        }

        @Test
        void getGameNews_emptyList() throws Exception {
                Long gameId = 1L;
                when(newsService.getNewsForGame(gameId)).thenReturn(Arrays.asList());
                mockMvc.perform(get("/api/news/{gameId}", gameId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void getGameNews_serviceThrowsException() throws Exception {
                Long gameId = 1L;
                when(newsService.getNewsForGame(gameId)).thenThrow(new RuntimeException("Service error"));
                mockMvc.perform(get("/api/news/{gameId}", gameId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        void fetchNewsManually_success() throws Exception {
                List<String> tickers = Arrays.asList("AAPL", "MSFT");
                String startDate = "2024-01-01";
                String endDate = "2024-01-31";
                doNothing().when(newsService).fetchAndSaveNewsForTickers(
                                eq(tickers),
                                eq(LocalDate.parse(startDate)),
                                eq(LocalDate.parse(endDate)));
                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL", "MSFT")
                                .param("startDate", startDate)
                                .param("endDate", endDate)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string(
                                                "News fetching process initiated for tickers: AAPL,MSFT between 2024-01-01 and 2024-01-31"));
        }

        @Test
        void fetchNewsManually_singleTicker() throws Exception {
                List<String> tickers = Arrays.asList("AAPL");
                String startDate = "2024-01-01";
                String endDate = "2024-01-31";
                doNothing().when(newsService).fetchAndSaveNewsForTickers(
                                eq(tickers),
                                eq(LocalDate.parse(startDate)),
                                eq(LocalDate.parse(endDate)));
                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL")
                                .param("startDate", startDate)
                                .param("endDate", endDate)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content()
                                                .string("News fetching process initiated for tickers: AAPL between 2024-01-01 and 2024-01-31"));
        }

        @Test
        void fetchNewsManually_invalidStartDateFormat() throws Exception {
                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL", "MSFT")
                                .param("startDate", "invalid-date")
                                .param("endDate", "2024-01-31")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(
                                                org.hamcrest.Matchers.containsString(
                                                                "Invalid date format. Please use YYYY-MM-DD. Error:")));
        }

        @Test
        void fetchNewsManually_invalidEndDateFormat() throws Exception {
                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL", "MSFT")
                                .param("startDate", "2024-01-01")
                                .param("endDate", "31-01-2024")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(
                                                org.hamcrest.Matchers.containsString(
                                                                "Invalid date format. Please use YYYY-MM-DD. Error:")));
        }

        @Test
        void fetchNewsManually_missingTickersParameter() throws Exception {
                mockMvc.perform(post("/api/news/fetch")
                                .param("startDate", "2024-01-01")
                                .param("endDate", "2024-01-31")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void fetchNewsManually_missingStartDateParameter() throws Exception {
                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL", "MSFT")
                                .param("endDate", "2024-01-31")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void fetchNewsManually_missingEndDateParameter() throws Exception {
                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL", "MSFT")
                                .param("startDate", "2024-01-01")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void fetchNewsManually_serviceThrowsException() throws Exception {
                List<String> tickers = Arrays.asList("AAPL", "MSFT");
                String startDate = "2024-01-01";
                String endDate = "2024-01-31";

                doThrow(new RuntimeException("API connection failed")).when(newsService)
                                .fetchAndSaveNewsForTickers(
                                                eq(tickers),
                                                eq(LocalDate.parse(startDate)),
                                                eq(LocalDate.parse(endDate)));

                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL", "MSFT")
                                .param("startDate", startDate)
                                .param("endDate", endDate)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string("Error initiating news fetch: API connection failed"));
        }

        @Test
        void fetchNewsManually_emptyTickersList() throws Exception {
                String startDate = "2024-01-01";
                String endDate = "2024-01-31";
                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "")
                                .param("startDate", startDate)
                                .param("endDate", endDate)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        void fetchNewsManually_futureDate() throws Exception {
                String startDate = "2025-01-01";
                String endDate = "2025-01-31";

                doNothing().when(newsService).fetchAndSaveNewsForTickers(
                                anyList(),
                                eq(LocalDate.parse(startDate)),
                                eq(LocalDate.parse(endDate)));

                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL")
                                .param("startDate", startDate)
                                .param("endDate", endDate)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content()
                                                .string("News fetching process initiated for tickers: AAPL between 2025-01-01 and 2025-01-31"));
        }

        @Test
        void fetchNewsManually_startDateAfterEndDate() throws Exception {
                String startDate = "2024-01-31";
                String endDate = "2024-01-01";
                doNothing().when(newsService).fetchAndSaveNewsForTickers(
                                anyList(),
                                eq(LocalDate.parse(startDate)),
                                eq(LocalDate.parse(endDate)));

                mockMvc.perform(post("/api/news/fetch")
                                .param("tickers", "AAPL")
                                .param("startDate", startDate)
                                .param("endDate", endDate)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content()
                                                .string("News fetching process initiated for tickers: AAPL between 2024-01-31 and 2024-01-01"));
        }
}