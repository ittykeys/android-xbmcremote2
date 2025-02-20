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
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import org.xbmc.android.remote2.R;
import org.xbmc.android.remote2.business.ManagerFactory;
import org.xbmc.android.remote2.presentation.activity.ListActivity;
import org.xbmc.android.remote2.presentation.widget.OneLabelItemView;
import org.xbmc.api.business.DataResponse;
import org.xbmc.api.business.IVideoManager;
import org.xbmc.api.object.Genre;

import java.util.ArrayList;

public class MovieGenreListController extends ListController implements IController {

    public static final int TYPE_MOVIE = 0;
    public static final int TYPE_TVSHOW = 1;
    private static final long serialVersionUID = 4360738733222799619L;
    private IVideoManager mVideoManager;
    private int mType;

    public MovieGenreListController(int type) {
        mType = type;
    }

    public void onCreate(Activity activity, Handler handler, AbsListView list) {

        mVideoManager = ManagerFactory.getVideoManager(this);

        if (!isCreated()) {
            super.onCreate(activity, handler, list);

            mList.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (isLoading()) return;
                    Intent nextActivity = new Intent(view.getContext(), ListActivity.class);
                    Genre genre = (Genre) mList.getAdapter().getItem(((OneLabelItemView) view).position);
                    if (mType == TYPE_MOVIE)
                        nextActivity.putExtra(ListController.EXTRA_LIST_CONTROLLER, new MovieListController());
                    else
                        nextActivity.putExtra(ListController.EXTRA_LIST_CONTROLLER, new TvShowListController());
                    nextActivity.putExtra(ListController.EXTRA_GENRE, genre);
                    mActivity.startActivity(nextActivity);
                }
            });

            mFallbackBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.icon_genre);

            final String title = mType == TYPE_MOVIE ? "Movie " : mType == TYPE_TVSHOW ? "TV Show " : "" + "genres";
            DataResponse<ArrayList<Genre>> response = new DataResponse<ArrayList<Genre>>() {
                public void run() {
                    if (value.size() > 0) {
                        setTitle(title + " (" + value.size() + ")");
                        ((AdapterView<ListAdapter>) mList).setAdapter(new GenreAdapter(mActivity, value));
                    } else {
                        setTitle(title);
                        setNoDataMessage("No genres found.", R.drawable.icon_genre_dark);
                    }
                }
            };

            mList.setOnKeyListener(new ListControllerOnKeyListener<Genre>());

            showOnLoading();
            setTitle(title + "...");
            switch (mType) {
                case TYPE_MOVIE:
                    mVideoManager.getMovieGenres(response, mActivity.getApplicationContext());
                    break;
                case TYPE_TVSHOW:
                    mVideoManager.getTvShowGenres(response, mActivity.getApplicationContext());
                    break;
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // no context menu here
    }

    @Override
    public void onContextItemSelected(MenuItem item) {
        // no context menu here
    }

    public void onActivityPause() {
        if (mVideoManager != null) {
            mVideoManager.setController(null);
        }
        super.onActivityPause();
    }

    public void onActivityResume(Activity activity) {
        super.onActivityResume(activity);
        if (mVideoManager != null) {
            mVideoManager.setController(this);
        }
    }

    private class GenreAdapter extends ArrayAdapter<Genre> {
        GenreAdapter(Activity activity, ArrayList<Genre> items) {
            super(activity, 0, items);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final OneLabelItemView view;
            if (convertView == null) {
                view = new OneLabelItemView(mActivity, parent.getWidth(), mFallbackBitmap, mList.getSelector(), true);
            } else {
                view = (OneLabelItemView) convertView;
            }
            final Genre genre = this.getItem(position);
            view.reset();
            view.position = position;
            view.title = genre.name;
            return view;
        }
    }
}