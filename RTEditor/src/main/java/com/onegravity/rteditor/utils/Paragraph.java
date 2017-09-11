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

/**
 * A Paragraph is a Selection but represents a whole text paragraph.
 */
public class Paragraph extends Selection {
    private static final long serialVersionUID = 2475227150049924994L;

    final private boolean mIsFirst;
    final private boolean mIsLast;

    public Paragraph(int start, int end, boolean isFirst, boolean isLast) {
        super(start, end);
        mIsFirst = isFirst;
        mIsLast = isLast;
    }

    /**
     * @return True if this is the first paragraph in the editor, False otherwise
     */
    public boolean isFirst() {
        return mIsFirst;
    }

    /**
     * @return True if this is the last paragraph in the editor, False otherwise
     */
    public boolean isLast() {
        return mIsLast;
    }

    /**
     * The paragraph is selected by the Selection if:
     * <p>
     * - they have at least one character in common
     * - the selection is a point within the paragraph ("01\n" -> 0 till 2 intersects while the span is [0, 3])
     * - the selection is a point within or at the end of the LAST paragraph ("01" -> 0 till 2 intersects while the span is [0, 2]),
     * e.g. [10, 10] will intersect the paragraph [0, 10] only if it's the last paragraph
     */
    public boolean isSelected(Selection sel) {
        if (sel == null) {
            return false;
        }
        if (sel.isEmpty()) {
            // selection is a point
            boolean isCompletelyWithin = sel.start() >= start() && sel.end() < end();  // selection is completely within paragraph (not at the end)
            boolean isWithin = sel.start() >= start() && sel.end() <= end();           // selection is within or at the end of the paragraph
            return isCompletelyWithin || (isWithin && mIsLast);
        } else {
            // selection is a range --> at least one character in common
            int start = Math.max(start(), sel.start());
            int end = Math.min(end(), sel.end());
            return start < end;
        }
    }

}