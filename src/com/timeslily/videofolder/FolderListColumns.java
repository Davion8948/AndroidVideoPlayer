package com.timeslily.videofolder;

import android.provider.BaseColumns;

public class FolderListColumns implements BaseColumns {
    /** 表名 */
    public static final String TABLE_NAME = "tb_folderlist";
    /** 文件名 */
    public static final String FOLDER_NAME = "folder_name";
    /** 文件路径 */
    public static final String FOLDER_PATH = "folder_path";
    /** 文件包含视频个数 */
    public static final String VIDEO_COUNT = "video_count";
    /** 上次播放 0:上次未播放过 1:上次播放过 */
    public static final String RECENT_PLAY = "last_play";
    /** 是否是新添加 0：否 1：是 */
    public static final String NEW_ADD = "new_add";
}
