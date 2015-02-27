package com.timeslily.videofile;

import java.util.List;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.timeslily.R;

public class VideoAdapter extends BaseAdapter {
    private List<VideoItem> videos;
    private LayoutInflater inflater;
    Context context;

    public VideoAdapter(List<VideoItem> videos, Context context) {
        super();
        this.videos = videos;
        this.inflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    @Override
    public Object getItem(int position) {
        return videos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.video_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        VideoItem video = videos.get(position);
        holder.videoName.setTextColor(context.getResources().getColor(video.getRecentPlay() == 1 ? R.color.blue : R.color.black));
        holder.videoName.setText(video.getVideoName());
        String duration = video.getVideoDuration();
        if (!"00:00".equals(duration)) {
            String time = String.format(context.getResources().getString(R.string.format_duration), duration);
            holder.videoDuration.setText(time);
            String lastPlayedTime = video.getLastPlayedTime();
            //播放完毕
            lastPlayedTime = duration.equals(lastPlayedTime) ? context.getResources().getString(R.string.play_over) : lastPlayedTime;
            holder.videoPlayed.setText(lastPlayedTime);
        }
//        Bitmap thumbnail = video.getVideoThumbnail();
//        if (thumbnail != null) {
//            holder.videoThubmnail.setImageBitmap(thumbnail);
//        }
        String thumbnailPath = video.getThumbnailPath();
        if (thumbnailPath != null && !"".equals(thumbnailPath)) {
            holder.videoThubmnail.setImageBitmap(BitmapFactory.decodeFile(thumbnailPath));
        }
        return convertView;
    }

    class ViewHolder {
        ImageView videoThubmnail;
        TextView videoName;
        TextView videoDuration;
        TextView videoPlayed;

        public ViewHolder(View view) {
            this.videoThubmnail = (ImageView) view.findViewById(R.id.iv_video_thubmnail);
            this.videoName = (TextView) view.findViewById(R.id.tv_video_name);
            this.videoDuration = (TextView) view.findViewById(R.id.tv_video_duration);
            videoPlayed = (TextView) view.findViewById(R.id.tv_video_played_time);
        }

    }
}
