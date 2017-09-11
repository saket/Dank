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

import android.text.style.URLSpan;
import android.view.View;

/**
 * A wrapper around an android.text.style.URLSpan that forwards clicks to the
 * View it's embedded in.
 * That View is the RTEditText which implements the LinkSpanListener interface.
 */
public class LinkSpan extends URLSpan implements RTSpan<String> {

    public interface LinkSpanListener {
        public void onClick(LinkSpan linkSpan);
    }

    public LinkSpan(String url) {
        super(url);
    }

    @Override
    public void onClick(View view) {
        if (view instanceof LinkSpanListener) {
            ((LinkSpanListener) view).onClick(this);
        }
    }

    @Override
    public String getValue() {
        return getURL();
    }

}
