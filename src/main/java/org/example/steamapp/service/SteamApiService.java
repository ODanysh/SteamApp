package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Genre;
import org.example.steamapp.service.SteamStoreParserService;
import org.example.steamapp.config.AppConfig;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.UserProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SteamApiService {

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final SteamStoreParserService steamStoreParserService;

    public SteamApiService(RestTemplate restTemplate, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
        this.objectMapper = new ObjectMapper();
        this.steamStoreParserService = new SteamStoreParserService();
    }

    public UserProfile getUserProfile(String steamId) {
        try {
            // Get user summary
            String userSummaryUrl = String.format(
                    "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
                    appConfig.getSteamApiKey(), steamId);

            String userSummaryResponse = restTemplate.getForObject(userSummaryUrl, String.class);
            JsonNode userSummaryJson = objectMapper.readTree(userSummaryResponse);

            JsonNode playerNode = userSummaryJson.path("response").path("players").get(0);
            log.info(playerNode.asText());
            String username = playerNode.path("personaname").asText();
            String avatarUrl = playerNode.path("avatarfull").asText();

            // Get owned games
            String ownedGamesUrl = String.format(
                    "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=%s&steamid=%s&format=json&include_appinfo=true",
                    appConfig.getSteamApiKey(), steamId);

            String ownedGamesResponse = restTemplate.getForObject(ownedGamesUrl, String.class);
            JsonNode ownedGamesJson = objectMapper.readTree(ownedGamesResponse);

            List<Game> games = new ArrayList<>();
            JsonNode gamesNodes = ownedGamesJson.path("response").path("games");

            if (gamesNodes.isArray()) {
                for (JsonNode gameNode : gamesNodes) {
                    log.info("Game: {}", gameNode.path("name").asText());
                    Game game = Game.builder()
                            .appId(gameNode.path("appid").asLong())
                            .name(gameNode.path("name").asText())
                            .playtimeForever(gameNode.path("playtime_forever").asInt())
                            //.imageUrl(String.format("https://media.steampowered.com/steamcommunity/public/images/apps/%d/%s.jpg",
                            .imageUrl(String.format("https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/%s/header.jpg",
                                    gameNode.path("appid").asLong()))
                            .build();
                    games.add(game);
                }
            }

            return UserProfile.builder()
                    .steamId(steamId)
                    .username(username)
                    .avatarUrl(avatarUrl)
                    .games(games)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error fetching user profile from Steam API", e);
        }
    }

    public List<Game> getTopGames(UserProfile userProfile, int limit) {
        return userProfile.getGames().stream()
                .sorted(Comparator.comparing(Game::getPlaytimeForever).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
