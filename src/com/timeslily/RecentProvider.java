package com.timeslily;

import android.content.SearchRecentSuggestionsProvider;

public class RecentProvider extends SearchRecentSuggestionsProvider {
    final static String AUTHORITY = "com.timeslily.RecentProvider";
    final static int MODE = DATABASE_MODE_QUERIES;

    public RecentProvider() {
        super();
        setupSuggestions(AUTHORITY, MODE);
    }
}
