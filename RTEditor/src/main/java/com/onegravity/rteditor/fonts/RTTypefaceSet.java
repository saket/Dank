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

import java.util.TreeSet;

/**
 * This is a SortedSet for RTTypeface objects with some convenience methods.
 */
public class RTTypefaceSet extends TreeSet<RTTypeface> {
    private RTTypeface tmp = new RTTypeface();

    /**
     * @return The RTTypeface with the specified name or false if no such RTTypeface exists.
     */
    RTTypeface get(String fontName) {
        if (contains(fontName)) {
            for (RTTypeface typeface : this) {
                if (typeface.getName().equals(fontName)) {
                    return typeface;
                }
            }
        }
        return null;
    }

    /**
     * @return True if the collections contains an RTTypeface with the specified name,
     *         false otherwise.
     */
    boolean contains(String fontName) {
        tmp.setName(fontName);
        return contains(tmp);
    }

}
