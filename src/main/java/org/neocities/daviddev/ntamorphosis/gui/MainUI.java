package org.neocities.daviddev.ntamorphosis.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

public class MainUI extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(new URL("file://localhost/home/david/Documents/NTAMorphosis/src/main/java/org/neocities/daviddev/ntamorphosis/gui/templates/hello-world.fxml"));
        VBox vbox = loader.<VBox>load();

        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        scene.getStylesheets().add("styles/styles.css");
        stage.show();
    }
}
