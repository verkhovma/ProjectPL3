package vnl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application{
    ClientController controller;

    @Override
    public void start(Stage primaryStage) throws Exception{
        // load GUI file.fxml
        FXMLLoader loader = new FXMLLoader(
            getClass().getClassLoader().getResource("client.fxml")
            );
        // create and set controller
        controller = new ClientController();
        loader.setController(controller);

        Parent root = loader.load();
        
        // set and show GUI window
        Scene scene = new Scene(root);
        primaryStage.setTitle("PL3-client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void stop(){
        // close connection when GUI window closed by user
        controller.disconnect();
    }

    public static void main(String[] args){
        Application.launch(args);
    }
}