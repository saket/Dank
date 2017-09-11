/*
 * Copyright (C) 2015-2017 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onegravity.rteditor.media;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.onegravity.rteditor.R;
import com.onegravity.rteditor.api.RTApi;

import java.util.ArrayList;

public class MonitoredActivity extends AppCompatActivity {

    protected Handler mHandler;

    private final ArrayList<LifeCycleListener> mListeners = new ArrayList<LifeCycleListener>();

    // ****************************************** MonitoredActivity Methods *******************************************

    public static interface LifeCycleListener {
        public void onActivityCreated(Activity activity);

        public void onActivityDestroyed(Activity activity);

        public void onActivityPaused(Activity activity);

        public void onActivityResumed(Activity activity);

        public void onActivityStarted(Activity activity);

        public void onActivityStopped(Activity activity);
    }

    public static class LifeCycleAdapter implements LifeCycleListener {
        public void onActivityCreated(Activity activity) {
        }

        public void onActivityDestroyed(Activity activity) {
        }

        public void onActivityPaused(Activity activity) {
        }

        public void onActivityResumed(Activity activity) {
        }

        public void onActivityStarted(Activity activity) {
        }

        public void onActivityStopped(Activity activity) {
        }
    }

    public void addLifeCycleListener(LifeCycleListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeLifeCycleListener(LifeCycleListener listener) {
        mListeners.remove(listener);
    }

    // ****************************************** Lifecycle Methods *******************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set theme
        setTheme(RTApi.useDarkTheme() ? R.style.RTE_BaseThemeDark : R.style.RTE_BaseThemeLight);

        if (!isFinishing()) {
            mHandler = new Handler();

            for (LifeCycleListener listener : mListeners) {
                listener.onActivityCreated(this);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        for (LifeCycleListener listener : mListeners) {
            listener.onActivityStarted(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        for (LifeCycleListener listener : mListeners) {
            listener.onActivityPaused(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        for (LifeCycleListener listener : mListeners) {
            listener.onActivityResumed(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        for (LifeCycleListener listener : mListeners) {
            listener.onActivityStopped(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (LifeCycleListener listener : mListeners) {
            listener.onActivityDestroyed(this);
        }
    }

    // ****************************************** Foreground Jobs (Modal) *******************************************

    /**
     * Start a foreground job showing a progress bar as long as the job runs.
     * A foreground job shows a modal dialog that can't be cancelled while the app
     * does some processing. This should be used for short jobs only that block the ui.
     * Of course this contradicts many of the Android guidelines but with short jobs
     * we mean < 1 second and only in rare cases should it me more (e.g. when the garbage
     * collector kicks in which would normally lead to stuttering).
     * Using foreground jobs gives a better user experience in these rare cases.
     * The user has to wait but he/she knows it because there's a progess bar.
     */
    public <T> T startForegroundJob(int msgId, ForegroundJob<T> job) {
        // make the progress dialog uncancelable, so that we can guarantee
        // that the thread is done before the activity gets destroyed
        ProgressDialog dialog = ProgressDialog.show(this, null, getString(msgId), true, false);
        Job<T> managedJob = new Job<T>(job, dialog);
        return managedJob.runForegroundJob();
    }

    /**
     * Start a background job showing a progress bar as long as the job runs.
     * This seems contradictory but with background job we mean one that runs off
     * the ui thread to prevent an ANR.
     * We still have to wait for the processing to be done because we need the result.
     */
    public void startBackgroundJob(int msgId, Runnable runnable) {
        // make the progress dialog uncancelable, so that we can guarantee
        // that the thread is done before the activity gets destroyed
        ProgressDialog dialog = ProgressDialog.show(this, null, getString(msgId), true, false);
        Job<Object> managedJob = new Job<Object>(runnable, dialog);
        managedJob.runBackgroundJob();
    }

    public interface ForegroundJob<T> {
        public T runForegroundJob();
    }

    private class Job<T> extends LifeCycleAdapter implements ForegroundJob<T> {

        private final ProgressDialog mDialog;
        private final Runnable mRunnable;
        private final ForegroundJob<T> mJob;

        private final Runnable mCleanupRunner = new Runnable() {
            public void run() {
                removeLifeCycleListener(Job.this);
                if (mDialog.getWindow() != null) {
                    mDialog.dismiss();
                }
            }
        };

        public Job(ForegroundJob<T> job, ProgressDialog dialog) {
            mDialog = dialog;
            mJob = job;
            mRunnable = null;
            addLifeCycleListener(this);
        }

        public Job(Runnable runnable, ProgressDialog dialog) {
            mDialog = dialog;
            mJob = null;
            mRunnable = runnable;
            addLifeCycleListener(this);
        }

        @Override
        public T runForegroundJob() {
            try {
                return mJob.runForegroundJob();
            } finally {
                mHandler.post(mCleanupRunner);
            }
        }

        public void runBackgroundJob() {
            try {
                mRunnable.run();
            } finally {
                mHandler.post(mCleanupRunner);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            // We get here only when the onDestroyed being called before
            // the mCleanupRunner. So, run it now and remove it from the queue
            mCleanupRunner.run();
            mHandler.removeCallbacks(mCleanupRunner);
        }

        @Override
        public void onActivityStopped(Activity activity) {
            mDialog.hide();
        }

        @Override
        public void onActivityStarted(Activity activity) {
            mDialog.show();
        }
    }

}