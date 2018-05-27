package com.ggx.editor.widget;

import com.ggx.editor.interfaces.TreeListAction;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import org.reactfx.EventStreams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextFieldTreeCellImpl extends TreeCell<File> {


    private TextField textField;

    private ContextMenu dirMenu=new ContextMenu();
    private ContextMenu fileMenu=new ContextMenu();
    private TreeListAction action;

    public TextFieldTreeCellImpl(TreeListAction action) {
        setPrefHeight(25);
        this.action=action;
        createDirMenu(dirMenu);
        createFileMenu(fileMenu);
        EventStreams.eventsOf(this,MouseEvent.MOUSE_CLICKED)
                .filter(mouseEvent -> getItem()!=null&&!getItem().isDirectory()&&mouseEvent.getButton()==MouseButton.PRIMARY)
                .subscribe(mouseEvent -> {
                    if(action!=null&&getItem()!=null){
                        action.openFile(getItem());
                    }
                });

        setOnDragDetected(event -> {
            Dragboard dragboard =startDragAndDrop(TransferMode.ANY);
            ClipboardContent content = new ClipboardContent();
            content.putImage(new Image(ClassLoader.getSystemResourceAsStream("icons/file_16.png")));
            content.putString("移动");
            List<File> files=new ArrayList<>();
            files.add(getItem());
            content.putFiles(files);
//            content.putUrl(getItem().toURI().toString());
            dragboard.setContent(content);
        });
        setOnDragEntered(event -> {
            if(event.getGestureSource() !=this){
                setStyle("-fx-border-color:#C0C0C0");
            }
        });
        setOnDragExited(event -> {
            if(event.getGestureSource() !=this){
                setStyle("-fx-border-color:transparent");
            }

        });
        setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasString()
                    &&getItem().isDirectory()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
        });
        //拖动放下
        setOnDragDropped(event -> {
            try {
                File file=event.getDragboard().getFiles().get(0);
                File dest=new File(getItem(),file.getName());
                if(dest.exists()){
                    System.out.println("目标文件已经存在");
                    return;
                }
                if(file.renameTo(dest)){
                    System.out.println("文件移动成功");
                }else {
                    System.out.println("文件移动失败");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });

    }

    @Override
    public void startEdit() {
        super.startEdit();
        createTextField();
        setText(null);
        setGraphic(textField);
        textField.requestFocus();
        textField.selectAll();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        System.out.println("取消编辑");
        setText(getItem().getName());
        setGraphic(getTreeItem().getGraphic());
    }

    @Override
    public void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                System.out.println("编辑");
                if (textField != null) {
                    textField.setText(getString());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getString());
                setGraphic(getTreeItem().getGraphic());
                if(getTreeItem().isLeaf()&&!getItem().isDirectory()){
                    setContextMenu(fileMenu);
                }else {
                    setContextMenu(dirMenu);
                }
            }
        }
    }
    private void createDirMenu(ContextMenu dirMenu){
        MenuItem addFile = new MenuItem("新建文件");
        MenuItem addDir = new MenuItem("新建目录");
        MenuItem deleteDir = new MenuItem("删除");
        addFile.setOnAction((ActionEvent t) -> {
            try {
                File newFile=new File(getItem(),"新建文件.md");
                int n=1;
                while (newFile.exists()){
                    newFile=new File(getItem(),"新建文件("+n+").md");
                    n++;
                }
                if(newFile.createNewFile()){
                    TreeItem<File> newDir = new TreeItem<>(newFile);
                    newDir.setGraphic(new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/file_16.png"))));
                    getTreeItem().getChildren().add(newDir);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        addDir.setOnAction(event -> {
            File dir=new File(getItem(),"newDir");
            if(dir.mkdir()){
                TreeItem<File> newDir = new TreeItem<>(dir);
                ImageView icon=new ImageView(new Image(ClassLoader.getSystemResourceAsStream("icons/folder_16.png")));
                icon.setSmooth(true);
                icon.setViewport(new Rectangle2D(0,0,16,16));
                newDir.setGraphic(icon);
                getTreeItem().getChildren().add(newDir);
            }
        });
        deleteDir.setOnAction(event -> {
            if(action!=null) {
                action.deleteDir(getTreeItem(),getItem());
            }
        });
        dirMenu.getItems().addAll(addFile,addDir,deleteDir);
    }

    private void createFileMenu(ContextMenu fileMenu){
        MenuItem addFile = new MenuItem("打开");
        addFile.setOnAction(event -> {
            if(action!=null)
                action.openFile(getItem());
        });
        MenuItem delete = new MenuItem("删除");
        delete.setOnAction(event -> {
            File file=getItem();
            if(file!=null&&file.delete()){
                getTreeItem().getParent().getChildren().remove(getTreeItem());
                if(action!=null){
                    action.deleteFile(file);
                }
            }
        });
        fileMenu.getItems().addAll(addFile,delete);
    }

    private void createTextField() {
        textField = new TextField(getString());
        textField.setPrefSize(getPrefWidth(),getPrefHeight());
        textField.setOnKeyReleased((KeyEvent t) -> {
            if (t.getCode() == KeyCode.ENTER) {
                File old=getItem();
                File newName=new File(old.getParentFile(),textField.getText());
                if(old.renameTo(newName)){
                    commitEdit(newName);
                    action.modifyFile(newName);
                }
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });

    }

    private String getString() {
        return getItem() == null ? "" : getItem().getName();
    }

}
