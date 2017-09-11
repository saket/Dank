/*
 * Copyright 2015 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.onegravity.rteditor;

import android.text.Spannable;

import com.onegravity.rteditor.spans.LinkSpan;

/**
 * The interface to be implemented by the RTManager to listen to RTEditText events.
 */
public interface RTEditTextListener {

    /**
     * When delivering MediaEvents to the RTManager, the RTEditText might not have its state fully
     * restored and the event can't be processed yet.
     * This method signals that the RTEditText is fully operational now --> process pending/sticky
     * MediaEvents.
     */
    void onRestoredInstanceState(RTEditText editor);

    /**
     * If this EditText changes focus the listener will be informed through this method.
     */
    void onFocusChanged(RTEditText editor, boolean focused);

    /**
     * Provides details of the new selection, including the start and ending
     * character positions, and the id of this RTEditText component.
     */
    void onSelectionChanged(RTEditText editor, int start, int end);

    /**
     * Text and or text effects have changed (used for undo/redo function).
     */
    void onTextChanged(RTEditText editor, Spannable before, Spannable after,
                       int selStartBefore, int selEndBefore, int selStartAfter, int selEndAfter);

    /**
     * A link in a LinkSpan has been clicked.
     */
    public void onClick(RTEditText editor, LinkSpan span);

    /**
     * Rich text editing was enabled/disabled for this editor.
     */
    public void onRichTextEditingChanged(RTEditText editor, boolean useRichText);

}
