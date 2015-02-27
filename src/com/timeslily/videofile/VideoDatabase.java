package com.timeslily.videofile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.timeslily.utils.VideoUtil;
import com.timeslily.videofolder.VideoDatabaseHelper;

public class VideoDatabase {

    private static VideoDatabaseHelper helper;//确保只有一个helper实例
    SQLiteDatabase db;

//    Context context;

    public VideoDatabase(Context context) {
//        this.context = context;
        getHelper(context);
    }

    private static synchronized VideoDatabaseHelper getHelper(Context context) {
        if (helper == null) {
            helper = new VideoDatabaseHelper(context);
        }
        return helper;
    }

//    public boolean inset(ContentValues cv) {
//        db = helper.getgetWritableDatabase();
//        return false;
//    }

    public void insertAll(ArrayList<ContentValues> allValues) {
        db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues cv : allValues) {
                db.insert(VideoColumns.TABLE_NAME, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {

        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * 查询父路径下的视频
     * 
     * @param parentPath
     * @return
     */
    public Cursor query(String parentPath) {
        db = helper.getReadableDatabase();
        Cursor c = db.query(VideoColumns.TABLE_NAME, null, VideoColumns.VIDEO_PARENT_PATH + "=?", new String[] { parentPath }, null, null,
                VideoColumns._ID);
        if (c == null) {
            return null;
        } else if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }

    public void closeDatabase() {
        if (db != null) {
            db.close();
        }
    }

    public void update(int id, ContentValues cv) {
        db = helper.getWritableDatabase();
        db.update(VideoColumns.TABLE_NAME, cv, VideoColumns._ID + "=?", new String[] { String.valueOf(id) });
        db.close();
    }

    public void updateRecentPlay() {
        db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(VideoColumns.VIDEO_RECENT_PLAY, 0);
        db.update(VideoColumns.TABLE_NAME, values, VideoColumns.VIDEO_RECENT_PLAY + "=?", new String[] { "1" });
        db.close();
    }

    /**
     * 根据文件夹路径删除数据库中视频文件记录， 同时删除图片缓存
     * 
     * @param folderPath
     */
    public void deleteByParentPath(String folderPath) {
        db = helper.getWritableDatabase();
        Cursor c = db.query(VideoColumns.TABLE_NAME, new String[] { VideoColumns.VIDEO_THUBMNAIL_PATH }, VideoColumns.VIDEO_PARENT_PATH + "=?",
                new String[] { folderPath }, null, null, null);
        if (c != null && c.getCount() > 0) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                String thumbPath = c.getString(c.getColumnIndex(VideoColumns.VIDEO_THUBMNAIL_PATH));
                if (thumbPath != null && !"".equals(thumbPath)) {
                    VideoUtil.deleteCache(thumbPath);
                }

            }
        }
        if (c != null) {
            c.close();
        }
        db.delete(VideoColumns.TABLE_NAME, VideoColumns.VIDEO_PARENT_PATH + "=?", new String[] { folderPath });
        db.close();
    }

    public void updateParentPath(String oldFilePath, String newFilePath) {
        db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(VideoColumns.VIDEO_PARENT_PATH, newFilePath);
        db.update(VideoColumns.TABLE_NAME, cv, VideoColumns.VIDEO_PARENT_PATH + "=?", new String[] { oldFilePath });
        db.close();
    }

    /**
     * 根据id获取视频路径
     * 
     * @param id 视频id
     * @return
     */
    public String queryPathById(int id) {
        db = helper.getReadableDatabase();
        String videoPath = "";
        Cursor cursor = db.query(VideoColumns.TABLE_NAME, new String[] { VideoColumns.VIDEO_PARENT_PATH, VideoColumns.VIDEO_NAME }, VideoColumns._ID
                + "=?", new String[] { id + "" }, null, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            videoPath = "";
        } else {
            cursor.moveToFirst();
            videoPath = cursor.getString(cursor.getColumnIndex(VideoColumns.VIDEO_PARENT_PATH)) + "/"
                    + cursor.getString(cursor.getColumnIndex(VideoColumns.VIDEO_NAME));
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return videoPath;
    }

    /**
     * 删除实际文件已经不存在的记录
     * 
     * @param parentPath
     */
    public void deleteVideoNotExist(File parentFile) {
        String parentPath = parentFile.getAbsolutePath();
        HashMap<String, String> nameMap = new HashMap<String, String>();//存放文件夹下视频
        List<String> thumbnailList = new ArrayList<String>();//存放缓存名称的list
        for (File subFile : parentFile.listFiles()) {
            String subFileName = subFile.getName();
            if (subFile.isFile() && VideoUtil.isSupportedVideo(subFileName)) {//视频文件
                nameMap.put(subFileName, subFileName);
            }
        }
        db = helper.getReadableDatabase();
        db.beginTransaction();
        Cursor c = null;
        try {
            c = db.query(VideoColumns.TABLE_NAME, new String[] { VideoColumns._ID, VideoColumns.VIDEO_NAME, VideoColumns.VIDEO_THUBMNAIL_PATH },
                    VideoColumns.VIDEO_PARENT_PATH + "=?", new String[] { parentPath }, null, null, null);
            if (c != null && c.getCount() > 0) {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    String name = c.getString(c.getColumnIndex(VideoColumns.VIDEO_NAME));
                    if (new File(parentFile, name).exists()) {//文件还存在
                        nameMap.remove(name);
//                        continue;
                    } else {//文件不存在
                        db.delete(VideoColumns.TABLE_NAME, VideoColumns._ID + "=?",
                                new String[] { c.getInt(c.getColumnIndex(VideoColumns._ID)) + "" });
                        //删除缓存图片
                        String thumbnailPath = c.getString(c.getColumnIndex(VideoColumns.VIDEO_THUBMNAIL_PATH));
                        if (thumbnailPath != null && !"".equals(thumbnailPath)) {
                            thumbnailList.add(thumbnailPath);
                        }
                    }
                }
            }
            for (Entry<String, String> entry : nameMap.entrySet()) {
                ContentValues cv = new ContentValues();
                cv.put(VideoColumns.VIDEO_NAME, entry.getValue());
                cv.put(VideoColumns.VIDEO_PARENT_PATH, parentPath);
                db.insert(VideoColumns.TABLE_NAME, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {

        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
            db.close();
            for (int i = 0, size = thumbnailList.size(); i < size; i++) {//删除数据之后删除缓存图片
                VideoUtil.deleteCache(thumbnailList.get(i));
            }
        }
    }

    /**
     * @param id
     */
    public void deleteById(int id) {
        db = helper.getWritableDatabase();
        db.delete(VideoColumns.TABLE_NAME, VideoColumns._ID + "=?", new String[] { id + "" });
        db.close();
    }

    /**
     * 搜索
     * 
     * @param keyword
     * @return
     */
    public Cursor queryByKeyword(String keyword) {
        db = helper.getReadableDatabase();
        Cursor c = db.query(VideoColumns.TABLE_NAME, null, VideoColumns.VIDEO_NAME + " like ?", new String[] { "%" + keyword + "%" }, null, null,
                VideoColumns._ID);
        if (c == null) {
            return null;
        } else if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        return c;
    }
}
