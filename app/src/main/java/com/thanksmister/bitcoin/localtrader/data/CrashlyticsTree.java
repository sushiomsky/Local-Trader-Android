/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thanksmister.bitcoin.localtrader.data;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.thanksmister.bitcoin.localtrader.BuildConfig;

import timber.log.Timber;

/**
 * A logging implementation which reports 'info', 'warning', and 'error' logs to Crashlytics.
 */
public class CrashlyticsTree extends Timber.Tree {
    public CrashlyticsTree() {
    }

    @Override
    public void d(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            Log.println(Log.DEBUG, "LocalTrader", message);
        }
        //logMessage(Log.DEBUG, message, args);
    }

    @Override
    public void i(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.INFO, message, args);
        }
    }

    @Override
    public void i(Throwable t, String message, Object... args) {
        //logMessage(Log.INFO, message, args);
        if (BuildConfig.DEBUG) {
            logMessage(Log.INFO, message, args);
        }
        // NOTE: We are explicitly not sending the exception to Crashlytics here.
    }

    @Override
    public void w(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.WARN, message, args);
        }
    }

    @Override
    public void w(Throwable t, String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.WARN, message, args);
        }
        // NOTE: We are explicitly not sending the exception to Crashlytics here.
    }

    @Override
    public void e(String message, Object... args) {
        if (BuildConfig.DEBUG) {
            logMessage(Log.ERROR, message, args);
        }
    }

    @Override
    public void e(Throwable t, String message, Object... args) {
        logMessage(Log.ERROR, message, args);
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority == Log.ERROR) {
            try {
                if (tag != null && tag.length() > 0) {
                    Crashlytics.log(priority, tag, String.format(message, tag));
                } else {
                    Crashlytics.log(priority, tag, message);
                }

                //Crashlytics.logException(t);

            } catch (Exception e) {
                //Timber.e(e.getMessage());
            }
        }
    }

    private void logMessage(int priority, String message, Object... args) {
        try {
            if (args.length > 0) {

                //Crashlytics.log(priority, "LocalTrader", String.format(message, args));

                if (priority == Log.ERROR)
                    Crashlytics.logException(new Throwable(String.format(message, args)));

            } else {
                //Crashlytics.log(priority, "LocalTrader", message);

                if (priority == Log.ERROR)
                    Crashlytics.logException(new Throwable(message));
            }

        } catch (Exception e) {
            //Timber.e(e.getMessage());
        }
    }
}
