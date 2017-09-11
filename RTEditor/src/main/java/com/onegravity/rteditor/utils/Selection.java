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

package com.onegravity.rteditor.utils;

import android.widget.EditText;

import java.io.Serializable;

/**
 * A Selection stands for selected text from a start to an end point. Don't mix
 * it up with android.text.Selection which has a completely different purpose.
 * <p>
 * It offers convenience methods to unify two Selections and the like.
 */
public class Selection implements Serializable {
    private static final long serialVersionUID = 8415527424030047664L;

    // all indices start with 0
    private int mStart;        // index of the first character
    private int mEnd;        // index of the first character after the selection

    public Selection(int start, int end) {
        mStart = start;
        mEnd = end;

        if (mStart > mEnd) {
            int temp = mEnd;
            mEnd = mStart;
            mStart = temp;
        }
    }

    public Selection(EditText editor) {
        this(editor.getSelectionStart(), editor.getSelectionEnd());
    }

    public int start() {
        return mStart;
    }

    public int end() {
        return mEnd;
    }

    public boolean isEmpty() {
        return mStart == mEnd;
    }

    public Selection offset(int offsetLeft, int offsetRight) {
        mStart = Math.max(0, mStart - offsetLeft);
        mEnd = mEnd + offsetRight;
        return this;
    }

    @Override
    public String toString() {
        return "[" + mStart + ", " + mEnd + "]";
    }

}
