package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Genre;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;


@Slf4j
@Service
public class SteamGameParserService {

    public List<Genre> getGameGenres(Long appId) {
        try {
            // Set up cookies directly to bypass age check
            Map<String, String> cookies = new HashMap<>();
            cookies.put("birthtime", "283993201"); // January 1, 1979
            cookies.put("mature_content", "1");    // Confirm we're mature
            cookies.put("lastagecheckage", "1-1-1979"); // Last check date
            cookies.put("wants_mature_content", "1");   // We want mature content

            log.info("Getting Steam Genres");
            String url = "https://store.steampowered.com/app/" + appId;
            log.info("Steam Genres: {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                    .cookies(cookies)
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            Elements genreElements = doc.select(".app_tag");
            log.info("Found {} elements", genreElements.size());
            List<Genre> genres = new ArrayList<>();

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
}
