package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.config.AppConfig;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.UserProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class SteamApiService {

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public SteamApiService(RestTemplate restTemplate, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
        this.objectMapper = new ObjectMapper();
    }

    public UserProfile getUserProfile(String steamId) {
        try {
            // Get user info
            String userSummaryUrl = String.format(
                    "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
                    appConfig.getSteamApiKey(), steamId);

            String userSummaryResponse = restTemplate.getForObject(userSummaryUrl, String.class);
            JsonNode userSummaryJson = objectMapper.readTree(userSummaryResponse);
            JsonNode playerNode = userSummaryJson.path("response").path("players").get(0);
            log.info(playerNode.asText());
            String username = playerNode.path("personaname").asText();
            String avatarUrl = playerNode.path("avatarfull").asText();

            // Get user owned games
            String ownedGamesUrl = String.format(
                    "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=%s&steamid=%s&format=json&include_appinfo=true",
                    appConfig.getSteamApiKey(), steamId);

            String ownedGamesResponse = restTemplate.getForObject(ownedGamesUrl, String.class);
            JsonNode ownedGamesJson = objectMapper.readTree(ownedGamesResponse);
            JsonNode gamesNodes = ownedGamesJson.path("response").path("games");

            // Create a list of games from json
            List<Game> games = new ArrayList<>();
            if (gamesNodes.isArray()) {
                for (JsonNode gameNode : gamesNodes) {
                    Game game = Game.builder()
                            .appId(gameNode.path("appid").asLong())
                            .name(gameNode.path("name").asText())
                            .playtimeForever(gameNode.path("playtime_forever").asInt())
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

    public Game getGameInfoById(Long appId) {
        try {
            // Get game info
            String gameUrl = "https://store.steampowered.com/api/appdetails?appids=" + appId +"&l=en";

            String gameResponse = restTemplate.getForObject(gameUrl, String.class);
            JsonNode gameJson = objectMapper.readTree(gameResponse);

            String gameName = gameJson.path(appId.toString()).path("data").path("name").asText();
            String imageUrl = gameJson.path(appId.toString()).path("data").path("header_image").asText();

            return Game.builder()
                    .appId(appId)
                    .name(gameName)
                    .imageUrl(imageUrl)
                    .build();

        } catch (IOException e) {
            System.err.println("Error parsing game details for appId: " + appId);
            System.err.println("Error parsing : " + e);
            return new Game();
        }
    }
}
