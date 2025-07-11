package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.Node;
import javafx.util.Duration;

import java.io.File;


public class PlayerController {

    @FXML private Label trackInfoLabel;
    @FXML private Button previousButton, playPauseButton, nextButton;
    @FXML private ToggleButton loopButton, shuffleButton;
    @FXML private Slider trackSlider, volumeSlider;
    @FXML private Label timestampLabel, volumeIcon;

    private Media media;
    private MediaPlayer mediaPlayer;

    private boolean isPlaying = false;
    private boolean isManuallySeeking = false;

    private enum LoopMode { OFF, ALL, ONE }
    private LoopMode 
        currentLoopMode = LoopMode.OFF, 
        currentShuffleMode = LoopMode.OFF;

    private boolean userManuallyMuted = false;
    private double lastVolume = 70; // default volume on start-up

    @SuppressWarnings("unused")
    @FXML
    private void initialize() {

        // Load a track for now (sample track for testing and debugging)
        String path = "src/assets/sample.mp3";
        media = new Media(new File(path).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isManuallySeeking) {
                trackSlider.setValue(newTime.toSeconds());
                updateTimestampLabel(newTime);
            }
        });

        mediaPlayer.setOnReady(() -> {
            trackSlider.setMax(media.getDuration().toSeconds());
        });

        trackSlider.setOnMousePressed(e -> isManuallySeeking = true);
        trackSlider.setOnMouseReleased(e -> {
            isManuallySeeking = false;
            mediaPlayer.seek(Duration.seconds(trackSlider.getValue()));
        });

        // Set track info label
        trackInfoLabel.setText("No track playing");

        // Volume slider color update
        Platform.runLater(() -> {
            updateSliderColor(volumeSlider.getValue());

            volumeSlider.valueProperty().addListener((_obs, _oldVal, newVal) -> {
                updateSliderColor(newVal.doubleValue());
            });
        });

        // Volume icon update
        updateVolumeIcon(volumeSlider.getValue());
        volumeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                if (volumeSlider.getValue() == 0) {
                    userManuallyMuted = true;
                } else {
                    userManuallyMuted = false;
                    lastVolume = volumeSlider.getValue();
                }
            }
        });

        volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateVolumeIcon(newValue.doubleValue());
        });

        volumeIcon.setOnMouseClicked(event -> {
            if (volumeSlider.getValue() == 0 && !userManuallyMuted) {
                volumeSlider.setValue(lastVolume > 0 ? lastVolume : 70);
            } else {
                userManuallyMuted = false;
                lastVolume = volumeSlider.getValue();
                volumeSlider.setValue(0);
            }
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
        if (mediaPlayer == null) return;

        if (isPlaying) {
            mediaPlayer.pause();
            playPauseButton.setText("▶");
            trackInfoLabel.setText("Paused: Love the Lord - Lincoln Brewster");
        } else {
            mediaPlayer.play();
            playPauseButton.setText("II");
            trackInfoLabel.setText("Playing: Love the Lord - Lincoln Brewster");
        }

        isPlaying = !isPlaying;
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
                mediaPlayer.setCycleCount(1);
                mediaPlayer.setOnEndOfMedia(null);
            }
            case ALL -> {
                loopButton.setStyle("-fx-opacity: 1;");
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setOnEndOfMedia(null);
            }
            case ONE -> {
                loopButton.setText("🔂");
                mediaPlayer.setCycleCount(1);
                mediaPlayer.setOnEndOfMedia(() -> {
                    mediaPlayer.seek(Duration.ZERO);
                    mediaPlayer.play();
                });
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

    private void updateVolumeIcon(double volume) {
        if (volume == 0) {
            volumeIcon.setText("🔇");
        } else if (volume <= 30) {
            volumeIcon.setText("🔈");
        } else if (volume <= 70) {
            volumeIcon.setText("🔉");
        } else {
            volumeIcon.setText("🔊");
        }
    }

    private void updateTimestampLabel(Duration time) {
        int minutes = (int) time.toMinutes();
        int seconds = (int) time.toSeconds() % 60;
        timestampLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }
}
