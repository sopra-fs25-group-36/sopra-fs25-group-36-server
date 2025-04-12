// src/main/java/ch/uzh/ifi/hase/soprafs24/service/NewsService.java
package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.FinvizNewsArticleDTO; // Import the NEW DTO
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class NewsService {

    private final Logger log = LoggerFactory.getLogger(NewsService.class);

    // Ensure these keys EXACTLY match your .env file keys
    @Value("${FINVIZ_API_URL}")
    private String finvizApiUrl;

    @Value("${FINVIZ_API_KEY}")
    private String finvizApiKey;

    private final RestTemplate restTemplate;

    // Constructor injection (no change needed here)
    public NewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Main method to fetch and parse Finviz news
    public List<FinvizNewsArticleDTO> fetchFinvizNews() {
        log.info("Requesting news from Finviz");

        // --- 1. Build Finviz URL ---
        URI uri;
        try {
             uri = UriComponentsBuilder.fromUriString(finvizApiUrl)
                    .queryParam("v", "1")            // Finviz specific param
                    .queryParam("auth", finvizApiKey) // Finviz API key param
                    // Add other params like &ticker=AAPL here if needed later
                    .build(true) // Use build(true) to ensure encoding of template vars if any
                    .toUri();
             log.debug("Constructed Finviz URI: {}", uri);
        } catch (Exception e) {
            log.error("Error building Finviz URI. Check FINVIZ_API_URL in config: {}", finvizApiUrl, e);
            return Collections.emptyList();
        }


        // --- 2. Make HTTP GET Request ---
        String csvData = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null, // No request entity/body needed for GET
                    String.class // Expect the raw CSV as a String
            );

            // --- 3. Check Response Status ---
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                csvData = response.getBody();
                // Log only a snippet to avoid flooding logs with large CSV data
                log.info("Successfully received CSV data from Finviz (status {}). Length: {}", response.getStatusCodeValue(), csvData.length());
                log.debug("CSV data snippet:\n{}", csvData.substring(0, Math.min(csvData.length(), 500))); // Log first 500 chars
            } else {
                log.error("Failed to fetch news from Finviz. Status code: {}", response.getStatusCode());
                return Collections.emptyList(); // Return empty on non-OK status
            }
        } catch (RestClientException e) {
            log.error("Error during REST call to Finviz URI [{}]: {}", uri, e.getMessage());
            // Consider logging e for stack trace in debug mode: log.debug("Stack trace:", e);
            return Collections.emptyList(); // Return empty on connection/HTTP error
        } catch (Exception e) { // Catch unexpected errors during HTTP call
             log.error("Unexpected error fetching Finviz news from URI [{}]: {}", uri, e.getMessage(), e);
             return Collections.emptyList();
        }


        // --- 4. Parse the CSV Data ---
        if (csvData != null && !csvData.isEmpty()) {
            try {
                return parseFinvizCsv(csvData);
            } catch (IOException e) {
                log.error("Error parsing CSV data received from Finviz: {}", e.getMessage(), e);
                return Collections.emptyList(); // Return empty on parsing error
            } catch (Exception e) { // Catch unexpected parsing errors
                 log.error("Unexpected error parsing Finviz CSV: {}", e.getMessage(), e);
                 return Collections.emptyList();
            }
        } else {
             log.warn("Received empty or null CSV data from Finviz.");
             return Collections.emptyList(); // Return empty if no CSV data was received
        }
    }


    // --- Helper method to parse the CSV string ---
    private List<FinvizNewsArticleDTO> parseFinvizCsv(String csvData) throws IOException {
        List<FinvizNewsArticleDTO> articles = new ArrayList<>();
        // Use try-with-resources for automatic closing of Reader and Parser
        try (StringReader reader = new StringReader(csvData);
             // Configure the parser based on the sample CSV provided
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()   // Uses the first line as headers
                     .withIgnoreHeaderCase()      // Matches "Title" and "title" etc.
                     .withTrim()                  // Removes whitespace around values
                     .withQuote('"')              // Defines the quote character
                     .parse(reader)
            ) {

            int recordCount = 0;
            for (CSVRecord record : parser) {
                 recordCount++;
                try {
                    // Create DTO using the EXACT (case-insensitive) header names
                    FinvizNewsArticleDTO article = new FinvizNewsArticleDTO(
                            record.get("Title"),    // Must match header in CSV
                            record.get("Source"),   // Must match header in CSV
                            record.get("Date"),     // Must match header in CSV
                            record.get("Url"),      // Must match header in CSV
                            record.get("Category")  // Must match header in CSV
                    );
                    articles.add(article);
                } catch (IllegalArgumentException e) {
                    // This usually means a header name in record.get("...") is wrong
                    log.warn("Skipping record #{} due to missing/incorrect header name. Check CSV header and record.get() calls. Record: {} | Error: {}", recordCount, record.toMap(), e.getMessage());
                } catch (Exception e) {
                    log.warn("Skipping record #{} due to unexpected error during processing: Record: {} | Error: {}", recordCount, record.toMap(), e.getMessage(), e);
                }
            }
            log.info("Successfully parsed {} news articles from {} CSV records.", articles.size(), recordCount);

        } // StringReader and CSVParser are automatically closed here
        return articles;
    }
}