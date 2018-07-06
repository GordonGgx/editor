package com.ggx.editor.controller;

import com.ggx.editor.Main;
import com.ggx.editor.editor.FooterPane;
import com.ggx.editor.editor.MarkDownEditorPane;
import com.ggx.editor.editor.preview.CommonmarkPreviewRenderer;
import com.ggx.editor.editor.preview.MarkDownPreviewPane;
import com.ggx.editor.editor.preview.WebViewPreview;
import com.ggx.editor.editor.setting.SettingsPane;
import com.ggx.editor.fileos.FileMonitor;
import com.ggx.editor.interfaces.TreeListAction;
import com.ggx.editor.options.Options;
import com.ggx.editor.utils.FileUtil;
import com.ggx.editor.utils.HTMLTagParser;
import com.ggx.editor.utils.Resource;
import com.ggx.editor.widget.TextFieldTreeCellImpl;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.NodeVisitor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.reactfx.Change;
import org.reactfx.EventStreams;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class MainController implements Initializable, TreeListAction, Runnable {

    @FXML
    public StackPane root;
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
    @FXML
    public MenuItem save;
    @FXML
    public JFXDialog dialog;
    @FXML
    public JFXButton acceptButton;
    @FXML
    public ToolBar titleBar;
    @FXML
    public MenuItem findAction;
    @FXML
    public BorderPane editorContainer;
    @FXML
    public MenuItem pasteAction;
    @FXML
    public MenuItem copyAction;
    @FXML
    public MenuItem cutAction;
    @FXML
    public MenuItem editorAction;
    @FXML
    public MenuItem eyeAction;
    @FXML
    public MenuItem previewAction;
    @FXML
    public VBox outLine;


    private final Image folderIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/folder_16.png"));
    private final Image fileIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/file_16.png"));

    private HamburgerBackArrowBasicTransition burgerTask3;

    private File currentFile;

    private MarkDownPreviewPane markDownPreview;
    private MarkDownEditorPane markDownEditorPane;
    private FooterPane footerPane;
    private SettingsPane settingsPane;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        markDownPreview = new MarkDownPreviewPane();
        markDownEditorPane = new MarkDownEditorPane(editorContainer);
        footerPane = new FooterPane(markDownEditorPane.getTextArea());
        rootPane.setBottom(footerPane.getNode());


        treeView.setShowRoot(true);
        treeView.setEditable(false);
        treeView.setCellFactory(param -> new TextFieldTreeCellImpl(this));
        String oldFilePath = Options.getLastFilePath();
        if (oldFilePath != null) {
            File dir = new File(oldFilePath);
            if (dir.exists() && dir.isDirectory()) {
                ImageView iv = new ImageView(folderIcon);
                iv.setSmooth(true);
                iv.setViewport(new Rectangle2D(0, 0, 16, 16));
                TreeItem<File> rootTree = new TreeItem<>(dir, iv);
                searchFile(dir, rootTree);
                treeView.setRoot(rootTree);
                rootTree.setExpanded(true);
                FileMonitor.get().addWatchFile(dir);
                FileMonitor.get().setListener(this);
                FileMonitor.get().watch();
            }

        }
        jfxHamburger.setMaxSize(20, 10);
        burgerTask3 = new HamburgerBackArrowBasicTransition(jfxHamburger);
        burgerTask3.setRate(-1);

        EventStreams.changesOf(rootPane.widthProperty()).subscribe(this::changeWidth);
        EventStreams.changesOf(toggle.selectedToggleProperty()).subscribe(this::changeEditorView);

        markDownPreview.markdownTextProperty().bind(markDownEditorPane.markDownTextProperty());
        markDownPreview.markdownASTProperty().bind(markDownEditorPane.markDownASTProperty());
        markDownPreview.scrollYProperty().bind(markDownEditorPane.scrollYProperty());
        markDownPreview.editorSelectionProperty().bind(markDownEditorPane.selectionProperty());
        markDownEditorPane.titleProperty().addListener((observable, oldValue, newValue) -> newTitle(newValue));
    }

    private void newTitle(String markDown) {
        outLine.getChildren().clear();
        lines = 0;
        CompletableFuture.runAsync(() -> {
            try (BufferedReader br = new BufferedReader(new StringReader(markDown))) {
                CommonmarkPreviewRenderer renderer = (CommonmarkPreviewRenderer) markDownPreview.getRenderer();
                br.lines().forEach(s -> {
                    extractTitles(renderer, s, lines);
                    lines++;
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private void changeWidth(Change<Number> numberChange) {
        splitePane.setDividerPosition(0, 0.18);
        if (rightPane.getCenter() != null) {
            markDownPreview.setWidth((rootPane.getWidth() - leftPane.getWidth()) / 2);
        } else {
            markDownPreview.setWidth(rootPane.getWidth() - leftPane.getWidth());
        }
    }

    private void changeEditorView(Change<Toggle> toggleChange) {
        RadioButton rb = (RadioButton) toggleChange.getNewValue();
        switch (rb.getId()) {
            case "editor":
                rightPane.setRight(null);
                rightPane.setCenter(editorContainer);
                break;
            case "eye":
                rightPane.setCenter(null);
                markDownPreview.setWidth(rootPane.getWidth() - leftPane.getWidth());
                rightPane.setRight(markDownPreview.getPreviewNode());
                break;
            case "realTime":
                rightPane.setCenter(editorContainer);
                markDownPreview.setWidth((rootPane.getWidth() - leftPane.getWidth()) / 2);
                rightPane.setRight(markDownPreview.getPreviewNode());
                break;
        }
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
    public void doBack() {
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

    private int lines;

    @Override
    public void openFile(File file) {
        if (!file.exists()) {
            return;
        }
        if (currentFile == file) {
            return;
        }
        String fileName=file.getName();

        if(!fileName.endsWith("txt")&&
                !fileName.endsWith("md")&&
                !fileName.endsWith("html")){
            showError("暂不支持打开此文件");
            return;
        }
        currentFile = file;
        markDownPreview.pathProperty().setValue(file.toPath());
        initOpenFile();
        changeTextType(file);
        rootPane.setCursor(Cursor.WAIT);
        if (fileContainer.getChildren().size() == 2) {
            fileContainer.getChildren().remove(1);
        }
        fileContainer.getChildren().add(markDownEditorPane.getScrollPane());
        outLine.getChildren().clear();
        CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                CommonmarkPreviewRenderer renderer = (CommonmarkPreviewRenderer) markDownPreview.getRenderer();
                br.lines().map(s -> s + "\n").forEach(sb::append);
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
            return sb.toString();
        }).thenAccept(s -> Platform.runLater(() -> {
            markDownEditorPane.setNewFileContent(s);
            markDownEditorPane.getScrollPane().scrollYToPixel(0);
            rootPane.setCursor(Cursor.DEFAULT);

        }));
    }

    private void extractTitles(CommonmarkPreviewRenderer renderer, String s, int lines) {
        StringBuilder title = new StringBuilder();
        boolean has = false;
        if (s.startsWith("# ")) {
            has = true;
        } else if (s.startsWith("## ")) {
            has = true;
            title.append(" ");
        } else if (s.startsWith("### ")) {
            has = true;
            title.append("  ");
        } else if (s.startsWith("#### ")) {
            has = true;
            title.append("   ");
        } else if (s.startsWith("###### ")) {
            has = true;
            title.append("    ");
        } else if (s.startsWith("######## ")) {
            has = true;
            title.append("     ");
        }
        if (has) {
            renderer.update(s, null);
            title.append(HTMLTagParser.getTextByHTMLParser(renderer.getHtml()));
            Hyperlink hyperlink = new Hyperlink(title.toString());
            hyperlink.getStyleClass().add("test");
            hyperlink.setUserData("" + lines);
            hyperlink.setOnAction(event -> {
                markDownEditorPane.jumpToLine(Integer.parseInt((String) hyperlink.getUserData()));
                hyperlink.setVisited(false);
            });
            Platform.runLater(() -> {
                outLine.getChildren().add(hyperlink);
            });
        }
    }

    @Override
    public void deleteFile(File file) {
        if (currentFile == file) {
            title.setGraphic(null);
            title.setText(null);
            fileContainer.getChildren().remove(1);
            currentFile = null;
            deleteFileAction();
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
            deleteFileAction();
        }
        if (FileUtil.deleteDir(file)) {
            item.getParent().getChildren().remove(item);

        }
    }

    @Override
    public void modifyFile(File file) {
        currentFile = file;
        changeTextType(file);
    }


    @FXML
    public void openDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(Main.get());
        if (dir != null && dir.exists()) {
            Options.setLastFilePath(dir.getAbsolutePath());
            clear();
            ImageView iv = new ImageView(folderIcon);
            iv.setSmooth(true);
            iv.setViewport(new Rectangle2D(0, 0, 16, 16));
            TreeItem<File> rootTree = new TreeItem<>(dir, iv);
            searchFile(dir, rootTree);
            treeView.setRoot(rootTree);
            rootTree.setExpanded(true);
            FileMonitor.get().stopWatch();
            FileMonitor.get().addWatchFile(dir);
            FileMonitor.get().setListener(this);
            FileMonitor.get().watch();
        }

    }

    @FXML
    public void createDir() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save dir");
        File dir = fileChooser.showSaveDialog(Main.get());
        if (!dir.exists()) {
            if (dir.mkdir()) {
                System.out.println("创建成功");
                Options.setLastFilePath(dir.getAbsolutePath());
                FileMonitor.get().addWatchFile(dir);
                clear();
                ImageView iv = new ImageView(folderIcon);
                iv.setSmooth(true);
                iv.setViewport(new Rectangle2D(0, 0, 16, 16));
                TreeItem<File> rootTree = new TreeItem<>(dir, iv);
                treeView.setShowRoot(true);
                treeView.setRoot(rootTree);
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("工作空间创建失败.");
                error.initOwner(Main.get());
                error.show();
            }
        }
    }

    @FXML
    public void exitApp() {
        if (currentFile != null && currentFile.exists()) {
            markDownEditorPane.saveFile(currentFile);
        }
        Main.get().close();
        Platform.exit();
    }

    @FXML
    public void aboutAction() {
        acceptButton.setOnAction(event -> dialog.close());
        dialog.setTransitionType(JFXDialog.DialogTransition.TOP);
        dialog.show(root);
    }

    @FXML
    public void onSaveAction() {
        if (currentFile != null && currentFile.exists()) {
            markDownEditorPane.saveFile(currentFile);
        }
    }

    private void initOpenFile() {
        save.setDisable(false);
        findAction.setDisable(false);
        cutAction.setDisable(false);
        copyAction.setDisable(false);
        pasteAction.setDisable(false);
        editorAction.setDisable(false);
        eyeAction.setDisable(false);
        previewAction.setDisable(false);
        titleBar.setVisible(true);
        titleBar.setManaged(true);
        toggleContainer.setVisible(true);
        footerPane.showCoordinate(true);
        footerPane.showTextType(true);
    }

    private void deleteFileAction() {
        save.setDisable(true);
        findAction.setDisable(true);
        cutAction.setDisable(true);
        copyAction.setDisable(true);
        pasteAction.setDisable(true);
        editorAction.setDisable(true);
        eyeAction.setDisable(true);
        previewAction.setDisable(true);
        titleBar.setVisible(false);
        titleBar.setManaged(false);
        toggleContainer.setVisible(false);
        footerPane.showCoordinate(false);
        footerPane.showTextType(false);
    }

    //关闭面板并清理一些东西
    private void clear() {
        if (fileContainer.getChildren().size() == 2) {
            fileContainer.getChildren().remove(1);
        }
        currentFile = null;
        deleteFileAction();
        title.setText(null);
        title.setGraphic(null);
    }

    private void changeTextType(File file) {
        footerPane.setTextType(FooterPane.TextType.None);
        title.setText(FileUtil.prefixName(file) + " " + DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.CHINESE).format(file.lastModified()));
        if (file.getName().endsWith(".md")) {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/md_24.png"))));
            footerPane.setTextType(FooterPane.TextType.MarkDown);
        } else {
            title.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/txt_24.png"))));
            footerPane.setTextType(FooterPane.TextType.Text);
        }
    }

    @FXML
    public void openFind() {
        if (currentFile == null) {
            return;
        }
        markDownEditorPane.showFindPane();
    }

    @FXML
    public void changeEditor() {
        if (currentFile == null) {
            return;
        }
        toggle.selectToggle(toggle.getToggles().get(0));
    }

    @FXML
    public void changeEye() {
        if (currentFile == null) {
            return;
        }
        toggle.selectToggle(toggle.getToggles().get(1));
    }

    @FXML
    public void changePreview() {
        if (currentFile == null) {
            return;
        }
        toggle.selectToggle(toggle.getToggles().get(2));
    }

    @FXML
    public void openSettings() {
        if (settingsPane == null) {
            settingsPane = new SettingsPane();
        }
        settingsPane.showSettings();

    }

    @FXML
    public void onCut() {
        if (currentFile == null) {
            return;
        }
        markDownEditorPane.getTextArea().cut();
    }

    @FXML
    public void onCopy() {
        if (currentFile == null) {
            return;
        }
        markDownEditorPane.getTextArea().copy();
    }

    @FXML
    public void onPaste() {
        if (currentFile == null) {
            return;
        }
        markDownEditorPane.getTextArea().paste();
    }


    @Override
    public void run() {
        TreeItem<File> rootTree = treeView.getRoot();
        File dir = rootTree.getValue();
        ImageView iv = new ImageView(folderIcon);
        iv.setSmooth(true);
        iv.setViewport(new Rectangle2D(0, 0, 16, 16));
        TreeItem<File> root = new TreeItem<>(dir, iv);
        searchFile(dir, root);
        Platform.runLater(() -> {
            treeView.setRoot(root);
            root.setExpanded(true);
        });
        FileMonitor.get().stopWatch();
        FileMonitor.get().addWatchFile(dir);
        FileMonitor.get().setListener(this);
        FileMonitor.get().watch();
    }

    @FXML
    public void onCatalog() {
        if (treeView.isVisible()) {
            return;
        }
        outLine.setManaged(false);
        outLine.setVisible(false);
        treeView.setManaged(true);
        treeView.setVisible(true);
    }

    @FXML
    public void onOutLine() {

        if (!outLine.isVisible()) {
            treeView.setManaged(false);
            treeView.setVisible(false);
            outLine.setManaged(true);
            outLine.setVisible(true);
        }
    }

    @FXML
    public void export2HTML(ActionEvent actionEvent) throws IOException {
        if (currentFile == null) {
            showError("请选择并打开一个要导出md文件");
            return;
        }
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Save HTML");
        File dir = directoryChooser.showDialog(Main.get());
        if (dir == null) {
            return;
        }
        if (!dir.exists()) {
            boolean res = dir.createNewFile();
            if (!res) {
                showError("文件夹创建失败");
                return;
            }
        }
        rootPane.setCursor(Cursor.WAIT);
        //导出并保存文件
        CompletableFuture.runAsync(() -> {
            //创建css文件
            File copyCssDir = new File(dir, "css");
            copyCssDir.mkdirs();
            File cssFile=new File(copyCssDir,"markdownpad-github.css");
            if(cssFile.exists()){
                cssFile.delete();
            }
            try (InputStream cssStream = Resource.getResource("css/markdownpad-github.css").openStream()){
                Files.copy(cssStream, cssFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            //导出js文件
            try {
                File jsDir = new File(Resource.getResource("js").toURI());
                FileUtil.copy(jsDir,dir);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String name = currentFile.getName();
            File htmlFile=new File(dir,name.substring(0, name.lastIndexOf(".")) + ".html");
            if(!htmlFile.exists()){
                try {
                    boolean res=htmlFile.createNewFile();
                    if(!res){
                        showError("html文件创建失败");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String text = markDownEditorPane.getMarkDownText();
            CommonmarkPreviewRenderer renderer = (CommonmarkPreviewRenderer) markDownPreview.getRenderer();
            renderer.update(text, null);
            String content = renderer.getHtml();
            //提取imgs
            File imgDir=new File(dir,"images");
            imgDir.mkdirs();
            List<String> imgs=HTMLTagParser.getImageTag(content);

            for (String img:imgs){
                String src=HTMLTagParser.getSingleImageSrc(img);
                if(src.isEmpty()||src.startsWith("http://")||src.startsWith("https://")){
                    continue;
                }
                File imgFile=new File(currentFile.toPath().getParent().toFile(),src);
                if(!imgFile.exists()){
                    continue;
                }
                //把这个图片文件copy到相应目录下
                try {
                    File copyFile=new File(imgDir,imgFile.getName());
                    if(!copyFile.exists()){
                        Files.copy(imgFile.toPath(),copyFile.toPath());
                    }
                    content=content.replace(src,"./images/"+imgFile.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            String html = "<!DOCTYPE html>\n"
                    + "<html>\n"
                    + "<head>\n"
                    + "<meta charset=\"utf-8\" />"
                    +"<title>"+htmlFile.getName()+"</title>"
                    + "<link rel=\"stylesheet\" href=\"./css/markdownpad-github.css\">\n"
                    + "<style>\n"
                    + ".mwfx-editor-selection {\n"
                    + "  border-right: 5px solid #f47806;\n"
                    + "  margin-right: -5px;\n"
                    + "  background-color: rgb(253, 247, 241);\n"
                    + "}\n"
                    + "</style>\n"
                    + "<script src=\"./js/preview.js\"></script>\n"
                    + prismSyntaxHighlighting(markDownEditorPane.getMarkDownAST())
                    + "</head>\n"
                    + "<body>\n"
                    + content
                    + "<script>preview.highlightNodesAt("+content.length()+");</script>\n"
                    + "</body>\n"
                    + "</html>";
            try (FileWriter writer = new FileWriter(htmlFile)) {
                writer.write(html);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).thenAccept(aVoid -> rootPane.setCursor(Cursor.DEFAULT));


    }

    private void showError(String content){
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setContentText(content);
        error.show();
    }

    private String prismSyntaxHighlighting(com.vladsch.flexmark.ast.Node astRoot) {
        HashMap<String, String> prismLangDependenciesMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Resource.getResAsStream("js/prism/lang_dependencies.txt"))))
        {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("{"))
                    continue;

                line = line.replaceAll("(\\[.+),(.+\\])", "$1;$2");
                line = WebViewPreview.trimDelim(line, "{", "}");
                for (String str : line.split(",")) {
                    String[] parts = str.split(":");
                    if (parts[1].startsWith("["))
                        continue; // not supported

                    String key = WebViewPreview.trimDelim(parts[0], "\"", "\"");
                    String value = WebViewPreview.trimDelim(parts[1], "\"", "\"");
                    prismLangDependenciesMap.put(key, value);
                }
            }
        } catch (IOException e) {
            // ignore
        }

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
}
