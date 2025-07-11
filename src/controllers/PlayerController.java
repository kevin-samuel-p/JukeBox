package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Node;

public class PlayerController {

    @FXML
    private Label trackInfoLabel;
    @FXML
    private Button previousButton, playPauseButton, nextButton;
    @FXML
    private Slider trackSlider, volumeSlider;
    @FXML
    private Label timestampLabel;

    @FXML
    private void initialize() {
        trackInfoLabel.setText("No track playing");

        Platform.runLater(() -> {
            updateSliderColor(volumeSlider.getValue());

            volumeSlider.valueProperty().addListener((_obs, _oldVal, newVal) -> {
                updateSliderColor(newVal.doubleValue());
            });
        });

        playPauseButton.setOnAction(e -> togglePlayback());
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

}
