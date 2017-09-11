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

package com.onegravity.rteditor.utils;

import android.text.Spanned;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class finds the Paragraphs in a Spanned text.
 *
 * A paragraph spans from one \n (exclusive) to the next one (inclusive):
 * first paragraph\nsecond paragraph\nthird paragraph
 * |_______________||________________||_____________|
 *
 * We need this for all the paragraph formatting (bullet points, indentation etc.).
 * While it's optimized for performance, it should still be used with caution.
 */
public class RTLayout implements Serializable {
    private static final long serialVersionUID = 2210969820444215580L;

    private static final Pattern LINEBREAK_PATTERN = Pattern.compile("\\r\\n|\\r|\\n");

    private int mNrOfLines = 0;
    private final ArrayList<Paragraph> mParagraphs = new ArrayList<>();

    public RTLayout(final Spanned spanned) {
        if (spanned != null) {
            final String s = spanned.toString();

            // find the line breaks and the according lines / paragraphs
            mNrOfLines = 1;
            final Matcher m = LINEBREAK_PATTERN.matcher(s);
            int groupStart = 0;
            while (m.find()) {
                // the line feeds are part of the paragraph              isFirst          isLast
                Paragraph paragraph = new Paragraph(groupStart, m.end(), mNrOfLines == 1, false);
                mParagraphs.add(paragraph);
                groupStart = m.end();
                mNrOfLines++;
            }

            // even an empty line after the last cr/lf is considered a paragraph
            if (mParagraphs.size() < mNrOfLines) {
                Paragraph paragraph = new Paragraph(groupStart, s.length(), mNrOfLines == 1, true);
                mParagraphs.add(paragraph);
            }
        }
    }

    /**
     * @return all Paragraphs for this layout / spanned text.
     */
    public ArrayList<Paragraph> getParagraphs() {
        return mParagraphs;
    }

    /**
     * @return the line for a certain position in the spanned text
     */
    public int getLineForOffset(final int offset) {
        int lineNr = 0;
        while (lineNr < mNrOfLines && offset >= mParagraphs.get(lineNr).end()) {
            lineNr++;
        }
        return Math.min(Math.max(0, lineNr), mParagraphs.size() - 1);
    }

    /**
     * @return the start position of a certain line in the spanned text
     */
    public int getLineStart(final int line) {
        return mNrOfLines == 0 || line < 0 ? 0 :
               line < mNrOfLines ? mParagraphs.get(line).start() :
               mParagraphs.get(mNrOfLines - 1).end() - 1;
    }

    /**
     * @return the end position of a certain line in the spanned text
     */
    public int getLineEnd(final int line) {
        return mNrOfLines == 0 || line < 0 ? 0 :
               line < mNrOfLines ? mParagraphs.get(line).end() :
               mParagraphs.get(mNrOfLines - 1).end() - 1;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        int line = 1;
        for (Paragraph p : mParagraphs) {
            s.append(line++).append(": ").append(p.start()).append("-").append(p.end())
                    .append(p.isLast() ? "" : ", ");
        }
        return s.toString();
    }
}
