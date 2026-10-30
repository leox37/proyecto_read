
package app_read.app;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Principal extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL recurso = getClass().getResource("/app_read/vista/fxml/inicio_reader.fxml");
        if (recurso == null) {
            throw new IOException("No se encontró la vista principal inicio_reader.fxml.");
        }
        FXMLLoader loader = new FXMLLoader(recurso);
        Parent root = loader.load();

        primaryStage.setTitle("Ventana Libros");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(Principal.class, args);
    }
}