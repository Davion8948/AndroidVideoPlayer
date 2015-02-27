package com.timeslily;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.timeslily.utils.VideoUtil;
import com.timeslily.videofile.VideoColumns;
import com.timeslily.videofile.VideoDatabase;
import com.timeslily.videofile.VideoItem;
import com.timeslily.videofolder.FolderAdapter;
import com.timeslily.videofolder.FolderItem;
import com.timeslily.videofolder.FolderListColumns;
import com.timeslily.videofolder.FolderListDatabase;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class VideoFolderListActivity extends ListActivity {
    /** 数据库查询列 */
    private static final String[] PROJECTION = { FolderListColumns._ID, FolderListColumns.FOLDER_NAME, FolderListColumns.VIDEO_COUNT,
            FolderListColumns.FOLDER_PATH, FolderListColumns.RECENT_PLAY };
    /** 文件夹list */
    List<FolderItem> folders = new ArrayList<FolderItem>();
    FolderAdapter adapter;
    private ProgressBar loadingProgressBar;
    FolderListDatabase db;
    VideoDatabase videoDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folder_list);
        if (VideoUtil.VERSION >= 11) {
            getActionBar().setDisplayShowHomeEnabled(false);
        }
        setTitle(getResources().getString(R.string.folder));
        loadingProgressBar = (ProgressBar) findViewById(R.id.pb_loading_folders);
        db = new FolderListDatabase(this);
        videoDB = new VideoDatabase(this);
        adapter = new FolderAdapter(this, folders);
        setListAdapter(adapter);
        getListView().setOnItemLongClickListener(myLongClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(true);
    }

//    private static final int WIDTH = 80;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        if (VideoUtil.VERSION < 11) {//menu不显示图片
            for (int i = 0, count = menu.size(); i < count; i++) {
                menu.getItem(i).setIcon(null);
            }
        } else {
            // Associate searchable configuration with the SearchView
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        return true;
    }

    //图片缩放
