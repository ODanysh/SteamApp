package org.example.steamapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRecommendation {
    private UserProfile userProfile;
    private Map<String, Integer> favoriteGenres;
    private List<Game> recommendedGames;
}

