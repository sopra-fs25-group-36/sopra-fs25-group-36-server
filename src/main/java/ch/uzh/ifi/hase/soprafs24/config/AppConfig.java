package ch.uzh.ifi.hase.soprafs24.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // Marks this class for configuration
public class AppConfig {

    @Bean // Tells Spring to manage the object returned by this method
    public RestTemplate restTemplate() {
        return new RestTemplate(); // Creates the RestTemplate instance
    }
}