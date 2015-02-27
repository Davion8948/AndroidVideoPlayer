package com.timeslily;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.provider.SearchRecentSuggestions;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.timeslily.utils.VideoUtil;
import com.timeslily.videofile.VideoAdapter;
import com.timeslily.videofile.VideoColumns;
import com.timeslily.videofile.VideoDatabase;
import com.timeslily.videofile.VideoItem;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class VideoListActivity extends ListActivity {
    /** 文件夹下视频数据 */
    private ArrayList<VideoItem> videos = new ArrayList<VideoItem>();
    VideoAdapter adapter;
//    private ProgressBar loadingProgressBar;
    private String filePath = "";
    private VideoInfoLoader loaderTask;
    private Bitmap defaultThumbnail;
    /** 缩略图宽度 */
    public int thumbnailWidth;
    /** 缩略图高度 */
    public int thumbnailHeight;
    File cacheFile;
    /** 默认图片缓存 */
    String defaultThumbnailPath;
    VideoDatabase videoDB;
    /** 搜索内容 */
    private String queryWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_list);
        if (VideoUtil.VERSION >= 11) {
            getActionBar().setDisplayShowHomeEnabled(false);
        }
        videoDB = new VideoDatabase(this);
        saveDefaultThumbnail();
        handleIntent(getIntent());
        adapter = new VideoAdapter(videos, this);
        setListAdapter(adapter);
        getListView().setOnItemLongClickListener(myLongClickListener);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            filePath = intent.getStringExtra("filePath");
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                queryWord = intent.getStringExtra(SearchManager.QUERY);
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, RecentProvider.AUTHORITY, RecentProvider.MODE);
                suggestions.saveRecentQuery(queryWord, null);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (filePath != null && !"".equals(filePath)) {//从文件夹来
            File file = new File(filePath);
            String title = file.getName();
            if (title.length() > 16) {
                title = title.substring(0, 16) + "...";
            }
            setTitle(title);
            if (loadData()) {//有数据
                loaderTask = new VideoInfoLoader(this);
                loaderTask.execute();
            } else {//无数据
                getListView().setVisibility(View.INVISIBLE);
                findViewById(R.id.tv_no_video).setVisibility(View.VISIBLE);
            }
        } else if (!"".equals(queryWord)) {//搜索
            setTitle(getResources().getString(R.string.search) + "'" + queryWord + "'");
            if (queryData(queryWord)) {
                loaderTask = new VideoInfoLoader(this);
                loaderTask.execute();
            } else {
                getListView().setVisibility(View.INVISIBLE);
                findViewById(R.id.tv_no_result).setVisibility(View.VISIBLE);
            }
        }
    }

    private OnItemLongClickListener myLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            createDialog(position);
            return false;
        }
    };

    private void createDialog(final int position) {
        final VideoItem item = videos.get(position);
        String title = item.getVideoName();
        title = title.substring(0, title.lastIndexOf('.'));
        Builder builder = new Builder(this);
        builder.setTitle(title);
        builder.setItems(R.array.video_dialog_items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0:
                    playVideo(position);
                    break;
                case 1:
                    deleteVideo(position, item);
                    break;
                case 2:
                    renameVideo(position, item);
                    break;
                case 3:
                    videoProperties(position, item);
                    break;
                }
            }
        });
        builder.create().show();
    }

    /**
     * 长按删除按钮
     * 
     * @param position
     */
    protected void deleteVideo(final int position, VideoItem item) {
        String videoPath = item.getVideoPath();
        final File videoFile = new File(videoPath);
        final int id = item.getId();
        Resources res = getResources();
        if (!videoFile.exists()) {//文件已经不存在 删除数据库中记录，并刷新界面
            videoDB.deleteById(id);
            Toast.makeText(this, res.getString(R.string.video_not_exist), Toast.LENGTH_SHORT).show();
            videos.remove(position);
            adapter.notifyDataSetChanged();
            return;
        }

        Builder builder = new Builder(this);
        builder.setTitle(res.getString(R.string.delete));
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        TextView videoNameteTextView = (TextView) layout.findViewById(R.id.tv_delete_video_name);
        videoNameteTextView.setText(item.getVideoName());
        builder.setView(layout);
        builder.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                videoDB.deleteById(id);
                videoFile.delete();
                videos.remove(position);
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(res.getString(R.string.cancel), null);
        builder.create().show();
    }

    /**
     * 重命名视频
     * 
     * @param position
     */
    protected void renameVideo(int position, final VideoItem item) {
        String videoPath = item.getVideoPath();
        final File videoFile = new File(videoPath);
        final int id = item.getId();
        Resources res = getResources();
        if (!videoFile.exists()) {//文件已经不存在 删除数据库中记录，并刷新界面
            videoDB.deleteById(id);
            Toast.makeText(this, res.getString(R.string.video_not_exist), Toast.LENGTH_SHORT).show();
            videos.remove(position);
            adapter.notifyDataSetChanged();
            return;
        }

        Builder builder = new Builder(this);
        builder.setTitle(res.getString(R.string.rename));
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.dialog_rename, null);
        final EditText renameEditText = (EditText) layout.findViewById(R.id.et_rename);
        String name = item.getVideoName();
        int suffixIndex = name.lastIndexOf('.');
        final String videoName = name.substring(0, suffixIndex);
        final String videoSuffix = name.substring(suffixIndex);
        final String renameFailed = res.getString(R.string.rename_failed);
        final String fileNameIllegal = res.getString(R.string.file_name_illegal);
        final String fileNameExist = res.getString(R.string.file_name_exist);
        final String parentPath = item.getVideoParentPath();
        renameEditText.setText(videoName);
        builder.setView(layout);
        builder.setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                setDialogDismiss(dialog, true);
            }
        });
        builder.setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = renameEditText.getText().toString().trim();//视频名称 无后缀
                setDialogDismiss(dialog, true);
                if (!videoName.equals(newName)) {
                    if (VideoUtil.isValidFileName(newName)) {
                        try {
                            String newVideoName = newName + videoSuffix;//新命名的视频名称 带后缀
                            File newFile = new File(parentPath, newVideoName);
                            if (newFile.exists()) {
                                setDialogDismiss(dialog, false);
                                Toast.makeText(getApplicationContext(), fileNameExist, Toast.LENGTH_SHORT).show();
                            } else {
                                videoFile.renameTo(newFile);
                                ContentValues cv = new ContentValues();
                                cv.put(VideoColumns.VIDEO_NAME, newVideoName);
                                videoDB.update(id, cv);
                                item.setVideoName(newVideoName);//更新item中的name
                                item.setVideoPath(newFile.getAbsolutePath());//更新文件路径
                                adapter.notifyDataSetChanged();
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

    /**
     * 视频信息
     * 
     * @param position
     */
    protected void videoProperties(final int position, VideoItem item) {
        String videoPath = item.getVideoPath();
        File videoFile = new File(videoPath);
        int id = item.getId();
        Resources res = getResources();
        if (!videoFile.exists()) {//文件已经不存在 删除数据库中记录，并刷新界面
            videoDB.deleteById(id);
            Toast.makeText(this, res.getString(R.string.video_not_exist), Toast.LENGTH_SHORT).show();
            videos.remove(position);
            adapter.notifyDataSetChanged();
            return;
        }
        Builder builder = new Builder(this);
        String videoName = item.getVideoName();
        builder.setTitle(videoName.substring(0, videoName.lastIndexOf('.')));
        ScrollView layout = (ScrollView) LayoutInflater.from(this).inflate(R.layout.video_properties, null);
        TextView nameTextView = (TextView) layout.findViewById(R.id.tv_video_name);
        nameTextView.setText(item.getVideoName());
        TextView locationTextView = (TextView) layout.findViewById(R.id.tv_video_location);
        locationTextView.setText(item.getVideoPath());
        TextView sizeTextView = (TextView) layout.findViewById(R.id.tv_video_size);
        sizeTextView.setText(VideoUtil.getFileSize(videoFile));
        TextView dateTextView = (TextView) layout.findViewById(R.id.tv_video_date);
        dateTextView.setText(VideoUtil.getFolderDate(videoFile));
        TextView playTimeTextView = (TextView) layout.findViewById(R.id.tv_video_play_time);
        playTimeTextView.setText(item.getVideoDuration());
        builder.setView(layout);
        builder.setPositiveButton(res.getString(R.string.ok), null);
        builder.create().show();
    }

    private void saveDefaultThumbnail() {
        defaultThumbnail = decodeSampledBitmap(getResources(), R.drawable.ic_default_thumbnail, 100, 100);
        thumbnailWidth = defaultThumbnail.getWidth();
        thumbnailHeight = defaultThumbnail.getHeight();
        cacheFile = this.getCacheDir();
        if (!cacheFile.exists()) {
            cacheFile.mkdirs();
        }
        //把defaultThumbnail存到缓存
        defaultThumbnailPath = saveBitmap(defaultThumbnail, -R.drawable.ic_default_thumbnail + "");
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private boolean loadData() {

        if (videos.size() > 0) {
            videos.clear();
        }
        //从数据库中获取
        Cursor c = videoDB.query(filePath);
        int count;
        if (c != null && (count = c.getCount()) > 0) {
            c.moveToFirst();
            for (int i = 0; i < count; i++) {
                VideoItem item = new VideoItem();
                int id = c.getInt(c.getColumnIndex(VideoColumns._ID));
                String videoName = c.getString(c.getColumnIndex(VideoColumns.VIDEO_NAME));
                String videoPath = filePath + "/" + videoName;
                String videoDuration = VideoUtil.formatTime(c.getInt(c.getColumnIndex(VideoColumns.VIDEO_DURATION)));
                long lastPlay = c.getLong(c.getColumnIndex(VideoColumns.VIDEO_LAST_PLAY_TIME));
                String lastPlayedTime = VideoUtil.formatTime((int) lastPlay);
                int recentPlay = c.getInt(c.getColumnIndex(VideoColumns.VIDEO_RECENT_PLAY));// z
                int isPlayed = c.getInt(c.getColumnIndex(VideoColumns.VIDEO_IS_PLAYED));
                String videoThumbnailPath = c.getString(c.getColumnIndex(VideoColumns.VIDEO_THUBMNAIL_PATH));
                item.setId(id);
                item.setVideoName(videoName);
                item.setVideoPath(videoPath);
                item.setVideoParentPath(filePath);
                item.setVideoDuration(videoDuration);
                item.setLastPlayedTime(lastPlayedTime);
                item.setRecentPlay(recentPlay);
                item.setIsPlayed(isPlayed);
                item.setThumbnailPath(videoThumbnailPath);
                item.setLastPlay(lastPlay);
                videos.add(item);
                c.moveToNext();
            }
        } else {//文件夹下没有视频
            return false;
        }
        if (c != null) {
            c.close();
        }
        videoDB.closeDatabase();
        return true;
    }

    private boolean queryData(String keyword) {
        if (videos.size() > 0) {
            videos.clear();
        }
        Cursor c = videoDB.queryByKeyword(keyword);
        if (c != null) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                VideoItem item = new VideoItem();
                int id = c.getInt(c.getColumnIndex(VideoColumns._ID));
                String videoName = c.getString(c.getColumnIndex(VideoColumns.VIDEO_NAME));
                String parentPath = c.getString(c.getColumnIndex(VideoColumns.VIDEO_PARENT_PATH));
                String videoPath = parentPath + "/" + videoName;
                String videoDuration = VideoUtil.formatTime(c.getInt(c.getColumnIndex(VideoColumns.VIDEO_DURATION)));
                String lastPlayedTime = VideoUtil.formatTime(c.getInt(c.getColumnIndex(VideoColumns.VIDEO_LAST_PLAY_TIME)));
                int recentPlay = c.getInt(c.getColumnIndex(VideoColumns.VIDEO_RECENT_PLAY));// z
                int isPlayed = c.getInt(c.getColumnIndex(VideoColumns.VIDEO_IS_PLAYED));
                String videoThumbnailPath = c.getString(c.getColumnIndex(VideoColumns.VIDEO_THUBMNAIL_PATH));
                item.setId(id);
                item.setVideoName(videoName);
                item.setVideoPath(videoPath);
                item.setVideoParentPath(videoPath);
                item.setVideoDuration(videoDuration);
                item.setLastPlayedTime(lastPlayedTime);
                item.setRecentPlay(recentPlay);
                item.setIsPlayed(isPlayed);
                item.setThumbnailPath(videoThumbnailPath);
                videos.add(item);
            }
        } else {
            return false;
        }
        c.close();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (loaderTask != null && loaderTask.getStatus() == AsyncTask.Status.RUNNING) {
            loaderTask.cancel(true);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        playVideo(position);
    }

    /**
     * 
     */
    private void playVideo(int position) {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putParcelableArrayListExtra("videos", videos);
        intent.putExtra("position", position);
        String videoName = videos.get(position).getVideoName();
        startActivity(intent);
    }

    private int caculateSampleSize(Options opts, int reqWidth, int reqHeight) {
        final int width = opts.outWidth;
        final int height = opts.outHeight;
        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    private Bitmap decodeSampledBitmap(Resources res, int id, int reqWidth, int reqHeight) {
        Options opts = new Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, id, opts);
        opts.inSampleSize = caculateSampleSize(opts, reqWidth, reqHeight);
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, id, opts);
    }

    class VideoInfoLoader extends AsyncTask<Void, Void, Void> {

        VideoDatabase db;

        public VideoInfoLoader(Context context) {
            db = new VideoDatabase(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (VideoItem item : videos) {
                if (!isCancelled()) {
                    int id = item.getId();
                    String thumbnail = item.getThumbnailPath();
                    String videoPath = item.getVideoPath();
                    if (thumbnail == null || "".equals(thumbnail)) {//加载图片
                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, Images.Thumbnails.MINI_KIND);
                        if (bitmap == null) {
                            item.setThumbnailPath(defaultThumbnailPath);//设置路径
                            ContentValues cv = new ContentValues();
                            cv.put(VideoColumns.VIDEO_THUBMNAIL_PATH, defaultThumbnailPath);
                            db.update(id, cv);
                        } else {
                            bitmap = ThumbnailUtils.extractThumbnail(bitmap, thumbnailWidth, thumbnailHeight);
                            String thumbnailPath = saveBitmap(bitmap, id + "");//缓存图片
                            item.setThumbnailPath(thumbnailPath);//设置路径
                            ContentValues cv = new ContentValues();
                            cv.put(VideoColumns.VIDEO_THUBMNAIL_PATH, thumbnailPath);
                            db.update(id, cv);
                        }
                    }

                    String videoDuration = item.getVideoDuration();
                    if (videoDuration == null || "".equals(videoDuration) || "00:00".equals(videoDuration)) {//未加载视频时长
                        int duration = VideoUtil.getDuration(videoPath);
                        item.setVideoDuration(VideoUtil.formatTime(duration));
                        ContentValues cv = new ContentValues();
                        cv.put(VideoColumns.VIDEO_DURATION, duration);
                        db.update(id, cv);
                    }
                    publishProgress();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

    }

    private String saveBitmap(Bitmap bitmap, String id) {
        File file = new File(cacheFile, id);
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        try {
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }
}
