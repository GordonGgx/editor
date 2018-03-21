package com.ggx.editor.interfaces;

import javafx.scene.control.TreeItem;

import java.io.File;

public interface TreeListAction {

    void openFile(File file);

    void deleteFile(File file);

    void deleteDir(TreeItem<File> file,File io);

    void modifyFile(File file);
}
