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
import com.onegravity.rteditor.utils.Selection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all effects.
 * An "effect" is a particular type of styling to apply to the selected text in a rich text editor.
 * Most of them are wrappers around the corresponding CharacterStyle (Bold, Italic, font size etc.)
 * or ParagraphStyle classes (e.g. BulletSpan).
 *
 * @param <V> is the sort of configuration information that the effect needs.
 *           Many will be Effect<Boolean>, meaning the effect is a toggle (on or off), such as bold.
 *
 * @param <C> is the RTSpan<V> used by the Effect (e.g. BoldEffect uses BoldSpan)
 */
abstract public class Effect<V, C extends RTSpan<V>> {

    private SpanCollector<V> mSpanCollector;

    /**
     * Check whether the effect exists in the currently selected text of the active RTEditText.
     *
     * @param editor The RTEditText we want the check.
     *
     * @return True if the effect exists in the current selection, False otherwise.
     */
    final public boolean existsInSelection(RTEditText editor) {
        Selection selection = getSelection(editor);
        List<RTSpan<V>> spans = getSpans(editor.getText(), selection, SpanCollectMode.SPAN_FLAGS);

        return ! spans.isEmpty();
    }

    /**
     * Returns the value(s) of this effect in the currently selected text of the active RTEditText.
     *
     * @return The returned list, must NEVER be null.
     */
    final public List<V> valuesInSelection(RTEditText editor) {
        List<V> result = new ArrayList<V>();

        Selection selection = getSelection(editor);
        List<RTSpan<V>> spans = getSpans(editor.getText(), selection, SpanCollectMode.SPAN_FLAGS);
        for (RTSpan<V> span : spans) {
            result.add( span.getValue() );
        }

        return result;
    }

    /**
     * Remove all effects of this type from the currently selected text of the active RTEditText.
     * If the selection is empty (cursor), formatting for the whole text is removed.
     */
    final public void clearFormattingInSelection(RTEditText editor) {
        Spannable text = editor.getText();

        // if no selection --> select the whole text
        // otherwise use the getSelection method (implented by sub classes)
        Selection selection = new Selection(editor);
        selection = selection.isEmpty() ? new Selection(0, text.length()) : getSelection(editor);

        List<RTSpan<V>> spans = getSpans(text, selection, SpanCollectMode.EXACT);
        for (Object span : spans) {
            editor.getText().removeSpan(span);
        }
    }

    /**
     * Equivalent to the Spanned.getSpans(int, int, Class<T>) method.
     * Return the markup objects (spans) attached to the specified slice of a Spannable.
     * The type of the spans is defined in the SpanCollector.
     *
     * @param str The Spannable to search for spans.
     * @param selection The selection within the Spannable to search for spans.
     * @param mode details see SpanCollectMode.
     *
     * @return the list of spans in this Spannable/Selection, never Null
     */
    final public List<RTSpan<V>> getSpans(Spannable str, Selection selection, SpanCollectMode mode) {
        if (mSpanCollector == null) {
            // lazy initialize the SpanCollector
            Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
            Class<? extends RTSpan<V>> spanClazz = (Class<? extends RTSpan<V>>) types[types.length - 1];
            mSpanCollector = newSpanCollector(spanClazz);
        }

        return mSpanCollector.getSpans(str, selection, mode);
    }

    /**
     * @return a new SpanCollector for this effect
     */
    abstract protected SpanCollector<V> newSpanCollector(Class<? extends RTSpan<V>> spanClazz);

    /**
     * @return the Selection for the specified RTEditText.
     */
    abstract protected Selection getSelection(RTEditText editor);

    /**
     * Apply this effect to the selection.
     * If value is Null then the effect will be removed from the current selection.
     *
     * @param editor The editor to apply the effect to (current selection)
     * @param value The value to apply (depends on the Effect)
     */
    abstract public void applyToSelection(RTEditText editor, V value);

}
