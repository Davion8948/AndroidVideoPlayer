package com.timeslily;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class MyDialogPreference extends DialogPreference {
    private DialogInterface.OnClickListener onClickListener = null;

    public MyDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        this.onClickListener = listener;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (onClickListener != null) {
            onClickListener.onClick(dialog, which);
        }
        super.onClick(dialog, which);
    }
}
