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
import com.onegravity.rteditor.spans.RTSpan;
import com.onegravity.rteditor.utils.Paragraph;
import com.onegravity.rteditor.utils.Selection;

import java.util.List;

/**
 * ParagraphEffect are always applied to whole paragraphs, like bullet points or alignment.
 *
 * If we apply ParagraphEffects we need to call the cleanupParagraphs() afterwards!
 */
abstract class ParagraphEffect<V, C extends RTSpan<V>> extends Effect<V, C> {

    @Override
    final protected SpanCollector<V> newSpanCollector(Class<? extends RTSpan<V>> spanClazz) {
        return new ParagraphSpanCollector<V>(spanClazz);
    }

    /**
     * @return the start and end of the paragraph(s) encompassing the current selection because
     *         ParagraphEffects always operate on whole paragraphs.
     */
    @Override
    final protected Selection getSelection(RTEditText editor) {
        return editor.getParagraphsInSelection();
    }

    /**
     * Make sure applyToSelection works on whole paragraphs and call
     * Effects.cleanupParagraphs(RTEditText) afterwards.
     */
    @Override
    public final void applyToSelection(RTEditText editor, V value) {
        Selection selection = getSelection(editor);
        applyToSelection(editor, selection, value);
        Effects.cleanupParagraphs(editor, this);
    }

    /**
     * Apply this effect to the selection.
     *
     * Effects.cleanupParagraphs(RTEditText) calls this method using:
     * applyToSelection(RTEditText, null, null).
     * The result must be that the no effects are added or removed but every effect is applied to
     * a whole paragraph and opposing effects aren't applied to the same paragraph (bullet points
     * and numbering can't be applied to the same paragraph).
     *
     * @param editor The editor to apply the effect to (current selection)
     * @param selectedParagraphs Apply the effect to the selected paragraphs
     * @param value The value to apply (depends on the Effect)
     */
    public abstract void applyToSelection(RTEditText editor, Selection selectedParagraphs, V value);

    /**
     * Find spans within that paragraph and add them to the ParagraphSpanProcessor to be removed
     * once the ParagraphSpanProcessor processes its spans.
     */
    protected void findSpans2Remove(Spannable str, Paragraph paragraph,
                                    ParagraphSpanProcessor<V> spanProcessor) {
        List<RTSpan<V>> spans = getSpans(str, paragraph, SpanCollectMode.EXACT);
        spanProcessor.removeSpans(spans, paragraph);
    }

}
