package controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Collections;
import java.util.List;

import services.TrackService;


public class LibraryController {
    
    @FXML private TilePane libraryTilePane;

    @FXML
    private void initialize() { 
        if (TrackService.getInstance().isMusicFolderEmpty()) {
            Label noTracks = new Label("No tracks found.");
            noTracks.setStyle("-fx-text-fill: gray;");
            libraryTilePane.getChildren().add(noTracks);
            return;
        }

        List<File> list = Collections.unmodifiableList(TrackService.getInstance().getTrackList());
        // for (File track : TrackService.getInstance().getTrackList())
        for (int i = 0; i < list.size(); i++) {
            File track = list.get(i);
            VBox trackButton = createTrackButton(track);
            trackButton.setId(Integer.toString(i));
            trackButton.setOnMouseClicked(event -> {
                TrackService.getInstance().setCurrentTrackIndex(Integer.parseInt(trackButton.getId()));
                System.out.println("Selected: " + track.getAbsolutePath()); // TODO: Potentially create a user log
            });
            libraryTilePane.getChildren().add(trackButton);
        }
    }

    private VBox createTrackButton(File track) {
        ImageView imageView = new ImageView(new Image("/assets/disk_icon.png"));
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);

        Label label = new Label(track.getName());
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

        VBox trackButton = new VBox(imageView, label);
        trackButton.setAlignment(Pos.CENTER);
        trackButton.setStyle("-fx-background-color: #2d2d2d; -fx-padding: 10px; -fx-background-radius: 10;");

        return trackButton;
    }
}