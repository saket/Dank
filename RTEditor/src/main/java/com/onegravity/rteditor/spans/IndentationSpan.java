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

package com.onegravity.rteditor.spans;

/**
 * Paragraph indentation.
 * <p>
 * Android seems to add the leading margin for an empty paragraph to the previous paragraph
 * (]0, 4][4, 4] --> the leading margin of the second span is added to the ]0, 4] paragraph regardless of the Spanned.flags)
 * --> therefore we ignore the leading margin for the last, empty paragraph unless it's the only one
 */
public class IndentationSpan extends android.text.style.LeadingMarginSpan.Standard implements RTSpan<Integer>, RTParagraphSpan<Integer> {

    private final int mIndentation;
    private final boolean mIgnoreSpan;

    public IndentationSpan(int indentation, boolean isEmpty, boolean isFirst, boolean isLast) {
        super(indentation);
        mIndentation = indentation;
        mIgnoreSpan = isEmpty && isLast && !isFirst;
    }

    private IndentationSpan(int indentation, boolean ignoreSpan) {
        super(indentation);
        mIndentation = indentation;
        mIgnoreSpan = ignoreSpan;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mIgnoreSpan ? 0 : mIndentation;
    }

    /**
     * While the getLeadingMargin(boolean) is "officially" used when rendering the span
     * this method returns the indentation regardless of whether we want to render it or not.
     * Internally we always use this method.
     */
    @Override
    public Integer getValue() {
        return mIndentation;
    }

    @Override
    public IndentationSpan createClone() {
        return new IndentationSpan(mIndentation, mIgnoreSpan);
    }

}
