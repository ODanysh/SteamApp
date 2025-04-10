package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.GameRecommendation;
import org.example.steamapp.model.Genre;
import org.example.steamapp.model.UserProfile;
import org.example.steamapp.service.SteamApiService;
import org.example.steamapp.service.SteamStoreParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    private final SteamApiService steamApiService;
    private final SteamStoreParserService steamStoreParserService;

    public RecommendationService(SteamApiService steamApiService, SteamStoreParserService steamStoreParserService) {
        this.steamApiService = steamApiService;
        this.steamStoreParserService = steamStoreParserService;
    }

    public GameRecommendation generateRecommendations(String steamId) {
        // Get user profile
        UserProfile userProfile = steamApiService.getUserProfile(steamId);

        // Get top 15 games by playtime
        List<Game> topGames = steamApiService.getTopGames(userProfile, 15);

        // Get genres for each game
        for (Game game : topGames) {
            List<Genre> genres = steamStoreParserService.getGameGenres(game.getAppId());
            game.setGenres(genres);
        }

        // Calculate favorite genres
        Map<String, Integer> genreCount = new HashMap<>();
        for (Game game : topGames) {
            for (Genre genre : game.getGenres()) {
                String genreEdit = genre.getHref().split("/")[5].replaceAll("%20", " ").replaceAll("%26", "&");
                genreCount.put(genreEdit, genreCount.getOrDefault(genreEdit, 0) + 1);
            }
        }

        // Sort genres by frequency
        Map<String, Integer> favoriteGenres = genreCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Get top 5 genres
        List<String> topGenres = favoriteGenres.keySet().stream()
                .limit(6)
                .collect(Collectors.toList());

        // Find recommended games for these genres
        List<Game> recommendedGames = steamStoreParserService.findTopGamesByGenres(topGenres, 6, 3);

        for(Game game : recommendedGames) {
            log.info("recommended games befeore: {}", game.getName());
        }

        // Filter out games the user already owns
        Set<Long> ownedGameIds = userProfile.getGames().stream()
                .map(Game::getAppId)
                .collect(Collectors.toSet());

        recommendedGames = recommendedGames.stream()
                .filter(game -> !ownedGameIds.contains(game.getAppId())).toList();
                //.collect(Collectors.toList());

        //Filter duplicates

        recommendedGames = recommendedGames.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Game::getAppId,
                                game -> game,
                                (existing, replacement) -> existing
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        for(Game game : recommendedGames) {
            log.info("recommended games after: {}", game.getName());
        }


        return GameRecommendation.builder()
                .userProfile(userProfile)
                .favoriteGenres(favoriteGenres)
                .recommendedGames(recommendedGames)
                .build();
    }
}

