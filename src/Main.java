import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create root layout
        BorderPane root = new BorderPane();

        // Load player view from FXML
        MenuBar menuBar = FXMLLoader.load(getClass().getResource("/views/topmenu.fxml"));
        VBox playerView = FXMLLoader.load(getClass().getResource("/views/playerview.fxml"));

        root.setTop(menuBar);
        root.setBottom(playerView);

        // (Optional) Add central content or top menu
        root.setCenter(new javafx.scene.control.Label("Main Content Area"));

        // Show the scene
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("GrooveFX");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
