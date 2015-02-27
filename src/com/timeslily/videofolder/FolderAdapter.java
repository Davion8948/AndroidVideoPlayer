package com.timeslily.videofolder;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.timeslily.R;

public class FolderAdapter extends BaseAdapter {
    private List<FolderItem> folders;
    private LayoutInflater inflater;
    private Context context;

    public FolderAdapter(Context context, List<FolderItem> folders) {
        inflater = LayoutInflater.from(context);
        this.folders = folders;
        this.context = context;
    }

    @Override
    public int getCount() {
        return folders.size();
    }

    @Override
    public Object getItem(int position) {
        return folders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.folder_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        FolderItem item = folders.get(position);
        holder.folderName.setTextColor(context.getResources().getColor(item.getRecentPlay() == 1 ? R.color.blue : R.color.black));
        holder.folderName.setText(item.getFolderName());
        holder.videoCount.setText(item.getVideoCount() + "");
        return convertView;
    }

    class ViewHolder {
        private TextView folderName;
        private TextView videoCount;

        public ViewHolder(View view) {
            folderName = (TextView) view.findViewById(R.id.tv_folder_name);
            videoCount = (TextView) view.findViewById(R.id.tv_video_count);
        }
    }

}
