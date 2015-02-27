package com.timeslily;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.timeslily.videofile.VideoItem;
import com.timeslily.videoplayer.FFmpeg;
import com.timeslily.videoplayer.PlayView;

public class PlayActivity_Old extends Activity implements OnClickListener {
    /** 时间线程中的循环标识 */
    private static boolean timeLoopFlag = true;
    /** 时间线程中消息标识 */
    private static final int TIME_MSG_WHAT = 1;
    /** 播放画面视图 */
    private PlayView playView;
    /** 顶部操作栏 */
    private static RelativeLayout topBar;
    /** 顶部操作栏 */
    private static LinearLayout bottomBar;
    /** 视频名称 */
    private TextView videoName;
    /** 系统时间 */
    private static TextView systemTime;
    /** 电量 */
    private TextView battery;
    /** 当前播放时间 */
    private TextView currentTime;
    /** 视频时长 */
    private TextView totalTime;
    /** 上一个 */
    private ImageButton previousBtn;
    /** 下一个 */
    private ImageButton nextBtn;
    /** 暂停 */
    private ImageButton pauseBtn;
    /** 播放 */
    private ImageButton playBtn;
    /** 电量receiver */
    private BatteryBoradcastReceiver batteryReceiver;
    /** 当前文件夹件下视频列表 */
    private ArrayList<VideoItem> videos;
    /** 当前播放视频位置 */
    private int currentVideoPosition;
    /** 视频数量 */
    private int videoCount;
    /** 控制栏显示时长 */
    private static int showTime = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play);
        initUI();
        new TimeThread().start();
        batteryReceiver = new BatteryBoradcastReceiver();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        Intent intent = getIntent();
        if (intent != null) {
            videos = intent.getParcelableArrayListExtra("videos");
            currentVideoPosition = intent.getIntExtra("position", 0);
        }
        if (videos != null && (videoCount = videos.size()) > 0) {
            playVideo();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playView.playVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playView.pauseVideo();
    }
    /**
     * 初始化UI
     */
    private void initUI() {
        playView = (PlayView) findViewById(R.id.pv_play_view);
        playView.setOnClickListener(this);
        topBar = (RelativeLayout) findViewById(R.id.rl_video_top);
        bottomBar = (LinearLayout) findViewById(R.id.ll_video_bottom);
        videoName = (TextView) findViewById(R.id.tv_video_name);
        systemTime = (TextView) findViewById(R.id.tv_time);
        battery = (TextView) findViewById(R.id.tv_battery);
        currentTime = (TextView) findViewById(R.id.tv_current_time);
        totalTime = (TextView) findViewById(R.id.tv_total_time);
        previousBtn = (ImageButton) findViewById(R.id.ib_previous);
        previousBtn.setOnClickListener(this);
        nextBtn = (ImageButton) findViewById(R.id.ib_next);
        nextBtn.setOnClickListener(this);
        pauseBtn = (ImageButton) findViewById(R.id.ib_pause);
        pauseBtn.setOnClickListener(this);
        playBtn = (ImageButton) findViewById(R.id.ib_play);
        playBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.ib_previous://上一个
            previousVideo();
            break;
        case R.id.ib_pause://开始操作
            playView.pauseVideo();
            togglePlayButton(true);
            break;
        case R.id.ib_play://暂停操作
            playView.playVideo();
            togglePlayButton(false);
            break;

        case R.id.ib_next://下一个
            nextVideo();
            break;
        case R.id.pv_play_view:
            toggleOperateBar();
        default:
            break;
        }
    }

    /**
     * 显示或隐藏操作栏
     */
    private static void toggleOperateBar() {
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
     * 上一个视频
     */
    private void previousVideo() {
        currentVideoPosition = currentVideoPosition == 0 ? videoCount - 1 : --currentVideoPosition;
        playView.stopVideo();
        FFmpeg.stopVideo();
        playVideo();

    }

    /**
     * 下一个视频
     */
    private void nextVideo() {
        currentVideoPosition = currentVideoPosition + 1 == videoCount ? 0 : ++currentVideoPosition;
        playView.stopVideo();
        FFmpeg.stopVideo();
        playVideo();
    }

    /**
     * 播放视频
     * 
     * @param position
     */
    private void playVideo() {
        VideoItem video = videos.get(currentVideoPosition);
        if (video != null) {
            String videoPath = video.getVideoPath();
            if (new File(videoPath).exists()) {
                videoName.setText(video.getVideoName());
                totalTime.setText(video.getVideoDuration());
                playView.prepareVideo(videoPath, this);
                playView.playVideo();
                togglePlayButton(false);
            } else {//视频文件不存在 TODO
                Toast.makeText(this, "视频文件不存在", Toast.LENGTH_SHORT).show();
//                this.finish();
            }
        }
    }

    /**
     * 暂停，开始按钮的切换
     * 
     * @param isPlay
     */
    private void togglePlayButton(boolean isPlay) {
        if (isPlay) {
            playBtn.setVisibility(View.VISIBLE);
            pauseBtn.setVisibility(View.GONE);
        } else {
            playBtn.setVisibility(View.GONE);
            pauseBtn.setVisibility(View.VISIBLE);
        }

    }

    private static Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case TIME_MSG_WHAT:
                systemTime.setText(DateFormat.format("hh:mm:ss", System.currentTimeMillis()));
                if (topBar.getVisibility() == View.VISIBLE && (--showTime) == 0) {//控制栏显示5s自动消失
                    toggleOperateBar();
                }
                break;

            default:
                break;
            }
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeLoopFlag = false;
        playView.stopVideo();//停止播放视频
        FFmpeg.stopVideo();//ffmpeg关闭视频流
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
    }

    /**
     * @author ZXC
     *         监听电池广播
     */
    class BatteryBoradcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int) (100 * level / (float) scale);
                battery.setText(batteryPct + "%");
            }
        }
    }

    /**
     * @author zxc
     *         更新时间线程
     */
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
}
