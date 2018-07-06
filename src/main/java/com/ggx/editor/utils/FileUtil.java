package com.ggx.editor.utils;

import org.omg.CORBA.PUBLIC_MEMBER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    public static void copy(File file, File toFile) throws Exception {
        byte[] b = new byte[1024];
        int a;
        FileInputStream fis;
        FileOutputStream fos;
        if (file.isDirectory()) {
            String filepath = file.getAbsolutePath();
            filepath=filepath.replaceAll("\\\\", "/");
            String toFilepath = toFile.getAbsolutePath();
            toFilepath=toFilepath.replaceAll("\\\\", "/");
            int lastIndexOf = filepath.lastIndexOf("/");
            toFilepath = toFilepath + filepath.substring(lastIndexOf ,filepath.length());
            File copy=new File(toFilepath);
            //复制文件夹
            if (!copy.exists()) {
                copy.mkdir();
            }
            //遍历文件夹
            for (File f : file.listFiles()) {
                copy(f, copy);
            }
        } else {
            if (toFile.isDirectory()) {
                String filepath = file.getAbsolutePath();
                filepath=filepath.replaceAll("\\\\", "/");
                String toFilepath = toFile.getAbsolutePath();
                toFilepath=toFilepath.replaceAll("\\\\", "/");
                int lastIndexOf = filepath.lastIndexOf("/");
                toFilepath = toFilepath + filepath.substring(lastIndexOf ,filepath.length());

                //写文件
                File newFile = new File(toFilepath);
                fis = new FileInputStream(file);
                fos = new FileOutputStream(newFile);
                while ((a = fis.read(b)) != -1) {
                    fos.write(b, 0, a);
                }
            } else {
                //写文件
                fis = new FileInputStream(file);
                fos = new FileOutputStream(toFile);
                while ((a = fis.read(b)) != -1) {
                    fos.write(b, 0, a);
                }
            }

        }
    }

}
