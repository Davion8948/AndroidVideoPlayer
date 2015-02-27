package com.timeslily;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.timeslily.ffmpeg.FFmpegDisplay;
import com.timeslily.ffmpeg.FFmpegError;
import com.timeslily.ffmpeg.FFmpegListener;
import com.timeslily.ffmpeg.FFmpegPlayer;
import com.timeslily.ffmpeg.FFmpegStreamInfo;
import com.timeslily.ffmpeg.FFmpegSurfaceView;
import com.timeslily.ffmpeg.NotPlayingException;
import com.timeslily.utils.VideoUtil;
import com.timeslily.videofile.VideoColumns;
import com.timeslily.videofile.VideoDatabase;
import com.timeslily.videofile.VideoItem;
import com.timeslily.videofolder.FolderListDatabase;

public class VideoActivity extends Activity implements OnClickListener, FFmpegListener, OnSeekBarChangeListener {

//    private static final String tag = "ccc";
    private FFmpegPlayer ffmpegPlayer;
    /** 是否正在播放 */
//    protected boolean isPlay = false;
    private FFmpegSurfaceView videoView;
    private boolean mTracking = false;

    private int mAudioStreamNo = FFmpegPlayer.UNKNOWN_STREAM;
    private int mSubtitleStreamNo = FFmpegPlayer.NO_STREAM;
    private long mCurrentTimeUs;

    /** 时间线程中的循环标识 */
    private boolean timeLoopFlag = true;
    /** 时间线程中消息标识 */
    private static final int TIME_MSG_WHAT = 1;
    /** 顶部操作栏 */
    private static RelativeLayout topBar;
    /** 顶部操作栏 */
    private static LinearLayout bottomBar;
    /** 视频名称 */
    private TextView videoNameTextView;
    /** 系统时间 */
    private static TextView systemTimeTextView;
    /** 电量 */
    private TextView batteryTextView;
    /** 当前播放时间 */
    private TextView currentTimeTextView;
    /** 视频时长 */
    private TextView totalTimeTextView;
    /** 上一个 */
    private ImageView previousBtn;
    /** 下一个 */
    private ImageView nextBtn;
    /** 锁定按钮 */
    private ImageView lockBtn;
    private ImageView unLockBtn;
    /** 播放暂停 */
    private ImageView playOrPauseBtn;
    /** 全屏 */
    private ImageView fullScreenBtn;
    /** 加载缓冲条 */
    private ProgressBar loadingProgressBar;
    /** 拖动条 */
    private SeekBar videoSeekBar;
    /** 电量receiver */
    private BatteryBroadcastReceiver batteryReceiver;
    /** 当前文件夹件下视频列表 */
    private ArrayList<VideoItem> videos;
    /** 当前播放视频位置 */
    private int currentVideoPosition;
    /** 视频数量 */
    private int videoCount;
    /** 控制栏显示时长 */
    private static int showTime;
    private boolean isPause;
    /** 宽度像素 */
    private int widthPixels;
    /** 高度像素 */
    private int heightPixels;

    /** 手势操作类 */
    private GestureDetector gestureDetector;
    /** 音量和亮度控制 */
    private LinearLayout volumeBrightnessLayout;
    /** 音量进度条 */
    private ProgressBar volumnProgressBar;
    /** 亮度进度条 */
    private ProgressBar brightnessPorgressBar;
    private AudioManager audioManager;
    /** 刚开始播放视频时的音量大小 */
    private int originalVolume;
    private int maxVolume;
    /** 调节音量标识 */
    private boolean volumeFlag;
    /** 调节亮度标识 */
    private boolean brightnessFlag;
    private TextView volumeTextView;
    private TextView brightTextView;
    private Drawable volumeDrawable;
    /** 静音 */
    private Drawable muteDrawable;
    /** 调整之后的音量 */
    private int myVolume;
    /** 调整之后的亮度 */
    private float myBrightness;
    private boolean isFullScreen;
    /** 视频资源原宽度 */
    private int originalWidth;
    /** 视频资源原高度 */
    private int originalHeight;
    private RelativeLayout videoLayout;
    /** 居中显示的暂停按钮 */
//    private ImageView pauseImageView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        gestureDetector = new GestureDetector(this, new MyGestureListener());
        setContentView(R.layout.video_surfaceview);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        this.widthPixels = dm.widthPixels;
        this.heightPixels = dm.heightPixels;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        showTime = 5;
        new TimeThread().start();
        initUI();
        batteryReceiver = new BatteryBroadcastReceiver();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//        TelephonyManager telManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
//        telManager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        ffmpegPlayer = new FFmpegPlayer((FFmpegDisplay) videoView, this);
        ffmpegPlayer.setMpegListener(this);
        Intent intent = getIntent();

