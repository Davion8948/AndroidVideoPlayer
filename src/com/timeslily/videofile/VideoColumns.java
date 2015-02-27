package com.timeslily.videofile;

import android.provider.BaseColumns;

public class VideoColumns implements BaseColumns {
    public static final String TABLE_NAME = "tb_video";
    /** 视频名称 */
    public static final String VIDEO_NAME = "video_name";
    /** 视频路径 */
//    public static final String VIDEO_PATH = "video_path";
    /** 缩略图缓存路径 */
    public static final String VIDEO_THUBMNAIL_PATH = "video_thumbnail_path";
    /** 父文件路径 */
    public static final String VIDEO_PARENT_PATH = "video_parent_path";
    /** 视频时长 */
    public static final String VIDEO_DURATION = "video_duration";
    /** 是否播放过 */
    public static final String VIDEO_IS_PLAYED = "video_is_played";
    /** 上次播放时间 */
    public static final String VIDEO_LAST_PLAY_TIME = "video_last_play_time";
    /** 最近播放标识 */
    public static final String VIDEO_RECENT_PLAY = "video_recent_play";

}
