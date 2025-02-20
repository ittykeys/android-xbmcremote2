/*
 *      Copyright (C) 2005-2009 Team XBMC
 *      http://xbmc.org
 *
 *  This Program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2, or (at your option)
 *  any later version.
 *
 *  This Program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XBMC Remote; see the file license.  If not, write to
 *  the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *  http://www.gnu.org/copyleft/gpl.html
 *
 */

package org.xbmc.android.remote2.presentation.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import org.xbmc.android.remote2.R;
import org.xbmc.android.remote2.business.AbstractManager;
import org.xbmc.android.remote2.business.ManagerFactory;
import org.xbmc.android.remote2.presentation.activity.EpisodeDetailsActivity;
import org.xbmc.android.remote2.presentation.activity.NowPlayingActivity;
import org.xbmc.android.remote2.presentation.widget.FiveLabelsItemView;
import org.xbmc.android.remote2.presentation.widget.FlexibleItemView;
import org.xbmc.android.util.ImportUtilities;
import org.xbmc.api.business.DataResponse;
import org.xbmc.api.business.IControlManager;
import org.xbmc.api.business.ISortableManager;
import org.xbmc.api.business.ITvShowManager;
import org.xbmc.api.object.Episode;
import org.xbmc.api.object.Movie;
import org.xbmc.api.object.Season;
import org.xbmc.api.type.SortType;
import org.xbmc.api.type.ThumbSize;

import java.util.ArrayList;

public class EpisodeListController extends ListController implements IController {

    public static final int ITEM_CONTEXT_PLAY = 1;
    public static final int ITEM_CONTEXT_INFO = 2;
    public static final int ITEM_CONTEXT_PLAY_FROM_HERE = 3;
    public static final int ITEM_CONTEXT_QUEUE = 4;
    public static final int MENU_PLAY_ALL = 1;
    public static final int MENU_SORT = 2;
    public static final int MENU_SORT_BY_TITLE_ASC = 21;
    public static final int MENU_SORT_BY_TITLE_DESC = 22;
    public static final int MENU_SORT_BY_YEAR_ASC = 23;
    public static final int MENU_SORT_BY_YEAR_DESC = 24;
    public static final int MENU_SORT_BY_RATING_ASC = 25;
    public static final int MENU_SORT_BY_RATING_DESC = 26;
    public static final int MENU_SORT_BY_EPISODE_ASC = 27;
    public static final int MENU_SORT_BY_EPISODE_DESC = 28;
    private static final int mThumbSize = ThumbSize.SMALL;
    private static final long serialVersionUID = 1088971882661811256L;
    private static Bitmap mWatchedBitmap;
    private Season mSeason;
    private boolean mRecentEpisodes = false;
    private ITvShowManager mTvManager;
    private IControlManager mControlManager;
    private boolean mLoadCovers = false;

    public void onCreate(Activity activity, Handler handler, AbsListView list) {

        mTvManager = ManagerFactory.getTvManager(this);
        mControlManager = ManagerFactory.getControlManager(this);

        final String sdError = ImportUtilities.assertSdCard();
        mLoadCovers = sdError == null;

        if (!isCreated()) {
            super.onCreate(activity, handler, list);

            if (!mLoadCovers) {
                Toast toast = Toast.makeText(activity, sdError + " Displaying place holders only.", Toast.LENGTH_LONG);
                toast.show();
            }

            mSeason = (Season) activity.getIntent().getSerializableExtra(ListController.EXTRA_SEASON);

            activity.registerForContextMenu(mList);

            mFallbackBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.default_poster);
            mWatchedBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.check_mark);
            setupIdleListener();

