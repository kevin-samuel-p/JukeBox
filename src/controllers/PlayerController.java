package controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.Node;
import javafx.util.Duration;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import services.SettingsService;
import services.TrackService;


public class PlayerController {

    @FXML private Label trackInfoLabel;
    @FXML private Button previousButton, playPauseButton, nextButton, loopButton, shuffleButton;
    @FXML private Slider trackSlider, volumeSlider;
    @FXML private Label currentTimeLabel, totalTimeLabel, volumeIcon;

    private MediaPlayer mediaPlayer;

    private ChangeListener<Number> volumeListener;
    private ChangeListener<Duration> timeListener;

    private boolean isManuallySeeking = false;

    private enum LoopMode { OFF, ALL, ONE } 
    private LoopMode currentLoopMode = LoopMode.OFF;

    private enum ShuffleMode { OFF, ON } 
    private ShuffleMode currentShuffleMode = ShuffleMode.OFF;

    // FIXME: Settings
    private String backgroundSetting;

    @FXML
    private void initialize() {

        Platform.runLater(() -> {    
            
            // Initialize volume slider styles
            volumeSlider.setValue(SettingsService.getInstance().getLastVolume());
            setTheme();

            // Listen for track selection
            TrackService.getInstance().selectedTrackProperty()
                        .addListener((obs, oldTrack, newTrack) -> {
                if (newTrack != null) {
                    loadAndPlayTrack(newTrack);
                }
            });

            // Listen for theme change
            SettingsService.getInstance().themeProperty()
                           .addListener((obs, oldTheme, newTheme) -> {
                if (newTheme != null) {
                    SettingsService.getInstance().setTheme(newTheme);
                    setTheme();
                }
            });
        });

        // Set track info label
        trackInfoLabel.setText("No track playing");

        // Update volume slider gradient and icon
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateVolumeSliderColor(newVal.doubleValue());
            updateVolumeIcon(newVal.doubleValue());
        });

        // Get lastVolume value for mute button
        volumeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging && volumeSlider.getValue() != 0) {
                SettingsService.getInstance().setLastVolume(volumeSlider.getValue());;
                System.out.println("Last volume was " + volumeSlider.getValue());
            }
        });

        // Mute/unmute using volume icon
        volumeIcon.setOnMouseClicked(e -> {
            if (volumeSlider.getValue() == 0) {
                volumeSlider.setValue(SettingsService.getInstance().getLastVolume());
                System.out.println("Unumted");
            } else {
                volumeSlider.setValue(0);
                System.out.println("Muted");
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
                    System.out.println("Paused track: " + trackName);
                }
                case MediaPlayer.Status.PAUSED -> {
                    mediaPlayer.play();
                    playPauseButton.setText("II");
                    trackInfoLabel.setText("Playing: " + trackName);
                    System.out.println("Playing track: " + trackName);
                }
            }
        });

        /*
         *  ___Actions__on__Previous__Button___
         *  -> Restarts track from beginning
         *  -> When clicked against instantaneously, goes to previous track
         *  -> Disables responsively for first 1 second if there is no previous track
         *  -> When loop-all is enabled, it simply fetches the last index from trackList
         *  -> No concerns for shuffle mode as Shuffler.previousTrack() rolls back to default 
         *     previousTrack() when false
         */
        previousButton.setOnAction(e -> {
            if (mediaPlayer.getCurrentTime().toSeconds() > 1) {
                mediaPlayer.seek(Duration.ZERO);
                if (currentLoopMode != LoopMode.ALL && currentShuffleMode != ShuffleMode.ON
                        && TrackService.getInstance().getCurrentTrackIndex() == 0) {
                    delayButtonEnable(previousButton);
                } 
            } else if (!TrackService.getInstance().previousTrack() 
                        && currentLoopMode == LoopMode.ALL) {
                TrackService.getInstance()
                            .setCurrentTrackIndex(
                                TrackService.getInstance()
                                            .getTrackList().size() - 1);
            }
        });

        /*
         *  ___Actions__On__Next__Button___
         *  -> Selects next index in trackList as currentTrackIndex
         *  -> If reached end of trackList, button is disabled by default
         *  -> Works for shuffle as well since Shuffler.nextTrack() is returned by default nextTrack()
         *  -> When in loop-all mode, sets currentTrackIndex to zero if traversed entire track
         *  -> If loop-all and shuffle mode are both enabled, then it simply turns shuffle mode off and on
         *     when Shuffler.nextTrack() returns false at the end of shuffled playlist
         */
        nextButton.setOnAction(e -> {
            if (currentLoopMode == LoopMode.ALL) {
                if (!TrackService.getInstance().nextTrack()) {
                    if (currentShuffleMode == ShuffleMode.ON) {
                        TrackService.getInstance().disableShuffle();
                        TrackService.getInstance().enableShuffle();
                    } else {
                        TrackService.getInstance().setCurrentTrackIndex(0);
                    }
                }
            } else {
                nextButton.setDisable(!TrackService.getInstance().nextTrack());
            }  
        });

        // Disable buttons until a track is selected
        setPlaybackControlsEnabled(false);
    }

    private void loadAndPlayTrack(File file) throws MediaException {
        if (!file.exists() || !file.canRead()) {
            if (mediaPlayer != null) {
                String fileName = TrackService.getInstance().getSelectedTrack().getName();
                String trackName = fileName.substring(0, fileName.length() - 4);
                mediaPlayer.pause();
                playPauseButton.setText("▶");
                trackInfoLabel.setText("Paused: " + trackName);
                System.out.println("Paused track: " + trackName);
            }

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Playback Error");
            alert.setHeaderText("Could not load media file");
            alert.setContentText(file.getName() + " might be corrupted, unsupported or missing.");
            alert.showAndWait();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            
            // Kill listeners and event handlers
            volumeSlider.valueProperty().removeListener(volumeListener);
            mediaPlayer.currentTimeProperty().removeListener(timeListener);
            trackSlider.setOnMousePressed(null);
            trackSlider.setOnMouseReleased(null);
            
            mediaPlayer.dispose();
        }
        
        Media media = new Media(file.toURI().toString());
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

            switch(currentLoopMode) {
                case ALL -> {
                    previousButton.setDisable(false);
                    nextButton.setDisable(false);
                }
                default -> {
                    if (currentShuffleMode == ShuffleMode.OFF) {
                        int currIndex = TrackService.getInstance().getCurrentTrackIndex();
                        if (currIndex == 0) {
                            delayButtonEnable(previousButton); 
                        } else if (currIndex == 
                                    TrackService.getInstance().getTrackList().size() - 1) {
                            nextButton.setDisable(true);
                        } else {
                            previousButton.setDisable(false);
                            nextButton.setDisable(false);
                        }
                    } else {
                        previousButton.setDisable(false);
                        nextButton.setDisable(false);
                    }
                }
            }
        });

        // Update mediaPlayer volume with volumeSlider
        volumeListener = (obs, oldVal, newVal) -> {
            mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
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
                    // Enable it
                    currentShuffleMode = ShuffleMode.ON;
                    nextButton.setDisable(false);
                    previousButton.setDisable(false);
                    System.out.println("Shuffle button was supposedly enabled");
                    TrackService.getInstance().enableShuffle();
                    shuffleButton.setStyle(String.format(
                                    "-fx-background-color: %s; -fx-opacity: 1;", 
                                    SettingsService.getInstance().getTheme()));
                }
                case ON -> {
                    // Turn it off
                    currentShuffleMode = ShuffleMode.OFF;
                    System.out.println("Shuffle button was supposedly disabled");
                    TrackService.getInstance().disableShuffle();
                    shuffleButton.setStyle(String.format(
                                    "-fx-background-color: %s; -fx-opacity: 0.3;", 
                                    SettingsService.getInstance().getTheme()));
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
                    "-fx-background-color: linear-gradient(to right, %s %.0f%%, #c0c0c0 %.0f%%);",
                    SettingsService.getInstance().getTheme(), percent, percent)); 
        }
    }

    private void setTheme() {
        updateVolumeSliderColor(volumeSlider.getValue());
        
        // Set button colors
        backgroundSetting = String.format(
            "-fx-background-color: %s;", 
            SettingsService.getInstance().getTheme());

        playPauseButton.setStyle(backgroundSetting);
        previousButton.setStyle(backgroundSetting);
        nextButton.setStyle(backgroundSetting);
        loopButton.setStyle(backgroundSetting);
        shuffleButton.setStyle(backgroundSetting);
    }

    private void updateLoopButton() {
        switch(currentLoopMode) {
            case OFF -> {
                loopButton.setText("🔁");
                loopButton.setStyle(String.format(
                                    "-fx-background-color: %s; -fx-opacity: 0.3;", 
                                    SettingsService.getInstance().getTheme()));
                mediaPlayer.setCycleCount(1);
                mediaPlayer.setOnEndOfMedia(() -> TrackService.getInstance().nextTrack());
            }
            case ALL -> {
                loopButton.setStyle(String.format(
                                    "-fx-background-color: %s; -fx-opacity: 1;", 
                                    SettingsService.getInstance().getTheme()));
                nextButton.setDisable(false);
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

    private void delayButtonEnable(Button button) {
        button.setDisable(true);
        // Delay enable by 1 second
        // Clicking previous after that will replay track from start
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> button.setDisable(false));
                timer.cancel();     // To stop timer after one execution
            }
        }, 1000); 
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
