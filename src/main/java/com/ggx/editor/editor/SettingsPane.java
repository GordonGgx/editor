package com.ggx.editor.editor;

import com.ggx.editor.Main;
import com.ggx.editor.editor.setting.AppearancePane;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class SettingsPane {

    private Stage setting;
    private AppearancePane appearancePane;

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
        }
        setting.show();
    }

    private  TabPane buildTab(){
        TabPane tabPane=new TabPane();
        tabPane.getTabs().addAll(appearancePane.getFontTab());
        return tabPane;
    }



}
