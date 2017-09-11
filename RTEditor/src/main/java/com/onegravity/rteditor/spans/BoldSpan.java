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

import android.graphics.Typeface;
import android.text.style.StyleSpan;

/**
 * Implementation for a bold span (android.text.style.StyleSpan(TypeFace.BOLD))
 */
public class BoldSpan extends StyleSpan implements RTSpan<Boolean> {

    public BoldSpan() {
        super(Typeface.BOLD);
    }

    @Override
    public Boolean getValue() {
        return Boolean.TRUE;
    }

}
