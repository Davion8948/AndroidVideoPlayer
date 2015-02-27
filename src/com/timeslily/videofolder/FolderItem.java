package com.timeslily.videofolder;

/**
 * 用于保存文件夹属性信息
 */
public class FolderItem {
    /** 数据库中id */
    private int folderId;

    /** 文件夹名 */
    private String folderName;
    /** 文件夹下视频数量 */
    private int videoCount;
    /** 文件夹路径 */
    private String folderPath;
    /** 最近播放 */
    private int recentPlay;

    public int getRecentPlay() {
        return recentPlay;
    }

    public void setRecentPlay(int recentPlay) {
        this.recentPlay = recentPlay;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public FolderItem(int folderId, String folderPath, String folderName, int videoCount) {
        super();
        this.folderId = folderId;
        this.folderPath = folderPath;
        this.folderName = folderName;
        this.videoCount = videoCount;
    }

    public FolderItem() {
        super();
    }

}
