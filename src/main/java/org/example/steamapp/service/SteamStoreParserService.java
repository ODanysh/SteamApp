package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.Genre;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SteamStoreParserService {


    private final SteamApiService steamApiService;

    @Autowired
    public SteamStoreParserService(SteamApiService steamApiService) {
        this.steamApiService = steamApiService;
    }

    public List<Game> findTopGamesByGenres(List<String> genres, int limit, int genreLimit) {
        log.info("Finding top games by genres");
        log.info("Genres: {}", genres);

        List<Game> recommendedGames = new ArrayList<>();
        Set<Long> processedAppIds = new HashSet<>();
        List<Genre> listOfGenres = new ArrayList<>();
        log.info("1");

        WebDriver driver = getWebDriver();

        int genreCount = 0;
        try {
            for (String genre : genres) {
                if (genreCount >= genreLimit) break;
                genreCount++;
                String url = "https://store.steampowered.com/category/" + genre.toLowerCase();
                log.info("url: {}", url);
                driver.get(url);

                log.info("genre: {}", genre);

                // Scroll down to trigger lazy loading
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollBy(0, 1000)");
                Thread.sleep(1000);


                // Wait for the page to load the game elements
                // Do a cycle where checking if found game elements
                // If not sleep for another 100-1000 and try to parse again

                log.info("Trying to find top games by genre {}", genre);

                // Find all links containing "/app/" which likely point to games
                List<WebElement> gameElements = driver.findElements(By.cssSelector("a[href*='/app/']"));
                log.info("Found " + gameElements.size() + " game links");


                log.info("Found " + gameElements.size() + " game elements");

                int count = 0;
                for (WebElement gameElement : gameElements) {
                    if (count >= limit) break;

                    String href = gameElement.getAttribute("href");
                    log.info("Found href: {}", href);

                    // Extract game ID using regex
                    Matcher matcher = Pattern.compile("/app/(\\d+)/").matcher(href);
                    if (matcher.find()) {
                        String gameId = matcher.group(1);
                        log.info("Found gameId: {}", gameId);
                        count++;
                        Game game = steamApiService.getGameInfoById(Long.valueOf(gameId));
                        log.info("Found game");
                        recommendedGames.add(game);
                        log.info("recommended games before: {}", game.getName());

                    }
                }
            }

        }
        catch (Exception e) {
            System.err.println("Error scraping Steam: " + e.getMessage());
        }
        finally {
            // Clean up
            driver.quit();
        }
        for(Game game : recommendedGames) {
            log.info("recommended games after: {}", game.getName());
        }
        return recommendedGames;
    }

    private static WebDriver getWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled"); // Avoid detection
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        return driver;
    }
}

