package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.Genre;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.stereotype.Service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SteamStoreParserService {

    public List<Genre> getGameGenres(Long appId) {
        try {
            log.info("Getting Steam Genres");
            String url = "https://store.steampowered.com/app/" + appId;
            log.info("Steam Genres: {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            //Elements genreElements = doc.select(".game_details_content a[href*='/tags/']");
            Elements genreElements = doc.select(".app_tag");
            log.info("Found {} elements", genreElements.size());
            List<Genre> genres = new ArrayList<>();

            //List<String> genres = new ArrayList<>();
            for (Element genreElement : genreElements) {
                genres.add(new Genre(genreElement.attr("href"), genreElement.text()));
                log.info("Added genre {}", genreElement.attr("href"));
                if (genres.size() >= 5) {
                    break;
                }
            }
            log.info("return genres");
            return genres;
        } catch (IOException e) {
            System.err.println("Error parsing game details for appId: " + appId);
            System.err.println("Error parsing : " + e);
            return Collections.emptyList();
        }
    }

    public Game getGameInfoById(Long appId) {
        try {
            log.info("Getting Steam game info by appId: {}", appId);
            String url = "https://store.steampowered.com/app/" + appId;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .timeout(10000)
                    .followRedirects(false)
                    .get();

            //Elements genreElements = doc.select(".game_details_content a[href*='/tags/']");
            Elements genreElements = doc.select(".app_tag");
            log.info("First try. Found {} elements", genreElements.size());

            if(genreElements.isEmpty())
            {
                // Set up cookies directly to bypass age check
                Map<String, String> cookies = new HashMap<>();
                cookies.put("birthtime", "283993201"); // January 1, 1979
                cookies.put("mature_content", "1");    // Confirm we're mature
                cookies.put("lastagecheckage", "1-1-1979"); // Last check date
                cookies.put("wants_mature_content", "1");   // We want mature content

                // Access the page with pre-set cookies
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                        .cookies(cookies)
                        .referrer("http://www.google.com")
                        .ignoreHttpErrors(true)
                        .timeout(10000)
                        .followRedirects(true)
                        .get();
            }

            genreElements = doc.select(".app_tag");

            log.info("Second try. Found {} elements", genreElements.size());

            if(genreElements.isEmpty())
            {
                log.info("No genres found for appId: {}", doc);
            }

            List<Genre> genres = new ArrayList<>();
            for (Element genreElement : genreElements) {
                genres.add(new Genre(genreElement.attr("href"), genreElement.text()));
                log.info("Added genre {}", genreElement.attr("href"));
                if (genres.size() >= 5) {
                    break;
                }
            }

            String gameName = doc.select(".apphub_AppName").first().text();

            String imageUrl = "https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/" + appId + "/header.jpg";

            Game game = Game.builder()
                    .appId(appId)
                    .name(gameName)
                    .imageUrl(imageUrl)
                    .genres(genres)
                    .build();

            return game;
        } catch (IOException e) {
            System.err.println("Error parsing game details for appId: " + appId);
            System.err.println("Error parsing : " + e);
            return new Game();
        }
    }

    public List<Game> findTopGamesByGenres(List<String> genres, int limit, int genreLimit) {
        log.info("Finding top games by genres");
        log.info("Genres: {}", genres);

        List<Game> recommendedGames = new ArrayList<>();
        Set<Long> processedAppIds = new HashSet<>();
        List<Genre> listOfGenres = new ArrayList<>();
        log.info("1");

        // Set up Chrome WebDriver
        //System.setProperty("webdriver.chrome.driver", "path/to/chromedriver"); // Update this path

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled"); // Avoid detection
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        int genreCount = 0;
        try {
            for (String genre : genres) {
                if (genreCount >= genreLimit) break;
                genreCount++;
                String url = "https://store.steampowered.com/category/" + genre.toLowerCase();
                driver.get(url);

                log.info("genre: {}", genre);

                // Scroll down to trigger lazy loading
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollBy(0, 500)");
                Thread.sleep(1000);
                js.executeScript("window.scrollBy(0, 500)");
                Thread.sleep(1000);


                // Wait for the page to load the game elements
                /*wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("._3EdZTDIisUpowxwm6uJ7Iq")
                ));*/

                log.info("Trying to find top games by genre {}", genre);

                // Find all links containing "/app/" which likely point to games
                List<WebElement> gameElements = driver.findElements(By.cssSelector("a[href*='/app/']"));
                log.info("Found " + gameElements.size() + " game links");


                log.info("Found " + gameElements.size() + " game elements");

                int count = 0;
                for (WebElement gameElement : gameElements) {
                    if (count >= limit) break;

                    //WebElement linkElement = gameElement.findElement(By.cssSelector("a"));
                    String href = gameElement.getAttribute("href");
                    log.info("Found href: {}", href);

                    // Extract game ID using regex
                    Matcher matcher = Pattern.compile("/app/(\\d+)/").matcher(href);
                    if (matcher.find()) {
                        String gameId = matcher.group(1);
                        log.info("Found gameId: {}", gameId);
                        count++;
                        if(!recommendedGames.isEmpty() && Long.valueOf(gameId).equals(recommendedGames.getLast().getAppId())) continue;
                        if(!recommendedGames.isEmpty() && Long.valueOf(gameId).equals(recommendedGames.getLast().getAppId())) log.info("same ids");
                        Game game = getGameInfoById(Long.valueOf(gameId));
                        recommendedGames.add(game);
                        for(Game game1 : recommendedGames) {
                            log.info("recommended games before: {}", game.getName());
                        }

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
}

