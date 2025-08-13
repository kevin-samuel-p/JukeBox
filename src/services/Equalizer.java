package services;

import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.MediaPlayer;

public class Equalizer {
    private final AudioEqualizer equalizer;

    public Equalizer(MediaPlayer player) {
        this.equalizer = player.getAudioEqualizer();
        this.equalizer.setEnabled(true);
    }

    public void setBandGain(int bandIndex, double gain) {
        equalizer.getBands().get(bandIndex).setGain(gain);
    }
}
