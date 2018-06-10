package com.ggx.editor.editor;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.EventStreams;

import java.io.IOException;

public class FooterPane {

    public FooterPane(CodeArea codeArea){
        EventStreams.merge(EventStreams.changesOf(codeArea.caretColumnProperty())
        ,EventStreams.changesOf(codeArea.currentParagraphProperty()))
                .subscribe(integerChange -> {
                    coordinateProperty.setValue((codeArea.getCurrentParagraph()+1)+":"+codeArea.getCaretColumn());
                });
    }


    public Node getNode(){
        FXMLLoader loader=new FXMLLoader(ClassLoader.getSystemResource("fxml/fooder.fxml"));
        try {
            StackPane root=loader.load();
            Label coordinate = (Label) root.lookup("#coordinate");
            Label textType = (Label) root.lookup("#textType");
            coordinate.textProperty().bind(coordinateProperty);
            textType.textProperty().bind(textTypeProperty);
            coordinate.visibleProperty().bind(coordinateVisible);
            textType.visibleProperty().bind(textTypeVisible);
            return root;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SimpleStringProperty coordinateProperty=new SimpleStringProperty("0:0");
    private SimpleStringProperty textTypeProperty=new SimpleStringProperty("text");
    private SimpleBooleanProperty coordinateVisible=new SimpleBooleanProperty(false);
    private SimpleBooleanProperty textTypeVisible=new SimpleBooleanProperty(false);

    public String getCoordinateProperty() {
        return coordinateProperty.get();
    }


    public String getTextTypeProperty() {
        return textTypeProperty.get();
    }


    public boolean isCoordinateVisible() {
        return coordinateVisible.get();
    }


    public boolean isTextTypeVisible() {
        return textTypeVisible.get();
    }

    public void showCoordinate(boolean show){
        coordinateVisible.setValue(show);
    }

    public void showTextType(boolean show){
        textTypeVisible.setValue(show);
    }

    public void setTextType(TextType type){
        if(type==TextType.None){
            textTypeProperty.setValue("");
        }else {
            textTypeProperty.setValue(type.name());
        }
    }

    public enum TextType{
        None,MarkDown,Text
    }
}
