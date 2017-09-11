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

import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ParagraphStyle;
import android.text.style.StrikethroughSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.URLSpan;
import com.onegravity.rteditor.spans.UnderlineSpan;

import com.onegravity.rteditor.api.format.RTFormat;
import com.onegravity.rteditor.api.format.RTHtml;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.converter.tagsoup.util.StringEscapeUtils;
import com.onegravity.rteditor.spans.AudioSpan;
import com.onegravity.rteditor.spans.BoldSpan;
import com.onegravity.rteditor.spans.TypefaceSpan;
import com.onegravity.rteditor.spans.ImageSpan;
import com.onegravity.rteditor.spans.ItalicSpan;
import com.onegravity.rteditor.spans.LinkSpan;
import com.onegravity.rteditor.spans.VideoSpan;
import com.onegravity.rteditor.utils.Helper;
import com.onegravity.rteditor.utils.Paragraph;
import com.onegravity.rteditor.utils.RTLayout;
import com.onegravity.rteditor.utils.Selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 * Converts Spanned text to html
 */
public class ConverterSpannedToHtml {

    private static final String BR = "<br/>\n";
    private static final String LT = "&lt;";
    private static final String GT = "&gt;";
    private static final String AMP = "&amp;";
    private static final String NBSP = "&nbsp;";

    private StringBuilder mOut;
    private Spanned mText;
    private RTFormat mRTFormat;
    private List<RTImage> mImages;
    private Stack<AccumulatedParagraphStyle> mParagraphStyles = new Stack<AccumulatedParagraphStyle>();

    /**
     * Converts a spanned text to HTML
     */
    public RTHtml<RTImage, RTAudio, RTVideo> convert(final Spanned text, RTFormat.Html rtFormat) {
        mText = text;
        mRTFormat = rtFormat;

        mOut = new StringBuilder();
        mImages = new ArrayList<RTImage>();
        mParagraphStyles.clear();

        // convert paragraphs
        convertParagraphs();

        return new RTHtml<RTImage, RTAudio, RTVideo>(rtFormat, mOut.toString(), mImages);
    }

    // ****************************************** Process Paragraphs *******************************************

    private void convertParagraphs() {
        RTLayout rtLayout = new RTLayout(mText);

        // a manual for loop is faster than the for-each loop for an ArrayList:
        // see https://developer.android.com/training/articles/perf-tips.html#Loops
        ArrayList<Paragraph> paragraphs = rtLayout.getParagraphs();
        for (int i = 0, size = paragraphs.size(); i < size; i++) {
            Paragraph paragraph = paragraphs.get(i);

            // retrieve all spans for this paragraph
            Set<SingleParagraphStyle> styles = getParagraphStyles(mText, paragraph);

            // get the alignment span if there is any
            ParagraphType alignmentType = null;
            for (SingleParagraphStyle style : styles) {
                if (style.getType().isAlignment()) {
                    alignmentType = style.getType();
                    break;
                }
            }

            /*
             * start tag: bullet points, numbering and indentation
             */
            int newIndent = 0;
            ParagraphType newType = ParagraphType.NONE;
            for (SingleParagraphStyle style : styles) {
                newIndent += style.getIndentation();
                ParagraphType type = style.getType();
                newType = type.isBullet() ? ParagraphType.BULLET :
                        type.isNumbering() ? ParagraphType.NUMBERING :
                                type.isIndentation() && newType.isUndefined() ? ParagraphType.INDENTATION_UL : newType;
            }
            // process leading margin style
            processLeadingMarginStyle(new AccumulatedParagraphStyle(newType, newIndent, 0));
            // add start list tag
            mOut.append(newType.getListStartTag());

            /*
             * start tag: alignment (left, center, right)
             */
            if (alignmentType != null) {
                mOut.append(alignmentType.getStartTag());
            }

            /*
             * Convert the plain text
             */
            withinParagraph(mText, paragraph.start(), paragraph.end());

            /*
             * end tag: alignment (left, center, right)
             */
            if (alignmentType != null) {
                removeTrailingLineBreak(alignmentType);
                mOut.append(alignmentType.getEndTag());
            }


            // add end list tag
            removeTrailingLineBreak(newType);
            mOut.append(newType.getListEndTag());
        }

        /*
         * end tag: bullet points and indentation
         */
        while (!mParagraphStyles.isEmpty()) {
            removeParagraph();
        }
    }

    private void removeTrailingLineBreak(ParagraphType type) {
        if (type.endTagAddsLineBreak() && mOut.length() >= BR.length()) {
            int start = mOut.length() - BR.length();
            int end = mOut.length();
            if (mOut.subSequence(start, end).equals(BR)) {
                mOut.delete(start, end);
            }
        }
    }

