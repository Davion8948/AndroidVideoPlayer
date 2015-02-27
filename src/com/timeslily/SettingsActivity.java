package com.timeslily;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.SearchRecentSuggestions;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements OnClickListener {
    private MyDialogPreference dialogPreference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        dialogPreference = (MyDialogPreference) findPreference("myDialogPreference");
        dialogPreference.setOnClickListener(this);
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog.equals(dialogPreference.getDialog())) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                SearchRecentSuggestions suggestion = new SearchRecentSuggestions(this, RecentProvider.AUTHORITY, RecentProvider.MODE);
                suggestion.clearHistory();
                Toast.makeText(this, getResources().getString(R.string.clear_ok), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
