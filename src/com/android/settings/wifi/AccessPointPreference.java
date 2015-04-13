/*
 * Copyright (C) 2015 The Android Open Source Project
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
 */
package com.android.settings.wifi;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.WifiConfiguration;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.Preference;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

public class AccessPointPreference extends Preference {

    private static final int[] STATE_SECURED = {
        R.attr.state_encrypted
    };
    private static final int[] STATE_NONE = {};

    private static int[] wifi_signal_attributes = { R.attr.wifi_signal };

    private TextView mSummaryView;
    private boolean showSummary = true;
    private AccessPoint mAccessPoint;

    public AccessPointPreference(AccessPoint accessPoint, Context context) {
        super(context);
        mAccessPoint = accessPoint;
        mAccessPoint.setTag(this);
        refresh();
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        updateIcon(mAccessPoint.getLevel(), getContext());

        mSummaryView = (TextView) view.findViewById(com.android.internal.R.id.summary);
        mSummaryView.setVisibility(showSummary ? View.VISIBLE : View.GONE);

        notifyChanged();
    }

    public void showAppIcon() {
        PackageManager pm = getContext().getPackageManager();
        String systemName = pm.getNameForUid(android.os.Process.SYSTEM_UID);
        WifiConfiguration mConfig = mAccessPoint.getConfig();

        Drawable drawable = pm.getDefaultActivityIcon();
        if (mConfig == null) {
            drawable.setAlpha(0);
        } else {
            int userId = UserHandle.getUserId(mConfig.creatorUid);
            ApplicationInfo appInfo = null;
            if (mConfig.creatorName.equals(systemName)) {
                appInfo = getContext().getApplicationInfo();
            } else {
                try {
                    IPackageManager ipm = AppGlobals.getPackageManager();
                    appInfo = ipm.getApplicationInfo(mConfig.creatorName, 0 /* flags */, userId);
                } catch (RemoteException rex) {
                    // use default app icon
                }
            }
            if (appInfo != null) {
                drawable = appInfo.loadIcon(pm);
                drawable = pm.getUserBadgedIcon(drawable, new UserHandle(userId));
            }
        }

        setIcon(drawable);
    }

    protected void updateIcon(int level, Context context) {
        if (level == -1) {
            setIcon(null);
        } else {
            Drawable drawable = getIcon();

            if (drawable == null) {
                // To avoid a drawing race condition, we first set the state (SECURE/NONE) and then
                // set the icon (drawable) to that state's drawable.
                StateListDrawable sld = (StateListDrawable) context.getTheme()
                        .obtainStyledAttributes(wifi_signal_attributes).getDrawable(0);
                // If sld is null then we are indexing and therefore do not have access to
                // (nor need to display) the drawable.
                if (sld != null) {
                    sld.setState((mAccessPoint.getSecurity() != AccessPoint.SECURITY_NONE)
                            ? STATE_SECURED
                            : STATE_NONE);
                    drawable = sld.getCurrent();
                    setIcon(drawable);
                }
            }

            if (drawable != null) {
                drawable.setLevel(level);
            }
        }
    }

    /**
     * Shows or Hides the Summary of an AccessPoint.
     *
     * @param showSummary true will show the summary, false will hide the summary
     */
    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
        if (mSummaryView != null) {
            mSummaryView.setVisibility(showSummary ? View.VISIBLE : View.GONE);
        } // otherwise, will be handled in onBindView.
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    public void refresh() {
        setTitle(mAccessPoint.getSsid());

        final Context context = getContext();
        updateIcon(mAccessPoint.getLevel(), context);

        // Force new summary
        setSummary(null);

        String summary = mAccessPoint.getSummary();
        if (summary.length() > 0) {
            setSummary(summary);
            setShowSummary(true);
        } else {
            setShowSummary(false);
        }
    }

    public void onLevelChanged() {
        notifyChanged();
    }

}