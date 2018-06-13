package com.ggx.editor.editor.setting;

import com.ggx.editor.Main;
import com.ggx.editor.editor.setting.AppearancePane;
import com.ggx.editor.options.Options;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class SettingsPane {

    private Stage setting;
    private AppearancePane appearancePane;
    private InvalidationListener listener;

    public SettingsPane(){
        appearancePane =new AppearancePane();
    }

    public  void showSettings(){
        if(setting==null){
            setting=new Stage();
            setting.setTitle("Settings");
            setting.setAlwaysOnTop(true);
            setting.initOwner(Main.get());
            Scene scene=new Scene(buildTab(),600,600);
            scene.getStylesheets().add("css/main-css.css");
            setting.setScene(scene);
            listener=observable -> updateFont(scene.getRoot());
            WeakInvalidationListener weakInvalidationListener=new WeakInvalidationListener(listener);
            Options.fontSizeProperty().addListener(weakInvalidationListener);
            Options.fontFamilyProperty().addListener(weakInvalidationListener);
        }
        setting.show();
    }

    private  TabPane buildTab(){
        TabPane tabPane=new TabPane();
        tabPane.getTabs().addAll(appearancePane.getFontTab());
        return tabPane;
    }
    private void updateFont(Parent root){
        root.setStyle("-fx-font-size: "+Options.getFontSize()
                +";-fx-font-family: "+Options.getFontFamily());
    }



}
