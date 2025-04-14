package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.FinvizNewsArticleDTO;
import ch.uzh.ifi.hase.soprafs24.service.NewsService;
import com.fasterxml.jackson.databind.ObjectMapper; // For comparing objects as JSON
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class) // Test only the NewsController slice
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc; // Simulate HTTP requests

    @MockBean // Create a mock of NewsService and add it to the ApplicationContext
    private NewsService newsService;

    @Autowired
    private ObjectMapper objectMapper; // Utility to convert objects to JSON strings if needed

    @Test
    void getNews_whenServiceReturnsArticles_thenReturnsJsonArray() throws Exception {
        // GIVEN
        FinvizNewsArticleDTO article1 = new FinvizNewsArticleDTO("Title 1", "Src 1", "Date 1", "Url 1", "Cat 1");
        FinvizNewsArticleDTO article2 = new FinvizNewsArticleDTO("Title 2", "Src 2", "Date 2", "Url 2", "Cat 2");
        List<FinvizNewsArticleDTO> articles = List.of(article1, article2);

        // Mock the service call
        given(newsService.fetchFinvizNews()).willReturn(articles);

        // WHEN: Perform GET request to /news
        MockHttpServletRequestBuilder getRequest = get("/news")
                .contentType(MediaType.APPLICATION_JSON);

        // THEN: Assert response
        mockMvc.perform(getRequest)
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$", hasSize(2))) // Expect JSON array with 2 elements
                .andExpect(jsonPath("$[0].title", is(article1.getTitle())))
                .andExpect(jsonPath("$[0].source", is(article1.getSource())))
                .andExpect(jsonPath("$[0].date", is(article1.getDate())))
                .andExpect(jsonPath("$[0].url", is(article1.getUrl())))
                .andExpect(jsonPath("$[0].category", is(article1.getCategory())))
                .andExpect(jsonPath("$[1].title", is(article2.getTitle()))); // Check second element partially

        // Verify that the service method was called exactly once
        verify(newsService, times(1)).fetchFinvizNews();
    }

    @Test
    void getNews_whenServiceReturnsEmptyList_thenReturnsEmptyJsonArray() throws Exception {
        // GIVEN
        // Mock the service call to return an empty list
        given(newsService.fetchFinvizNews()).willReturn(Collections.emptyList());

        // WHEN: Perform GET request to /news
        MockHttpServletRequestBuilder getRequest = get("/news")
                .contentType(MediaType.APPLICATION_JSON);

        // THEN: Assert response
        mockMvc.perform(getRequest)
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$", hasSize(0))); // Expect empty JSON array

        // Verify that the service method was called exactly once
        verify(newsService, times(1)).fetchFinvizNews();
    }

    // Optional: Test case if service throws an exception (though usually service handles it and returns empty)
    // If the service were to throw an exception *up* to the controller, you might test
    // for a 500 Internal Server Error status, depending on your GlobalExceptionHandler.
    // But based on current NewsService, it catches exceptions and returns empty list,
    // so the above tests cover the controller's expected behavior.

}