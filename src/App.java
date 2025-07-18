import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create root layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: black;");

        // Load player view from FXML
        MenuBar menuBar = FXMLLoader.load(getClass().getResource("/views/topmenu.fxml"));
        VBox playerView = FXMLLoader.load(getClass().getResource("/views/playerview.fxml"));
        ScrollPane libraryView = FXMLLoader.load(getClass().getResource("/views/libraryview.fxml"));

        root.setTop(menuBar);
        root.setCenter(libraryView);
        root.setBottom(playerView);

        // Show the scene
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);

        Image icon = new Image("/assets/icon.png");
        primaryStage.getIcons().add(icon);
        
        primaryStage.setTitle("JukeBox");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