//    private Drawable zoomDrawable(Drawable drawable, int w, int h) {
//        int width = drawable.getIntrinsicWidth();
//        int height = drawable.getIntrinsicHeight();
//        Bitmap oldbmp = drawableToBitmap(drawable);
//        Matrix matrix = new Matrix();
//        float scaleWidth = ((float) w / width);
//        float scaleHeight = ((float) h / height);
//        matrix.postScale(scaleWidth, scaleHeight);
//        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height, matrix, true);
//        return new BitmapDrawable(null, newbmp);
//    }
//
//    private Bitmap drawableToBitmap(Drawable drawable) {
//        int width = drawable.getIntrinsicWidth();
//        int height = drawable.getIntrinsicHeight();
//        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
//        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
//        Canvas canvas = new Canvas(bitmap);
//        drawable.setBounds(0, 0, width, height);
//        drawable.draw(canvas);
//        return bitmap;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
        case R.id.add_video_folder:
            Intent intent = new Intent(this, FileListActivity.class);
            startActivity(intent);
            break;
        case R.id.menu_play_last:
            playLastVideo();
            break;
        case R.id.menu_refresh:
            refreshList(true);
            break;
        case R.id.menu_search:
            if (VideoUtil.VERSION < 11) {
                onSearchRequested();
            }
            break;
        case R.id.menu_settings:
            startActivity(new Intent(VideoFolderListActivity.this, SettingsActivity.class));
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
//        Bundle bundle = new Bundle();
//        bundle.putString("data", "haha");
        startSearch("", false, null, false);
        return true;
    }

    /**
     * 播放上一次视频
     */
    private void playLastVideo() {
        SharedPreferences sp = getSharedPreferences(VideoUtil.SETTING_INFOS, Context.MODE_PRIVATE);
        int lastPlayId = sp.getInt(VideoUtil.LAST_PLAY_VIDEO_INFO, -1);
        if (lastPlayId == -1) {//不存在播放记录
            Toast.makeText(this, getResources().getString(R.string.last_play_not_exist), Toast.LENGTH_SHORT).show();
            return;
        }
        String lastPlayPath = videoDB.queryPathById(lastPlayId);
        File file = new File(lastPlayPath);
        if (!file.exists()) {//判断上次播放视频是否还存在
            Toast.makeText(this, getResources().getString(R.string.last_play_video_not_exist), Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<VideoItem> videos = new ArrayList<VideoItem>();
        int position = 0;
        String parentPath = file.getParent();
        Cursor c = videoDB.query(parentPath);
        int count;
        if (c != null && (count = c.getCount()) > 0) {
            c.moveToFirst();
            for (int i = 0; i < count; i++) {
                VideoItem item = new VideoItem();
                int id = c.getInt(c.getColumnIndex(VideoColumns._ID));
                String videoName = c.getString(c.getColumnIndex(VideoColumns.VIDEO_NAME));
//                String videoPath = c.getString(c.getColumnIndex(VideoColumns.VIDEO_PATH));
                String videoPath = parentPath + "/" + videoName;
                if (lastPlayPath.equals(videoPath)) {//添加到当前的视频
                    position = i;
                }
//                String videoDuration = VideoUtil.formatTime(c.getInt(c.getColumnIndex(VideoColumns.VIDEO_DURATION)));
                long lastPlay = c.getLong(c.getColumnIndex(VideoColumns.VIDEO_LAST_PLAY_TIME));
                String lastPlayedTime = VideoUtil.formatTime((int) lastPlay);
//                int recentPlay = c.getInt(c.getColumnIndex(VideoColumns.VIDEO_RECENT_PLAY));//0 未播放 1 播放过
//                int isPlayed = c.getInt(c.getColumnIndex(VideoColumns.VIDEO_IS_PLAYED));
                item.setId(id);
                item.setVideoName(videoName);
                item.setVideoPath(videoPath);
//                item.setVideoParentPath(filePath);
                item.setLastPlayedTime(lastPlayedTime);
                item.setLastPlay(lastPlay);
                videos.add(item);
                c.moveToNext();
            }
        }
        if (c != null) {
            c.close();
        }
        videoDB.closeDatabase();
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putParcelableArrayListExtra("videos", videos);
        intent.putExtra("position", position);
        startActivity(intent);
    }

    /**
     * 刷新列表
     * 
     * @param isCheck 是否去校验数据库
     */
    public void refreshList(boolean isCheck) {
        if (isCheck) {
            for (FolderItem item : folders) {//遍历文件
                int folderId = item.getFolderId();//id
                String folderPath = item.getFolderPath();
                //如果文件已经不存在
                File folderFile = new File(folderPath);
                if (!folderFile.exists()) {
                    db.delete(folderId);//删除数据
                    videoDB.deleteByParentPath(folderPath);
                    continue;//进行下一个文件夹检验
                }
                int videoCount = VideoUtil.getVideoCount(folderPath);//现在视频数量
//                if (videoCount != db.getVideoCount(folderId)) {//数据不匹配 更新数据
                // 更新底下文件
                ContentValues cv = new ContentValues();
                cv.put(FolderListColumns.VIDEO_COUNT, videoCount);
                db.update(folderId, cv);
                //判断增加文件
                videoDB.deleteVideoNotExist(folderFile);
//                    VideoUtil.updateVideosUnderFolder(this, folderFile);
//                }
            }
        }
        loadingProgressBar.setVisibility(View.VISIBLE);
        new VideoFolderTask().execute();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FolderItem folder = (FolderItem) l.getItemAtPosition(position);
        String filePath = folder.getFolderPath();
        if (new File(filePath).exists()) {//文件夹存在
            Intent intent = new Intent(this, VideoListActivity.class);
            intent.putExtra("filePath", filePath);
            startActivity(intent);
        } else {
            Toast.makeText(this, getResources().getString(R.string.folder_not_exist), Toast.LENGTH_SHORT).show();
            db.delete(folder.getFolderId());
            videoDB.deleteByParentPath(folder.getFolderPath());
            refreshList(false);
        }
    }

    /**
     * 长按事件
     */
    private OnItemLongClickListener myLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            createDialog(position);
            return false;
        }
    };

    private void createDialog(int position) {
        final FolderItem item = folders.get(position);
        Builder builder = new Builder(this);
        builder.setTitle(item.getFolderName());
        builder.setItems(R.array.dialog_items, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                case 0://delete
                    deleteFoler(item);
                    break;
                case 1:
                    renameFolder(item);
                    break;
                case 2:
                    getFolderInfo(item);
                    break;
                }
            }

        });
        builder.create().show();
    }

    /**
     * 删除文件夹
     */
    private void deleteFoler(final FolderItem item) {
        final Resources res = getResources();
        Builder builder = new Builder(this);
        builder.setTitle(res.getString(R.string.delete));
        builder.setMessage(item.getFolderName());
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.dialog_delete_folder, null);
        final CheckBox chk = (CheckBox) layout.findViewById(R.id.cb_delete_folder);//checkbox选择删除文件
        final String folderPath = item.getFolderPath();//
        if (folderPath.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {//sdcard不显示checkbox
            chk.setVisibility(View.GONE);
        } else {
            chk.setVisibility(View.VISIBLE);
        }
        builder.setView(layout);
        builder.setPositiveButton(res.getString(R.string.ok), new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                int folderId = item.getFolderId();
                db.delete(folderId);//删除数据库
                videoDB.deleteByParentPath(folderPath);//删除数据库视频信息
                if (chk.isChecked()) {
                    try {
//                        new File(folderPath).delete();
                        new DeleteAyncTask().execute(folderPath);
//                        VideoUtil.deleteFile(new File(folderPath));
//                        Toast.makeText(getApplicationContext(), res.getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
//                        Toast.makeText(getApplicationContext(), res.getString(R.string.delete_failed), Toast.LENGTH_SHORT).show();
                    }
                }
                refreshList(false);
            }
        });
        builder.setNegativeButton(res.getString(R.string.cancel), null);
        builder.create().show();
    }

    /**
     * 重命名文件夹
     * 
     * @param item
     * @param db
     */
    private void renameFolder(final FolderItem item) {
        final String folderPath = item.getFolderPath();//文件路径
        final File folder = new File(folderPath);
        if (!folder.exists()) {//文件夹已经不存在
            Toast.makeText(this, getResources().getString(R.string.folder_not_exist), Toast.LENGTH_SHORT).show();
            db.delete(item.getFolderId());
            videoDB.deleteByParentPath(folderPath);
            refreshList(false);//刷新列表
            return;
        }
        final String folderName = item.getFolderName();
        Resources res = getResources();
        final String renameFailed = res.getString(R.string.rename_failed);
        final String fileNameIllegal = res.getString(R.string.file_name_illegal);
        final String fileNameExist = res.getString(R.string.file_name_exist);
        final String cannotRenameSdcard = res.getString(R.string.can_not_rename_sdcard);
        LinearLayout layout = (LinearLayout) (LayoutInflater.from(this).inflate(R.layout.dialog_rename, null));
        final EditText renameEditText = (EditText) layout.findViewById(R.id.et_rename);
        renameEditText.setText(folderName);
        Builder builder = new Builder(this);
        builder.setTitle(res.getString(R.string.rename));
        builder.setView(layout);
        builder.setPositiveButton(res.getString(R.string.ok), new AlertDialog.OnClickListener() {//保存文件名

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = renameEditText.getText().toString().trim();
                        setDialogDismiss(dialog, true);

                        if (!folderName.equals(newName)) {//文件名更改了
                            String sdcard = Environment.getExternalStorageDirectory().getName();
                            if (folderName.equals(sdcard)) { //sdcard不能重命名
                                Toast.makeText(getApplicationContext(), cannotRenameSdcard, Toast.LENGTH_SHORT).show();
                            } else {//!sdcard
                                if (VideoUtil.isValidFileName(newName)) {//合法
                                    try {
                                        File newFile = new File(folder.getParent(), newName);
                                        if (newFile.exists()) {//文件名已经存在
                                            setDialogDismiss(dialog, false);
                                            Toast.makeText(getApplicationContext(), fileNameExist, Toast.LENGTH_SHORT).show();
                                        } else {
                                            folder.renameTo(newFile);
                                            ContentValues cv = new ContentValues();
                                            cv.put(FolderListColumns.FOLDER_NAME, newName);
                                            String newFilePath = newFile.getAbsolutePath();
                                            cv.put(FolderListColumns.FOLDER_PATH, newFilePath);
                                            db.update(item.getFolderId(), cv);
                                            videoDB.updateParentPath(folderPath, newFilePath);
                                            refreshList(false);
                                        }
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(), renameFailed, Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                } else {
                                    setDialogDismiss(dialog, false);
                                    Toast.makeText(getApplicationContext(), fileNameIllegal, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
        builder.setNegativeButton(res.getString(R.string.cancel), new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                setDialogDismiss(dialog, true);
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        renameEditText.selectAll();
        renameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    /**
     * 设置点击按钮对话框是否消失
     * 
     * @param dialog
     * @param dismiss true对话框关闭 false 对话框不关闭
     */
    private void setDialogDismiss(DialogInterface dialog, boolean dismiss) {
        try {
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, dismiss); //将mShowing变量设为false，表示对话框已关闭 
        } catch (Exception e) {

        }
    }

    private void getFolderInfo(FolderItem item) {
        String path = item.getFolderPath();
        File file = new File(path);
        if (!file.exists()) {//文件夹已经不存在
            Toast.makeText(this, getResources().getString(R.string.folder_not_exist), Toast.LENGTH_SHORT).show();
            db.delete(item.getFolderId());
            videoDB.deleteByParentPath(item.getFolderPath());
            refreshList(false);//刷新列表
            return;
        }
        Builder builder = new Builder(this);
        builder.setTitle(item.getFolderName());
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.folder_properties_layout, null);
        TextView pathTextView = (TextView) layout.findViewById(R.id.tv_folder_path);
        TextView dateTextView = (TextView) layout.findViewById(R.id.tv_folder_date);
        TextView videoCountTextView = (TextView) layout.findViewById(R.id.tv_folder_video_count);
        TextView sizeTextView = (TextView) layout.findViewById(R.id.tv_folder_size);
        pathTextView.setText(path);
        dateTextView.setText(VideoUtil.getFolderDate(file));
        videoCountTextView.setText(item.getVideoCount() + "");
        sizeTextView.setText(VideoUtil.getFolderVideosSize(file));
        builder.setView(layout);
        builder.setPositiveButton(getResources().getString(R.string.ok), null);
        builder.create().show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class VideoFolderTask extends AsyncTask<Void, FolderItem, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {

            if (folders.size() > 0) {
                folders.clear();
            }
            Cursor cursor = db.query(PROJECTION, null, null);
            int count = 0;
            if (cursor != null && (count = cursor.getCount()) > 0) {//初始化count
                cursor.moveToFirst();
                for (int i = 0; i < count; i++) {
                    if (!isCancelled()) {//未取消
                        int folderId = cursor.getInt(cursor.getColumnIndex(FolderListColumns._ID));
                        String folderName = cursor.getString(cursor.getColumnIndex(FolderListColumns.FOLDER_NAME));
                        int videoCount = cursor.getInt(cursor.getColumnIndex(FolderListColumns.VIDEO_COUNT));
                        String folderPath = cursor.getString(cursor.getColumnIndex(FolderListColumns.FOLDER_PATH));
                        int recentPlay = cursor.getInt(cursor.getColumnIndex(FolderListColumns.RECENT_PLAY));
                        FolderItem folder = new FolderItem(folderId, folderPath, folderName, videoCount);
                        folder.setRecentPlay(recentPlay);
                        publishProgress(folder);
                        cursor.moveToNext();
                    }
                }
            } else {//删除最后一个文件夹
                return 0;
            }
            if (isCancelled()) {
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
            if (cursor != null) {
                cursor.close();
            }
            db.closeDatabase();
            return 1;
        }

        @Override
        protected void onProgressUpdate(final FolderItem... values) {
            super.onProgressUpdate(values);
            runOnUiThread(new Runnable() {
                public void run() {
                    folders.add(values[0]);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 0) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
            loadingProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    class DeleteAyncTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            if (params != null && params[0] != null) {
                String folderPath = params[0];
                if (VideoUtil.deleteFolder(new File(folderPath))) {
                    return true;
                } else {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.delete_failed), Toast.LENGTH_SHORT).show();

            }
        }
    }
}
