package com.ggx.editor.preview;

import com.ggx.editor.utils.Range;
import com.vladsch.flexmark.ast.Node;
import javafx.scene.control.IndexRange;

import java.nio.file.Path;
import java.util.List;

public class MarkDownPreviewPane {

    private WebViewPreview webViewPreview=new WebViewPreview();

    interface Renderer {
        void update(String markdownText, Node astRoot, Path path);
        String getHtml(boolean source);
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
