/*
 * Copyright 2011 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.widget.Toast;

import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.ui.dialogs.TraktCancelCheckinDialogFragment;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.enumerations.Rating;
import com.jakewharton.trakt.services.MovieService;
import com.jakewharton.trakt.services.ShowService.CheckinBuilder;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

public class TraktTask extends AsyncTask<Void, Void, Response> {

    private static final String TAG = "TraktTask";

    private Bundle mArgs;

    private final Context mContext;

    private final FragmentManager mFm;

    private TraktAction mAction;

    private OnTraktActionCompleteListener mListener;

    public interface InitBundle {
        String TRAKTACTION = "traktaction";

        String IMDB_ID = "imdbid";

        String TVDBID = "tvdbid";

        String SEASON = "season";

        String EPISODE = "episode";

        String MESSAGE = "message";

        String RATING = "rating";

        String ISSPOILER = "isspoiler";
    }

    public interface OnTraktActionCompleteListener {
        public void onTraktActionComplete(boolean wasSuccessfull);
    }

    /**
     * Initial constructor. Call <b>one</b> of the setup-methods, like
     * {@code shout(tvdbid, shout, isSpoiler)}, afterwards.
     * 
     * @param context
     * @param fm
     * @param listener
     */
    public TraktTask(Context context, FragmentManager fm, OnTraktActionCompleteListener listener) {
        mContext = context;
        mFm = fm;
        mListener = listener;
        mArgs = new Bundle();
    }

    /**
     * Fast constructor, allows passing of an already pre-built {@code args}
     * {@link Bundle}.
     * 
     * @param context
     * @param manager
     * @param args
     * @param listener
     */
    public TraktTask(Context context, FragmentManager manager, Bundle args,
            OnTraktActionCompleteListener listener) {
        this(context, manager, listener);
        mArgs = args;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     * 
     * @param tvdbid
     * @param season
     * @param episode
     * @param message
     * @return TraktTask
     */
    public TraktTask checkInEpisode(int tvdbid, int season, int episode, String message) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.CHECKIN_EPISODE.index);
        mArgs.putInt(InitBundle.TVDBID, tvdbid);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        mArgs.putString(InitBundle.MESSAGE, message);
        return this;
    }

    /**
     * Check into an episode. Optionally provide a checkin message.
     * 
     * @param tvdbid
     * @param season
     * @param episode
     * @param message
     * @return TraktTask
     */
    public TraktTask checkInMovie(String imdbId, String message) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.CHECKIN_MOVIE.index);
        mArgs.putString(InitBundle.IMDB_ID, imdbId);
        mArgs.putString(InitBundle.MESSAGE, message);
        return this;
    }

    /**
     * Rate an episode.
     * 
     * @param showTvdbid
     * @param season
     * @param episode
     * @param rating
     * @return TraktTask
     */
    public TraktTask rateEpisode(int showTvdbid, int season, int episode, Rating rating) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.RATE_EPISODE.index);
        mArgs.putInt(InitBundle.TVDBID, showTvdbid);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        mArgs.putString(InitBundle.RATING, rating.toString());
        return this;
    }

    /**
     * Rate a show.
     * 
     * @param tvdbid
     * @param rating
     * @return TraktTask
     */
    public TraktTask rateShow(int tvdbid, Rating rating) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.RATE_SHOW.index);
        mArgs.putInt(InitBundle.TVDBID, tvdbid);
        mArgs.putString(InitBundle.RATING, rating.toString());
        return this;
    }

    /**
     * Post a shout for a show.
     * 
     * @param tvdbid
     * @param shout
     * @param isSpoiler
     * @return TraktTask
     */
    public TraktTask shout(int tvdbid, String shout, boolean isSpoiler) {
        mArgs.putInt(InitBundle.TRAKTACTION, TraktAction.SHOUT.index);
        mArgs.putInt(InitBundle.TVDBID, tvdbid);
        mArgs.putString(InitBundle.MESSAGE, shout);
        mArgs.putBoolean(InitBundle.ISSPOILER, isSpoiler);
        return this;
    }

    /**
     * Post a shout for an episode.
     * 
     * @param tvdbid
     * @param season
     * @param episode
     * @param shout
     * @param isSpoiler
     * @return TraktTask
     */
    public TraktTask shout(int tvdbid, int season, int episode, String shout, boolean isSpoiler) {
        shout(tvdbid, shout, isSpoiler);
        mArgs.putInt(InitBundle.SEASON, season);
        mArgs.putInt(InitBundle.EPISODE, episode);
        return this;
    }

    @Override
    protected Response doInBackground(Void... params) {
        // we need this value in onPostExecute, so get it already here
        mAction = TraktAction.values()[mArgs.getInt(InitBundle.TRAKTACTION)];

        // check for network connection
        if (!AndroidUtils.isNetworkConnected(mContext)) {
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.offline);
            return r;
        }

        // check for valid credentials
        if (!ServiceUtils.isTraktCredentialsValid(mContext)) {
            // return null so a credentials dialog is displayed
            // it will call us again with valid credentials
            return null;
        }

        // get an authenticated trakt-java ServiceManager
        ServiceManager manager = ServiceUtils.getTraktServiceManagerWithAuth(mContext, false);
        if (manager == null) {
            // password could not be decrypted
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        }

        // get values used by all actions
        final int tvdbid = mArgs.getInt(InitBundle.TVDBID);
        final int season = mArgs.getInt(InitBundle.SEASON);
        final int episode = mArgs.getInt(InitBundle.EPISODE);

        // last chance to abort
        if (isCancelled()) {
            return null;
        }

        try {
            Response r = null;

            switch (mAction) {
                case CHECKIN_EPISODE: {
                    final String message = mArgs.getString(InitBundle.MESSAGE);

                    final CheckinBuilder checkinBuilder = manager.showService().checkin(tvdbid)
                            .season(season).episode(episode);
                    if (!TextUtils.isEmpty(message)) {
                        checkinBuilder.message(message);
                    }
                    r = checkinBuilder.fire();

                    if (TraktStatus.SUCCESS.equals(r.status)) {
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(mContext);
                        r.message = mContext
                                .getString(R.string.checkin_success_trakt,
                                        (r.show != null ? r.show.title + " " : "")
                                                + Utils.getEpisodeNumber(prefs, season, episode));
                    }

                    break;
                }
                case CHECKIN_MOVIE: {
                    final String imdbId = mArgs.getString(InitBundle.IMDB_ID);
                    final String message = mArgs.getString(InitBundle.MESSAGE);

                    final MovieService.CheckinBuilder checkinBuilder = manager
                            .movieService().checkin(imdbId);
                    if (!TextUtils.isEmpty(message)) {
                        checkinBuilder.message(message);
                    }
                    r = checkinBuilder.fire();

                    if (TraktStatus.SUCCESS.equals(r.status)) {
                        r.message = mContext
                                .getString(R.string.checkin_success_trakt,
                                        (r.movie != null ? r.movie.title + " " : ""));
                    }

                    break;
                }
                case RATE_EPISODE: {
                    final Rating rating = Rating.fromValue(mArgs.getString(InitBundle.RATING));
                    r = manager.rateService().episode(tvdbid).season(season).episode(episode)
                            .rating(rating).fire();
                    break;
                }
                case RATE_SHOW: {
                    final Rating rating = Rating.fromValue(mArgs.getString(InitBundle.RATING));
                    r = manager.rateService().show(tvdbid).rating(rating).fire();
                    break;
                }
                case SHOUT: {
                    final String shout = mArgs.getString(InitBundle.MESSAGE);
                    final boolean isSpoiler = mArgs.getBoolean(InitBundle.ISSPOILER);

                    if (episode == 0) {
                        r = manager.shoutService().show(tvdbid).shout(shout).spoiler(isSpoiler)
                                .fire();
                    } else {
                        r = manager.shoutService().episode(tvdbid).season(season).episode(episode)
                                .shout(shout).spoiler(isSpoiler).fire();
                    }
                }
                default:
                    break;
            }

            return r;
        } catch (TraktException e) {
            Utils.trackExceptionAndLog(mContext, TAG, e);
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        } catch (ApiException e) {
            Utils.trackExceptionAndLog(mContext, TAG, e);
            Response r = new Response();
            r.status = TraktStatus.FAILURE;
            r.error = mContext.getString(R.string.trakt_generalerror);
            return r;
        }
    }

    @Override
    protected void onPostExecute(Response r) {
        // dismiss a potential progress dialog
        if (mAction == TraktAction.CHECKIN_EPISODE || mAction == TraktAction.CHECKIN_MOVIE) {
            Fragment prev = mFm.findFragmentByTag("progress-dialog");
            if (prev != null) {
                FragmentTransaction ft = mFm.beginTransaction();
                ft.remove(prev);
                ft.commit();
            }
        }

        if (r != null) {
            if (TraktStatus.SUCCESS.equals(r.status)) {
                // all good

                switch (mAction) {
                    case CHECKIN_EPISODE:
                    case CHECKIN_MOVIE:
                        Toast.makeText(mContext, r.message, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(mContext,
                                r.message + " " + mContext.getString(R.string.ontrakt),
                                Toast.LENGTH_SHORT).show();
                        break;
                }

                if (mListener != null) {
                    mListener.onTraktActionComplete(true);
                }

            } else if (TraktStatus.FAILURE.equals(r.status)) {
                if (r.wait != 0) {

                    // looks like a check in is in progress
                    TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                            .newInstance(mArgs, r.wait);
                    FragmentTransaction ft = mFm.beginTransaction();
                    newFragment.show(ft, "cancel-checkin-dialog");

                } else {

                    // well, something went wrong
                    Toast.makeText(mContext, r.error, Toast.LENGTH_LONG).show();

                }

                if (mListener != null) {
                    mListener.onTraktActionComplete(false);
                }
            }
        } else {
            // notify that our first run completed, however due to invalid
            // credentials we have not done anything
            if (mListener != null) {
                mListener.onTraktActionComplete(true);
            }
        }
    }
}
