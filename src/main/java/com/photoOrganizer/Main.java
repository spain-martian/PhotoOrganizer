package com.photoOrganizer;

import com.photoOrganizer.controller.MainPageController;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    private static HostServices hostServices;
    private MainPageController mainPageController;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Fonts.load();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        mainPageController = loader.getController();
        Scene scene = new Scene(root, 1500, 900);
        primaryStage.setScene(scene);
        primaryStage.show();

        root.setFocusTraversable(true);
        root.requestFocus();

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            mainPageController.handleKey(event);
        });

        primaryStage.getIcons().add(new Image(getClass().getResource("/icons/app.ico").toExternalForm()));

        hostServices = getHostServices();
    }

    public static HostServices getHostServicesInstance() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch(args);
    }

}

class Fonts {
    public static void load() {
        Font.loadFont(Fonts.class.getResource("/rawline/rawline-400.ttf").toExternalForm(), 12);
        Font.loadFont(Fonts.class.getResource("/rawline/rawline-500.ttf").toExternalForm(), 12);
        Font.loadFont(Fonts.class.getResource("/rawline/rawline-600.ttf").toExternalForm(), 12);
        Font.loadFont(Fonts.class.getResource("/rawline/rawline-700.ttf").toExternalForm(), 12);
        Font.loadFont(Fonts.class.getResource("/rawline/rawline-800.ttf").toExternalForm(), 12);
    }
}