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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.widget.Toast;

/**
 * The rich text editor needs to call certain methods e.g. to show a Toast or to
 * open the LinkFragment. This interface provides access to these methods.
 */
public interface RTProxy {

    /**
     * Launch an activity for which you would like a result when it finished.
     * When this activity exits, your onActivityResult() method will be called
     * with the given requestCode. Using a negative requestCode is the same as
     * calling startActivity(Intent) (the activity is not launched as a
     * sub-activity).
     *
     * @param intent      The intent to start.
     * @param requestCode If >= 0, this code will be returned in onActivityResult() when
     *                    the activity exits.
     */
    public void startActivityForResult(Intent intent, int requestCode);

    /**
     * Runs the specified action on the UI thread. If the current thread is the
     * UI thread, then the action is executed immediately. If the current thread
     * is not the UI thread, the action is posted to the event queue of the UI
     * thread.
     *
     * @param action the action to run on the UI thread
     */
    public void runOnUiThread(Runnable action);

    /**
     * Make a standard toast that just contains a text view with the text from a
     * resource.
     *
     * @param resId    The resource id of the string resource to use. Can be
     *                 formatted text.
     * @param duration How long to display the message. Either Toast.LENGTH_SHORT or
     *                 Toast.LENGTH_LONG
     */
    public Toast makeText(int resId, int duration);

    /**
     * Make a standard toast that just contains a text view.
     *
     * @param text     The text to show. Can be formatted text.
     * @param duration How long to display the message. Either Toast.LENGTH_SHORT or
     *                 Toast.LENGTH_LONG
     */
    public Toast makeText(CharSequence text, int duration);

    /**
     * Display a DialogFragment, adding the fragment using an existing
     * transaction and then committing the transaction.
     *
     * @param tag      The tag for this fragment, as per
     *                 {@link FragmentTransaction#add(Fragment, String)
     *                 FragmentTransaction.add}.
     * @param fragment The DialogFragment to show.
     */
    public void openDialogFragment(String fragmentTag, DialogFragment fragment);

    /**
     * Remove an existing fragment. If it was added to a container, its view is
     * also removed from that container.
     *
     * @param fragmentTag The tag of the fragment to remove.
     */
    public void removeFragment(String fragmentTag);

}