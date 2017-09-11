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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.util.AndroidRuntimeException;
import android.widget.Toast;

import java.lang.ref.SoftReference;

/**
 * A standard implementation for the RTProxy interface.
 * <p>
 * It's using a SoftReference for the Activity object to reduce the risk of
 * memory leaks. You're welcome to extend the class and provide your own
 * implementation.
 */
public class RTProxyImpl implements RTProxy {

    final private SoftReference<Activity> mActivity;

    public RTProxyImpl(Activity activity) {
        mActivity = new SoftReference<Activity>(activity);
    }

    @Override
    /* @inheritDoc */
    public void startActivityForResult(Intent intent, int requestCode) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    /* @inheritDoc */
    public void runOnUiThread(Runnable action) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(action);
        }
    }

    @Override
    /* @inheritDoc */
    public Toast makeText(int resId, int duration) {
        return Toast.makeText(RTApi.getApplicationContext(), resId, duration);
    }

    @Override
    /* @inheritDoc */
    public Toast makeText(CharSequence text, int duration) {
        return Toast.makeText(RTApi.getApplicationContext(), text, duration);
    }

    @Override
    /* @inheritDoc */
    public void openDialogFragment(String fragmentTag, DialogFragment fragment) {
        Activity activity = getActivity();
        if (activity != null) {
            FragmentManager fragmentMgr = activity.getFragmentManager();
            FragmentTransaction ft = fragmentMgr.beginTransaction();
            DialogFragment oldFragment = (DialogFragment) fragmentMgr
                    .findFragmentByTag(fragmentTag);
            if (oldFragment == null) {
                fragment.show(ft, fragmentTag);
            }
        }
    }

    @Override
    /* @inheritDoc */
    public void removeFragment(String fragmentTag) {
        Activity activity = getActivity();
        if (activity != null) {
            FragmentManager fragmentMgr = activity.getFragmentManager();
            Fragment fragment = fragmentMgr.findFragmentByTag(fragmentTag);
            fragmentMgr.beginTransaction().remove(fragment).commit();
        }
    }

    private static class IncorrectInitializationException extends
            AndroidRuntimeException {
        private static final long serialVersionUID = 327389536289485672L;

        public IncorrectInitializationException(String msg) {
            super(msg);
        }
    }

    private Activity getActivity() {
        if (mActivity == null && mActivity.get() == null) {
            throw new IncorrectInitializationException(
                    "The RTApi was't initialized correctly or the Activity was released by Android (SoftReference)");
        }
        return mActivity.get();
    }

}