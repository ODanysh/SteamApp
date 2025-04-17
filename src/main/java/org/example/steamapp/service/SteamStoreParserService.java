package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.Genre;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
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
        // Create driver to load store page
        List<Game> recommendedGames = new ArrayList<>();
        WebDriver driver = getWebDriver();

        // Find top games in each genre
        int genreCount = 0;
        try {
            for (String genre : genres) {
                if (genreCount >= genreLimit) break;
                genreCount++;
                String url = "https://store.steampowered.com/category/" + genre.toLowerCase();
                driver.get(url);


                // Scroll down to trigger lazy loading
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollBy(0, 1000)");

                List<WebElement> gameElements = driver.findElements(By.cssSelector("a[href*='/app/']"));

                //Waiting for elements to load
                if (gameElements.isEmpty()) {
                    try {
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(7));
                        wait.until(_ -> !driver.findElements(By.cssSelector("a[href*='/app/']")).isEmpty());
                        gameElements = driver.findElements(By.cssSelector("a[href*='/app/']"));
                    } catch (TimeoutException e) {
                        System.out.println("No game elements found after waiting 7 seconds");
                    }
                }


                int count = 0;
                for (WebElement gameElement : gameElements) {
                    if (count >= limit) break;

                    String href = gameElement.getAttribute("href");

                    // Extract game ID from url
                    Matcher matcher = Pattern.compile("/app/(\\d+)/").matcher(href);
                    if (matcher.find()) {
                        String gameId = matcher.group(1);
                        count++;
                        // Get Game info from id
                        Game game = steamApiService.getGameInfoById(Long.valueOf(gameId));
                        recommendedGames.add(game);

                    }
                }
            }

        }
        catch (Exception e) {
            System.err.println("Error scraping Steam: " + e.getMessage());
        }
        finally {
            driver.quit();
        }
        return recommendedGames;
    }

    private static WebDriver getWebDriver() {
        // Set up WebDriver
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled"); // Avoid detection
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        return new ChromeDriver(options);
    }
}

