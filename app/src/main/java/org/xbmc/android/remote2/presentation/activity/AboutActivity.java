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

package org.xbmc.android.remote2.presentation.activity;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.KeyEvent;
import android.widget.TextView;

import org.xbmc.android.remote2.R;
import org.xbmc.android.remote2.business.ManagerFactory;
import org.xbmc.api.business.IEventClientManager;
import org.xbmc.api.type.ThumbSize;
import org.xbmc.eventclient.ButtonCodes;

public class AboutActivity extends Activity {

    private ConfigurationManager mConfigurationManager;
    private IEventClientManager mEventClientManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        // set display size
        final Display display = getWindowManager().getDefaultDisplay();
        ThumbSize.setScreenSize(display.getWidth(), display.getHeight());

        try {
            mEventClientManager = ManagerFactory.getEventClientManager(null);
            final String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            final int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            ((TextView) findViewById(R.id.about_version)).setText("v" + versionName);
            ((TextView) findViewById(R.id.about_revision)).setText("Revision " + versionCode);
            TextView message = (TextView) findViewById(R.id.about_url_message);

            message.setText(Html.fromHtml("Visit our project page at <a href=\"http://code.google.com/p/android-xbmcremote\">Google Code</a>."));
            message.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (NameNotFoundException e) {
            ((TextView) findViewById(R.id.about_version)).setText("Error reading version");
        }
        mConfigurationManager = ConfigurationManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConfigurationManager.onActivityResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConfigurationManager.onActivityPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                mEventClientManager.sendButton("R1", ButtonCodes.REMOTE_VOLUME_PLUS, false, true, true, (short) 0, (byte) 0);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mEventClientManager.sendButton("R1", ButtonCodes.REMOTE_VOLUME_MINUS, false, true, true, (short) 0, (byte) 0);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}