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
import android.util.SparseIntArray;

import com.onegravity.rteditor.RTEditText;
import com.onegravity.rteditor.spans.NumberSpan;
import com.onegravity.rteditor.spans.RTSpan;
import com.onegravity.rteditor.utils.Helper;
import com.onegravity.rteditor.utils.Paragraph;
import com.onegravity.rteditor.utils.Selection;

import java.util.ArrayList;
import java.util.List;

/**
 * Numbering.
 * <p>
 * NumberSpans are always applied to whole paragraphs and each paragraphs gets its "own" NumberSpan (1:1).
 * Editing might violate this rule (deleting a line feed merges two paragraphs).
 * Each call to applyToSelection will make sure that each paragraph has again its own NumberSpan
 * (call applyToSelection(RTEditText, null, null) and all will be good again).
 */
public class NumberEffect extends ParagraphEffect<Boolean, NumberSpan> {

    private ParagraphSpanProcessor<Boolean> mSpans2Process = new ParagraphSpanProcessor();

    @Override
    public synchronized void applyToSelection(RTEditText editor, Selection selectedParagraphs, Boolean enable) {
        final Spannable str = editor.getText();

        mSpans2Process.clear();

        int lineNr = 1;
        SparseIntArray indentations = new SparseIntArray();
        SparseIntArray numbers = new SparseIntArray();

        // a manual for loop is faster than the for-each loop for an ArrayList:
        // see https://developer.android.com/training/articles/perf-tips.html#Loops
        ArrayList<Paragraph> paragraphs = editor.getParagraphs();
        for (int i = 0, size = paragraphs.size(); i < size; i++) {
            Paragraph paragraph = paragraphs.get(i);

            /*
             * We need to know the indentation for each paragraph to be able
             * to determine which paragraphs belong together (same indentation)
             */
            int currentIndentation = 0;
            List<RTSpan<Integer>> indentationSpans = Effects.INDENTATION.getSpans(str, paragraph, SpanCollectMode.EXACT);
            if (! indentationSpans.isEmpty()) {
                for (RTSpan<Integer> span : indentationSpans) {
                    currentIndentation += span.getValue();
                }
            }
            indentations.put(lineNr, currentIndentation);

            // find existing NumberSpans and add them to mSpans2Process to be removed
            List<RTSpan<Boolean>> existingSpans = getSpans(str, paragraph, SpanCollectMode.SPAN_FLAGS);
            mSpans2Process.removeSpans(existingSpans, paragraph);

            /*
             * If the paragraph is selected then we sure have a number
             */
            boolean hasExistingSpans = ! existingSpans.isEmpty();
            boolean hasNumber = paragraph.isSelected(selectedParagraphs) ? enable : hasExistingSpans;

            /*
             * If we have a number then apply a new span
             */
            if (hasNumber) {
                // let's determine the number for this paragraph
                int nr = 1;
                for (int line = 1; line < lineNr; line++) {
                    int indentation = indentations.get(line);
                    int number = numbers.get(line);
                    if (indentation < currentIndentation) {
                        // 1) less indentation -> number 1
                        nr = 1;
                    } else if (indentation == currentIndentation) {
                        // 2) same indentation + no numbering -> number 1
                        // 3) same indentation + numbering -> increment number
                        nr = number == 0 ? 1 : number + 1;
                    }
                }
                numbers.put(lineNr, nr);

                int margin = Helper.getLeadingMarging();
                NumberSpan numberSpan = new NumberSpan(nr++, margin, paragraph.isEmpty(), paragraph.isFirst(), paragraph.isLast());
                mSpans2Process.addSpan(numberSpan, paragraph);

                // if the paragraph has bullet spans, then remove them
                Effects.BULLET.findSpans2Remove(str, paragraph, mSpans2Process);
            }

            lineNr++;
        }

        // add or remove spans
        mSpans2Process.process(str);
    }

}
