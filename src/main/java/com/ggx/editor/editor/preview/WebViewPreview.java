package com.ggx.editor.editor.preview;

import com.ggx.editor.utils.Resource;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.NodeVisitor;
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

public class WebViewPreview implements MarkDownPreviewPane.Preview{

    //代码块语法关键词集合
    private static final HashMap<String, String> prismLangDependenciesMap = new HashMap<>();

    private WebView webView;
    private final ArrayList<Runnable> runWhenLoadedList = new ArrayList<>();
    private int lastScrollX;
    private int lastScrollY;
    private IndexRange lastEditorSelection;

    public WebViewPreview() {
        getNode();
    }

    private void createNodes(){
        webView=new WebView();
        webView.setFocusTraversable(false);
        // 禁用webView默认拖拽事件
        webView.setOnDragEntered(null);
        webView.setOnDragExited(null);
        webView.setOnDragOver(null);
        webView.setOnDragDropped(null);
        webView.setOnDragDetected(null);
        webView.setOnDragDone(null);

        webView.getEngine().getLoadWorker().stateProperty().addListener(new HyperlinkRedirectListener(webView));

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
    @Override
    public Node getNode() {
        if(webView==null){
            createNodes();
        }
        return webView;
    }

    /**
     * 更新webView
     */
    @Override
    public void update(MarkDownPreviewPane.PreviewContext context,
                       MarkDownPreviewPane.Renderer renderer) {
        if(!webView.getEngine().getLoadWorker().isRunning()){
            // get window.scrollX and window.scrollY from web engine,
            // but only if no worker is running (in this case the result would be zero)
            Object scrollXobj = webView.getEngine().executeScript("window.scrollX");
            Object scrollYobj = webView.getEngine().executeScript("window.scrollY");
            lastScrollX = (scrollXobj instanceof Number) ? ((Number)scrollXobj).intValue() : 0;
            lastScrollY = (scrollYobj instanceof Number) ? ((Number)scrollYobj).intValue() : 0;
        }
        lastEditorSelection=context.getEditorSelection();
        Path path=context.getPath();
        String base = (path != null)
                ? ("<base href=\"" + path.getParent().toUri().toString() + "\">\n")
                : "";
        String scrollScript = (lastScrollX > 0 || lastScrollY > 0)
                ? ("  onload='window.scrollTo("+lastScrollX+", "+lastScrollY+");'")
                : "";
        String content=renderer.getHtml();
        StringBuilder html=new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<meta charset=\"utf-8\" />\n");
        html.append(base);
        html.append("<link rel=\"stylesheet\" href=\"")
                .append(Resource.getResource("css/markdownpad-github.css"))
                .append("\">\n");
        html.append("<style>\n");
        html.append(".mwfx-editor-selection {\n");
        html.append("  border-right: 5px solid #f47806;\n");
        html.append("  margin-right: -5px;\n");
        html.append("  background-color: rgb(253, 247, 241);\n");
        html.append("}\n");
        html.append("</style>\n");
        html.append("<script src=\"")
                .append(Resource.getResource("js/preview.js"))
                .append("\"></script>\n");
        html.append(prismSyntaxHighlighting(context.getMarkdownAST()));
        html.append("</head>\n");
        html.append("<body").append(scrollScript).append(">\n");
        html.append(content);
        html.append("<script>\n");
        html.append("<script>").append(highlightNodesAt(lastEditorSelection)).append("</script>\n");
        html.append("</body>\n");
        html.append("</html>");
        webView.getEngine().loadContent(html.toString());
    }

    /**
     * 控制当前webView垂直滚动
     * @param value
     */
    @Override
    public void scrollY(MarkDownPreviewPane.PreviewContext context,double value) {
        runWhenLoaded(()->{
                webView.getEngine().executeScript("preview.scrollTo(" + value + ");");
        });
    }

    @Override
    public void editorSelectionChanged(MarkDownPreviewPane.PreviewContext context, IndexRange range) {
        if(range.equals(lastEditorSelection)){
            return;
        }
        lastEditorSelection=range;
        runWhenLoaded(()->
            webView.getEngine().executeScript("preview.highlightNodesAt(" + range.getEnd() + ")"));
    }

    private String highlightNodesAt(IndexRange range) {
        return "preview.highlightNodesAt(" + range.getEnd() + ")";
    }

    public static String prismSyntaxHighlighting(com.vladsch.flexmark.ast.Node astRoot) {
        initPrismLangDependencies();

        // check whether markdown contains fenced code blocks and remember languages
        ArrayList<String> languages = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor(Collections.emptyList()) {
            @Override
            public void visit(com.vladsch.flexmark.ast.Node node) {
                if (node instanceof FencedCodeBlock) {
                    String language = ((FencedCodeBlock)node).getInfo().toString();
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

        //在构建html的时候只使用用到的语言插件，这里不使用prism的自动载入插件因为他们是延迟加载的会导致快速打字的时候出现
        //闪烁的情况
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
     * 载入并解析 prism/lang_dependencies.txt
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
        } catch (IOException ignored) {
        }
    }

    public static String trimDelim(String str, String leadingDelim, String trailingDelim) {
        str = str.trim();
        if (!str.startsWith(leadingDelim) || !str.endsWith(trailingDelim))
            throw new IllegalArgumentException(str);
        return str.substring(leadingDelim.length(), str.length() - trailingDelim.length());
    }


}
