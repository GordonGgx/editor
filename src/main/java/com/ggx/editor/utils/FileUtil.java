package com.ggx.editor.utils;

import org.omg.CORBA.PUBLIC_MEMBER;

import java.io.File;
import java.nio.file.FileStore;

public class FileUtil {

    public static String suffixName(File file){
        if(file==null){
            return "";
        }
        String fileName=file.getName();
        if(fileName.contains(".")){
            int index=fileName.lastIndexOf(".");
            return fileName.substring(index,fileName.length());
        }else {
            return "";
        }
    }

    public static String prefixName(File file){
        if(file==null){
            return "";
        }
        String fileName=file.getName();
        if(fileName.contains(".")){
            int index=fileName.lastIndexOf(".");
            return fileName.substring(0,index);
        }else {
            return "";
        }
    }

    public static boolean deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return true;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                if (!file.delete()) {
                    return false;
                }
            }
        }
        return !dir.exists() || dir.delete();
    }

    public static void moveTo(File file,File to){

    }
}
