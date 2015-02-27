package com.timeslily.videoplayer;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

/**
 * 视频播放视图的最原始方式 用线程来画bitmap
 * 
 * @author zxc
 */
public class PlayView extends View implements Runnable {
    /** 控制视频刷新速度 */
    public int frameRate = 250;
    private boolean isPlaying;
    private boolean existPlay;
    /** 显示视频帧的位图 */
    private Bitmap bitmap;
    /** 画视频帧的画笔 */
    private Paint paint;
    private FFmpeg ffmpeg;
    public PlayView(Context context) {
        super(context);
    }

    public PlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }
    /**
     * 暂停视频
     */
    public void pauseVideo() {
        isPlaying = false;
    }

    /**
     * 播放视频
     */
    public void playVideo() {
        isPlaying = true;
        existPlay = false;
    }

    /**
     * 停止视频
     */
    public void stopVideo() {
        existPlay = true;
    }

    /**
     * 初始化
     * 
     * @param filePath 视频文件路径
     * @param activity 活动 用于获取播放视频宽高
     */
    public void prepareVideo(String filePath, Activity activity) {
        ffmpeg = FFmpeg.getInstance();
        if (ffmpeg.initFFmpeg(filePath)) {//成功初始化
            int[] fps = ffmpeg.getFPS();
            frameRate = (int) ((double) (1000 * fps[1]) / fps[0]);//帧率
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            ffmpeg.setVideoScreenSize(dm.widthPixels, dm.heightPixels);
            ffmpeg.prepareResources();
            bitmap = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.RGB_565);
            new Thread(this).start();//开启线程
        } else {
            Log.e("ffmpeg", "初始化失败");
        }
    }
    @Override
    public void run() {//刷新视图
        while (!Thread.currentThread().isInterrupted() && !existPlay) {
            try {
                Thread.sleep(frameRate);
//                Log.i("ccc", "frameRate====" + frameRate);
                if (isPlaying) {
                    postInvalidate();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (ffmpeg != null) {
            byte[] nativePixels = ffmpeg.getNextDecodedFrame();
            if (nativePixels != null) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(nativePixels);
                bitmap.copyPixelsFromBuffer(byteBuffer);
            }
            canvas.drawBitmap(bitmap, 0, 0, paint);
        }
    }
}