    private Set<SingleParagraphStyle> getParagraphStyles(final Spanned text, Selection selection) {
        Set<SingleParagraphStyle> styles = new HashSet<SingleParagraphStyle>();

        for (ParagraphStyle style : text.getSpans(selection.start(), selection.end(), ParagraphStyle.class)) {
            ParagraphType type = ParagraphType.getInstance(style);
            if (type != null) {
                styles.add(new SingleParagraphStyle(type, style));
            }
        }

        return styles;
    }

    private void processLeadingMarginStyle(AccumulatedParagraphStyle newStyle) {
        int currentIndent = 0;
        ParagraphType currentType = ParagraphType.NONE;
        if (!mParagraphStyles.isEmpty()) {
            AccumulatedParagraphStyle currentStyle = mParagraphStyles.peek();
            currentIndent = currentStyle.getAbsoluteIndent();
            currentType = currentStyle.getType();
        }

        if (newStyle.getAbsoluteIndent() > currentIndent) {
            newStyle.setRelativeIndent(newStyle.getAbsoluteIndent() - currentIndent);
            addParagraph(newStyle);
        } else if (newStyle.getAbsoluteIndent() < currentIndent) {
            removeParagraph();
            processLeadingMarginStyle(newStyle);
        } else if (newStyle.getType() != currentType) {
            newStyle.setRelativeIndent(removeParagraph());
            addParagraph(newStyle);
        }
    }

    private int removeParagraph() {
        if (!mParagraphStyles.isEmpty()) {
            AccumulatedParagraphStyle style = mParagraphStyles.pop();
            String tag = style.getType().getEndTag();
            int indent = style.getRelativeIndent();
            for (int i = 0; i < indent; i++) {
                mOut.append(tag);
            }
            return style.getRelativeIndent();
        }
        return 0;
    }

    private void addParagraph(AccumulatedParagraphStyle style) {
        String tag = style.getType().getStartTag();
        int indent = style.getRelativeIndent();
        for (int i = 0; i < indent; i++) {
            mOut.append(tag);
        }
        mParagraphStyles.push(style);
    }

    // ****************************************** Process Text *******************************************

    /**
     * Convert a spanned text within a paragraph
     */
    private void withinParagraph(final Spanned text, int start, int end) {
        // create sorted set of CharacterStyles
        SortedSet<CharacterStyle> sortedSpans = new TreeSet<CharacterStyle>(new Comparator<CharacterStyle>() {
            @Override
            public int compare(CharacterStyle s1, CharacterStyle s2) {
                int start1 = text.getSpanStart(s1);
                int start2 = text.getSpanStart(s2);
                if (start1 != start2)
                    return start1 - start2;        // span which starts first comes first

                int end1 = text.getSpanEnd(s1);
                int end2 = text.getSpanEnd(s2);
                if (end1 != end2) return end2 - end1;                // longer span comes first

                // if the paragraphs have the same span [start, end] we compare their name
                // compare the name only because local + anonymous classes have no canonical name
                return s1.getClass().getName().compareTo(s2.getClass().getName());
            }
        });
        List<CharacterStyle> spanList = Arrays.asList(text.getSpans(start, end, CharacterStyle.class));
        sortedSpans.addAll(spanList);

        // process paragraphs/divs
        convertText(text, start, end, sortedSpans);
    }

    private void convertText(Spanned text, int start, int end, SortedSet<CharacterStyle> spans) {
        while (start < end) {

            // get first CharacterStyle
            CharacterStyle span = spans.isEmpty() ? null : spans.first();
            int spanStart = span == null ? Integer.MAX_VALUE : text.getSpanStart(span);
            int spanEnd = span == null ? Integer.MAX_VALUE : text.getSpanEnd(span);

            if (start < spanStart) {

                // no paragraph, just plain text
                escape(text, start, Math.min(end, spanStart));
                start = spanStart;

            } else {

                // CharacterStyle found

                spans.remove(span);

                if (handleStartTag(span)) {
                    convertText(text, Math.max(spanStart, start), Math.min(spanEnd, end), spans);
                }
                handleEndTag(span);

                start = spanEnd;
            }

        }
    }

