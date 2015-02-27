package com.timeslily.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.util.Log;

public class MemoryCache {
    private Map<String, Bitmap> cache = Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>(10, 1.5f, true));
    private long size = 0;
    private long limit = 1000000;

    public MemoryCache() {
        setLimit(Runtime.getRuntime().maxMemory());
    }

    private void setLimit(long limit) {
        this.limit = limit;
        Log.i("ccc", "MemoryCache will use up to " + limit / 1024. / 1024. + "MB");
    }

    public Bitmap get(String id) {
        try {
            if (!cache.containsKey(id)) {
                return null;
            }
            return cache.get(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void put(String id, Bitmap bitmap) {
        if (cache.containsKey(id)) {
            size -= getSizeInByte(cache.get(id));
        }
        cache.put(id, bitmap);
        size += getSizeInByte(bitmap);
        checkSize();
    }

    private void checkSize() {
        Log.i("ccc", "cache size=" + size + " length=" + cache.size());
        if (size > limit) {
            Iterator<Entry<String, Bitmap>> iter = cache.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, Bitmap> entry = iter.next();
                size -= getSizeInByte(entry.getValue());
                iter.remove();
                if (size < limit) {
                    break;
                }
            }
            Log.i("ccc", "Clean cache. New size " + cache.size());
        }
    }

    public void clear() {
        cache.clear();
    }

    private long getSizeInByte(Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
}
