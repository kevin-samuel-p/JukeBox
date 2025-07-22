package controllers;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

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
            noTracks.setStyle("-fx-text-fill: gray; -fx-font-size: 16;");
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
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);

        String trackName = track.getName();
        Label label = new Label(trackName.substring(0, trackName.length() - 4));
        label.setMaxWidth(120);

        VBox trackButton = new VBox(imageView, label);
        trackButton.getStyleClass().add("tile-button");

        // To clip ImageView inside VBox
        double arc = 12; 
        Rectangle clip = new Rectangle(120, 120 + label.getMaxHeight()); 
        clip.setArcWidth(arc * 2);
        clip.setArcHeight(arc * 2);
        trackButton.setClip(clip);

        trackButton.layoutBoundsProperty().addListener((obs, old, bounds) -> {
            clip.setWidth(bounds.getWidth());
            clip.setHeight(bounds.getHeight());
        });

        // Create the transition once
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(500), trackButton);
        scaleUp.setToX(1.05);
        scaleUp.setToY(1.05);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(500), trackButton);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        trackButton.setOnMouseEntered(e -> {
            scaleDown.stop();
            scaleUp.playFromStart();
            trackButton.setClip(null); // remove clip for jagged expansion
            label.setWrapText(true);
        });
        trackButton.setOnMouseExited(e -> {
            scaleUp.stop();
            scaleDown.playFromStart();
            trackButton.setClip(clip); // restore rounded corners
            label.setWrapText(false);
        }); // TODO: Fix upper unclipped part peeking out on pop-out

        return trackButton;
    }
}