package controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import services.DownloadService;
import services.SettingsService;
import controllers.DownloadDialog.ServiceRequest;


/*
 *  ----- MAIN MENU -----
 *  (XXX: Section under construction)
 */
public class MenuController {
    
    @FXML private MenuItem bff000Theme;
    @FXML private MenuItem o0ffffTheme;
    @FXML private MenuItem cbc1aeTheme;
    @FXML private MenuItem fdb0c0Theme;
    @FXML private MenuItem ff7f7fTheme;
    @FXML private MenuItem fcb001Theme;

    @FXML private MenuItem loudnessMenuItem;

    @FXML @SuppressWarnings("unused")
    private void initialize() {
        bff000Theme.setOnAction(e -> SettingsService.getInstance().setTheme("#bff000"));
        o0ffffTheme.setOnAction(e -> SettingsService.getInstance().setTheme("#00ffff"));
        cbc1aeTheme.setOnAction(e -> SettingsService.getInstance().setTheme("#cbc1ae"));
        fdb0c0Theme.setOnAction(e -> SettingsService.getInstance().setTheme("#fdb0c0"));
        ff7f7fTheme.setOnAction(e -> SettingsService.getInstance().setTheme("#ff7f7f"));
        fcb001Theme.setOnAction(e -> SettingsService.getInstance().setTheme("#fcb001"));

        loudnessMenuItem.setText((
            (SettingsService.getInstance().isNormalizerEnabled()) ? 
                            "Disable " : "Enable ") + "Loudness Normalizer");
        loudnessMenuItem.setOnAction(e -> {
            SettingsService.getInstance().enableNormalizer(!
                SettingsService.getInstance().isNormalizerEnabled());
            loudnessMenuItem.setText((
                (SettingsService.getInstance().isNormalizerEnabled()) ? 
                                "Disable " : "Enable ") + "Loudness Normalizer");
        });
    }

    @FXML
    private void handleEqualizer(ActionEvent event) {
        new EqualizerDialog().showAndWait();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        showInfo("About", "JukeBox\nVersion 1.0\nDeveloped by Sadge");
    }

    @FXML
    private void downloadSong() {
        new DownloadDialog(ServiceRequest.Song).showAndWait();
    }

    @FXML
    private void downloadPlaylist() {
        new DownloadDialog(ServiceRequest.Playlist).showAndWait();
    }

    @Deprecated
    private void showInfo(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}


/*
 *  ----- DOWNLOAD DIALOG BOX (for both song & playlist) -----
 */
class DownloadDialog extends Stage {
    enum ServiceRequest {Song, Playlist};

    private static final Pattern SONG_PATTERN = Pattern.compile(
	    "^https?://(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/)[\\w-]+.*");
    private static final Pattern PLAYLIST_PATTERN = Pattern.compile(
	   "^https?://(www\\.)?youtube\\.com/playlist\\?list=[\\w-]+.*");

    private final TextField urlField = new TextField();
    private final Label feedbackLabel = new Label();
    private final Button enterButton = new Button("Enter");
    private final Button cancelButton = new Button("Cancel");

    @SuppressWarnings("unused")
    DownloadDialog(ServiceRequest service) {
        setTitle("Download YouTube " + service.toString());
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);
        setAlwaysOnTop(isFocused());

        urlField.getStyleClass().add("url-field");
        feedbackLabel.getStyleClass().add("feedback-label");

        urlField.setPromptText("Paste YouTube " + service.toString() + " Link");
        urlField.setPrefWidth(400);
        feedbackLabel.setMaxWidth(Double.MAX_VALUE);

        enterButton.setDisable(true);
        enterButton.setDefaultButton(true);
        enterButton.setOnAction(e -> {
            String url = stripParameters(urlField.getText());
            switch(service) {
                case Song     -> DownloadService.getInstance().downloadSong(url);
                case Playlist -> DownloadService.getInstance().downloadPlaylist(url);
            } 
            close();
        });

        cancelButton.setOnAction(e -> close());

        urlField.textProperty().addListener((obs, oldText, newText) -> {
            feedbackLabel.getStyleClass().removeAll("valid", "invalid");
            String plainUrl = newText.trim();

            if (((service == ServiceRequest.Song && SONG_PATTERN.matcher(plainUrl).matches()) ||
                 (service == ServiceRequest.Playlist && PLAYLIST_PATTERN.matcher(plainUrl).matches()))
                    && plainUrl.indexOf(' ') == -1) {
                feedbackLabel.setText("Valid link format");
                feedbackLabel.getStyleClass().add("valid");
                enterButton.setDisable(false);
            } else {
                feedbackLabel.setText("Invalid link format");
                feedbackLabel.getStyleClass().add("invalid");
                enterButton.setDisable(true);
            }
        });

        HBox buttonRow = new HBox(10, enterButton, cancelButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, urlField, feedbackLabel, buttonRow);
        
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/downloaddialog.css").toExternalForm());
        setScene(scene);
        Platform.runLater(() -> getScene().getRoot().requestFocus());
    }

    private String stripParameters(String url) {
        int ampIndex = url.indexOf('&');
        return (ampIndex != -1) ? url.substring(0, ampIndex) : url;
    }
}


/*
 *  ----- EQUALIZER DIALOG BOX -----
 */
