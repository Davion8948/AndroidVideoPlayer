package com.timeslily.utils;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.timeslily.R;

public class ImageLoader {
    MemoryCache memoryCache = new MemoryCache();
    FileCache fileCache;
    private Map<ImageView, String> imagesViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    ExecutorService executorService;

    public ImageLoader(Context context) {
        fileCache = new FileCache(context);
        executorService = Executors.newFixedThreadPool(5);
    }

    final int stud_id = R.drawable.ic_default_thumbnail;

    public void displayImage(String id, ImageView imageView) {
        Bitmap bitmap = memoryCache.get(id);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            queuePhoto(id, imageView);
            imageView.setImageResource(stud_id);

        }
    }

    private void queuePhoto(String id, ImageView imageView) {


    }

    private class PhotoToLoad {
        public String id;
        public ImageView imageView;

        public PhotoToLoad(String id, ImageView imageView) {
            this.id = id;
            this.imageView = imageView;

        }
    }

    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;

        public PhotosLoader(PhotoToLoad photoToLoad) {
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {

        }

    }
}
