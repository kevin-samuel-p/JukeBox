package controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.Node;
import javafx.util.Duration;

import java.io.File;

import services.TrackService;


public class PlayerController {

    @FXML private Label trackInfoLabel;
    @FXML private Button previousButton, playPauseButton, nextButton;
    @FXML private ToggleButton loopButton, shuffleButton;
    @FXML private Slider trackSlider, volumeSlider;
    @FXML private Label currentTimeLabel, totalTimeLabel, volumeIcon;

    private Media media;
    private MediaPlayer mediaPlayer;

    private ChangeListener<Number> volumeListener;
    private ChangeListener<Duration> timeListener;

    private boolean isManuallySeeking = false;

    private enum LoopMode { OFF, ALL, ONE }
    private LoopMode currentLoopMode = LoopMode.OFF;

    private enum ShuffleMode { OFF, ON }
    private ShuffleMode currentShuffleMode = ShuffleMode.OFF;

    private double lastVolume = 70; // default volume on start-up

    @FXML
    private void initialize() {

        // Listen for track selection
        Platform.runLater(() -> {
            TrackService.getInstance().selectedTrackProperty()
                        .addListener((obs, oldTrack, newTrack) -> {
                if (newTrack != null) {
                    loadAndPlayTrack(newTrack);
                }
            });
        });

        // Set track info label
        trackInfoLabel.setText("No track playing");

        // Volume icon update
        updateVolumeIcon(volumeSlider.getValue());
        volumeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging && volumeSlider.getValue() != 0) {
                lastVolume = volumeSlider.getValue();
            }
        });

        // Mute/unmute using volume icon
        volumeIcon.setOnMouseClicked(e -> {
            if (volumeSlider.getValue() == 0) {
                volumeSlider.setValue(lastVolume);
            } else {
                volumeSlider.setValue(0);
            }
        });

        // Action on play/pause button
        playPauseButton.setOnAction(e -> {
            String fileName = TrackService.getInstance().getSelectedTrack().getName();
            String trackName = fileName.substring(0, fileName.length() - 4);
            switch(mediaPlayer.getStatus()) {
                case MediaPlayer.Status.PLAYING -> {
                    mediaPlayer.pause();
                    playPauseButton.setText("▶");
                    trackInfoLabel.setText("Paused: " + trackName);
                }
                case MediaPlayer.Status.PAUSED -> {
                    mediaPlayer.play();
                    playPauseButton.setText("II");
                    trackInfoLabel.setText("Playing: " + trackName);
                }
            }
        });

        // Action on previous button: disable if previous track not found
        previousButton.setOnAction(e -> {
            previousButton.setDisable(
                !TrackService.getInstance().previousTrack());
        });

        // Action on next button: disable if next track not found
        nextButton.setOnAction(e -> {
            nextButton.setDisable(
                !TrackService.getInstance().previousTrack());
        });

        // Disable buttons until a track is selected
        setPlaybackControlsEnabled(false);
    }

    private void loadAndPlayTrack(File file) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            
            // Kill listeners and event handlers
            volumeSlider.valueProperty().removeListener(volumeListener);
            mediaPlayer.currentTimeProperty().removeListener(timeListener);
            trackSlider.setOnMousePressed(null);
            trackSlider.setOnMouseReleased(null);
            
            mediaPlayer.dispose();
        }

        media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        String fileName = file.getName();
        String trackName = fileName.substring(0, fileName.length() - 4);
        trackInfoLabel.setText("Playing: " + trackName);

        mediaPlayer.setVolume(volumeSlider.getValue() / 100);
        mediaPlayer.play();
        playPauseButton.setText("II");

        mediaPlayer.setOnReady(() -> {
            Duration total = media.getDuration();
            trackSlider.setMax(total.toSeconds());
            totalTimeLabel.setText(formatTime(total));
        });

        // Update mediaPlayer volume with volumeSlider
        volumeListener = (obs, oldVal, newVal) -> {
            mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
            updateVolumeSliderColor(newVal.doubleValue());
            updateVolumeIcon(newVal.doubleValue());
        };

        // Update trackSlider while media is playing
        timeListener = (obs, oldTime, newTime) -> {
            if (!isManuallySeeking) {
                trackSlider.setValue(newTime.toSeconds());
                currentTimeLabel.setText(formatTime(newTime));
            }
        };
        
        volumeSlider.valueProperty().addListener(volumeListener);
        mediaPlayer.currentTimeProperty().addListener(timeListener);

        // Seek timestamp in media using trackSlider
        trackSlider.setOnMousePressed(e -> isManuallySeeking = true);
        trackSlider.setOnMouseReleased(e -> {
            isManuallySeeking = false;
            mediaPlayer.seek(Duration.seconds(trackSlider.getValue()));
        });

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
        shuffleButton.setOnAction(e -> {
            switch(currentShuffleMode) {
                case OFF -> {
                    currentShuffleMode = ShuffleMode.ON;
                    shuffleButton.setStyle("-fx-opacity: 1;");
                }
                case ON -> {
                    currentShuffleMode = ShuffleMode.OFF;
                    shuffleButton.setStyle("-fx-opacity: 0.3;");
                }
            }
        });

        setPlaybackControlsEnabled(true);
    }

    private String formatTime(Duration time) {
        int minutes = (int) time.toMinutes();
        int seconds = (int) time.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateVolumeSliderColor(double value) {
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
                mediaPlayer.setOnEndOfMedia(() -> {
                    if (!TrackService.getInstance().nextTrack()) {
                        TrackService.getInstance().setCurrentTrackIndex(0);
                    }
                });
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

    

    private void setPlaybackControlsEnabled(boolean enabled) {
        playPauseButton.setDisable(!enabled);
        previousButton.setDisable(!enabled);
        nextButton.setDisable(!enabled);
        loopButton.setDisable(!enabled);
        shuffleButton.setDisable(!enabled);
        trackSlider.setDisable(!enabled);
    }
}
