package controllers;

import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import services.DownloadService;
import controllers.DownloadDialog.ServiceRequest;


/*
 *  ----- MAIN MENU -----
 *  (XXX: Section under construction)
 */
public class MenuController {
    
    // @FXML
    // private void handleTheme(ActionEvent event) {
    //     showInfo("Theme", "Theme picker will be added here.");
    // }

    @FXML
    private void handleEqualizer(ActionEvent event) {
        showInfo("Equalizer", "Equalizer settings will appear here.");
    }

    @FXML
    private void handleLoudness(ActionEvent event) {
        showInfo("Loudness Equalizer", "Loudness normalization setup goes here.");
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

    public DownloadDialog(ServiceRequest service) {
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
