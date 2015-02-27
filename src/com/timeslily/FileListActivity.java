package com.timeslily;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.timeslily.utils.VideoUtil;
import com.timeslily.videofile.VideoColumns;
import com.timeslily.videofile.VideoDatabase;
import com.timeslily.videofolder.FileAdapter;
import com.timeslily.videofolder.FileItem;
import com.timeslily.videofolder.FolderListColumns;
import com.timeslily.videofolder.FolderListDatabase;

/**
 * @author zxc
 */
public class FileListActivity extends ListActivity implements OnClickListener {
    private ListView listView;
    private List<FileItem> fileList = new ArrayList<FileItem>();
    private FileAdapter adapter;
    /** 返回按钮 */
    private TextView backBtn;
    /** 取消按钮 */
    private TextView cancelBtn;
    /** 确定按钮 */
    private TextView okBtn;
    /** 当前文件名称 */
    private TextView currentFileNameTextView;
    /** 当前文件夹 */
    private File currentFile;
    /** 保存时显示的缓冲 */
    private ProgressBar savingProgressBar;
    CheckBox currentCheckBox;
    /** 列表位置 */
    private int listPosition;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_list);
        listView = getListView();
        listView.setOnScrollListener(scrollListener);
        backBtn = (TextView) findViewById(R.id.tv_back);
        cancelBtn = (TextView) findViewById(R.id.tv_cancel);
        okBtn = (TextView) findViewById(R.id.tv_save);
        savingProgressBar = (ProgressBar) findViewById(R.id.pb_save_folder);
        currentFileNameTextView = (TextView) findViewById(R.id.tv_file_path);
        currentCheckBox = (CheckBox) findViewById(R.id.cb_current_file);
        backBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        okBtn.setOnClickListener(this);
        initAdapter();
    }

    private OnScrollListener scrollListener = new OnScrollListener() {

        @Override
        public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                listPosition = listView.getFirstVisiblePosition();
            }
        }
    };

    /**
     * 获取根目录
     */
    private void initAdapter() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {//sd卡存在
            adapter = new FileAdapter(this, fileList);
            getFileList(Environment.getExternalStorageDirectory());
            setListAdapter(adapter);
        } else {//sd卡不存在
            //TODO
            Toast.makeText(this, getResources().getString(R.string.sdcard_not_exist), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取文件列表信息
     * 
     * @param file
     */
    private void getFileList(File file) {
        currentFile = file;
        currentFileNameTextView.setText(file.getName() + " " + getResources().getString(R.string.add_current_folder));
        if (fileList.size() > 0) {
            fileList.clear();
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            Arrays.sort(files, fileComparator);
            for (File subFile : files) {
                if (subFile.isDirectory()) {
                    if (!subFile.getName().startsWith(".")) {//子文件不是以'.'开头
                        fileList.add(new FileItem(subFile.getName(), getFileTime(subFile), false, R.drawable.ic_folder, subFile.getAbsolutePath()));
                    }
                } else {//文件 类型
                    boolean isVideo = VideoUtil.isSupportedVideo(subFile.getName());
                    if (isVideo) {
                        fileList.add(new FileItem(subFile.getName(), getFileTime(subFile), false, R.drawable.ic_video, ""));
                    }
                }
            }
        }
        adapter.notifyDataSetInvalidated();
        listView.setSelection(listPosition);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FileItem item = (FileItem) l.getItemAtPosition(position);
        String filePath = item.getFilePath();
        if (!"".equals(filePath)) {//是目录
            getFileList(new File(filePath));
        }
    }

    /**
     * 文件排序
     */
    private Comparator<File> fileComparator = new Comparator<File>() {

        @Override
        public int compare(File lhs, File rhs) {
            boolean l1 = lhs.isDirectory();
            boolean l2 = rhs.isDirectory();
            if (l1 && !l2) {
                return -1;
            } else if (!l1 && l2) {
                return 1;
            } else {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        }
    };

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 获取文件最后修改时间
     * 
     * @param file 文件对象
     * @return 文件时间，如：2013-08-08
     */
    private String getFileTime(File file) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return format.format(new Date(file.lastModified()));//文件最后修改时间
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.tv_back:
            this.backToParentFile();
            break;
        case R.id.tv_cancel:
            this.finish();
            break;
        case R.id.tv_save:
            saveVideoFolder();
            break;
        default:
            break;
        }
    }

    /**
     * 返回上一级目录
     */
    private void backToParentFile() {
        if (currentFile.equals(Environment.getExternalStorageDirectory())) {//sd卡根目录
            Toast.makeText(this, getResources().getString(R.string.is_root), Toast.LENGTH_SHORT).show();
        } else {
            File parentFile = currentFile.getParentFile();
            getFileList(parentFile);
        }
    }

    /**
     * 保存选择的文件
     */
    private void saveVideoFolder() {
        savingProgressBar.setVisibility(View.VISIBLE);
        new SaveAsyncTask(this, this).execute();

    }

    @Override
    public void onBackPressed() {//返回键
        if (currentFile.equals(Environment.getExternalStorageDirectory())) {
            super.onBackPressed();
        } else {
            backToParentFile();
        }
    }

    class SaveAsyncTask extends AsyncTask<Void, Void, Void> {
        Context context;
        Activity activity;
        int addFolderCount = 0;//统计成功添加的文件夹数量

        public SaveAsyncTask(Context context, Activity activity) {
            this.context = context;
            this.activity = activity;
        }

        @Override
        protected Void doInBackground(Void... params) {
            FolderListDatabase database = new FolderListDatabase(context);
            VideoDatabase videoDB = new VideoDatabase(context);
            //最顶部选中
            if (currentCheckBox.isChecked()) {
//                if (videoCount > 0) {//文件夹下存在视频则添加到数据库
                ContentValues cv = new ContentValues();
                cv.put(FolderListColumns.FOLDER_NAME, currentFile.getName());
                cv.put(FolderListColumns.FOLDER_PATH, currentFile.getAbsolutePath());
                cv.put(FolderListColumns.VIDEO_COUNT, VideoUtil.getVideoCount(currentFile.getAbsolutePath()));
                if (database.insert(cv)) {//同时保存文件夹下的视频到数据库
                    ArrayList<ContentValues> values = new ArrayList<ContentValues>();
                    for (File subFile : currentFile.listFiles()) {
                        String subFileName = subFile.getName();//文件名
                        if (subFile.isFile() && VideoUtil.isSupportedVideo(subFile.getName())) {
                            ContentValues value = new ContentValues();
                            value.put(VideoColumns.VIDEO_NAME, subFileName);
                            value.put(VideoColumns.VIDEO_PARENT_PATH, currentFile.getAbsolutePath());
                            values.add(value);
                        }
                    }
                    if (values.size() > 0) {
                        videoDB.insertAll(values);//批量插入
                    }
                    addFolderCount++;
//                    }
                }
            }
            ArrayList<ContentValues> values = new ArrayList<ContentValues>();
            for (int i = 0; i < listView.getCount(); i++) {
                FileItem item = (FileItem) listView.getItemAtPosition(i);
                if (item.isChecked()) {
                    int videoCount = VideoUtil.getVideoCount(item.getFilePath());
//                    if (videoCount > 0) {
                        ContentValues cv = new ContentValues();
                        String filePath = item.getFilePath();
                        cv.put(FolderListColumns.FOLDER_NAME, item.getFileName());
                        cv.put(FolderListColumns.FOLDER_PATH, filePath);
                        cv.put(FolderListColumns.VIDEO_COUNT, videoCount);
                        if (database.insert(cv)) {
                            for (File subFile : new File(filePath).listFiles()) {
                                String subFileName = subFile.getName();//文件名
                                if (subFile.isFile() && VideoUtil.isSupportedVideo(subFile.getName())) {
                                    ContentValues value = new ContentValues();
                                    value.put(VideoColumns.VIDEO_NAME, subFileName);
                                    value.put(VideoColumns.VIDEO_PARENT_PATH, filePath);
                                    values.add(value);
                                }
                            }
                            addFolderCount++;
//                        }
                    }
                }
            }
            if (values.size() > 0) {
                videoDB.insertAll(values);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
//            Toast.makeText(context, String.format(getResources().getString(R.string.add_folder_success), addFolderCount), Toast.LENGTH_SHORT).show();
            savingProgressBar.setVisibility(View.INVISIBLE);
            activity.finish();
        }
    }
}
