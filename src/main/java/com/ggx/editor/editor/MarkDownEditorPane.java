package com.ggx.editor.editor;

import com.ggx.editor.options.Options;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.Parser;

public class MarkDownEditorPane {

    private MarkdownTextArea textArea;
    private Parser parser;
    private String lineSeparator = getLineSeparatorOrDefault();


    public MarkDownEditorPane() {
        textArea=new MarkdownTextArea();
        textArea.setWrapText(true);
        textArea.setUseInitialStyleForInsertion(true);
        textArea.getStyleClass().add("markdown-editor");
        textArea.getStylesheets().add("css/MarkdownEditor.css");
        textArea.getStylesheets().add("css/prism.css");

        textArea.textProperty().addListener((observable, oldValue, newValue) -> textChanged(newValue));

    }

    private String getLineSeparatorOrDefault() {
        String lineSeparator = Options.getLineSeparator();
        return (lineSeparator != null) ? lineSeparator : System.getProperty( "line.separator", "\n" );
    }

    private void textChanged(String newText) {

    }
}
