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

import com.onegravity.rteditor.RTEditText;
import com.onegravity.rteditor.spans.IndentationSpan;
import com.onegravity.rteditor.spans.RTSpan;
import com.onegravity.rteditor.utils.Paragraph;
import com.onegravity.rteditor.utils.Selection;

import java.util.ArrayList;
import java.util.List;

/**
 * Text indentation.
 * <p>
 * IndentationSpan are always applied to whole paragraphs and each paragraphs gets its "own" IndentationSpan (1:1).
 * Editing might violate this rule (deleting a line feed merges two paragraphs).
 * Each call to applyToSelection will make sure that each paragraph has again its own IndentationSpan
 * (call applyToSelection(RTEditText, null, null) and all will be good again).
 * <p>
 */
public class IndentationEffect extends ParagraphEffect<Integer, IndentationSpan> {

    private ParagraphSpanProcessor<Integer> mSpans2Process = new ParagraphSpanProcessor();

    public void applyToSelection(RTEditText editor, Selection selectedParagraphs, Integer increment) {
        final Spannable str = editor.getText();

        mSpans2Process.clear();

        // a manual for loop is faster than the for-each loop for an ArrayList:
        // see https://developer.android.com/training/articles/perf-tips.html#Loops
        ArrayList<Paragraph> paragraphs = editor.getParagraphs();
        for (int i = 0, size = paragraphs.size(); i < size; i++) {
            Paragraph paragraph = paragraphs.get(i);

            // find existing IndentationSpan and add them to mSpans2Process to be removed
            List<RTSpan<Integer>> existingSpans = getSpans(str, paragraph, SpanCollectMode.EXACT);
            mSpans2Process.removeSpans(existingSpans, paragraph);

            // compute the indentation
            int indentation = 0;
            for (RTSpan<Integer> span : existingSpans) {
                indentation += span.getValue();
                // Only consider the first span since the span flags (SPAN_EXCLUSIVE_INCLUSIVE)
                // can lead to a paragraph having two IndentationSpans after hitting enter/return.
                break;
            }

            // if the paragraph is selected inc/dec the existing indentation
            int incIndentation = increment == null ? 0 : increment;
            indentation += paragraph.isSelected(selectedParagraphs) ? incIndentation : 0;

            // if we have an indentation then apply a new span
            if (indentation > 0) {
                IndentationSpan leadingMarginSpan = new IndentationSpan(indentation, paragraph.isEmpty(), paragraph.isFirst(), paragraph.isLast());
                mSpans2Process.addSpan(leadingMarginSpan, paragraph);
            }
        }

        // add or remove spans
        mSpans2Process.process(str);
    }

}
