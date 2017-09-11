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

package com.onegravity.rteditor.api;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.util.AndroidRuntimeException;
import android.widget.Toast;

import com.onegravity.rteditor.R;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTMediaSource;
import com.onegravity.rteditor.api.media.RTVideo;

/**
 * The RTApi is a convenience class that combines the RTProxy and the
 * RTMediaFactory implementations in one class and acts as a proxy to them. It
 * also provides the Application context as a singleton to all rich text editor
 * components that need access to a (non-Activity) context.
 * <p>
 * Usually there's no need to extends this class because all the real logic is
 * in the RTProxy and RTMediaFactory implementations.
 * <p>
 * Important: the RTProxy class isn't Serializable. If RTApi is serialized (e.g.
 * as extra in an Intent) the receiver of that Intent will be able to use the
 * RTMediaFactory but not the RTProxy part.
 */
public class RTApi implements RTProxy, RTMediaFactory<RTImage, RTAudio, RTVideo> {

    private static final long serialVersionUID = -3877685955074371741L;

    /*
     * Application Context Part
     */

    private static final class IncorrectInitializationException extends AndroidRuntimeException {
        private static final long serialVersionUID = 327389536289485672L;

        IncorrectInitializationException(String msg) {
            super(msg);
        }
    }


    /*
     * The application context used throughout the different rich text editor
     * components.
     */
    private static Object sTheLock = new Object();    // synchronize access to sAppContext
    private static Context sAppContext;
    private static boolean sDarkTheme;

    /**
     * Return the context of the single, global Application object for the
     * current process. This generally should only be used if you need a Context
     * whose lifecycle is separate from the current context, that is tied to the
     * lifetime of the process rather than the current component
     * (Activity/Fragment).
     */
    public static Context getApplicationContext() {
        synchronized (sTheLock) {
            if (sAppContext == null) {
                throw new IncorrectInitializationException(
                        "Create an RTApi object before calling RTApi.getApplicationContext()");
            }
            return sAppContext;
        }
    }

    /**
     * Since we can't use the application context to retrieve the current theme,
     * we retrieve the theme from the Activity context when the object is initialized.
     */
    public static boolean useDarkTheme() {
        return sDarkTheme;
    }

    /*
     * Constructor
     */

    transient final private RTProxy mRTProxy;    // not Serializable
    final private RTMediaFactory<RTImage, RTAudio, RTVideo> mMediaFactory;

    /**
     * @param context      Can be an Application or an Activity context
     * @param rtProxy      The RTProxy provided by the app.
     * @param mediaFactory the RTMediaFactory provided by the app.
     */
    public RTApi(Context context, RTProxy rtProxy, RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory) {
        synchronized (sTheLock) {
            sAppContext = context.getApplicationContext();
        }
        sDarkTheme = resolveBoolean(context, R.attr.rte_darkTheme, false);

        mRTProxy = rtProxy;
        mMediaFactory = mediaFactory;
    }

    private boolean resolveBoolean(Context context, @AttrRes int attr, boolean fallback) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getBoolean(0, fallback);
        } finally {
            a.recycle();
        }
    }

    /*
     * RTProxy Methods
     */

    /**
     * The RTProxy is the link to the "outside world".
     * Since the class is part of the app and not the rich text editor component
     * we allow access through this method as a convenience.
     */
    public RTProxy getRTProxy() {
        return mRTProxy;
    }

    @Override
    /* @inheritDoc */
    public void startActivityForResult(Intent intent, int requestCode) {
        mRTProxy.startActivityForResult(intent, requestCode);
    }

    @Override
    /* @inheritDoc */
    public void runOnUiThread(Runnable action) {
        mRTProxy.runOnUiThread(action);
    }

    @Override
    /* @inheritDoc */
    public Toast makeText(int resId, int duration) {
        return mRTProxy.makeText(resId, duration);
    }

    @Override
    /* @inheritDoc */
    public Toast makeText(CharSequence text, int duration) {
        return mRTProxy.makeText(text, duration);
    }

    @Override
    /* @inheritDoc */
    public void openDialogFragment(String fragmentTag, DialogFragment fragment) {
        mRTProxy.openDialogFragment(fragmentTag, fragment);
    }

    @Override
    /* @inheritDoc */
    public void removeFragment(String fragmentTag) {
        mRTProxy.removeFragment(fragmentTag);
    }

    /*
     * RTMediaFactory Methods
     */

    /**
     * The media factory allows custom storage implementations for media files.
     * Since the class is part of the app and not the rich text editor component
     * we allow access through this method as a convenience.
     */
    public RTMediaFactory<RTImage, RTAudio, RTVideo> getMediaFactory() {
        return mMediaFactory;
    }

    @Override
    /* @inheritDoc */
    public RTImage createImage(RTMediaSource mediaSource) {
        return mMediaFactory.createImage(mediaSource);
    }

    @Override
    /* @inheritDoc */
    public RTAudio createAudio(RTMediaSource mediaSource) {
        return mMediaFactory.createAudio(mediaSource);
    }

    @Override
    /* @inheritDoc */
    public RTVideo createVideo(RTMediaSource mediaSource) {
        return mMediaFactory.createVideo(mediaSource);
    }

    @Override
    /* @inheritDoc */
    public RTImage createImage(String path) {
        return mMediaFactory.createImage(path);
    }

    @Override
    /* @inheritDoc */
    public RTAudio createAudio(String path) {
        return mMediaFactory.createAudio(path);
    }

    @Override
    /* @inheritDoc */
    public RTVideo createVideo(String path) {
        return mMediaFactory.createVideo(path);
    }

}
