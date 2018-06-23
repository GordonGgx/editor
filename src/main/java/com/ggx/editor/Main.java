package com.ggx.editor;

import com.ggx.editor.fileos.FileMonitor;
import com.ggx.editor.options.Options;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Main extends Application{

    private static ExecutorService executor;
    private static Stage main;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Options.load(getConfig());
        main=primaryStage;
        executor=Executors.newSingleThreadExecutor();
        primaryStage.getIcons().add(new Image("icons/markdownwriterfx32.png"));
        primaryStage.setTitle("Editor");
        FXMLLoader loader=new FXMLLoader(ClassLoader.getSystemResource("fxml/main.fxml"));
        Parent root=loader.load();
        Scene scene=new Scene(root);
        scene.setFill(Color.GHOSTWHITE);
        InvalidationListener listener = e -> updateFont(root);
        WeakInvalidationListener weakInvalidationListener=new WeakInvalidationListener(listener);
        Options.fontSizeProperty().addListener(weakInvalidationListener);
        Options.fontFamilyProperty().addListener(weakInvalidationListener);

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

    }

    private void updateFont(Parent root){
        root.setStyle("-fx-font-size: "+Options.getFontSize()
                +";-fx-font-family: "+Options.getFontFamily());
    }

    @Override
    public void stop() throws Exception {
        executor.shutdown();
        main=null;
        FileMonitor.get().stopWatch();
    }
    public static Stage get(){
        return main;
    }

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Preferences getUserPreferences(){
        return Preferences.userRoot().node("GMarkdownEditor");
    }
    private Preferences getConfig(){
        return getUserPreferences().node("configs");
    }
}
