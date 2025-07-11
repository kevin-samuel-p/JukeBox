package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Node;

public class PlayerController {

    @FXML private Label trackInfoLabel;
    @FXML private Button previousButton, playPauseButton, nextButton;
    @FXML private ToggleButton loopButton, shuffleButton;
    @FXML private Slider trackSlider, volumeSlider;
    @FXML private Label timestampLabel;

    private enum LoopMode {
        OFF, ALL, ONE
    }
    private LoopMode 
        currentLoopMode = LoopMode.OFF, 
        currentShuffleMode = LoopMode.OFF;

    @FXML
    private void initialize() {
        // Set track info label
        trackInfoLabel.setText("No track playing");

        // Volume slider color update
        Platform.runLater(() -> {
            updateSliderColor(volumeSlider.getValue());

            volumeSlider.valueProperty().addListener((_obs, _oldVal, newVal) -> {
                updateSliderColor(newVal.doubleValue());
            });
        });

        // Action on play/pause button
        playPauseButton.setOnAction(e -> togglePlayback());

        // Loop button actions
        updateLoopButton();
        loopButton.setOnAction(e -> {
            switch(currentLoopMode) {
                case OFF -> currentLoopMode = LoopMode.ALL;
                case ALL -> currentLoopMode = LoopMode.ONE;
                case ONE -> currentLoopMode = LoopMode.OFF;
            }
            updateLoopButton();
        });

        // Shuffle button actions
        updateShuffleButton();
        shuffleButton.setOnAction(e -> {
            switch(currentShuffleMode) {
                case OFF -> currentShuffleMode = LoopMode.ALL;
                default -> currentShuffleMode = LoopMode.OFF;
            }
            updateShuffleButton();
        });
    }

    private void togglePlayback() {
        if (playPauseButton.getText().equals("▶")) {
            playPauseButton.setText("II");
            trackInfoLabel.setText("Playing: Chill Beats - Lofi Girl");
        } else {
            playPauseButton.setText("▶");
            trackInfoLabel.setText("Paused: Chill Beats - Lofi Girl");
        }
    }

    private void updateSliderColor(double value) {
        double percent = value / volumeSlider.getMax() * 100;

        Node track = volumeSlider.lookup(".track");
        if (track != null) {
            track.setStyle(String.format(
                    "-fx-background-color: linear-gradient(to right, #bfff00 %.0f%%, #c0c0c0 %.0f%%);",
                    percent, percent));
        }
    }

    private void updateLoopButton() {
        switch(currentLoopMode) {
            case OFF -> {
                loopButton.setText("🔁");
                loopButton.setStyle("-fx-opacity: 0.3;");
            }
            case ALL -> {
                loopButton.setStyle("-fx-opacity: 1;");
            }
            case ONE -> {
                loopButton.setText("🔂");
            }
        }
    }

    private void updateShuffleButton() {
        switch(currentShuffleMode) {
            case OFF -> {
                shuffleButton.setStyle("-fx-opacity: 0.3;");
            }
            default -> {
                shuffleButton.setStyle("-fx-opacity: 1;");
            }
        }
    }
}
