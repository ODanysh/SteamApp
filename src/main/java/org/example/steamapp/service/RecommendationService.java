package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.GameRecommendation;
import org.example.steamapp.model.Genre;
import org.example.steamapp.model.UserProfile;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    private final SteamApiService steamApiService;
    private final SteamStoreParserService steamStoreParserService;
    private final SteamGameParserService steamGameParserService;

    public RecommendationService(SteamApiService steamApiService, SteamStoreParserService steamStoreParserService, SteamGameParserService steamGameParserService) {
        this.steamApiService = steamApiService;
        this.steamStoreParserService = steamStoreParserService;
        this.steamGameParserService = steamGameParserService;
    }

    public List<Game> getTopGames(UserProfile userProfile, int limit) {
        return userProfile.getGames().stream()
                .sorted(Comparator.comparing(Game::getPlaytimeForever).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public GameRecommendation generateRecommendations(String steamId) {
        // Get user profile
        log.info("Geting user info for {}", steamId);
        UserProfile userProfile = steamApiService.getUserProfile(steamId);


        // Get top 15 games by playtime
        log.info("Getting top played games");
        List<Game> topGames = getTopGames(userProfile, 15);

        // Get genres for this 15 games
        log.info("Finding most played genres");
        for (Game game : topGames) {
            List<Genre> genres = steamGameParserService.getGameGenres(game.getAppId());
            game.setGenres(genres);
        }

        // Sum-up genres
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

        // Get top 6 genres
        List<String> topGenres = favoriteGenres.keySet().stream()
                .limit(6)
                .collect(Collectors.toList());

        // Find games in favorite genres
        log.info("Getting game recommendations");
        List<Game> recommendedGames = steamStoreParserService.findTopGamesByGenres(topGenres, 6, 3);

        // Get owned games ID`s
        Set<Long> ownedGameIds = userProfile.getGames().stream()
                .map(Game::getAppId)
                .collect(Collectors.toSet());

        // Filter out games the user already owns
        recommendedGames = recommendedGames.stream()
                .filter(game -> !ownedGameIds.contains(game.getAppId())).toList();
                //.collect(Collectors.toList());

        //Filter out duplicates
        recommendedGames = new ArrayList<>(
                recommendedGames.stream()
                        .collect(Collectors.toMap(
                                Game::getAppId,
                                Function.identity(),
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ))
                        .values()
        );

        // Get genres for recommended games
        log.info("Loading genres");
        for (Game game : recommendedGames) {
            List<Genre> genres = steamGameParserService.getGameGenres(game.getAppId());
            game.setGenres(genres);
        }

        return GameRecommendation.builder()
                .userProfile(userProfile)
                .favoriteGenres(favoriteGenres)
                .recommendedGames(recommendedGames)
                .build();
    }
}

