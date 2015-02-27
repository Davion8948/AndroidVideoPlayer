package com.timeslily.utils;

import java.io.File;
import java.text.DecimalFormat;

import android.graphics.Bitmap;
import android.text.format.DateFormat;

import com.timeslily.ffmpeg.FFmpegPlayer;

public class VideoUtil {
    public static final String[] EXTENSIONS = { ".mp4", ".flv", ".avi", ".wmv", ".m4v", ".rmvb", ".mkv", ".mg2", ".mov", "m2v" };
    /** 保存最后播放视频列表 */
    public static final String LAST_PLAY_VIDEO_INFO = "lastPlayInfo";
    /** sp中保存的属性信息 */
    public static final String SETTING_INFOS = "SettingInfos";
    /** 音量 */
    public static final String VOLUME_INFO = "volumeInfo";
    /** 亮度 */
    public static final String BRIGHTNESS_INFO = "brightnessInfo";
    public static final int VERSION = android.os.Build.VERSION.SDK_INT;
    public static final String DATABASE_NAME = "video_player.db";
    public static final int DATABASE_VERSION = 1;
    /**
     * 判断文件格式是否支持
     * 
     * @param fileName 文件名
     * @return
     */
    public static boolean isSupportedVideo(String fileName) {
        for (int i = 0; i < EXTENSIONS.length; i++) {
            if (fileName.indexOf(EXTENSIONS[i]) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取视频文件第一帧图像
     * 
     * @param subFilePath 视频
     * @param width
     * @param height
     * @return
     */
    @Deprecated
    public static Bitmap getFirstFrameBitmap(String subFilePath, int width, int height) {

        Bitmap map = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
//        ffmpeg.getFirstFrame(map, subFilePath);
        return map;
    }

    /**
     * 获取视频时长
     * 
     * @param videoPath
     * @return
     */
    public static int getDuration(String videoPath) {
        return FFmpegPlayer.getDuration(videoPath);
    }

    /**
     * 将秒数转为时分秒
     * 
     * @param second 秒数
     * @return
     */
    public static String formatTime(int second) {
        if (second == 0) {
            return "00:00";
        }
        StringBuilder sb = new StringBuilder();
        int minute = second / 60;
        sb.append(minute > 9 ? minute : "0" + minute);
        sb.append(":");
        int sec = (second % 3600) % 60;
        sb.append(sec > 9 ? sec : "0" + sec);
        return sb.toString();
    }

    /**
     * 获取文件夹下视频个数
     * 
     * @param file 文件夹
     * @return
     */
    public static int getVideoCount(String path) {
        File file = new File(path);
        int count = 0;
        for (File subFile : file.listFiles()) {
            if (subFile.isFile() && isSupportedVideo(subFile.getName())) {//统计视频数量
                count++;
            }
        }
        return count;
    }

    private static final String FILE_NAME_REGULAR = "[^\\s\\\\/:\\*\\?\\\"<>\\|](\\x20|[^\\s\\\\/:\\*\\?\\\"<>\\|])*[^\\s\\\\/:\\*\\?\\\"<>\\|\\.]$";

    /**
     * 判断文件名是否合法
     * 
     * @param fileName
     * @return
     */
    public static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.length() > 255 || "".equals(fileName)) {
            return false;
        } else {
            return fileName.matches(FILE_NAME_REGULAR);
        }
    }

    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";

    /**
     * 文件夹最后修改时间
     * 
     * @param file
     * @return
     */
    public static CharSequence getFolderDate(File file) {
        long time = file.lastModified();
        return DateFormat.format(DATE_FORMAT, time);
    }

    /**
     * 获取文件夹下视频大小
     * 
     * @param file
     * @return
     */
    public static CharSequence getFolderVideosSize(File file) {
        long size = 0l;
        for (File subFile : file.listFiles()) {
            if (subFile.isFile() && isSupportedVideo(subFile.getName())) {//视频格式
                size += subFile.length();
            }
        }
        return formatFileSize(size);
    }

    public static CharSequence getFileSize(File file) {
        return formatFileSize(file.length());
    }

    /**
     * 转换文件大小
     * 
     * @param fileS
     * @return
     */
    private static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileS == 0) {
            return wrongSize;
        }
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    /**
     * 删除视频缩略图缓存
     * 
     * @param cacheFile
     * @param name
     */
    public static void deleteCache(String name) {
        File thumbnailFile = new File(name);
        if (thumbnailFile.exists()) {
            thumbnailFile.delete();
        }
    }

    public static boolean deleteFolder(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFolder(files[i]);
                }
                file.delete();
            }
        } else {
            return false;
        }
        return true;
    }
}