    /**
     * @return True if the text between the tags should be converted too, False if it should be skipped (ImageSpan e.g.)
     */
    private boolean handleStartTag(CharacterStyle style) {
        if (style instanceof BoldSpan) {
            mOut.append("<b>");
        } else if (style instanceof ItalicSpan) {
            mOut.append("<i>");
        } else if (style instanceof UnderlineSpan) {
            mOut.append("<u>");
        } else if (style instanceof SuperscriptSpan) {
            mOut.append("<sup>");
        } else if (style instanceof SubscriptSpan) {
            mOut.append("<sub>");
        } else if (style instanceof StrikethroughSpan) {
            mOut.append("<strike>");
        }
        /* Examples for fonts styles:
           <font face="verdana" style="font-size:25px;background-color:#00ff00;color:#ff0000">This is heading 1</font>
           <font face="DroidSans" style="font-size:50px;background-color:#0000FF;color:#FFFF00">This is heading 2</font>
        */
        else if (style instanceof TypefaceSpan) {
            mOut.append("<font face=\"");
            String fontName = ((TypefaceSpan) style).getValue().getName();
            mOut.append(StringEscapeUtils.escapeHtml4(fontName));
            mOut.append("\">");
        } else if (style instanceof AbsoluteSizeSpan) {
            mOut.append("<font style=\"font-size:");
            int size = ((AbsoluteSizeSpan) style).getSize();
            size = Helper.convertSpToPx(size);
            mOut.append(size);
            mOut.append("px\">");
        } else if (style instanceof ForegroundColorSpan) {
            mOut.append("<font style=\"color:#");
            String color = Integer.toHexString(((ForegroundColorSpan) style).getForegroundColor() + 0x01000000);
            while (color.length() < 6) {
                color = "0" + color;
            }
            mOut.append(color);
            mOut.append("\">");
        } else if (style instanceof BackgroundColorSpan) {
            mOut.append("<font style=\"background-color:#");
            String color = Integer.toHexString(((BackgroundColorSpan) style).getBackgroundColor() + 0x01000000);
            while (color.length() < 6) {
                color = "0" + color;
            }
            mOut.append(color);
            mOut.append("\">");
        } else if (style instanceof LinkSpan) {
            mOut.append("<a href=\"");
            mOut.append(((URLSpan) style).getURL());
            mOut.append("\">");
        } else if (style instanceof ImageSpan) {
            ImageSpan span = ((ImageSpan) style);
            RTImage image = span.getImage();
            mImages.add(image);
            String filePath = image.getFilePath(mRTFormat);
            mOut.append("<img src=\"" + filePath + "\">");
            return false;    // don't output the dummy character underlying the image.
        } else if (style instanceof AudioSpan) {
            AudioSpan span = ((AudioSpan) style);
            RTAudio audio = span.getAudio();
            String filePath = audio.getFilePath(mRTFormat);
            mOut.append("<embed src=\"" + filePath + "\">");
            return false;    // don't output the dummy character underlying the audio file.
        } else if (style instanceof VideoSpan) {
            VideoSpan span = ((VideoSpan) style);
            RTVideo video = span.getVideo();
            String filePath = video.getFilePath(mRTFormat);
            mOut.append("<video controls src=\"" + filePath + "\">");
            return false;    // don't output the dummy character underlying the video.
        }
        return true;
    }

    private void handleEndTag(CharacterStyle style) {
        if (style instanceof URLSpan) {
            mOut.append("</a>");
        } else if (style instanceof TypefaceSpan) {
            mOut.append("</font>");
        } else if (style instanceof ForegroundColorSpan) {
            mOut.append("</font>");
        } else if (style instanceof BackgroundColorSpan) {
            mOut.append("</font>");
        } else if (style instanceof AbsoluteSizeSpan) {
            mOut.append("</font>");
        } else if (style instanceof StrikethroughSpan) {
            mOut.append("</strike>");
        } else if (style instanceof SubscriptSpan) {
            mOut.append("</sub>");
        } else if (style instanceof SuperscriptSpan) {
            mOut.append("</sup>");
        } else if (style instanceof UnderlineSpan) {
            mOut.append("</u>");
        } else if (style instanceof BoldSpan) {
            mOut.append("</b>");
        } else if (style instanceof ItalicSpan) {
            mOut.append("</i>");
        }
    }

    /**
     * Escape plain text parts: <, >, &, Space --> ^lt;, &gt; etc.
     */
    private void escape(CharSequence text, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                mOut.append(BR);
            } else if (c == '<') {
                mOut.append(LT);
            } else if (c == '>') {
                mOut.append(GT);
            } else if (c == '&') {
                mOut.append(AMP);
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    mOut.append(NBSP);
                    i++;
                }
                mOut.append(' ');
            }
            // removed the c > 0x7E check to leave emoji unaltered
            else if (/*c > 0x7E || */c < ' ') {
                mOut.append("&#" + ((int) c) + ";");
            } else {
                mOut.append(c);
            }
        }
    }

}
