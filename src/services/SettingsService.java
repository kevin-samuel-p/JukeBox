package services;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;


public class SettingsService {
    private static final SettingsService instance = new SettingsService();
    private static final String SETTINGS_PATH = "data/user-settings.json";
    private JSONObject settings;

    private final ObjectProperty<String> theme = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Double> volume = new SimpleObjectProperty<>(null);

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
        } catch (IOException e) {
            System.err.println("Failed to load settings: " + e.getMessage());
            settings = new JSONObject();
            
            // Default settings
            theme.set("#bff000");
            volume.set(70.000);
        }
    }

    public void saveSettings() {
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

    // ----- GETTERS -----

    public String getTheme() {
        return theme.get();
    }

    public double getLastVolume() {
        return volume.get();
    }

    // ----- SETTERS -----

    public void setTheme(String newTheme) {
        theme.set(newTheme);
        settings.put("theme", newTheme);
    }

    public void setLastVolume(Double lastVolume) {
        volume.set(lastVolume);
        settings.put("volume", lastVolume);
    }
}
