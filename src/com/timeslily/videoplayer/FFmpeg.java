package com.timeslily.videoplayer;

import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * @author zxc
 *         FFmpeg类，主要是调用ffmpeg的本地方法
 */
@Deprecated
public class FFmpeg {
    private static final String TAG = "ffmpeg";
    private static FFmpeg ffmpeg;
    public static int frameRate;
    /**
     * 私有化构造器
     */
    private FFmpeg() {
        avRegisterAll();
    }

    /**
     * 获取单例ffmpeg对象
     * 
     * @return ffmpeg对象
     */
    public static FFmpeg getInstance() {
        if (ffmpeg == null) {
            ffmpeg = new FFmpeg();
        }
        return ffmpeg;
    }

    /**
     * 初始化ffmpeg相关设置
     * 
     * @param filePath 视频文件路径
     * @return
     */
    public boolean initFFmpeg(String filePath) {
        Log.i(TAG, filePath);
        if (avFormatOpenInput(filePath)) {
            Log.i(TAG, "success open");
        } else {
            Log.i(TAG, "failed open");
            return false;
        }
        if (avFormatFindStreamInfo()) {
            Log.i(TAG, "success find stream info");
        } else {
            Log.i(TAG, "failed find stream info");
            return false;
        }
        if (avFormatFindVideoStream()) {
            Log.i(TAG, "success find stream");
        } else {
            Log.i(TAG, "failed find stream");
            return false;
        }
        if (avCodecFindDecoder()) {
            Log.i(TAG, "success find decoder");
        } else {
            Log.i(TAG, "failed find decoder");
            return false;
        }
        if (avCodecOpen2()) {
            Log.i(TAG, "success codec open");
        } else {
            Log.i(TAG, "failed codec open");
            return false;
        }
        Log.i(TAG, getCodecName());
        return true;
    }

    public void prepareVideo(String filePath, DisplayMetrics dm) {
        if (ffmpeg.initFFmpeg(filePath)) {
            int[] fps = ffmpeg.getFPS();
            frameRate = (int) ((double) (100 * fps[1]) / fps[0]);
            ffmpeg.setVideoScreenSize(dm.widthPixels, dm.heightPixels);
            ffmpeg.prepareResources();
        } else {
            Log.e("ccc", "初始化错误");
        }
    }
    /**
     * 判断是否是视频文件
     * 
     * @param filePath
     * @return
     */
    public boolean isVideo(String filePath) {
        Log.i("ccc", "filepath=====" + filePath);
        if (avFormatOpenInput(filePath)) {
            avFormatCloseInput();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 准备播放视频的资源
     */
    public void prepareResources() {
        allocateBuffer();
    }

    /**
     * 用以注册支持的文件格式以及编码器
     */
    private native void avRegisterAll();

    /**
     * 读取文件头
     * 
     * @param filePath 文件路径
     * @return
     */
    private native boolean avFormatOpenInput(String filePath);

    /**
     * 读取流信息
     * 
     * @return
     */
    private native boolean avFormatFindStreamInfo();

    /**
     * 读取流
     * 
     * @return
     */
    private native boolean avFormatFindVideoStream();

    /**
     * 在库里面查找支持该格式的解码器
     * 
     * @return
     */
    private native boolean avCodecFindDecoder();

    /**
     * 打开解码器
     * 
     * @return
     */
    private native boolean avCodecOpen2();

    /**
     * 关闭文件流
     */
    private native void avFormatCloseInput();
//    //分配一个帧指针，指向解码后的原始帧
//    private native void avCodecAllocFrame();

    public native String getCodecName();

    public native boolean allocateBuffer();

    /**
     * 设置视频播放的长宽
     * 
     * @param width
     * @param height
     */
    public native void setVideoScreenSize(int width, int height);

    /**
     * 视频资源的宽度
     * 
     * @return
     */
    public native int getWidth();

    /**
     * 视频资源的高度
     * 
     * @return
     */
    public native int getHeight();

    /**
     * 停止视频播放
     */
    public native static void stopVideo();

    /**
     * 获取下一个解码的帧数据
     * 
     * @return
     */
    public native byte[] getNextDecodedFrame();

    /**
     * 获取视频某一秒图像
     * 
     * @param bitmap 存储图像的对象
     * @param second 秒
     * @param videoPath 视频路径
     */
    public native void getFrameAt(Bitmap bitmap, int second, String videoPath);

    /**
     * 获取视频第一帧图像
     * 
     * @param bitmap 存储图像的对象
     * @param videoPath 视频路径
     */
    public native void getFirstFrame(Bitmap bitmap, String videoPath);

    /** 获取视频时长 */
    public native String getDuration(String videoPath);
    public native Bitmap createBitmap(int width, int height);

    /**
     * 获取视频帧率
     * 
     * @return int[0]:num int[1]:den
     */
    public native int[] getFPS();
    static {
        System.loadLibrary("avutil-52");
        System.loadLibrary("avcodec-55");
        System.loadLibrary("avformat-55");
        System.loadLibrary("swscale-2");
        System.loadLibrary("swresample-0");
        System.loadLibrary("ffmpeg");
    }

}