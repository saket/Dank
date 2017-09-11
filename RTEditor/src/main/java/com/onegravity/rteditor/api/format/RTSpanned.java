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

package com.onegravity.rteditor.api.format;

import android.text.Spanned;

/**
 * RTText representing rich text in android.text.Spanned format.
 * <p>
 * Use this class if the source text is immutable and RTEditable if the source
 * text is mutable (from an RTEditText). This distinction is important when
 * converting the text.
 * The editable version will be pre-processed to eliminate certain spans and to
 * clean up the paragraph formatting.
 * The immutable version on the other hand doesn't support conversions at all.
 *
 * @see RTEditable
 */
public class RTSpanned extends RTText {

    public RTSpanned(Spanned spanned) {
        super(RTFormat.SPANNED, spanned);
    }

}