package com.timeslily.utils;

import java.io.File;

import android.content.Context;

public class FileCache {
    private File cacheDir;

    public FileCache(Context context) {
        cacheDir = context.getCacheDir();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public File getFile(String id) {
        File file = new File(cacheDir, id);
        return file;
    }

    public void clear(){
        File[] files = cacheDir.listFiles();
        if(files==null){
            return;
        }
        for(File file :files){
            file.delete();
        }
    }
}
