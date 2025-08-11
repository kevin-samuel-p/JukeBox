package controllers;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.File;
import java.util.Collections;
import java.util.List;

import services.SettingsService;
import services.TrackService;


public class LibraryController {
    private static final Image DISK_ICON = new Image("/assets/disk_icon.png");
    
    @FXML private ScrollPane libraryScrollPane;
    @FXML private FlowPane libraryFlowPane;

    @FXML
    private void initialize() {

        // Set theme and listener for theme change
        Platform.runLater(() -> {
            updateBackgroundColor();
            SettingsService.getInstance().themeProperty()
                           .addListener((obs, oldTheme, newTheme) -> updateBackgroundColor());
        });

        if (TrackService.getInstance().isMusicFolderEmpty()) {
            Label noTracks = new Label("No tracks found.");
            noTracks.setStyle("-fx-text-fill: gray; -fx-font-size: 16;");
            libraryFlowPane.getChildren().add(noTracks);
            return;
        }

        List<File> list = Collections.unmodifiableList(TrackService.getInstance().getTrackList());
        // XXX: Find a different, more memory-friendly way to create trackButtons
        // XXX: Scroll lag
        // FIXME: Implement lazy loading on scrollpane/tile pane
        for (int i = 0; i < list.size(); i++) {
            File track = list.get(i);
            VBox trackButton = createTrackButton(track);
            trackButton.setId(Integer.toString(i));
            trackButton.setOnMouseClicked(event -> {
                TrackService.getInstance().setCurrentTrackIndex(Integer.parseInt(trackButton.getId()));
                System.out.println("Selected: " + track.getAbsolutePath()); // TODO: Potentially create a user log
            });
            libraryFlowPane.getChildren().add(trackButton);
        }
    }

    private VBox createTrackButton(File track) {
        ImageView imageView = new ImageView(DISK_ICON);
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);

        String trackName = track.getName();
        Label label = new Label(trackName.substring(0, trackName.length() - 4));
        label.setMaxWidth(120);

        Region spacer = new Region();
        Region spacer2 = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        VBox trackButton = new VBox(imageView, spacer, label, spacer2);
        trackButton.getStyleClass().add("tile-button");
        // trackButton.setMaxHeight(150);

        // To clip ImageView inside VBox 
        Rectangle clip = new Rectangle(120, 120 + label.getMaxHeight()); 
        clip.setArcWidth(24);
        clip.setArcHeight(24);
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
        }); 

        return trackButton;
    }

    private void updateBackgroundColor() {
        StringBuilder scrollPaneStyle = new StringBuilder();
        libraryScrollPane.lookup(".viewport").setStyle(
               scrollPaneStyle.append("-fx-background-color: ")
                              .append("linear-gradient(to bottom, ")
                              .append(SettingsService.getInstance().getTheme())
                              .append(" 0%, transparent 100%), ")
                              .append("radial-gradient(focus-angle 0deg, focus-distance 0%, center 50% 50%, radius 80%, ")
                              .append("#14141480 30%, #14141480 30%, #00000080 60%);")
                            .toString());
    }
}