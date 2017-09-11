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

import com.onegravity.rteditor.spans.RTSpan;
import com.onegravity.rteditor.utils.Selection;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Spanned.getSpans(int, int, Class) unfortunately doesn't respect the mark/point flags
 * (SPAN_EXCLUSIVE_EXCLUSIVE, SPAN_INCLUSIVE_EXCLUSIVE etc.).
 *
 * Android finds spans that precede or follow a selection (adjacent spans).
 *
 * - if the span is a point (start == end) --> every adjacent selection (cursor, range = selected text) will match:
 *   01234[]|56789     point selection after point span   --> getSpans returns span
 *   01234|[]56789     point selection before point span  --> getSpans returns span
 *   01234[]|56|789    range selection after point span   --> getSpans returns span
 *   012|34|[]56789    range selection before point span  --> getSpans returns span
 *
 * - if the span is a range (start < end) --> only point selections (cursors) will match:
 *   01[234]|56789     point selection after range span  --> getSpans returns span
 *   01234|[567]89     point selection before range span --> getSpans returns span
 *   01[234]|56|789    range selection after range span  --> getSpans returns nothing
 *   012|34|[567]89    range selection before range span --> getSpans returns nothing
 *
 * The span flags (SPAN_EXCLUSIVE_INCLUSIVE etc.) have no impact!
 *
 * (exception SPAN_EXCLUSIVE_EXCLUSIVE for point spans because those can't have a length of 0 and
 *  will be removed automatically:
 *  http://developer.android.com/reference/android/text/Spanned.html#SPAN_EXCLUSIVE_EXCLUSIVE)
 *
 * Sometimes we need to find or ignore adjacent spans depending on the span flags, the position of a
 * span, the selection (last line, empty lines...) and the type of span (character, paragraph).
 *
 * This class allows us to implement different getSpan methods that honor the span flags.
 *
 * @param <V> the Effect's configuration information.
 */
abstract class SpanCollector<V> {

    private Class<? extends RTSpan<V>> mSpanClazz;

    protected SpanCollector(Class<? extends RTSpan<V>> spanClazz) {
        mSpanClazz = spanClazz;
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
    protected abstract List<RTSpan<V>> getSpans(Spannable str, Selection selection, SpanCollectMode mode);

    /**
     * Return an array of the markup objects attached to the specified slice of a Spannable and whose
     * type is the specified type or a subclass of it (see Spanned.getSpans(int, int, Class<T>)).
     */
    final protected RTSpan<V>[] getSpansAndroid(Spannable str, int selStart, int selEnd) {
        RTSpan<V>[] spans = str.getSpans(selStart, selEnd, mSpanClazz);
        return spans == null ? (RTSpan<V>[]) Array.newInstance(mSpanClazz) : spans;
    }

    /**
     * @return True if the flags contain at least one of the values, False otherwise.
     */
    final protected boolean isOneFlagSet(int flags, int...value) {
        for (int flag : value) {
            if ((flags & flag) == flag) {
                return true;
            }
        }
        return false;
    }

}
