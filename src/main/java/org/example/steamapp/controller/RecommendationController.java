package org.example.steamapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.GameRecommendation;
import org.example.steamapp.model.Game;
import org.example.steamapp.service.ExcelReportService;
import org.example.steamapp.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ExcelReportService excelReportService;
    private final Map<String, GameRecommendation> recommendationCache = new ConcurrentHashMap<>();

    public RecommendationController(RecommendationService recommendationService, ExcelReportService excelReportService) {
        this.recommendationService = recommendationService;
        this.excelReportService = excelReportService;
    }

    @GetMapping("/")
    public String homePage() {
        return "index";
    }

    @PostMapping("/recommendations")
    public String getRecommendations(@RequestParam String steamId, Model model) {
        GameRecommendation recommendation = recommendationService.generateRecommendations(steamId);

        List<Game> topGames = recommendation.getUserProfile().getGames().stream()
                .sorted(Comparator.comparing(Game::getPlaytimeForever).reversed())
                .limit(15)
                .collect(Collectors.toList());

        recommendationCache.put(steamId, recommendation);

        model.addAttribute("recommendation", recommendation);
        model.addAttribute("topGames", topGames);
        return "recommendations";
    }

    @GetMapping("/api/recommendations/{steamId}")
    @ResponseBody
    public GameRecommendation getRecommendationsApi(@PathVariable String steamId) {
        /*
        // Check if we have cached recommendations
        GameRecommendation cachedRecommendation = recommendationCache.get(steamId);
        if (cachedRecommendation != null) {
            return cachedRecommendation;
        }

        // Generate new recommendations if not in cache
        GameRecommendation recommendation = recommendationService.generateRecommendations(steamId);
        recommendationCache.put(steamId, recommendation);
        return recommendation;*/

        return recommendationService.generateRecommendations(steamId);
    }

    @GetMapping("/download-report/{steamId}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String steamId) {
        try {
            GameRecommendation recommendation = recommendationCache.get(steamId);
            if (recommendation.getRecommendedGames().isEmpty()) {
                log.info("cached recommendation is EMPTY!");
                recommendation = recommendationService.generateRecommendations(steamId);
            }
            recommendationCache.put(steamId, recommendation);
            //GameRecommendation recommendation = recommendationService.generateRecommendations(steamId);
            byte[] report = excelReportService.generateReport(recommendation);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=game-recommendations.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(report);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
