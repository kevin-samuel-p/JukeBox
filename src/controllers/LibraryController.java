package controllers;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.File;
import java.util.Collections;
import java.util.List;

import services.SettingsService;
import services.TrackService;


@SuppressWarnings("unused")
public class LibraryController {
    private static final Image DISK_ICON = new Image("/assets/disk_icon.png");
    
    @FXML private ScrollPane libraryScrollPane;
    @FXML private TilePane libraryTilePane;

    @FXML
    private void initialize() {

        // Configure scroll bar
        libraryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        // Set theme and listener for theme change
        Platform.runLater(() -> {
            updateBackgroundColor();
            SettingsService.getInstance().themeProperty()
                           .addListener((obs, oldTheme, newTheme) -> updateBackgroundColor());
        });

        if (TrackService.getInstance().isMusicFolderEmpty()) {
            Label noTracks = new Label("No tracks found.");
            noTracks.setStyle("-fx-text-fill: gray; -fx-font-size: 16;");
            libraryTilePane.getChildren().add(noTracks);
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
            libraryTilePane.getChildren().add(trackButton);
        }
    }

    private VBox createTrackButton(File track) {
        ImageView imageView = new ImageView(DISK_ICON);
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);

        String trackName = track.getName();
        Text text = new Text(trackName.substring(0, trackName.length() - 4));
        text.setFill(Color.WHITE);
        text.setFont(Font.font(12));

        StackPane labelPane = new StackPane(text);
        labelPane.setMinWidth(120);
        labelPane.setMaxWidth(120);
        labelPane.setPrefWidth(120);
        labelPane.setPrefHeight(40);
        labelPane.setPadding(new Insets(4, 8, 4, 8));
        labelPane.setAlignment(Pos.CENTER_LEFT);

        Rectangle miniClip = new Rectangle(120, 40);
        labelPane.setClip(miniClip);

        VBox trackButton = new VBox(imageView, labelPane);
        trackButton.getStyleClass().add("tile-button");
        trackButton.setMaxHeight(170);

        // To clip ImageView inside VBox 
        Rectangle clip = new Rectangle(120, 120 + labelPane.getMaxHeight()); 
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

        // Create scroll animation
        TranslateTransition scroll = new TranslateTransition(Duration.seconds(2.5), text);
        double textWidth = text.getLayoutBounds().getWidth();
        scroll.setInterpolator(Interpolator.LINEAR);
        scroll.setCycleCount(Animation.INDEFINITE);
        scroll.setAutoReverse(true);

        trackButton.setOnMouseEntered(e -> {
            scaleDown.stop();
            scaleUp.playFromStart();
            trackButton.setClip(null); // remove clip for jagged expansion
            
            if (textWidth > 120) {
                scroll.setFromX(0);
                scroll.setToX(-(textWidth - 100));
                scroll.playFromStart();
            }
        });
        trackButton.setOnMouseExited(e -> {
            scaleUp.stop();
            scaleDown.playFromStart();
            trackButton.setClip(clip); // restore rounded corners
            
            scroll.stop();
            text.setTranslateX(0);
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