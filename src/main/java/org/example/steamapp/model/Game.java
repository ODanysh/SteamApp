package org.example.steamapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private Long appId;
    private String name;
    private int playtimeForever; // in minutes
    private List<Genre> genres;
    private String imageUrl;
}

