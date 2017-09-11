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

package com.onegravity.rteditor.effects;

import android.text.Spannable;
import android.text.Spanned;

import com.onegravity.rteditor.spans.RTParagraphSpan;
import com.onegravity.rteditor.spans.RTSpan;
import com.onegravity.rteditor.utils.Paragraph;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a container for paragraph spans and their meta information that will be processed later
 * (added to or removed from a Spannable).
 */
class ParagraphSpanProcessor<V extends Object> {

    private static class ParagraphSpan<V extends Object> {
        final RTParagraphSpan<V> mSpan;
        final Paragraph mParagraph;
        final boolean mRemove;

        ParagraphSpan(RTParagraphSpan<V> span, Paragraph paragraph, boolean remove) {
            mSpan = span;
            mParagraph = paragraph;
            mRemove = remove;
        }
    }

    final private ArrayList<ParagraphSpan<V>> mParagraphSpans = new ArrayList<ParagraphSpan<V>>();

    void clear() {
        mParagraphSpans.clear();
    }

    void removeSpans(List<RTSpan<V>> spans, Paragraph paragraph) {
        for (RTSpan<V> span : spans) {
            removeSpan(span, paragraph);
        }
    }

    void removeSpan(RTSpan<V> span, Paragraph paragraph) {
        if (span instanceof RTParagraphSpan) {
            mParagraphSpans.add(new ParagraphSpan<V>((RTParagraphSpan<V>) span, paragraph, true));
        }
    }

    void addSpan(RTParagraphSpan<V> span, Paragraph paragraph) {
        mParagraphSpans.add( new ParagraphSpan<V>(span, paragraph, false) );
    }

    void process(Spannable str) {
        for (ParagraphSpan paragraphSpan : mParagraphSpans) {
            RTParagraphSpan<V> span = paragraphSpan.mSpan;
            int paraStart = paragraphSpan.mParagraph.start();

            if (paragraphSpan.mRemove) {
                int spanStart = str.getSpanStart(span);
                if (spanStart > -1  && spanStart < paraStart) {
                    // process preceding spans
                    str.setSpan(span.createClone(), spanStart, paraStart, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                }

                str.removeSpan(span);
            }

            else {
                Paragraph paragraph = paragraphSpan.mParagraph;
                int paraEnd = paragraphSpan.mParagraph.end();
                int flags = paragraph.isLast() && paragraph.isEmpty() ? Spanned.SPAN_INCLUSIVE_INCLUSIVE :
                            paragraph.isLast() && paragraph.isFirst() ? Spanned.SPAN_INCLUSIVE_INCLUSIVE :
                            paragraph.isLast() ? Spanned.SPAN_EXCLUSIVE_INCLUSIVE :
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
                str.setSpan(span, paraStart, paraEnd, flags);
            }
        }
    }

}
