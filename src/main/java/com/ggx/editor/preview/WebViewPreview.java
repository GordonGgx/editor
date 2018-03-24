package com.ggx.editor.preview;

import com.ggx.editor.utils.Resource;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.NodeVisitor;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.web.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class WebViewPreview {

    private static final HashMap<String, String> prismLangDependenciesMap = new HashMap<>();

    private WebView webView;
    private final ArrayList<Runnable> runWhenLoadedList = new ArrayList<>();
    private int lastScrollX;
    private int lastScrollY;
    private IndexRange lastEditorSelection;
    // 'editorSelection' property
    private final ObjectProperty<IndexRange> editorSelection = new SimpleObjectProperty<>();
    private DoubleProperty scrollY=new SimpleDoubleProperty();
    public DoubleProperty scrollYProperty() {return scrollY; }
    public ObjectProperty<IndexRange> editorSelectionProperty() { return editorSelection; }

    private boolean scrollYrunLaterPending;

    public WebViewPreview() {
        getNode();
    }

    private void createNodes(){
        webView=new WebView();
        webView.setFocusTraversable(false);
        // disable WebView default drag and drop handler to allow dropping markdown files
        webView.setOnDragEntered(null);
        webView.setOnDragExited(null);
        webView.setOnDragOver(null);
        webView.setOnDragDropped(null);
        webView.setOnDragDetected(null);
        webView.setOnDragDone(null);

        scrollY.addListener((observable, oldValue, newValue) -> {
            if(scrollYrunLaterPending)return;
            scrollYrunLaterPending=true;
            Platform.runLater(()->{
                scrollYrunLaterPending=false;
                scrollY(scrollY.get());
            });
        });

        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue== Worker.State.SUCCEEDED&&!runWhenLoadedList.isEmpty()){
                ArrayList<Runnable> runnables = new ArrayList<>(runWhenLoadedList);
                runWhenLoadedList.clear();
                for (Runnable runnable : runnables)
                    runnable.run();
            }
        });
    }

    public double getWidth(){
        return webView.getWidth();
    }

    public double getHeight(){
        return webView.getHeight();
    }

    public void setWidth(double w){
        webView.setPrefWidth(w);
    }

    public void setHeight(double h){
        webView.setPrefHeight(h);
    }

    private void runWhenLoaded(Runnable runnable) {
        if (webView.getEngine().getLoadWorker().isRunning())
            runWhenLoadedList.add(runnable);
        else
            runnable.run();
    }

    /**
     * 获取webView控件
     * @return
     */
    public Node getNode() {
        if(webView==null){
            createNodes();
        }
        return webView;
    }

    /**
     * 更新webView
     */
    public void update(com.vladsch.flexmark.ast.Node node,String text/*MarkDownPreviewPane.PreviewContext context,
                       MarkDownPreviewPane.Renderer renderer*/) {
        if(!webView.getEngine().getLoadWorker().isRunning()){
            // get window.scrollX and window.scrollY from web engine,
            // but only if no worker is running (in this case the result would be zero)
            Object scrollXobj = webView.getEngine().executeScript("window.scrollX");
            Object scrollYobj = webView.getEngine().executeScript("window.scrollY");
            lastScrollX = (scrollXobj instanceof Number) ? ((Number)scrollXobj).intValue() : 0;
            lastScrollY = (scrollYobj instanceof Number) ? ((Number)scrollYobj).intValue() : 0;
        }
        lastEditorSelection=editorSelection.get();
//        Path path=context.getPath();
//        String base = (path != null)
//                ? ("<base href=\"" + path.getParent().toUri().toString() + "\">\n")
//                : "";
        String scrollScript = (lastScrollX > 0 || lastScrollY > 0)
                ? ("  onload='window.scrollTo("+lastScrollX+", "+lastScrollY+");'")
                : "";
        webView.getEngine().loadContent(
                "<!DOCTYPE html>\n"
                        + "<html>\n"
                        + "<head>\n"
                        + "<link rel=\"stylesheet\" href=\"" + Resource.getResource("css/markdownpad-github.css") + "\">\n"
                        + "<style>\n"
                        + ".mwfx-editor-selection {\n"
                        + "  border-right: 5px solid #f47806;\n"
                        + "  margin-right: -5px;\n"
                        + "  background-color: rgb(253, 247, 241);\n"
                        + "}\n"
                        + "</style>\n"
                        + "<script src=\"" + Resource.getResource("js/preview.js") + "\"></script>\n"
                        + prismSyntaxHighlighting(node)
                        /*+ base*/
                        + "</head>\n"
                        + "<body" + scrollScript + ">\n"
                        + text
                        + "<script>" + highlightNodesAt(lastEditorSelection) + "</script>\n"
                        + "</body>\n"
                        + "</html>"
        );
    }

    /**
     * 控制当前webView垂直滚动
     * @param value
     */
    public void scrollY(double value) {
        runWhenLoaded(()->{
            webView.getEngine().executeScript("preview.scrollTo(" + value + ");");
        });
    }


    public void editorSelectionChanged(MarkDownPreviewPane.PreviewContext context, IndexRange range) {
        if(range.equals(lastEditorSelection)){
            return;
        }
        lastEditorSelection=range;
        runWhenLoaded(()->{
            webView.getEngine().executeScript(highlightNodesAt(range));
        });
    }

    private String highlightNodesAt(IndexRange range) {
        return "preview.highlightNodesAt(" + range.getEnd() + ")";
    }

    private String prismSyntaxHighlighting(com.vladsch.flexmark.ast.Node astRoot) {
        initPrismLangDependencies();

        // check whether markdown contains fenced code blocks and remember languages
        ArrayList<String> languages = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
            @Override
            public void visit(com.vladsch.flexmark.ast.Node node) {
                if (node instanceof FencedCodeBlock) {
                    String language = ((FencedCodeBlock)node).getInfo().toString();
                    System.out.println(language.contains(language));
                    if (language.contains(language))
                        languages.add(language);

                    // dependencies
                    while ((language = prismLangDependenciesMap.get(language)) != null) {
                        if (language.contains(language))
                            languages.add(0, language); // dependencies must be loaded first
                    }
                } else
                    visitChildren(node);
            }
        };
        visitor.visit(astRoot);

        if (languages.isEmpty())
            return "";

        // build HTML (only load used languages)
        // Note: not using Prism Autoloader plugin because it lazy loads/highlights, which causes flicker
        //       during fast typing; it also does not work with "alias" languages (e.g. js, html, xml, svg, ...)
        StringBuilder buf = new StringBuilder();
        buf.append("<link rel=\"stylesheet\" href=\"").append(Resource.getResource("js/prism/prism.css")).append("\">\n");
        buf.append("<script src=\"").append(Resource.getResource("js/prism/prism-core.min.js")).append("\"></script>\n");
        for (String language : languages) {
            URL url = Resource.getResource("js/prism/components/prism-"+language+".min.js");
            if (url != null)
                buf.append("<script src=\"").append(url).append("\"></script>\n");
        }
        return buf.toString();
    }

    /**
     * load and parse prism/lang_dependencies.txt
     */
    private static void initPrismLangDependencies() {
        if (!prismLangDependenciesMap.isEmpty())
            return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Resource.getResAsStream("js/prism/lang_dependencies.txt"))))
        {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("{"))
                    continue;

                line = line.replaceAll("(\\[.+),(.+\\])", "$1;$2");
                line = trimDelim(line, "{", "}");
                for (String str : line.split(",")) {
                    String[] parts = str.split(":");
                    if (parts[1].startsWith("["))
                        continue; // not supported

                    String key = trimDelim(parts[0], "\"", "\"");
                    String value = trimDelim(parts[1], "\"", "\"");
                    prismLangDependenciesMap.put(key, value);
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private static String trimDelim(String str, String leadingDelim, String trailingDelim) {
        str = str.trim();
        if (!str.startsWith(leadingDelim) || !str.endsWith(trailingDelim))
            throw new IllegalArgumentException(str);
        return str.substring(leadingDelim.length(), str.length() - trailingDelim.length());
    }
}
