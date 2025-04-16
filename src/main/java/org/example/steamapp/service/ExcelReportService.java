package org.example.steamapp.service;

import lombok.extern.slf4j.Slf4j;
import org.example.steamapp.model.Game;
import org.example.steamapp.model.Genre;
import org.example.steamapp.model.GameRecommendation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class ExcelReportService {

    public byte[] generateReport(GameRecommendation recommendation) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create user info sheet
            Sheet userSheet = workbook.createSheet("User Info");

            Row userHeaderRow = userSheet.createRow(0);
            userHeaderRow.createCell(0).setCellValue("Steam ID");
            userHeaderRow.createCell(1).setCellValue("Username");
            userHeaderRow.getCell(0).setCellStyle(headerStyle);
            userHeaderRow.getCell(1).setCellStyle(headerStyle);

            Row userDataRow = userSheet.createRow(1);
            userDataRow.createCell(0).setCellValue(recommendation.getUserProfile().getSteamId());
            userDataRow.createCell(1).setCellValue(recommendation.getUserProfile().getUsername());

            // Create favorite genres sheet
            Sheet genresSheet = workbook.createSheet("Favorite Genres");

            Row genresHeaderRow = genresSheet.createRow(0);
            genresHeaderRow.createCell(0).setCellValue("Genre");
            genresHeaderRow.createCell(1).setCellValue("Count");
            genresHeaderRow.getCell(0).setCellStyle(headerStyle);
            genresHeaderRow.getCell(1).setCellStyle(headerStyle);

            int genreRowIndex = 1;
            for (Map.Entry<String, Integer> entry : recommendation.getFavoriteGenres().entrySet()) {
                Row genreRow = genresSheet.createRow(genreRowIndex++);
                genreRow.createCell(0).setCellValue(entry.getKey());
                genreRow.createCell(1).setCellValue(entry.getValue());
            }

            // Create top played games sheet
            Sheet topGamesSheet = workbook.createSheet("Top Played Games");

            Row topGamesHeaderRow = topGamesSheet.createRow(0);
            topGamesHeaderRow.createCell(0).setCellValue("Game ID");
            topGamesHeaderRow.createCell(1).setCellValue("Name");
            topGamesHeaderRow.createCell(2).setCellValue("Playtime (hours)");
            topGamesHeaderRow.createCell(3).setCellValue("Genres");
            topGamesHeaderRow.getCell(0).setCellStyle(headerStyle);
            topGamesHeaderRow.getCell(1).setCellStyle(headerStyle);
            topGamesHeaderRow.getCell(2).setCellStyle(headerStyle);
            topGamesHeaderRow.getCell(3).setCellStyle(headerStyle);

            int topGameRowIndex = 1;
            for (Game game : recommendation.getUserProfile().getGames().stream()
                    .sorted((g1, g2) -> Integer.compare(g2.getPlaytimeForever(), g1.getPlaytimeForever()))
                    .limit(10)
                    .toList()) {
                Row gameRow = topGamesSheet.createRow(topGameRowIndex++);
                gameRow.createCell(0).setCellValue(game.getAppId());
                gameRow.createCell(1).setCellValue(game.getName());
                gameRow.createCell(2).setCellValue(game.getPlaytimeForever() / 60.0); // Convert minutes to hours

                String genres = "";
                for (Genre genre : game.getGenres().stream().toList()) {
                    genres = genre.getText() != null ? String.join(", ", genre.getText()) : "";
                }
                gameRow.createCell(3).setCellValue(genres);
            }

            // Create recommendations sheet
            Sheet recommendationsSheet = workbook.createSheet("Recommendations");

            Row recommendationsHeaderRow = recommendationsSheet.createRow(0);
            recommendationsHeaderRow.createCell(0).setCellValue("Game ID");
            recommendationsHeaderRow.createCell(1).setCellValue("Name");
            recommendationsHeaderRow.createCell(2).setCellValue("Matching Genre");
            recommendationsHeaderRow.getCell(0).setCellStyle(headerStyle);
            recommendationsHeaderRow.getCell(1).setCellStyle(headerStyle);
            recommendationsHeaderRow.getCell(2).setCellStyle(headerStyle);

            int recRowIndex = 1;
            for (Game game : recommendation.getRecommendedGames()) {
                Row recRow = recommendationsSheet.createRow(recRowIndex++);
                recRow.createCell(0).setCellValue(game.getAppId());
                recRow.createCell(1).setCellValue(game.getName());

                String genres = "";
                for (Genre genre : game.getGenres().stream().toList()) {
                    genres = genre.getText() != null ? String.join(", ", genre.getText()) : "";
                }
                recRow.createCell(2).setCellValue(genres);
            }

            // Auto size columns for all sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int j = 0; j < 4; j++) {
                    sheet.autoSizeColumn(j);
                }
            }

            // Write to output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
