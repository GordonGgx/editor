package com.ggx.editor;

import com.ggx.editor.fileos.FileMonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class Test {

    public static void main(String[] args) throws IOException {
        FileMonitor monitor=FileMonitor.get();
        monitor.addWatchFile(new File("E://test"));
        monitor.watch();


    }
}
