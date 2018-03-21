package com.ggx.editor;

import com.ggx.editor.interfaces.TreeListAction;
import com.ggx.editor.markdown.MarkDownHtmlWrapper;
import com.ggx.editor.markdown.MarkdownEntity;
import com.ggx.editor.utils.FileUtil;
import com.ggx.editor.widget.TextFieldTreeCellImpl;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.EventStreams;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController implements Initializable, TreeListAction {

    @FXML
    public BorderPane rootPane;
    @FXML
    public TreeView<File> treeView;
    @FXML
    public StackPane fileContainer;
    @FXML
    public SplitPane splitePane;
    @FXML
    public JFXHamburger jfxHamburger;
    @FXML
    public StackPane leftBtn;
    @FXML
    public Label title;
    @FXML
    public BorderPane leftPane;
    @FXML
    public BorderPane rightPane;
    @FXML
    public ToggleGroup toggle;
    @FXML
    public HBox toggleContainer;

    private Stage stage;


    private final Image folderIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/folder_16.png"));
    private final Image fileIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/file_16.png"));

    private HamburgerBackArrowBasicTransition burgerTask3;

    private File currentFile;
    private WebView webView = new WebView();

    private static final String[] KEYWORDS={
            "#","##","###","####","#####","######","#######",
            ">",">>",">>>","`","`","```","```", "!","---"
    };
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
    private static final String OTHER_PATTERN = "\\*";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<OTHER>" + OTHER_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    /*+ "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"*/
    );
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.widthProperty().addListener((observable, oldValue, newValue) -> splitePane.setDividerPosition(0, 0.18));
        File dir = new File("E:/vertx/vertxdoc");
        ImageView iv = new ImageView(folderIcon);
        iv.setSmooth(true);
        iv.setViewport(new Rectangle2D(0, 0, 16, 16));
        TreeItem<File> rootTree = new TreeItem<>(dir, iv);
        searchFile(dir, rootTree);
        treeView.setRoot(rootTree);
        treeView.setShowRoot(false);
        treeView.setEditable(true);
        treeView.setCellFactory(param -> new TextFieldTreeCellImpl(this));

        jfxHamburger.setMaxSize(20, 10);
        burgerTask3 = new HamburgerBackArrowBasicTransition(jfxHamburger);
        burgerTask3.setRate(-1);

        rootPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (rightPane.getCenter() != null) {
                webView.setPrefWidth((rootPane.getWidth() - leftPane.getWidth()) / 2);
            } else {
                webView.setPrefWidth((rootPane.getWidth() - leftPane.getWidth()));
            }
        });
        toggle.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            RadioButton rb = (RadioButton) newValue;
            switch (rb.getId()) {
                case "editor":
                    rightPane.setRight(null);
                    rightPane.setCenter(fileContainer);
                    break;
                case "eye":
                    rightPane.setCenter(null);
                    webView.setPrefWidth((rootPane.getWidth() - leftPane.getWidth()));
                    rightPane.setRight(webView);
                    break;
                case "realTime":
                    rightPane.setCenter(fileContainer);
                    webView.setPrefWidth((rootPane.getWidth() - leftPane.getWidth()) / 2);
                    rightPane.setRight(webView);
                    break;
            }
        });

        executor = Executors.newSingleThreadExecutor();

    }

    private void searchFile(File fileOrDir, TreeItem<File> rootItem) {
        File[] list = fileOrDir.listFiles();
        if (list == null) {
            return;
        }
        Consumer<File> consumer = f -> {
            TreeItem<File> item = new TreeItem<>(f);
            if (f.isDirectory()) {
                ImageView iv = new ImageView(folderIcon);
                iv.setSmooth(true);
                iv.setViewport(new Rectangle2D(0, 0, 16, 16));
                item.setGraphic(iv);
                rootItem.getChildren().add(item);
                searchFile(f, item);
            } else {
                item.setGraphic(new ImageView(fileIcon));
                rootItem.getChildren().add(item);
            }
        };
        Arrays.stream(list).filter(f -> !f.isHidden() && f.isDirectory()).sorted().forEach(consumer);
        Arrays.stream(list).filter(f -> !f.isHidden() && f.isFile()).sorted().forEach(consumer);
    }

    @FXML
    public void doBack(MouseEvent mouseEvent) {
        burgerTask3.setRate(burgerTask3.getRate() * -1);
        burgerTask3.play();
        int size = splitePane.getItems().size();
        if (size > 1) {
            splitePane.setDividerPosition(0, 0);
            splitePane.getItems().remove(0);
        } else {
            splitePane.getItems().add(0, leftPane);
            splitePane.setDividerPosition(0, 0.2);
        }

    }

    CodeArea codeArea;
    private ExecutorService executor;
    @Override
    public void openFile(File file) {
        currentFile = file;
        title.setText(FileUtil.prefixName(file) + " " + DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.CHINESE).format(file.lastModified()));
        if (file.getName().endsWith(".md")) {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/md_24.png"))));
        } else {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/txt_24.png"))));
        }
        //读取文件的内容设置到编辑器区域
        codeArea = new CodeArea();
        codeArea.setStyle("-fx-font-size:" + 16);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .successionEnds(Duration.ofMillis(500))
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(codeArea.richChanges())
                .filterMap(t -> {
                    if(t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(styleSpans -> {
                    //应用高亮样式
                    codeArea.setStyleSpans(0, styleSpans);
                });
        codeArea.getStylesheets().add(ClassLoader.getSystemResource("css/java-keywords.css").toExternalForm());
        EventStreams.changesOf(codeArea.textProperty())
                .reduceSuccessions((stringChange, stringChange2) -> stringChange2,
                        Duration.ofMillis(500))
                .subscribe(stringChange -> {
            MarkdownEntity html = MarkDownHtmlWrapper.ofContent(codeArea.getText());
            webView.getEngine().loadContent(html.toString());
        });

        BufferedReader br = null;
        try {
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new FileReader(file));
            br.lines().map(s -> s + "\n").forEach(sb::append);
            codeArea.replaceText(0, 0, sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        VirtualizedScrollPane<CodeArea> pane = new VirtualizedScrollPane<>(codeArea);
        MarkdownEntity html = MarkDownHtmlWrapper.ofContent(codeArea.getText());
        webView.getEngine().loadContent(html.toString());
        if (fileContainer.getChildren().size() == 2) {
            fileContainer.getChildren().remove(1);
        }
        fileContainer.getChildren().addAll(pane);
        pane.scrollYToPixel(0);
        toggleContainer.setVisible(true);

    }

    @Override
    public void deleteFile(File file) {
        if (currentFile == file) {
            title.setGraphic(null);
            title.setText(null);
            fileContainer.getChildren().remove(1);
            currentFile = null;
        }
    }

    @Override
    public void deleteDir(TreeItem<File> item, File file) {
        // TODO: 2017/12/28 弹出Dialog 确认删除目录
        File[] list = file.listFiles();
        if (list == null) {
            return;
        }
        if (Arrays.stream(list).allMatch(f -> f.getAbsolutePath().equals(currentFile.getAbsolutePath()))) {
            title.setGraphic(null);
            title.setText(null);
            if (fileContainer.getChildren().size() == 2) {
                fileContainer.getChildren().remove(1);
            }
            currentFile = null;
        }
        if (FileUtil.deleteDir(file)) {
            item.getParent().getChildren().remove(item);

        }
    }

    @Override
    public void modifyFile(File file) {

    }

    @FXML
    public void add(MouseEvent mouseEvent) {
        ChoiceDialog<String> dialog=new ChoiceDialog<>();
        dialog.setTitle("新建");
        dialog.setContentText("Hello Ggx");
        dialog.getItems().addAll("markdown");
        dialog.show();
    }

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("OTHER")!=null?"keyword":
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("SEMICOLON") != null ? "semicolon" :
                    matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
