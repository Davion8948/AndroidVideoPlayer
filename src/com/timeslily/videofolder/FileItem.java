package com.timeslily.videofolder;

/**
 * 用于保存文件信息
 */
public class FileItem {
    /** 文件名 */
    private String fileName;
    /** 文件时间信息 */
    private String fileTime;
    /** 是否选中 */
    private boolean checked;
    /** 文件图标 */
    private int drawableId;
    /** 文件路径 */
    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getDrawableId() {
        return drawableId;
    }

    public void setDrawableId(int drawableId) {
        this.drawableId = drawableId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileTime() {
        return fileTime;
    }

    public void setFileTime(String fileTime) {
        this.fileTime = fileTime;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public FileItem(String fileName, String fileTime, boolean checked, int drawableId, String filePath) {
        super();
        this.fileName = fileName;
        this.fileTime = fileTime;
        this.checked = checked;
        this.drawableId = drawableId;
        this.filePath = filePath;
    }

    public FileItem() {
        super();
    }

}
