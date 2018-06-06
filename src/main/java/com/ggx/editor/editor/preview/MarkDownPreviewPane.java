package com.ggx.editor.editor.preview;

import com.ggx.editor.utils.Range;
import com.vladsch.flexmark.ast.Node;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.IndexRange;
import javafx.scene.web.WebView;
import org.reactfx.EventStreams;

import java.nio.file.Path;
import java.util.List;

public class MarkDownPreviewPane {

    private PreviewContext previewContext;
    //preview
    private Preview webViewPreview;
    //renderer
    private Renderer renderer;

    // 'markdownText' value
    private final SimpleStringProperty markdownText = new SimpleStringProperty();
    public SimpleStringProperty markdownTextProperty() { return markdownText; }
    // 'markdownAST' property
    private final ObjectProperty<Node> markdownAST = new SimpleObjectProperty<>();
    public ObjectProperty<Node> markdownASTProperty() { return markdownAST; }

    private DoubleProperty scrollY=new SimpleDoubleProperty();
    public double getScrollY() { return scrollY.get(); }
    public DoubleProperty scrollYProperty() { return scrollY; }

    private final ObjectProperty<IndexRange> editorSelection = new SimpleObjectProperty<>();
    public ObjectProperty<IndexRange> editorSelectionProperty() { return editorSelection; }

    public MarkDownPreviewPane(){
        previewContext=new PreviewContext() {
            @Override
            public Renderer getRenderer() { return renderer; }
            @Override
            public String getMarkdownText() { return markdownText.get(); }
            @Override
            public Node getMarkdownAST() { return markdownAST.get(); }
            @Override
            public Path getPath() { return null; }
            @Override
            public IndexRange getEditorSelection() { return editorSelection.get(); }
        };
        webViewPreview=new WebViewPreview();
        renderer=new CommonmarkPreviewRenderer();


        EventStreams.changesOf(markdownText).subscribe(stringChange -> update());
        EventStreams.changesOf(markdownAST).subscribe(nodeChange -> update());
        EventStreams.changesOf(scrollY).subscribe(numberChange -> scrollY());
        EventStreams.changesOf(editorSelection).subscribe(indexRangeChange -> editorSelectionChanged());
    }

    public WebView getPreviewNode(){
        return (WebView) webViewPreview.getNode();
    }
    public void setWidth(double w){
         getPreviewNode().setPrefWidth(w);
    }

    public void setHeight(double h){
        getPreviewNode().setPrefHeight(h);
    }

    private void update(){
        Platform.runLater(()->{
            renderer.update(markdownText.get(),markdownAST.get());
            webViewPreview.update(previewContext,renderer);
        });
    }

    private boolean scrollYrunLaterPending;
    private void scrollY() {
        if (webViewPreview == null||scrollYrunLaterPending) return;
        scrollYrunLaterPending = true;
        Platform.runLater(() -> {
            scrollYrunLaterPending = false;
            webViewPreview.scrollY(previewContext, scrollY.get());
        });
    }

    private boolean editorSelectionChangedRunLaterPending;
    private void editorSelectionChanged(){
        if (webViewPreview == null||editorSelectionChangedRunLaterPending) return;
        Platform.runLater(()->{
            editorSelectionChangedRunLaterPending=true;
            webViewPreview.editorSelectionChanged(previewContext,editorSelection.get());
        });
    }


    interface Renderer {
        void update(String markdownText, Node astRoot);
        String getHtml();
        String getAST();
        List<Range> findSequences(int startOffset, int endOffset);
    }

    interface Preview {
        javafx.scene.Node getNode();
        void update(PreviewContext context, Renderer renderer);
        void scrollY(PreviewContext context, double value);
        void editorSelectionChanged(PreviewContext context, IndexRange range);
    }

    interface PreviewContext {
        Renderer getRenderer();
        String getMarkdownText();
        Node getMarkdownAST();
        Path getPath();
        IndexRange getEditorSelection();
    }
}
