package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;

import services.TrackService;


public class LibraryController {
    
    @FXML private VBox trackListContainer;

    @FXML
    private void initialize() { 
        if (TrackService.getInstance().isMusicFolderEmpty()) {
            Label noTracks = new Label("No tracks found.");
            noTracks.setStyle("-fx-text-fill: gray;");
            trackListContainer.getChildren().add(noTracks);
            return;
        }

        for (File track : TrackService.getInstance().getTrackList()) {
            Button trackButton = new Button(track.getName());
            trackButton.setMaxWidth(Double.MAX_VALUE);
            trackButton.setOnAction(event -> {
                TrackService.getInstance().setSelectedTrack(track);
                System.out.println("Selected: " + track.getAbsolutePath()); // TODO: Potentially create a user log
            });
            trackListContainer.getChildren().add(trackButton);
        }
    }
}