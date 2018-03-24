package com.ggx.editor.utils;

import java.io.InputStream;
import java.net.URL;

public class Resource {

    public static URL getResource(String path){
        return ClassLoader.getSystemResource(path);
    }

    public static InputStream getResAsStream(String path){
        return ClassLoader.getSystemResourceAsStream(path);
    }
}
