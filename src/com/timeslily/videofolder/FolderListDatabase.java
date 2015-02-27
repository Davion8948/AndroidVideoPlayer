package com.timeslily.videofolder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * 文件夹数据库
 */
public class FolderListDatabase {
    SQLiteDatabase db;
    private static VideoDatabaseHelper helper;
    Context context;

    public FolderListDatabase(Context context) {
        getHelper(context);
    }

    private static synchronized VideoDatabaseHelper getHelper(Context context) {
        if (helper == null) {
            helper = new VideoDatabaseHelper(context);
        }
        return helper;
    }

    public boolean insert(ContentValues values) {
        String filePath = (String) values.get(FolderListColumns.FOLDER_PATH);
        if (!folderExist(filePath)) {
            db = helper.getWritableDatabase();
            db.insert(FolderListColumns.TABLE_NAME, FolderListColumns.FOLDER_PATH, values);
            db.close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 删除一条记录
     * 
     * @param id:记录的id
     * @return
     */
    public boolean delete(int id) {
        db = helper.getWritableDatabase();
        try {
            db.delete(FolderListColumns.TABLE_NAME, FolderListColumns._ID + "=?", new String[] { String.valueOf(id) });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
        return true;
    }

    /**
     * 查询视频数量
     * 
     * @param id
     * @return
     */
    public int getVideoCount(int id) {
        int videoCount = 0;
        db = helper.getReadableDatabase();
        Cursor c = db.query(FolderListColumns.TABLE_NAME, new String[] { FolderListColumns.VIDEO_COUNT }, FolderListColumns._ID + "=?",
                new String[] { "" + id }, null, null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            videoCount = c.getInt(c.getColumnIndex(FolderListColumns.VIDEO_COUNT));
            c.close();
            db.close();
        }
        return videoCount;
    }

    /**
     * 检测数据库中是否已经存在文件夹路径
     * 
     * @param folderPath 文件夹路径
     * @return false:不存在 true:存在
     */
    private boolean folderExist(String folderPath) {
        db = helper.getReadableDatabase();
        Cursor cursor = db.query(FolderListColumns.TABLE_NAME, null, FolderListColumns.FOLDER_PATH + "=?", new String[] { folderPath }, null, null,
                null);
        return !(cursor == null || cursor.getCount() == 0);

    }

    /**
     * 查询文件夹列表数据库
     * 
     * @param columns 要查询的列
     * @param selection 查询条件
     * @param selectionArgs 查询条件参数
     * @return cursor 数据库游标
     */
    public Cursor query(String[] columns, String selection, String[] selectionArgs) {
        db = helper.getReadableDatabase();
        Cursor cursor = db.query(FolderListColumns.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
        return cursor;
    }

    /**
     * 关闭SQLiteDatabase
     */
    public void closeDatabase() {
        if (db != null) {
            db.close();
        }
    }

    /**
     * @param id 要更新的数据
     * @param cv 数据存储
     */
    public void update(int id, ContentValues cv) {
        db = helper.getWritableDatabase();
        db.update(FolderListColumns.TABLE_NAME, cv, FolderListColumns._ID + "=?", new String[] { id + "" });
        db.close();
    }

    /**
     * 修改最近播放记录
     * 
     * @param path 视频文件夹路径
     */
    public void updateRecentPlay(String path) {
        db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FolderListColumns.RECENT_PLAY, 0);
        db.update(FolderListColumns.TABLE_NAME, values, FolderListColumns.RECENT_PLAY + "=?", new String[] { "1" });
        values.clear();
        values.put(FolderListColumns.RECENT_PLAY, 1);
        db.update(FolderListColumns.TABLE_NAME, values, FolderListColumns.FOLDER_PATH + "=?", new String[] { path });
        db.close();
    }
}