        if (intent != null) {
            videos = intent.getParcelableArrayListExtra("videos");
            currentVideoPosition = intent.getIntExtra("position", 0);
        }
        if (videos != null && (videoCount = videos.size()) > 0) {
            setDataSource();
            //是否从头播放
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isContinueLastPlay = settings.getBoolean("continueLastPlay", true);
            long lastPlay = videos.get(currentVideoPosition).getLastPlay();
            if (isContinueLastPlay && lastPlay != 0) {//从上次播放记录开始播放 TODO
//                Log.i("ccc", "ffffff" + lastPlay);
//                ffmpegPlayer.seek(lastPlay);//跳转到上次播放位置
//                Log.i("ccc", "after seek");
            }
        }

    }

//    private PhoneStateListener myPhoneStateListener = new PhoneStateListener() {
//        @Override
//        public void onCallStateChanged(int state, String incomingNumber) {
//            if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {//来电话
//            }
//            if (state == TelephonyManager.CALL_STATE_IDLE) {
//            }
//        };
//    };

    /**
     * 初始化UI
     */
    private void initUI() {
        videoLayout = (RelativeLayout) findViewById(R.id.rl_video_surface);
        topBar = (RelativeLayout) findViewById(R.id.rl_top_bar);
        bottomBar = (LinearLayout) findViewById(R.id.ll_bottom_bar);
        videoNameTextView = (TextView) findViewById(R.id.tv_video_name);
        systemTimeTextView = (TextView) findViewById(R.id.tv_time);
        batteryTextView = (TextView) findViewById(R.id.tv_battery);
        currentTimeTextView = (TextView) findViewById(R.id.tv_current_time);
        totalTimeTextView = (TextView) findViewById(R.id.tv_total_time);
        previousBtn = (ImageView) findViewById(R.id.iv_previous);
        loadingProgressBar = (ProgressBar) findViewById(R.id.pb_loading);
        videoSeekBar = (SeekBar) findViewById(R.id.sb_video_seek);
        videoSeekBar.setOnSeekBarChangeListener(this);
        previousBtn.setOnClickListener(this);
        nextBtn = (ImageView) findViewById(R.id.iv_next);
        nextBtn.setOnClickListener(this);
        playOrPauseBtn = (ImageView) findViewById(R.id.iv_play_pause);
        fullScreenBtn = (ImageView) findViewById(R.id.iv_full_screen);
        fullScreenBtn.setOnClickListener(this);
        playOrPauseBtn.setOnClickListener(this);
        videoView = (FFmpegSurfaceView) findViewById(R.id.video_view);
        volumeBrightnessLayout = (LinearLayout) findViewById(R.id.ll_volume_bright);
        volumnProgressBar = (ProgressBar) findViewById(R.id.pb_volumn);
        brightnessPorgressBar = (ProgressBar) findViewById(R.id.pb_bright);
        volumeTextView = (TextView) findViewById(R.id.tv_volume_number);
        brightTextView = (TextView) findViewById(R.id.tv_brightness_number);
        volumeDrawable = getResources().getDrawable(R.drawable.volume);
        muteDrawable = getResources().getDrawable(R.drawable.mute);
        muteDrawable.setBounds(0, 0, muteDrawable.getMinimumWidth(), muteDrawable.getMinimumHeight());
        volumeDrawable.setBounds(0, 0, volumeDrawable.getMinimumWidth(), volumeDrawable.getMinimumHeight());
        lockBtn = (ImageView) findViewById(R.id.iv_lock);
        lockBtn.setOnClickListener(this);
        unLockBtn = (ImageView) findViewById(R.id.iv_unlock);
        unLockBtn.setOnClickListener(this);
        initVolumeBrightness();
//        pauseImageView = (ImageView) findViewById(R.id.iv_pause);
//        pauseImageView.setOnClickListener(this);
    }

    /**
     * 初始设置音量和亮度
     */
    private void initVolumeBrightness() {
        SharedPreferences sp = getSharedPreferences(VideoUtil.SETTING_INFOS, Context.MODE_PRIVATE);
        int volume = sp.getInt(VideoUtil.VOLUME_INFO, originalVolume);
        LayoutParams params = getWindow().getAttributes();
        float brightness = sp.getFloat(VideoUtil.BRIGHTNESS_INFO, params.screenBrightness);
        myVolume = volume;
        myBrightness = brightness;
        volumnProgressBar.setProgress((int) (volume * 100 / (float) maxVolume));
        brightnessPorgressBar.setProgress((int) (100 * brightness));
        adjustBrightness(brightness);
        adjustVolumn(volume);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP && volumeFlag) {//音量调节之后保存音量  
            SharedPreferences sp = getSharedPreferences(VideoUtil.SETTING_INFOS, Context.MODE_PRIVATE);
            sp.edit().putInt(VideoUtil.VOLUME_INFO, myVolume).commit();
            volumeFlag = false;
            volumeBrightnessLayout.setVisibility(View.INVISIBLE);
            volumeTextView.setVisibility(View.INVISIBLE);
        }
        if (action == MotionEvent.ACTION_UP && brightnessFlag) {//保存亮度
            SharedPreferences sp = getSharedPreferences(VideoUtil.SETTING_INFOS, Context.MODE_PRIVATE);
            sp.edit().putFloat(VideoUtil.BRIGHTNESS_INFO, myBrightness).commit();
            brightnessFlag = false;
            volumeBrightnessLayout.setVisibility(View.INVISIBLE);
            brightTextView.setVisibility(View.INVISIBLE);
        }
        return gestureDetector.onTouchEvent(event);//手势操作
    }

    @Override
    protected void onResume() {
        Log.i("ccc", "onresumeeeeee");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ffmpegPlayer.resume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("ccc", "onpauseeee");
        if (ffmpegPlayer.isPlaying()) {//正在播放
            ffmpegPlayer.pause();
            ffmpegPlayer.setPlaying(false);
        }
        updatePlayedTime(videos.get(currentVideoPosition).getId());
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("ccc", "ondestory.............");
        timeLoopFlag = false;
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);//注销电池监听
        }
        ffmpegPlayer.setMpegListener(null);
        ffmpegPlayer.renderFrameStop();
        videoView.setStopRender(true);
        adjustVolumn(originalVolume);//回复系统音量
        if (videos != null) {//保存最后播放视频
            SharedPreferences sp = getSharedPreferences(VideoUtil.SETTING_INFOS, Context.MODE_PRIVATE);
            Editor editor = sp.edit();
            VideoItem item = videos.get(currentVideoPosition);
            editor.putInt(VideoUtil.LAST_PLAY_VIDEO_INFO, item.getId());
            editor.commit();
        }
    }

    private void updatePlayedTime(int id) {
        VideoDatabase videoDB = new VideoDatabase(this);
        ContentValues cv = new ContentValues();
        cv.put(VideoColumns.VIDEO_LAST_PLAY_TIME, currentTimeS);
        cv.put(VideoColumns.VIDEO_RECENT_PLAY, 1);
        videoDB.updateRecentPlay();//在下面的语句之前
        videoDB.update(id, cv);
        FolderListDatabase folderDB = new FolderListDatabase(this);
        folderDB.updateRecentPlay(videos.get(currentVideoPosition).getVideoParentPath());
    }

    /**
     * 设置资源，播放视频
     */
    private void setDataSource() {
        HashMap<String, String> params = new HashMap<String, String>();//暂时没用
        VideoItem video = videos.get(currentVideoPosition);
        String videoName = video.getVideoName();
        videoNameTextView.setText(videoName);
        videoSeekBar.setEnabled(!videoName.endsWith(".rmvb"));
        String url = video.getVideoPath();
        ffmpegPlayer.setPlaying(false);
        loadingProgressBar.setVisibility(View.VISIBLE);
        ffmpegPlayer.setDataSource(url, params, FFmpegPlayer.UNKNOWN_STREAM, mAudioStreamNo, mSubtitleStreamNo);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
        case R.id.iv_play_pause:
            resumePause();
            break;
        case R.id.video_view:
            toogleOperateBar();
            break;
        case R.id.iv_previous:
            playPreviousVideo();
            break;
        case R.id.iv_next:
            playNextVideo();
            break;
        case R.id.iv_lock:
            lockScreen(true);
            break;
        case R.id.iv_unlock:
            lockScreen(false);
            break;
        case R.id.iv_full_screen:
            changeVideoSize();
            break;
        }
    }

    /**
     * 改变视频大小
     * 
     * @param isFullScreen
     */
    private void changeVideoSize() {
        setVideoSize(isFullScreen ? originalWidth : widthPixels, isFullScreen ? originalHeight : heightPixels);
        isFullScreen = !isFullScreen;
    }

    /**
     * 顺序播放下一个视频
     */
    private void playNextVideo() {
        updatePlayedTime(videos.get(currentVideoPosition).getId());
        totalTimeTextView.setText("");//清空时长 用于判断时长为空
        currentVideoPosition = currentVideoPosition + 1 == videoCount ? 0 : ++currentVideoPosition;
        stop();
        setDataSource();
        showTime = 5;
    }

    /**
     * 顺序播放上一个视频
     */
    private void playPreviousVideo() {
        updatePlayedTime(videos.get(currentVideoPosition).getId());
        totalTimeTextView.setText("");//清空时长
        currentVideoPosition = currentVideoPosition == 0 ? videoCount - 1 : --currentVideoPosition;
        stop();
        setDataSource();
        showTime = 5;
    }

    /**
     * 停止视频播放
     */
    private void stop() {
//        ffmpegPlayer.renderFrameStop();
        ffmpegPlayer.stop();
        videoView.setStopRender(true);
    }

    /**
     * 解锁屏
     * 
     * @param lock true：锁屏 false：解屏
     */
    private void lockScreen(boolean lock) {//锁定屏幕
        toogleOperateBar();
        videoLayout.setClickable(lock);
        unLockBtn.setVisibility(lock ? View.VISIBLE : View.GONE);

    }

    private int currentTimeS;

    @Override
    public void onFFUpdateTime(long currentTimeUs, long videoDurationUs, boolean isFinished) {
        mCurrentTimeUs = currentTimeUs;
        if (!mTracking) {
            currentTimeS = (int) (currentTimeUs / 1000 / 1000);//当前播放秒数
            int videoDurationS = (int) (videoDurationUs / 1000 / 1000);//时长
            currentTimeTextView.setText(VideoUtil.formatTime(currentTimeS));
            if ("".equals(totalTimeTextView.getText())) {//视频时长只获取一次
                totalTimeTextView.setText(VideoUtil.formatTime(videoDurationS));
            }
            videoSeekBar.setMax(videoDurationS);
            videoSeekBar.setProgress(currentTimeS);
        }

        if (isFinished) {//结束视频
            this.finish();
        }
    }

    @Override
    public void onFFDataSourceLoaded(FFmpegError err, FFmpegStreamInfo[] streams) {
        if (err != null) {
            Builder builder = new AlertDialog.Builder(VideoActivity.this);
            builder.setTitle(R.string.app_name).setMessage(getResources().getString(R.string.could_not_play_video))
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
            return;
        }
        this.loadingProgressBar.setVisibility(View.GONE);
        ffmpegPlayer.resume();
        ffmpegPlayer.setPlaying(true);
    }

    /**
     * 恢复或暂停
     */
    public void resumePause() {
        this.playOrPauseBtn.setEnabled(false);
        if (ffmpegPlayer.isPlaying()) {
            ffmpegPlayer.pause();
        } else {
            ffmpegPlayer.resume();
        }
//        ffmpegPlayer.setPlaying(!ffmpegPlayer.isPlaying());
    }

    @Override
    public void onFFResume(NotPlayingException result) {
        this.playOrPauseBtn.setImageResource(R.drawable.pause_button_selecter);
        this.playOrPauseBtn.setEnabled(true);
        ffmpegPlayer.setPlaying(true);
    }

    @Override
    public void onFFPause(NotPlayingException err) {
        this.playOrPauseBtn.setImageResource(R.drawable.play_button_selecter);
        this.playOrPauseBtn.setEnabled(true);
        ffmpegPlayer.setPlaying(false);
    }

    @Override
    public void onFFStop() {
    }

    @Override
    public void onFFSeeked(NotPlayingException result) {
//		if (result != null)
//			throw new RuntimeException(result);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            long timeUs = progress * 1000 * 1000;
            ffmpegPlayer.seek(timeUs);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTracking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTracking = false;
        showTime = 5;
    }

    class BatteryBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPercent = (int) (100 * level / (float) scale);//电池百分比
                batteryTextView.setText(getResources().getString(R.string.electric_quantity) + batteryPercent + "%");
            }
        }

    }

    private static Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case TIME_MSG_WHAT:
                systemTimeTextView.setText(DateFormat.format("hh:mm:ss", System.currentTimeMillis()));
                if (topBar.getVisibility() == View.VISIBLE && (--showTime) == 0) {
                    toogleOperateBar();
                }
                break;
            }
        };
    };

    class TimeThread extends Thread {
        @Override
        public void run() {
            while (timeLoopFlag) {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = TIME_MSG_WHAT;
                    myHandler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected static void toogleOperateBar() {
        if (topBar.getVisibility() == View.VISIBLE) {//点击隐藏操作栏
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
        } else {
            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            showTime = 5;
        }
    }

    /**
     * 调节音量
     * 
     * @param volume 音量大小
     */
    private void adjustVolumn(int volume) {
//        volume = volume <= MIN_VOLUME ? MIN_VOLUME : volume;//设置一个最小音量
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    /**
     * 调节亮度
     * 
     * @param brightness 亮度值
     */
    private void adjustBrightness(float brightness) {
        brightness = brightness <= MIN_BRIGHTNESS ? MIN_BRIGHTNESS : brightness;//设置亮度最小值，不让屏幕全黑
        try {
//            int brightnessMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
//            if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {//自动模式改为手动模式
//                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
//            }
            LayoutParams mparams = getWindow().getAttributes();
            mparams.screenBrightness = brightness;
            getWindow().setAttributes(mparams);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /** 最小亮度 */
    private static final float MIN_BRIGHTNESS = 0.2f;
//    /** 最小音量 */
//    private static final int MIN_VOLUME = 2;

    /** 判断上下滑动的最小精度 */
    private static final float MIN_PERCISION = 15f;

    class MyGestureListener extends SimpleOnGestureListener {
        public boolean onSingleTapUp(MotionEvent e) {
            toogleOperateBar();
            return false;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float percision = Math.abs(e2.getY() - e1.getY());//上下滑动像素
            int x = (int) e1.getX();
            if (x < widthPixels / 2) {//左边  亮度
                if (brightnessPorgressBar.getProgress() <= 100 && percision >= MIN_PERCISION) {
                    volumeBrightnessLayout.setVisibility(View.VISIBLE);
                    brightnessPorgressBar.setVisibility(View.VISIBLE);
                    volumnProgressBar.setVisibility(View.GONE);
                    brightTextView.setVisibility(View.VISIBLE);
                    float offset = brightnessPorgressBar.getProgress() + distanceY;
                    offset = offset < 0f ? 0f : offset;
                    offset = offset >= 100 ? 100 : offset;
                    brightnessPorgressBar.setProgress((int) offset);//进度不超过100
                    brightTextView.setText("" + (int) (offset * 3 / 20));
                    myBrightness = offset / 100;
                    adjustBrightness(myBrightness);
                    brightnessFlag = true;
                }
            } else {//右边
                if (volumnProgressBar.getProgress() <= 100 && percision >= MIN_PERCISION) {
                    volumeBrightnessLayout.setVisibility(View.VISIBLE);
                    brightnessPorgressBar.setVisibility(View.GONE);
                    volumnProgressBar.setVisibility(View.VISIBLE);
                    volumeTextView.setVisibility(View.VISIBLE);
                    float offset = volumnProgressBar.getProgress() + distanceY;
                    offset = offset < 0f ? 0f : offset;
                    offset = offset >= 100 ? 100 : offset;
                    volumnProgressBar.setProgress((int) offset);
                    if (offset == 0f) {
                        volumeTextView.setCompoundDrawables(muteDrawable, null, null, null);
                        volumeTextView.setText(null);
                    } else {
                        volumeTextView.setCompoundDrawables(volumeDrawable, null, null, null);
                        volumeTextView.setText("" + (int) (offset * 3 / 20));
                    }
                    myVolume = (int) (offset * maxVolume / 100);
                    adjustVolumn(myVolume);
                    volumeFlag = true;
                }
            }
            return false;
        }

    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        originalWidth = width;
        originalHeight = height;
        setVideoSize(width, height);
        if (fullScreenBtn != null) {
            if (width > widthPixels && height > heightPixels) {//视频本身宽高 大于手机屏幕宽高 不用设置全屏
                fullScreenBtn.setVisibility(View.INVISIBLE);
            } else {//设置视频大小
                fullScreenBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setVideoSize(int width, int height) {
        videoView.getHolder().setFixedSize(width, height);
    }
}
