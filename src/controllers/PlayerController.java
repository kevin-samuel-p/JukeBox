package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PlayerController {
    
    @FXML private Label trackInfoLabel;
    @FXML private Button previousButton, playPauseButton, nextButton;
    @FXML private Slider trackSlider, volumeSlider;
    @FXML private Label timestampLabel;

    @FXML
    private void initialize() {
        trackInfoLabel.setText("No track playing");

        playPauseButton.setOnAction(e -> togglePlayback());
    }

    private void togglePlayback() {
        if (playPauseButton.getText().equals("▶")) {
            playPauseButton.setText("⏸");
            trackInfoLabel.setText("Playing: Chill Beats - Lofi Girl");
        } else {
            playPauseButton.setText("▶");
            trackInfoLabel.setText("Paused: Chill Beats - Lofi Girl");
        }
    }
}
