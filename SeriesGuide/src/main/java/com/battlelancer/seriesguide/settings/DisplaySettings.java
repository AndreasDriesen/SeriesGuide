package com.battlelancer.seriesguide.settings;

import com.battlelancer.seriesguide.Constants;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Settings related to appearance, display formats and sort orders.
 */
public class DisplaySettings {

    public static final String KEY_THEME = "com.battlelancer.seriesguide.theme";

    public static final String KEY_LANGUAGE = "language";

    public static final String KEY_NUMBERFORMAT = "numberformat";

    public static final String NUMBERFORMAT_DEFAULT = "default";

    public static final String NUMBERFORMAT_ENGLISHLOWER = "englishlower";

    public static final String KEY_NO_RELEASED_EPISODES = "onlyFutureEpisodes";

    public static final String KEY_NO_WATCHED_EPISODES
            = "com.battlelancer.seriesguide.activity.nowatched";

    public static final String KEY_SEASON_SORT_ORDER = "seasonSorting";

    public static final String KEY_EPISODE_SORT_ORDER = "episodeSorting";

    public static final String KEY_HIDE_SPECIALS = "onlySeasonEpisodes";

    public static String getThemeIndex(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).
                getString(KEY_THEME, "0");
    }

    public static String getContentLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LANGUAGE, "en");
    }

    public static String getNumberFormat(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_NUMBERFORMAT, NUMBERFORMAT_DEFAULT);
    }

    public static Constants.EpisodeSorting getEpisodeSortOrder(Context context) {
        String orderId = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_EPISODE_SORT_ORDER, Constants.EpisodeSorting.OLDEST_FIRST.value());
        return Constants.EpisodeSorting.fromValue(orderId);
    }

    public static Constants.SeasonSorting getSeasonSortOrder(Context context) {
        String orderId = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SEASON_SORT_ORDER, Constants.SeasonSorting.LATEST_FIRST.value());
        return Constants.SeasonSorting.fromValue(orderId);
    }

    public static boolean isNoReleasedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_NO_RELEASED_EPISODES, false);
    }

    public static boolean isNoWatchedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_NO_WATCHED_EPISODES, false);
    }

    /**
     * Whether to exclude special episodes wherever possible (except in the actual seasons and
     * episode lists of a show).
     */
    public static boolean isHidingSpecials(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_HIDE_SPECIALS, false);
    }

}
