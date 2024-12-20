/*
 * Copyright (C) 2017 Jeff Gilfelt.
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
package com.readystatesoftware.chuck;

import android.content.Context;
import android.content.Intent;

import com.readystatesoftware.chuck.internal.ui.ChuckMainActivity;
import com.readystatesoftware.chuck.monitor.MonitorHelper;

/**
 * Chuck utilities.
 */
public class Chuck {
    static String mChuckNotification=null;
    /**
     * Get an Intent to launch the Chuck UI directly.
     *
     * @param context A Context.
     * @return An Intent for the main Chuck Activity that can be started with {@link Context#startActivity(Intent)}.
     */
    public static Intent getLaunchIntent(Context context) {
        return new Intent(context, ChuckMainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static String getWifiIpPort(){
        if(MonitorHelper.getPhoneWifiIpAddress() != null){
           return MonitorHelper.getPhoneWifiIpAddress() + ":" + MonitorHelper.port +"/index";
        }
        return "";
    }

    public static void setShowNotification(boolean showNotification){
        mChuckNotification = showNotification ? "1" : null;
    }
}