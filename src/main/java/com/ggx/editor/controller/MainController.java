package com.ggx.editor.controller;

import com.ggx.editor.Main;
import com.ggx.editor.editor.FooterPane;
import com.ggx.editor.editor.MarkDownEditorPane;
import com.ggx.editor.editor.preview.MarkDownPreviewPane;
import com.ggx.editor.fileos.FileMonitor;
import com.ggx.editor.interfaces.TreeListAction;
import com.ggx.editor.utils.FileUtil;
import com.ggx.editor.widget.TextFieldTreeCellImpl;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.transitions.hamburger.HamburgerBackArrowBasicTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.reactfx.Change;
import org.reactfx.EventStreams;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MainController  implements Initializable, TreeListAction,Runnable {

    @FXML public StackPane root;
    @FXML public BorderPane rootPane;
    @FXML public TreeView<File> treeView;
    @FXML public StackPane fileContainer;
    @FXML public SplitPane splitePane;
    @FXML public JFXHamburger jfxHamburger;
    @FXML public StackPane leftBtn;
    @FXML public Label title;
    @FXML public BorderPane leftPane;
    @FXML public BorderPane rightPane;
    @FXML public ToggleGroup toggle;
    @FXML public HBox toggleContainer;
    @FXML public MenuItem save;
    @FXML public JFXDialog dialog;
    @FXML public JFXButton acceptButton;
    @FXML public ToolBar titleBar;
    @FXML public MenuItem findAction;
    @FXML public BorderPane editorContainer;
    @FXML public MenuItem pasteAction;
    @FXML public MenuItem copyAction;
    @FXML public MenuItem cutAction;
    @FXML public MenuItem editorAction;
    @FXML public MenuItem eyeAction;
    @FXML public MenuItem previewAction;


    private final Image folderIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/folder_16.png"));
    private final Image fileIcon = new Image(ClassLoader.getSystemResourceAsStream("icons/file_16.png"));

    private HamburgerBackArrowBasicTransition burgerTask3;

    private File currentFile;

    private MarkDownPreviewPane markDownPreview;
    private MarkDownEditorPane markDownEditorPane;
    private FooterPane footerPane;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        markDownPreview=new MarkDownPreviewPane();
        markDownEditorPane=new MarkDownEditorPane(editorContainer);
        footerPane=new FooterPane(markDownEditorPane.getTextArea());
        rootPane.setBottom(footerPane.getNode());


        treeView.setShowRoot(true);
        treeView.setEditable(true);
        treeView.setCellFactory(param -> new TextFieldTreeCellImpl(this));


        jfxHamburger.setMaxSize(20, 10);
        burgerTask3 = new HamburgerBackArrowBasicTransition(jfxHamburger);
        burgerTask3.setRate(-1);

        EventStreams.changesOf(rootPane.widthProperty()).subscribe(this::changeWidth);
        EventStreams.changesOf(toggle.selectedToggleProperty()).subscribe(this::changeEditorView);

        markDownPreview.markdownTextProperty().bind(markDownEditorPane.markDownTextProperty());
        markDownPreview.markdownASTProperty().bind(markDownEditorPane.markDownASTProperty());
        markDownPreview.scrollYProperty().bind(markDownEditorPane.scrollYProperty());
        markDownPreview.editorSelectionProperty().bind(markDownEditorPane.selectionProperty());
    }

    private void changeWidth(Change<Number> numberChange){
        splitePane.setDividerPosition(0, 0.18);
        if (rightPane.getCenter() != null) {
            markDownPreview.setWidth((rootPane.getWidth() - leftPane.getWidth()) / 2);
        } else {
            markDownPreview.setWidth(rootPane.getWidth() - leftPane.getWidth());
        }
    }

    private void changeEditorView(Change<Toggle> toggleChange){
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

    @Override
    public void openFile(File file) {
        if(!file.exists()){
            return;
        }
        currentFile = file;
        initOpenFile();
        changeTextType(file);
        BufferedReader br = null;
        try {
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new FileReader(file));
            br.lines().map(s -> s + "\n").forEach(sb::append);
            markDownEditorPane.setNewFileContent(sb.toString());
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
        if (fileContainer.getChildren().size() == 2) {
            fileContainer.getChildren().remove(1);
        }
        fileContainer.getChildren().add(markDownEditorPane.getScrollPane());
        markDownEditorPane.getScrollPane().scrollYToPixel(0);


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
        DirectoryChooser chooser=new DirectoryChooser();
        File dir=chooser.showDialog(Main.get());
        if(dir!=null&&dir.exists()){
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
        FileChooser fileChooser=new FileChooser();
        fileChooser.setTitle("Save dir");
        File dir=fileChooser.showSaveDialog(Main.get());
        if(!dir.exists()){
            if(dir.mkdir()){
                System.out.println("创建成功");
                FileMonitor.get().addWatchFile(dir);
                clear();
                ImageView iv = new ImageView(folderIcon);
                iv.setSmooth(true);
                iv.setViewport(new Rectangle2D(0, 0, 16, 16));
                TreeItem<File> rootTree = new TreeItem<>(dir, iv);
                treeView.setShowRoot(true);
                treeView.setRoot(rootTree);
            }else {
                Alert error=new Alert(Alert.AlertType.ERROR);
                error.setContentText("工作空间创建失败.");
                error.initOwner(Main.get());
                error.show();
            }
        }
    }

    @FXML
    public void exitApp() {
        if(currentFile!=null&&currentFile.exists()){
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
        if(currentFile!=null&&currentFile.exists()){
            markDownEditorPane.saveFile(currentFile);
        }
    }
    private void initOpenFile(){
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

    private void deleteFileAction(){
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
    private void clear(){
        if (fileContainer.getChildren().size() == 2) {
            fileContainer.getChildren().remove(1);
        }
        currentFile=null;
        deleteFileAction();
        title.setText(null);
        title.setGraphic(null);
    }

    private void changeTextType(File file){
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
        if(currentFile==null){
            return;
        }
        markDownEditorPane.showFindPane();
    }

    @FXML
    public void changeEditor() {
        if(currentFile==null){
            return;
        }
        toggle.selectToggle(toggle.getToggles().get(0));
    }

    @FXML
    public void changeEye() {
        if(currentFile==null){
            return;
        }
        toggle.selectToggle(toggle.getToggles().get(1));
    }

    @FXML
    public void changePreview() {
        if(currentFile==null){
            return;
        }
        toggle.selectToggle(toggle.getToggles().get(2));
    }

    @FXML
    public void openSettings() {

    }

    @FXML
    public void onCut() {
        if(currentFile==null){
            return;
        }
        markDownEditorPane.getTextArea().cut();
    }

    @FXML
    public void onCopy() {
        if(currentFile==null){
            return;
        }
        markDownEditorPane.getTextArea().copy();
    }

    @FXML
    public void onPaste() {
        if(currentFile==null){
            return;
        }
        markDownEditorPane.getTextArea().paste();
    }


    @Override
    public void run() {
        TreeItem<File> rootTree=treeView.getRoot();
        File dir=rootTree.getValue();
        ImageView iv = new ImageView(folderIcon);
        iv.setSmooth(true);
        iv.setViewport(new Rectangle2D(0, 0, 16, 16));
        TreeItem<File> root = new TreeItem<>(dir, iv);
        searchFile(dir, root);
        Platform.runLater(()->{
            treeView.setRoot(root);
            root.setExpanded(true);
        });
        FileMonitor.get().stopWatch();
        FileMonitor.get().addWatchFile(dir);
        FileMonitor.get().setListener(this);
        FileMonitor.get().watch();
    }
}
