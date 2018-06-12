package com.ggx.editor.editor.setting;

import com.ggx.editor.options.Options;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AppearancePane {

    private Tab fontTab;
    private final ObservableList<Integer> fontSize=
            FXCollections.observableArrayList(
                    8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28);

    public AppearancePane(){
        fontTab =new Tab("Appearance");
        fontTab.closableProperty().setValue(false);
        ScrollPane scrollPane=new ScrollPane();
        try {
            GridPane gridPane=FXMLLoader.load(ClassLoader.getSystemResource("fxml/appearancePane.fxml"));

            JFXComboBox<String> fontFamilyBox= (JFXComboBox<String>) gridPane.lookup("#fonts");
            CompletableFuture.supplyAsync(()-> GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                    .thenAccept(strings -> Platform.runLater(()-> {
                        fontFamilyBox.setItems(FXCollections.observableArrayList(strings));
                        fontFamilyBox.setValue(Options.getFontFamily());
                    }));
            JFXComboBox<Integer> fontSizeBox= (JFXComboBox<Integer>) gridPane.lookup("#fontSize");
            fontSizeBox.setItems(fontSize);
            fontSizeBox.setValue(Options.getFontSize());
            JFXButton save= (JFXButton) gridPane.lookup("#save");
            save.setOnMouseClicked(event -> {
                Options.setFontSize(fontSizeBox.getSelectionModel().getSelectedItem());
                Options.setFontFamily(fontFamilyBox.getSelectionModel().getSelectedItem());
            });

            scrollPane.setContent(gridPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fontTab.setContent(scrollPane);
    }

    public Tab getFontTab() {
        return fontTab;
    }
}
