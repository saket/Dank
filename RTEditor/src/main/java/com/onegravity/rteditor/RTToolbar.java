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

package com.onegravity.rteditor;

import android.text.Layout;
import android.view.ViewGroup;

import com.onegravity.rteditor.fonts.RTTypeface;

/**
 * An interface describing a rich text toolbar.
 * <p>
 * There are methods to set/clear effects (like bold on/off) and
 * there are callback methods to let the listener know if the user
 * selected some effect on the toolbar.
 */
public interface RTToolbar {

    /**
     * Set this toolbar's listener.
     * There can be only one since this should be the RTManager and there's only
     * one of those (per layout).
     */
    public void setToolbarListener(RTToolbarListener listener);

    /**
     * Remove this toolbar's listener.
     */
    public void removeToolbarListener();

    /**
     * We can have more than one toolbar identified by this unique Id
     * (unique per layout).
     * It can be implemented e.g. by using a static counter.
     */
    public int getId();

    /**
     * This is merely a way to store the container in which the toolbar is shown
     * since we might need to hide/show the toolbar which happens by
     * hiding/showing the container.
     *
     * @param toolbarContainer The ViewGroup that contains this RTToolbar.
     */
    public void setToolbarContainer(ViewGroup toolbarContainer);

    /**
     * @return The ViewGroup that contains this RTToolbar.
     */
    public ViewGroup getToolbarContainer();

    public void setBold(boolean enabled);

    public void setItalic(boolean enabled);

    public void setUnderline(boolean enabled);

    public void setStrikethrough(boolean enabled);

    public void setSuperscript(boolean enabled);

    public void setSubscript(boolean enabled);

    public void setBullet(boolean enabled);

    public void setNumber(boolean enabled);

    public void setAlignment(Layout.Alignment alignment);

    public void setFont(RTTypeface typeface);

    /**
     * Set the text size.
     *
     * @param size the text size, if -1 then no text size is set (e.g. when selection spans more than one text size)
     */
    public void setFontSize(int size);

    public void setFontColor(int color);

    public void setBGColor(int color);

    public void removeFontColor();

    public void removeBGColor();

}