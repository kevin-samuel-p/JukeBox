package services;

import javafx.scene.media.MediaPlayer;

public class LoudnessNormalizer {
    
    private final MediaPlayer mediaPlayer;
    private boolean enabled = SettingsService.getInstance().isNormalizerEnabled();

    private static final double TARGET_LEVEL = 0.35;  // Target average loudness (arbitrary units, tweakable)
    private static final double SMOOTHING_FACTOR = 0.05;  // How quickly to adjust
    private double baseVolume;      // Get from volumeSlider

    @SuppressWarnings("unused")
    public LoudnessNormalizer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;

        baseVolume = this.mediaPlayer.getVolume();

        SettingsService.getInstance().normalizerStatusProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) enabled = newVal;
            if (!enabled) mediaPlayer.setVolume(baseVolume);
        });

        // Attach spectrum listener to monitor loudness
        this.mediaPlayer.setAudioSpectrumInterval(0.05);    // 50 ms updates
        this.mediaPlayer.setAudioSpectrumNumBands(32);

        this.mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
            if (!enabled) return;   // Do nothing if disabled

            double average = 0.0;
            for (double magnitude : magnitudes) {
                average += Math.pow(10, magnitude / 20.0);  // Convert dB to linear amplitude
            }
            average /= magnitudes.length;

            // Calculate gain adjustment towards target
            double adjustment = TARGET_LEVEL / (average + 1e-9);

            // Smooth changes to avoid pumping effect
            double currentVolume = mediaPlayer.getVolume();
            double targetVolume = clamp(baseVolume * adjustment, 0.0, 1.0);
            double newVolume = currentVolume + (targetVolume - currentVolume) * SMOOTHING_FACTOR;

            mediaPlayer.setVolume(newVolume);
        });
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setBaseVolume(double volume) {
        baseVolume = clamp(volume, 0.0, 1.0);
        if (!enabled) {
            mediaPlayer.setVolume(baseVolume);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}