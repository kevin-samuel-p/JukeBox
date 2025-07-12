package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import services.TrackService;


public class LibraryController {
    
    @FXML private VBox trackListContainer;

    @FXML
    private void initialize() {
        Path musicFolderPath = Paths.get(System.getProperty("user.home"), "Music");
        loadTracksFromFolder(musicFolderPath);
    }

    private void loadTracksFromFolder(Path folderPath) {
        File[] mp3Files = folderPath.toFile().listFiles(
            (dir, name) -> name.toLowerCase().endsWith(".mp3"));

        trackListContainer.getChildren().clear();

        if (mp3Files == null || mp3Files.length == 0) {
            Label noTracks = new Label("No tracks found.");
            noTracks.setStyle("-fx-text-fill: gray;");
            trackListContainer.getChildren().add(noTracks);
            return;
        }

        for (File file : mp3Files) {
            Button trackButton = new Button(file.getName());
            trackButton.setMaxWidth(Double.MAX_VALUE);
            trackButton.setOnAction(e -> {
                // TODO: Notify PlayerController or App about selected track
                TrackService.getInstance().setSelectedTrack(file);
                System.out.println("Selected: " + file.getAbsolutePath());
            });
            trackListContainer.getChildren().add(trackButton);
        }
    }
}