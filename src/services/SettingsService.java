package services;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;


public class SettingsService {
    private static final SettingsService instance = new SettingsService();
    private static final String SETTINGS_PATH = "data/user-settings.json";
    private JSONObject settings;
    private JSONObject equalizerSettings;
    private JSONArray gainValues;

    private final ObjectProperty<String> theme = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Double> volume = new SimpleObjectProperty<>(null);
    
    @SuppressWarnings("unchecked")
    private final ObjectProperty<Double>[] equalizerGainValues = (ObjectProperty<Double>[]) new SimpleObjectProperty[10];
    private String equalizerPreset = null;
    private final ObjectProperty<Boolean> normalizerEnabled = new SimpleObjectProperty<>(null);

    private SettingsService() {
        loadSettings();
    }

    public static SettingsService getInstance() {
        return instance;
    }

    private void loadSettings() {
        try {
            String content = Files.readString(Paths.get(SETTINGS_PATH));
            settings = new JSONObject(content);

            theme.set(settings.get("theme").toString());
            volume.set(Double.parseDouble(settings.get("volume").toString()));

            equalizerSettings = settings.getJSONObject("equalizer");
            equalizerPreset = equalizerSettings.getString("preset");
            gainValues = equalizerSettings.getJSONArray("bands");
            for (int i = 0; i < 10; i++) {
                equalizerGainValues[i] = new SimpleObjectProperty<>();
                equalizerGainValues[i].set(gainValues.getDouble(i));
            }

            normalizerEnabled.set(settings.getBoolean("normalizer_enabled"));
        } catch (IOException e) {
            System.err.println("Failed to load settings: " + e.getMessage());
            settings = new JSONObject();
            
            // Default settings
            theme.set("#bff000");
            volume.set(70.000);
            for (int i = 0; i < 10; i++) {
                equalizerGainValues[i] = new SimpleObjectProperty<>();
                equalizerGainValues[i].set(0d);
            }
            normalizerEnabled.set(false);
        }
    }

    public void saveSettings() {
        settings.put("theme", getTheme());
        settings.put("volume", getLastVolume());

        equalizerSettings.put("preset", getPreset());
        equalizerSettings.put("bands", gainValues);
        settings.put("equalizer", equalizerSettings);
        settings.put("normalizer_enabled", isNormalizerEnabled());

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(SETTINGS_PATH))) {
            writer.write(settings.toString(4)); // indentation amount
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // ----- PROPERTIES -----

    public ObjectProperty<String> themeProperty() {
        return theme;
    }

    public ObjectProperty<Double> volumeProperty() {
        return volume;
    }

    public ObjectProperty<Double> gainProperty(int band) {
        return equalizerGainValues[band];
    }

    public ObjectProperty<Boolean> normalizerStatusProperty() {
        return normalizerEnabled;
    }

    // ----- GETTERS -----

    public String getTheme() {
        return theme.get();
    }

    public double getLastVolume() {
        return volume.get();
    }

    public double getGainValue(int band) {
        return equalizerGainValues[band].get();
    }

    public String getPreset() {
        return equalizerPreset;
    }

    public boolean isNormalizerEnabled() {
        return normalizerEnabled.get();
    }



    // ----- SETTERS -----

    public void setTheme(String newTheme) {
        theme.set(newTheme);
    }

    public void setLastVolume(Double lastVolume) {
        volume.set(lastVolume);
    }

    public void setGainValue(int band, Double newValue) {
        equalizerGainValues[band].set(newValue);
        gainValues.put(band, newValue);
    }

    public void setPreset(String newPreset) {
        equalizerPreset = newPreset;
    }

    public void enableNormalizer(boolean status) {
        normalizerEnabled.set(status);
    }
}