@SuppressWarnings("unused")
class EqualizerDialog extends Stage {
    
    private static final int NUM_BANDS = 10;
    private static final double MIN_GAIN = -12.0;
    private static final double MAX_GAIN = 12.0;
    private static final String[][] BANDS = {
        {"Sub-bass"   ,  "32Hz"},
        {"Bass"       ,  "64Hz"},
        {"Low Mids"   , "125Hz"},
        {"Mid-lows"   , "250Hz"},
        {"Mids"       , "500Hz"},
        {"Mid-highs"  ,  "1kHz"},
        {"Upper Mids" ,  "2kHz"},
        {"Presence"   ,  "4kHz"},
        {"Brilliance" ,  "8kHz"},
        {"Air"        , "16kHz"}
    };
    private static final String[] EQUALIZER_PRESETS = {
        "Flat", "Treble Boost", "Bass Boost", "Headphones", "Laptop", 
        "Portable Speakers", "Home Stereo", "TV", "Car", "Custom"
    };

    private final Slider[] sliders = new Slider[NUM_BANDS];
    private final ComboBox<String> presetBox = new ComboBox<>();
    private final Map<String, double[]> presets = new HashMap<>();

    EqualizerDialog() {
        setTitle("Equalizer Settings");
        initModality(Modality.APPLICATION_MODAL);

        // Create presets
        presets.put("Flat"              , new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        presets.put("Treble Boost"      , new double[]{0, 0, 0, -1, -1, 0, 2, 4, 5, 6});
        presets.put("Bass Boost"        , new double[]{6, 5, 4, 2, 1, 0, 0, -1, -2, -3});
        presets.put("Headphones"        , new double[]{3, 2, 2, 1, 0, 0, 2, 3, 4, 5});
        presets.put("Laptop"            , new double[]{-2, -3, -3, -2, 0, 0, 2, 3, 4, 5});
        presets.put("Portable Speakers" , new double[]{3, 2, 1, 1, 0, 0, 1, 2, 3, 4});
        presets.put("Home Stereo"       , new double[]{2, 2, 2, 1, 0, 0, 1, 2, 3, 4});
        presets.put("TV"                , new double[]{-4, -4, -3, -2, 0, 0, 3, 4, 5, 6});
        presets.put("Car"               , new double[]{4, 4, 3, 2, 1, 0, 1, 2, 3, 4});
        presets.put("Custom"            , null);

        // Sliders layout
        HBox sliderBox = new HBox(10);
        sliderBox.setSpacing(30);
        sliderBox.setPadding(new Insets(20));
        sliderBox.setAlignment(Pos.CENTER);
        sliderBox.setStyle("-fx-background-color: #1e1e1e;");

        for (int i = 0; i < NUM_BANDS; i++) {
            VBox bandBox = new VBox(5);
            bandBox.setAlignment(Pos.CENTER);

            Label bandName = new Label(BANDS[i][0]);
            Label band = new Label(BANDS[i][1]);

            Slider slider = new Slider(MIN_GAIN, MAX_GAIN, SettingsService.getInstance().getGainValue(i));
            slider.setOrientation(Orientation.VERTICAL);
            slider.setPrefHeight(150);
            slider.setMajorTickUnit(1);
            slider.setSnapToTicks(true);
            slider.setStyle(String.format(
                    "-thumb-color: %s;", 
                    SettingsService.getInstance().getTheme()));

            Label gain = new Label(String.format("%.0f", slider.getValue()));
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    gain.setText(String.format("%.0f", slider.getValue()));
                }
            });

            final int bandIndex = i;
            slider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                if (!isChanging) {
                    SettingsService.getInstance().setGainValue(bandIndex, slider.getValue());
                    if (!presetBox.getValue().equals("Custom")) {
                        presetBox.setValue("Custom");
                    }
                }
            });

            sliders[i] = slider;
            bandBox.getChildren().addAll(bandName, band, slider, gain);
            sliderBox.getChildren().add(bandBox);
        }

        // Presets dropdown
        presetBox.getItems().addAll(EQUALIZER_PRESETS);
        presetBox.setValue(SettingsService.getInstance().getPreset());
        presetBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            presetBox.setValue(newVal);
            SettingsService.getInstance().setPreset(newVal);
            if (newVal != null && presets.containsKey(newVal) && !"Custom".equals(newVal)) {
                for (int i = 0; i < NUM_BANDS; i++) {
                    sliders[i].setValue(presets.get(newVal)[i]);
                    SettingsService.getInstance().setGainValue(i, presets.get(newVal)[i]);
                }
            }
        });
        presetBox.setPrefSize(220, 50);

        HBox presetRow = new HBox(10, new Label("Preset:"), presetBox);
        presetRow.setAlignment(Pos.CENTER);
        presetRow.setPadding(new Insets(30));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons
        Button closeButton = new Button("Close");
        closeButton.setPrefSize(75, 50);
        closeButton.setOnAction(e -> close());

        HBox lastRow = new HBox(spacer, closeButton);
        lastRow.setPadding(new Insets(20));

        VBox root = new VBox(sliderBox, presetRow, lastRow);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 900, 500);
        scene.getStylesheets().add(getClass().getResource("/styles/equalizerdialog.css").toExternalForm());
        setScene(scene);
    }
}
