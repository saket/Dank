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

import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;

import com.onegravity.rteditor.RTEditText;
import com.onegravity.rteditor.spans.AlignmentSpan;
import com.onegravity.rteditor.spans.RTSpan;
import com.onegravity.rteditor.utils.Helper;
import com.onegravity.rteditor.utils.Paragraph;
import com.onegravity.rteditor.utils.Selection;

import java.util.ArrayList;
import java.util.List;

/**
 * Left, Center, Right alignment.
 * <p>
 * AlignmentSpans are always applied to whole paragraphs and each paragraphs gets its "own" AlignmentSpan (1:1).
 * Editing might violate this rule (deleting a line feed merges two paragraphs).
 * Each call to applyToSelection will again make sure that each paragraph has again its own AlignmentSpan
 * (call applyToSelection(RTEditText, null, null) and all will be good again).
 */
public class AlignmentEffect extends ParagraphEffect<Layout.Alignment, AlignmentSpan> {

    private ParagraphSpanProcessor<Layout.Alignment> mSpans2Process = new ParagraphSpanProcessor();

    @Override
    public void applyToSelection(RTEditText editor, Selection selectedParagraphs, Layout.Alignment alignment) {
        final Spannable str = editor.getText();

        mSpans2Process.clear();

        // a manual for loop is faster than the for-each loop for an ArrayList:
        // see https://developer.android.com/training/articles/perf-tips.html#Loops
        ArrayList<Paragraph> paragraphs = editor.getParagraphs();
        for (int i = 0, size = paragraphs.size(); i < size; i++) {
            Paragraph paragraph = paragraphs.get(i);

            // find existing AlignmentSpan and add them to mSpans2Process to be removed
            List<RTSpan<Layout.Alignment>> existingSpans = getSpans(str, paragraph, SpanCollectMode.SPAN_FLAGS);
            mSpans2Process.removeSpans(existingSpans, paragraph);

            // if the paragraph is selected then we sure have an alignment
            boolean hasExistingSpans = !existingSpans.isEmpty();
            Alignment newAlignment = paragraph.isSelected(selectedParagraphs) ? alignment :
                                     hasExistingSpans ? existingSpans.get(0).getValue() : null;

            if (newAlignment != null) {
                boolean isRTL = Helper.isRTL(str, paragraph.start(), paragraph.end());
                AlignmentSpan alignmentSpan = new AlignmentSpan(newAlignment, isRTL);
                mSpans2Process.addSpan(alignmentSpan, paragraph);
            }
        }

        // add or remove spans
        mSpans2Process.process(str);
    }

}
