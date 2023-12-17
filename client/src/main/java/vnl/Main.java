package vnl;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application{
    public void start(Stage primaryStage){
        Label root = new Label("this is client\ngraphic javafx app");
        Scene scene = new Scene(root, 1280, 720);
        primaryStage.setTitle("PL3-client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public void run(Stage primaryStage){
        primaryStage.toFront();
    }
    public static void main(String[] args) {
        Application.launch(args);
    }
}