            mList.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (isLoading()) return;
                    final Episode episode = (Episode) mList.getAdapter().getItem(((FiveLabelsItemView) view).position);
                    Intent nextActivity = new Intent(view.getContext(), EpisodeDetailsActivity.class);
                    nextActivity.putExtra(ListController.EXTRA_EPISODE, episode);
                    mActivity.startActivity(nextActivity);
                }
            });
            mList.setOnKeyListener(new ListControllerOnKeyListener<Movie>());
            fetch();
        }
    }

    private void fetch() {
        // tv show and episode both are using the same manager so set the sort key here
        ((ISortableManager) mTvManager).setSortKey(AbstractManager.PREF_SORT_KEY_EPISODE);
        ((ISortableManager) mTvManager).setPreferences(mActivity.getPreferences(Context.MODE_PRIVATE));

        final String title = mSeason != null ? mSeason.getName() + " - " : "" + "Episodes";
        DataResponse<ArrayList<Episode>> response = new DataResponse<ArrayList<Episode>>() {
            public void run() {
                if (value.size() > 0) {
                    setTitle(title + " (" + value.size() + ")");
                    ((AdapterView<ListAdapter>) mList).setAdapter(new EpisodeAdapter(mActivity, value));
                } else {
                    setNoDataMessage("No episodes found.", R.drawable.icon_movie_dark);
                }
            }
        };

        showOnLoading();
        setTitle(title + "...");
        if (mSeason != null) {
            mTvManager.getEpisodes(response, mSeason, mActivity.getApplicationContext());
        } else {
            mTvManager.getRecentlyAddedEpisodes(response, mActivity.getApplicationContext());
            mRecentEpisodes = true;
        }

    }

    /**
     * Shows a dialog and refreshes the movie library if user confirmed.
     *
     * @param activity
     */
    public void refreshMovieLibrary(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Are you sure you want XBMC to rescan your movie library?")
                .setCancelable(false)
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mControlManager.updateLibrary(new DataResponse<Boolean>() {
                            public void run() {
                                final String message;
                                if (value) {
                                    message = "Movie library updated has been launched.";
                                } else {
                                    message = "Error launching movie library update.";
                                }
                                Toast toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }, "video", mActivity.getApplicationContext());
                    }
                })
                .setNegativeButton("Uh, no.", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        final FiveLabelsItemView view = (FiveLabelsItemView) ((AdapterContextMenuInfo) menuInfo).targetView;
        menu.setHeaderTitle(view.title);
        menu.add(0, ITEM_CONTEXT_PLAY, 1, "Play Episode");
        menu.add(0, ITEM_CONTEXT_PLAY_FROM_HERE, 2, "Play From Here");
        menu.add(0, ITEM_CONTEXT_QUEUE, 3, "Queue Episode");
        menu.add(0, ITEM_CONTEXT_INFO, 4, "View Details");
    }

    public void onContextItemSelected(MenuItem item) {
        int listpos = ((FiveLabelsItemView) ((AdapterContextMenuInfo) item.getMenuInfo()).targetView).position;
        final Episode episode = (Episode) mList.getAdapter().getItem(listpos);
        switch (item.getItemId()) {
            case ITEM_CONTEXT_PLAY:
                mControlManager.playFile(new DataResponse<Boolean>() {
                    public void run() {
                        if (value) {
                            mActivity.startActivity(new Intent(mActivity, NowPlayingActivity.class));
                        }
                    }
                }, episode.fileName, 1, mActivity.getApplicationContext());
                break;
            case ITEM_CONTEXT_INFO:
                Intent nextActivity = new Intent(mActivity, EpisodeDetailsActivity.class);
                nextActivity.putExtra(ListController.EXTRA_EPISODE, episode);
                mActivity.startActivity(nextActivity);
                break;
            case ITEM_CONTEXT_PLAY_FROM_HERE:
                mControlManager.clearPlaylist(new DataResponse<Boolean>(), 1, mActivity);
                //mControlManager.setPlaylistId(new DataResponse<Boolean>(), 1, mActivity);
                int numitems = mList.getAdapter().getCount();
                for (int i = 0; i < numitems; i++)
                    mControlManager.addToPlaylist(new DataResponse<Boolean>(), ((Episode) mList.getAdapter().getItem(i)).fileName, 1, mActivity);
                mControlManager.setPlaylistPos(new DataResponse<Boolean>() {
                    public void run() {
                        if (value) {
                            mActivity.startActivity(new Intent(mActivity, NowPlayingActivity.class));
                        }
                    }
                }, 1, listpos, mActivity.getApplicationContext());
                break;
            case ITEM_CONTEXT_QUEUE:
                mControlManager.addToPlaylist(new QueryResponse(mActivity, "Queued episode: " + episode.getName(), "Error queueing file."), episode.fileName, 1, mActivity);
                break;
            default:
                return;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
//		if (mActor != null || mGenre != null) {
//			menu.add(0, MENU_PLAY_ALL, 0, "Play all").setIcon(R.drawable.menu_album);
//		}
        SubMenu sortMenu = menu.addSubMenu(0, MENU_SORT, 0, "Sort").setIcon(R.drawable.menu_sort);
        sortMenu.add(2, MENU_SORT_BY_EPISODE_ASC, 0, "by Episode ascending");
        sortMenu.add(2, MENU_SORT_BY_EPISODE_DESC, 0, "by Episode descending");
        sortMenu.add(2, MENU_SORT_BY_TITLE_ASC, 0, "by Title ascending");
        sortMenu.add(2, MENU_SORT_BY_TITLE_DESC, 0, "by Title descending");
        sortMenu.add(2, MENU_SORT_BY_RATING_ASC, 0, "by Rating ascending");
        sortMenu.add(2, MENU_SORT_BY_RATING_DESC, 0, "by Rating descending");
//		menu.add(0, MENU_SWITCH_VIEW, 0, "Switch view").setIcon(R.drawable.menu_view);
        createShowHideWatchedToggle(menu);
    }

    @Override
    public void onOptionsItemSelected(MenuItem item) {
        final SharedPreferences.Editor ed;
        switch (item.getItemId()) {
            case MENU_PLAY_ALL:
                break;
            case MENU_SORT_BY_EPISODE_ASC:
                ed = mActivity.getPreferences(Context.MODE_PRIVATE).edit();
                ed.putInt(AbstractManager.PREF_SORT_BY_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.EPISODE_NUM);
                ed.putString(AbstractManager.PREF_SORT_ORDER_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.ORDER_ASC);
                ed.commit();
                fetch();
                break;
            case MENU_SORT_BY_EPISODE_DESC:
                ed = mActivity.getPreferences(Context.MODE_PRIVATE).edit();
                ed.putInt(AbstractManager.PREF_SORT_BY_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.EPISODE_NUM);
                ed.putString(AbstractManager.PREF_SORT_ORDER_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.ORDER_DESC);
                ed.commit();
                fetch();
                break;
            case MENU_SORT_BY_TITLE_ASC:
                ed = mActivity.getPreferences(Context.MODE_PRIVATE).edit();
                ed.putInt(AbstractManager.PREF_SORT_BY_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.EPISODE_TITLE);
                ed.putString(AbstractManager.PREF_SORT_ORDER_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.ORDER_ASC);
                ed.commit();
                fetch();
                break;
            case MENU_SORT_BY_TITLE_DESC:
                ed = mActivity.getPreferences(Context.MODE_PRIVATE).edit();
                ed.putInt(AbstractManager.PREF_SORT_BY_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.EPISODE_TITLE);
                ed.putString(AbstractManager.PREF_SORT_ORDER_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.ORDER_DESC);
                ed.commit();
                fetch();
                break;
            case MENU_SORT_BY_RATING_ASC:
                ed = mActivity.getPreferences(Context.MODE_PRIVATE).edit();
                ed.putInt(AbstractManager.PREF_SORT_BY_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.EPISODE_RATING);
                ed.putString(AbstractManager.PREF_SORT_ORDER_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.ORDER_ASC);
                ed.commit();
                fetch();
                break;
            case MENU_SORT_BY_RATING_DESC:
                ed = mActivity.getPreferences(Context.MODE_PRIVATE).edit();
                ed.putInt(AbstractManager.PREF_SORT_BY_PREFIX + AbstractManager.PREF_SORT_KEY_EPISODE, SortType.EPISODE_RATING);
                ed.putString(AbstractManager.PREF_SORT_ORDER_PREFIX + AbstractManager.PREF_SORT_KEY_SHOW, SortType.ORDER_DESC);
                ed.commit();
                fetch();
                break;
            default:
                super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void refreshList() {
        hideMessage();
        fetch();
    }

    public void onActivityPause() {
        if (mTvManager != null) {
            mTvManager.setController(null);
            mTvManager.postActivity();
        }
        if (mControlManager != null) {
            mControlManager.setController(null);
        }
        super.onActivityPause();
    }

    public void onActivityResume(Activity activity) {
        super.onActivityResume(activity);
        if (mTvManager != null) {
            mTvManager.setController(this);
        }
        if (mControlManager != null) {
            mControlManager.setController(this);
        }
    }

    private class EpisodeAdapter extends ArrayAdapter<Episode> {
        EpisodeAdapter(Activity activity, ArrayList<Episode> items) {
            super(activity, 0, items);
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            final FlexibleItemView view;
            if (convertView == null) {
                view = new FlexibleItemView(mActivity, mTvManager, parent.getWidth(), mFallbackBitmap, mList.getSelector(), false);
            } else {
                view = (FlexibleItemView) convertView;
            }

            final Episode episode = getItem(position);
            view.reset();
            view.position = position;
            view.posterOverlay = episode.numWatched > 0 ? mWatchedBitmap : null;
            view.title = episode.episode + ". " + episode.title;
            if (mRecentEpisodes) {
                view.title = episode.showTitle;
                view.subtitle = episode.season + "x" + (episode.episode < 10 ? "0" : "") + episode.episode + ". " + episode.title;
            }
            view.subtitleRight = episode.firstAired != null ? episode.firstAired : "";
//			view.bottomtitle = show.numEpisodes + " episodes";
            view.bottomright = String.valueOf(((float) Math.round(episode.rating * 10)) / 10);

            if (mLoadCovers) {
                if (mTvManager.coverLoaded(episode, mThumbSize)) {
                    view.setCover(mTvManager.getCoverSync(episode, mThumbSize));
                } else {
                    view.setCover(null);
                    view.getResponse().load(episode, !mPostScrollLoader.isListIdle());
                }
            }
            return view;
        }
    }

}
