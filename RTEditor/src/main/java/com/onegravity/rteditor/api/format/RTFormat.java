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

/**
 * The RTFormat class defines the format of an RTText.
 * <p>
 * It's not an enum because we need to be able to extend the pre-defined formats
 * (HTML, PLAIN_TEXT, SPANNED).
 * <p>
 * Extending the formats allows us to add custom conversions
 * (RTText.convertTo(RTFormat, RTMediaFactory)) and to add logic in custom
 * RTMedia implementations (RTMedia.getFilePath(RTFormat)) without breaking the
 * rest of the code.
 * <p>
 * We could e.g. return cid:picture.jpg for a html link to an image (<a
 * href="cid:picture.jpg">) if the output format is a mime message and the image
 * will be sent in an email. We could return database://picture.jpg if the
 * output is stored in a database (database://<db_id>/picture.jpg if we use
 * multiple databases) or content://picture.jpg if the image is served by a
 * content provider. We can basically add any information needed to retrieve the
 * image (or rather its file path) when converting it back to rich text (Spanned
 * text).
 */
public abstract class RTFormat {

    /*
     * These are the instances of the pre-defined rich text formats
     * (the actual "enum").
     */
    public static final Spanned SPANNED = new Spanned();
    public static final PlainText PLAIN_TEXT = new PlainText();
    public static final Html HTML = new Html();

    /**
     * Spanned is the text format used by the editor itself. It's used by the
     * RTSpanned that keeps its content as an android.text.Spanned
     */
    public static class Spanned extends RTFormat {
    }

    /**
     * PlainText is the plain text format. It's used by the RTPlainText that
     * keeps its content as unformatted text.
     */
    public static class PlainText extends RTFormat {
    }

    /**
     * Html is the html text format. It's used by the RTHtmlText that keeps its
     * content as formatted html and can be used as storage format, in emails or
     * be rendered as html page.
     */
    public static class Html extends RTFormat {
    }

}