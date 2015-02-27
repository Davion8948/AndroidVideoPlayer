package com.timeslily.videofolder;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.timeslily.R;

public class FileAdapter extends BaseAdapter {
    private List<FileItem> fileList;
    private LayoutInflater inflater;

    public FileAdapter(Context context, List<FileItem> fileList) {
        this.fileList = fileList;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_item, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final FileItem item = fileList.get(position);
//        if(item.getFileName().length()>20){
//            
//        }
        holder.fileName.setText(item.getFileName());
        holder.fileTime.setText(item.getFileTime());
        holder.fileCheckBox.setChecked(item.isChecked());
        holder.fileDrawable.setImageResource(item.getDrawableId());
        if (item.getDrawableId() == R.drawable.ic_video) {//单个文件不再显示checkbox跟下一级目录箭头
            holder.fileCheckBox.setVisibility(View.GONE);
            holder.fileEnterDrawable.setVisibility(View.GONE);
        } else {
            holder.fileCheckBox.setVisibility(View.VISIBLE);
            holder.fileEnterDrawable.setVisibility(View.VISIBLE);
        }
        holder.fileCheckBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                item.setChecked(!item.isChecked());
            }
        });
        return convertView;
    }

    class ViewHolder {
        TextView fileName;
        TextView fileTime;
        CheckBox fileCheckBox;
        ImageView fileDrawable;
        ImageView fileEnterDrawable;

        public ViewHolder(View view) {
            fileName = (TextView) view.findViewById(R.id.tv_file_name);
            fileTime = (TextView) view.findViewById(R.id.tv_file_time);
            fileCheckBox = (CheckBox) view.findViewById(R.id.cb_choose_file);
            fileDrawable = (ImageView) view.findViewById(R.id.iv_file_icon);
            fileEnterDrawable = (ImageView) view.findViewById(R.id.iv_file_enter);
        }
    }

}
