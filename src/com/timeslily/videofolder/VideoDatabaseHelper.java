package com.timeslily.videofolder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.timeslily.utils.VideoUtil;
import com.timeslily.videofile.VideoColumns;

public class VideoDatabaseHelper extends SQLiteOpenHelper {


    public VideoDatabaseHelper(Context context) {
        super(context, VideoUtil.DATABASE_NAME, null, VideoUtil.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + FolderListColumns.TABLE_NAME + " (" + FolderListColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FolderListColumns.FOLDER_NAME + " TEXT, " + FolderListColumns.FOLDER_PATH + " TEXT," + FolderListColumns.RECENT_PLAY
                + " INTEGER DEFAULT 0, " + FolderListColumns.NEW_ADD + " INTEGER DEFAULT 0, " + FolderListColumns.VIDEO_COUNT + " INTEGER" + ");");
        String sql = "CREATE TABLE " + VideoColumns.TABLE_NAME + "(" + VideoColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + VideoColumns.VIDEO_NAME
                + " TEXT, "
                +
//                VideoColumns.VIDEO_PATH + 
                " TEXT, " + VideoColumns.VIDEO_THUBMNAIL_PATH + " TEXT, "
                + VideoColumns.VIDEO_PARENT_PATH + " TEXT, " + VideoColumns.VIDEO_DURATION + " INTEGER, " + VideoColumns.VIDEO_IS_PLAYED
                + " INTEGER DEFAULT 0, " + VideoColumns.VIDEO_LAST_PLAY_TIME + " INTEGER DEFAULT 0, " + VideoColumns.VIDEO_RECENT_PLAY
                + " INTEGER DEFAULT 0" + ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("ffmpeg", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + FolderListColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + VideoColumns.TABLE_NAME);
        onCreate(db);
    }
}