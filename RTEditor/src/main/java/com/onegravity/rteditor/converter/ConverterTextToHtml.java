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

package com.onegravity.rteditor.converter;

import android.text.TextUtils;
import android.util.Patterns;

import com.onegravity.rteditor.api.format.RTFormat;
import com.onegravity.rteditor.api.format.RTHtml;
import com.onegravity.rteditor.api.format.RTPlainText;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts plain text to html
 */
public class ConverterTextToHtml {

    // Number of extra bytes to allocate in a string buffer for htmlification.
    private static final int TEXT_TO_HTML_EXTRA_BUFFER_LENGTH = 512;

    private static final String BITCOIN_URI_PATTERN = "bitcoin:[1-9a-km-zA-HJ-NP-Z]{27,34}(\\?[a-zA-Z0-9$\\-_.+!*'(),%:@&=]*)?";

    private static final boolean USE_REPLACE_ALL = false;

    public static RTHtml<RTImage, RTAudio, RTVideo> convert(RTPlainText input) {
        String text = input.getText();
        String result = convert(text);
        return new RTHtml<RTImage, RTAudio, RTVideo>(RTFormat.HTML, result);
    }

    public static String convert(String text) {
        // Escape the entities and add newlines.
        String htmlified = text == null ? "" : TextUtils.htmlEncode(text);

        // Linkify the message.
        StringBuffer linkified = new StringBuffer(htmlified.length() + TEXT_TO_HTML_EXTRA_BUFFER_LENGTH);
        linkifyText(htmlified, linkified);

        // For some reason, TextUtils.htmlEncode escapes ' into &apos;, which is technically part of the XHTML 1.0
        // standard, but Gmail doesn't recognize it as an HTML entity. We unescape that here.
        String result = linkified.toString().replace("\n", "<br>\n").replace("&apos;", "&#39;");
        return result;
    }

    private static void linkifyText(final String text, final StringBuffer outputBuffer) {
        String prepared = replaceAll(text, BITCOIN_URI_PATTERN, "<a href=\"$0\">$0</a>");
        Matcher m = Patterns.WEB_URL.matcher(prepared);
        while (m.find()) {
            int start = m.start();
            if (start == 0 || (start != 0 && text.charAt(start - 1) != '@')) {
                if (m.group().indexOf(':') > 0) { // With no URI-schema we may get "http:/" links with the second / missing
                    m.appendReplacement(outputBuffer, "<a href=\"$0\">$0</a>");
                } else {
                    m.appendReplacement(outputBuffer, "<a href=\"http://$0\">$0</a>");
                }
            } else {
                m.appendReplacement(outputBuffer, "$0");
            }
        }
        m.appendTail(outputBuffer);
    }

    /**
     * A memory optimized algorithm for String.replaceAll
     */
    private static String replaceAll(String source, String search, String replace) {
        if (USE_REPLACE_ALL) {
            return source.replaceAll(search, replace);
        } else {
            Pattern p = Pattern.compile(search);
            Matcher m = p.matcher(source);
            StringBuffer sb = new StringBuffer();
            boolean atLeastOneFound = false;
            while (m.find()) {
                m.appendReplacement(sb, replace);
                atLeastOneFound = true;
            }
            if (atLeastOneFound) {
                m.appendTail(sb);
                return sb.toString();
            } else {
                return source;
            }
        }
    }

}