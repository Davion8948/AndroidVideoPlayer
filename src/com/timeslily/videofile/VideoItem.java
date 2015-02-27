package com.timeslily.videofile;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class VideoItem implements Parcelable {
    private String videoName;
    private String videoDuration;
    private String videoPath;
    private String thumbnailPath;
    private int id;
    private Bitmap videoThumbnail;
    private String videoParentPath;
    private String lastPlayedTime;
    private int recentPlay;
    private int isPlayed;
    /** 上次播放记录毫秒数 */
    private long lastPlay;

    public long getLastPlay() {
        return lastPlay;
    }

    public void setLastPlay(long lastPlay) {
        this.lastPlay = lastPlay;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getVideoParentPath() {
        return videoParentPath;
    }

    public void setVideoParentPath(String videoParentPath) {
        this.videoParentPath = videoParentPath;
    }

    public String getLastPlayedTime() {
        return lastPlayedTime;
    }

    public void setLastPlayedTime(String lastPlayedTime) {
        this.lastPlayedTime = lastPlayedTime;
    }

    public int getRecentPlay() {
        return recentPlay;
    }

    public void setRecentPlay(int recentPlay) {
        this.recentPlay = recentPlay;
    }

    public int getIsPlayed() {
        return isPlayed;
    }

    public void setIsPlayed(int isPlayed) {
        this.isPlayed = isPlayed;
    }

    public Bitmap getVideoThumbnail() {
        return videoThumbnail;
    }

    public void setVideoThumbnail(Bitmap videoThumbnail) {
        this.videoThumbnail = videoThumbnail;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(String videoDuration) {
        this.videoDuration = videoDuration;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public VideoItem(String videoPath) {
        this.videoPath = videoPath;

    }

    public VideoItem() {
    }

    public VideoItem(String videoName, String videoDuration, String videoPath, Bitmap videoThumbnail) {
        super();
        this.videoName = videoName;
        this.videoDuration = videoDuration;
        this.videoPath = videoPath;
        this.videoThumbnail = videoThumbnail;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(videoDuration);
        dest.writeString(videoName);
        dest.writeString(videoPath);
        dest.writeString(videoParentPath);
        dest.writeInt(id);
        dest.writeInt(recentPlay);
        dest.writeLong(lastPlay);
    }

    public static final Parcelable.Creator<VideoItem> CREATOR = new Creator<VideoItem>() {

        @Override
        public VideoItem[] newArray(int size) {
            return new VideoItem[size];
        }

        @Override
        public VideoItem createFromParcel(Parcel source) {
            VideoItem item = new VideoItem();
            item.videoDuration = source.readString();
            item.videoName = source.readString();
            item.videoPath = source.readString();
            item.videoParentPath = source.readString();
            item.id = source.readInt();
            item.recentPlay = source.readInt();
            item.lastPlay = source.readLong();
            return item;
        }
    };
}
