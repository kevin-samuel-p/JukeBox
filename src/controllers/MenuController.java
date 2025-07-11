package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class MenuController {
    
    @FXML
    private void handleTheme(ActionEvent event) {
        showInfo("Theme", "Theme picker will be added here.");
    }

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

    private void showInfo(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
