package com.ggx.editor.fileos;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * 文件监控类
 */
public class FileMonitor implements Runnable{

    private static FileMonitor monitor;
    private WatchService watchService;
    private Thread thread;
    private Runnable listener;
    private FileMonitor(){
        try {
            watchService=FileSystems.getDefault().newWatchService();
            thread=new Thread(this,"FileMonitorThread");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            try {
                WatchKey watchKey=watchService.take();
                for (WatchEvent event:watchKey.pollEvents()){
                    WatchEvent.Kind kind=event.kind();
                    if(kind==StandardWatchEventKinds.ENTRY_CREATE){
                        System.out.println("创建="+event.context());
                    }else if(kind==StandardWatchEventKinds.ENTRY_DELETE){
                        System.out.println("删除="+event.context());
                    }else if(kind==StandardWatchEventKinds.ENTRY_MODIFY){
                        System.out.println("修改="+event.context());
                    }else if(kind==StandardWatchEventKinds.OVERFLOW){
                        System.out.println("覆盖="+event.context());
                    }
                    if(listener!=null){
                        listener.run();
                    }
                }
                boolean res=watchKey.reset();
                if(!res){
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }


    public static FileMonitor get(){
        if(monitor==null){
            monitor=new FileMonitor();
        }
        return monitor;
    }

    public void addWatchFile(File file){
        try {
            Paths.get(file.toURI()).register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE
                    /*StandardWatchEventKinds.ENTRY_MODIFY,*/
                    /*StandardWatchEventKinds.OVERFLOW*/);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setListener(Runnable listener) {
        this.listener = listener;
    }

    public void watch(){
        thread.start();
    }

    public void stopWatch(){
        if(thread.isAlive()){
            thread.interrupt();
            thread=null;
        }
        monitor=null;
    }
}
