package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Collections;
import java.util.List;

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

        List<File> list = Collections.unmodifiableList(TrackService.getInstance().getTrackList());
        // for (File track : TrackService.getInstance().getTrackList())
        for (int i = 0; i < list.size(); i++) {
            File track = list.get(i);
            Button trackButton = new Button(track.getName());
            trackButton.setId(Integer.toString(i));
            trackButton.setMaxWidth(Double.MAX_VALUE);
            trackButton.setOnAction(event -> {
                // TrackService.getInstance().setSelectedTrack(track);
                TrackService.getInstance().setCurrentTrackIndex(Integer.parseInt(trackButton.getId()));
                System.out.println("Selected: " + track.getAbsolutePath()); // TODO: Potentially create a user log
            });
            trackListContainer.getChildren().add(trackButton);
        }
    }
}