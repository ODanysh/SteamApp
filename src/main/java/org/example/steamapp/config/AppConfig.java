package org.example.steamapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;


@Configuration
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Value("${steam.api.key}")
    private String steamApiKey;

    public String getSteamApiKey() {
        return steamApiKey;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
