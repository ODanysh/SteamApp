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
            // Cookies to bypass age check
            Map<String, String> cookies = new HashMap<>();
            cookies.put("birthtime", "283993201");
            cookies.put("mature_content", "1");
            cookies.put("lastagecheckage", "1-1-1979");
            cookies.put("wants_mature_content", "1");

            // Parse steam game store
            String url = "https://store.steampowered.com/app/" + appId;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                    .cookies(cookies)
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .timeout(10000)
                    .followRedirects(true)
                    .get();

            // Get genres of the game
            Elements genreElements = doc.select(".app_tag");
            List<Genre> genres = new ArrayList<>();

            for (Element genreElement : genreElements) {
                genres.add(new Genre(genreElement.attr("href"), genreElement.text()));
                if (genres.size() >= 5) {
                    break;
                }
            }

            return genres;

        } catch (IOException e) {
            System.err.println("Error parsing game genres for appId: " + appId);
            System.err.println("Error parsing : " + e);
            return Collections.emptyList();
        }
    }
}
