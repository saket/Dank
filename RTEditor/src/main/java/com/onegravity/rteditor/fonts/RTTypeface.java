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

package com.onegravity.rteditor.fonts;

import android.graphics.Typeface;

import java.util.Locale;

/**
 * This is a container class for a android.graphics.Typeface and the font name.
 */
public class RTTypeface implements Comparable<RTTypeface> {

    protected String mName;
    private Typeface mTypeface;

    public RTTypeface(String fontName, Typeface typeface) {
        if (fontName == null) {
            throw new IllegalArgumentException("fontName mustn't be null");
        }
        if (typeface == null) {
            throw new IllegalArgumentException("typeface mustn't be null");
        }
        mName = fontName;
        mTypeface = typeface;
    }

    /**
     * Use these methods only in the RTTypefaceSet.
     */
    protected RTTypeface() {}
    protected void setName(String fontName) {
        mName = fontName;
    }

    public String getName() {
        return mName;
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    @Override
    public boolean equals(Object object) {
        if (! (object instanceof RTTypeface)) {
            return false;
        }
        String name = ((RTTypeface)object).getName();
        return mName.equalsIgnoreCase(name);
    }

    @Override
    public int compareTo(RTTypeface another) {
        Locale locale = Locale.getDefault();
        String name1 = mName.toLowerCase(locale);
        String name2 = another.getName().toLowerCase(locale);
        return name1.compareTo(name2);
    }
}
