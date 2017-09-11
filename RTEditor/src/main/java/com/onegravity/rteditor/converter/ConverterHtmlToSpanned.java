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

import android.annotation.SuppressLint;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;

import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.format.RTFormat;
import com.onegravity.rteditor.api.format.RTHtml;
import com.onegravity.rteditor.api.format.RTSpanned;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.converter.tagsoup.HTMLSchema;
import com.onegravity.rteditor.converter.tagsoup.Parser;
import com.onegravity.rteditor.fonts.FontManager;
import com.onegravity.rteditor.fonts.RTTypeface;
import com.onegravity.rteditor.spans.AbsoluteSizeSpan;
import com.onegravity.rteditor.spans.AlignmentSpan;
import com.onegravity.rteditor.spans.BackgroundColorSpan;
import com.onegravity.rteditor.spans.BoldSpan;
import com.onegravity.rteditor.spans.BulletSpan;
import com.onegravity.rteditor.spans.ForegroundColorSpan;
import com.onegravity.rteditor.spans.ImageSpan;
import com.onegravity.rteditor.spans.IndentationSpan;
import com.onegravity.rteditor.spans.ItalicSpan;
import com.onegravity.rteditor.spans.LinkSpan;
import com.onegravity.rteditor.spans.NumberSpan;
import com.onegravity.rteditor.spans.StrikethroughSpan;
import com.onegravity.rteditor.spans.SubscriptSpan;
import com.onegravity.rteditor.spans.SuperscriptSpan;
import com.onegravity.rteditor.spans.TypefaceSpan;
import com.onegravity.rteditor.spans.UnderlineSpan;
import com.onegravity.rteditor.utils.Helper;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts html to Spanned text using TagSoup
 */
public class ConverterHtmlToSpanned implements ContentHandler {

    private static final float[] HEADER_SIZES = {
            1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
    };

    private String mSource;
    private RTMediaFactory<? extends RTImage, ? extends RTAudio, ? extends RTVideo> mMediaFactory;
    private Parser mParser;
    private SpannableStringBuilder mResult;

    private Stack<AccumulatedParagraphStyle> mParagraphStyles = new Stack<AccumulatedParagraphStyle>();

    /**
     * If this is set to True we ignore all characters till it's set to false again.
     * This way be can ignore e.g. style information or even the whole header
     */
    private boolean mIgnoreContent;

    private static final Set<String> sIgnoreTags = new HashSet<String>();

    static {
        sIgnoreTags.add("header");
        sIgnoreTags.add("style");
        sIgnoreTags.add("meta");
    }

    /**
     * Lazy initialization holder for HTML parser. This class will
     * a) be pre-loaded by zygote, or
     * b) not loaded until absolutely necessary.
     */
    private static class HtmlParser {
        private static final HTMLSchema SCHEMA = new HTMLSchema();
    }

    public RTSpanned convert(RTHtml<? extends RTImage, ? extends RTAudio, ? extends RTVideo> input,
                             RTMediaFactory<? extends RTImage, ? extends RTAudio, ? extends RTVideo> mediaFactory) {
        mSource = input.getText();
        mMediaFactory = mediaFactory;

        mParser = new Parser();
        try {
            mParser.setProperty(Parser.schemaProperty, HtmlParser.SCHEMA);
        } catch (SAXNotRecognizedException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        } catch (SAXNotSupportedException shouldNotHappen) {
            throw new RuntimeException(shouldNotHappen);
        }

        mResult = new SpannableStringBuilder();
        mIgnoreContent = false;
        mParagraphStyles.clear();

        mParser.setContentHandler(this);
        try {
            mParser.parse(new InputSource(new StringReader(mSource)));
        } catch (IOException e) {
            // We are reading from a string. There should not be IO problems.
            throw new RuntimeException(e);
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }

        // remove trailing line breaks
        removeTrailingLineBreaks();

        // replace all TemporarySpans by the "real" spans
        for (TemporarySpan span : mResult.getSpans(0, mResult.length(), TemporarySpan.class)) {
            span.swapIn(mResult);
        }

        return new RTSpanned(mResult);
    }

    private void removeTrailingLineBreaks() {
        int end = mResult.length();
        while (end > 0 && mResult.charAt(end - 1) == '\n') {
            end--;
        }
        if (end < mResult.length()) {
            mResult = SpannableStringBuilder.valueOf(mResult.subSequence(0, end));
        }
    }

    // ****************************************** org.xml.sax.ContentHandler *******************************************

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        handleStartTag(localName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        handleEndTag(localName);
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if (mIgnoreContent) return;

        StringBuilder sb = new StringBuilder();

        /*
         * Ignore whitespace that immediately follows other whitespace; newlines count as spaces.
         */

        for (int i = 0; i < length; i++) {
            char c = ch[i + start];
            if (c == ' ' || c == '\n') {
                char pred;
                int len = sb.length();

                if (len == 0) {
                    len = mResult.length();

                    if (len == 0) {
                        pred = '\n';
                    } else {
                        pred = mResult.charAt(len - 1);
                    }
                } else {
                    pred = sb.charAt(len - 1);
                }

                if (pred != ' ' && pred != '\n') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }

        mResult.append(sb);
    }

    @Override
    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    // ****************************************** Handle Tags *******************************************

    private void handleStartTag(String tag, Attributes attributes) {
        if (tag.equalsIgnoreCase("br")) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely omit the line breaks when we handle the close tag.
        } else if (tag.equalsIgnoreCase("p")) {
            handleP();
        } else if (tag.equalsIgnoreCase("div")) {
            startDiv(attributes);
        } else if (tag.equalsIgnoreCase("ul")) {
            startList(false, attributes);
        } else if (tag.equalsIgnoreCase("ol")) {
            startList(true, attributes);
        } else if (tag.equalsIgnoreCase("li")) {
            startList(attributes);
        } else if (tag.equalsIgnoreCase("strong")) {
            start(new Bold());
        } else if (tag.equalsIgnoreCase("b")) {
            start(new Bold());
        } else if (tag.equalsIgnoreCase("em")) {
            start(new Italic());
        } else if (tag.equalsIgnoreCase("cite")) {
            start(new Italic());
        } else if (tag.equalsIgnoreCase("dfn")) {
            start(new Italic());
        } else if (tag.equalsIgnoreCase("i")) {
            start(new Italic());
        } else if (tag.equalsIgnoreCase("strike")) {
            start(new Strikethrough());
        } else if (tag.equalsIgnoreCase("del")) {
            start(new Strikethrough());
        } else if (tag.equalsIgnoreCase("big")) {
            start(new Big());
        } else if (tag.equalsIgnoreCase("small")) {
            start(new Small());
        } else if (tag.equalsIgnoreCase("font")) {
            startFont(attributes);
        } else if (tag.equalsIgnoreCase("blockquote")) {
            handleP();
            start(new Blockquote());
        } else if (tag.equalsIgnoreCase("tt")) {
            start(new Monospace());
        } else if (tag.equalsIgnoreCase("a")) {
            startAHref(attributes);
        } else if (tag.equalsIgnoreCase("u")) {
            start(new Underline());
        } else if (tag.equalsIgnoreCase("sup")) {
            start(new Super());
        } else if (tag.equalsIgnoreCase("sub")) {
            start(new Sub());
        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            handleP();
            start(new Header(tag.charAt(1) - '1'));
        } else if (tag.equalsIgnoreCase("img")) {
            startImg(attributes);
        } else if (tag.equalsIgnoreCase("video")) {
            startVideo(attributes);
        } else if (tag.equalsIgnoreCase("embed")) {
            startAudio(attributes);
        } else if (sIgnoreTags.contains(tag.toLowerCase(Locale.getDefault()))) {
            mIgnoreContent = true;
        }
    }

    private void handleEndTag(String tag) {
        if (tag.equalsIgnoreCase("br")) {
            handleBr();
        } else if (tag.equalsIgnoreCase("p")) {
            handleP();
        } else if (tag.equalsIgnoreCase("div")) {
            endDiv();
        } else if (tag.equalsIgnoreCase("ul")) {
            endList(false);
        } else if (tag.equalsIgnoreCase("ol")) {
            endList(true);
        } else if (tag.equalsIgnoreCase("li")) {
            endList();
        } else if (tag.equalsIgnoreCase("strong")) {
            end(Bold.class, new BoldSpan());
        } else if (tag.equalsIgnoreCase("b")) {
            end(Bold.class, new BoldSpan());
        } else if (tag.equalsIgnoreCase("em")) {
            end(Italic.class, new ItalicSpan());
        } else if (tag.equalsIgnoreCase("cite")) {
            end(Italic.class, new ItalicSpan());
        } else if (tag.equalsIgnoreCase("dfn")) {
            end(Italic.class, new ItalicSpan());
        } else if (tag.equalsIgnoreCase("i")) {
            end(Italic.class, new ItalicSpan());
        } else if (tag.equalsIgnoreCase("strike")) {
            end(Strikethrough.class, new StrikethroughSpan());
        } else if (tag.equalsIgnoreCase("del")) {
            end(Strikethrough.class, new StrikethroughSpan());
        } else if (tag.equalsIgnoreCase("big")) {
            int size = Helper.convertPxToSp(32);
            end(Big.class, new AbsoluteSizeSpan(size));
        } else if (tag.equalsIgnoreCase("small")) {
            int size = Helper.convertPxToSp(14);
            end(Small.class, new AbsoluteSizeSpan(size));
        } else if (tag.equalsIgnoreCase("font")) {
            endFont();
        } else if (tag.equalsIgnoreCase("blockquote")) {
            handleP();
            end(Blockquote.class, new QuoteSpan());
        } else if (tag.equalsIgnoreCase("a")) {
            endAHref();
        } else if (tag.equalsIgnoreCase("u")) {
            end(Underline.class, new UnderlineSpan());
        } else if (tag.equalsIgnoreCase("sup")) {
            end(Super.class, new SuperscriptSpan());
        } else if (tag.equalsIgnoreCase("sub")) {
            end(Sub.class, new SubscriptSpan());
        } else if (tag.length() == 2 &&
                Character.toLowerCase(tag.charAt(0)) == 'h' &&
                tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
            handleP();
            endHeader();
        } else if (sIgnoreTags.contains(tag.toLowerCase(Locale.getDefault()))) {
            mIgnoreContent = false;
        }
    }

    private void startDiv(Attributes attributes) {
        String sAlign = attributes.getValue("align");
        int len = mResult.length();
        mResult.setSpan(new Div(sAlign), len, len, Spanned.SPAN_MARK_MARK);
    }

    private void endDiv() {
        int end = mResult.length();
        Object obj = getLast(mResult, Div.class);
        int start = mResult.getSpanStart(obj);

        mResult.removeSpan(obj);
        if (start != end) {
            if (!checkDuplicateSpan(mResult, start, AlignmentSpan.class)) {
                Div divObj = (Div) obj;
                Layout.Alignment align = divObj.mAlign.equalsIgnoreCase("center") ? Layout.Alignment.ALIGN_CENTER :
                        divObj.mAlign.equalsIgnoreCase("right") ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
                if (align != null) {
                    if (mResult.charAt(end - 1) != '\n') {
                        // yes we need that linefeed, or we will get crashes
                        mResult.append('\n');
                    }
                    // use SPAN_EXCLUSIVE_EXCLUSIVE here, will be replaced later anyway when the cleanup function is called
                    boolean isRTL = Helper.isRTL(mResult, start, end);
                    mResult.setSpan(new AlignmentSpan(align, isRTL), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    /**
     * Handles OL and UL start tags
     */
    private void startList(boolean isOrderedList, Attributes attributes) {
        boolean isIndentation = isIndentation(attributes);

        ParagraphType newType = isIndentation && isOrderedList ? ParagraphType.INDENTATION_OL :
                isIndentation && !isOrderedList ? ParagraphType.INDENTATION_UL :
                        isOrderedList ? ParagraphType.NUMBERING :
                                ParagraphType.BULLET;

        AccumulatedParagraphStyle currentStyle = mParagraphStyles.isEmpty() ? null : mParagraphStyles.peek();

        if (currentStyle == null) {
            // no previous style found -> create new AccumulatedParagraphStyle with indentations of 1
            AccumulatedParagraphStyle newStyle = new AccumulatedParagraphStyle(newType, 1, 1);
            mParagraphStyles.push(newStyle);
        } else if (currentStyle.getType() == newType) {
            // same style found -> increase indentations by 1
            currentStyle.setAbsoluteIndent(currentStyle.getAbsoluteIndent() + 1);
            currentStyle.setRelativeIndent(currentStyle.getRelativeIndent() + 1);
        } else {
            // different style found -> create new AccumulatedParagraphStyle with incremented indentations
            AccumulatedParagraphStyle newStyle = new AccumulatedParagraphStyle(newType, currentStyle.getAbsoluteIndent() + 1, 1);
            mParagraphStyles.push(newStyle);
        }
    }

    /**
     * Handles OL and UL end tags
     */
    private void endList(boolean orderedList) {
        if (!mParagraphStyles.isEmpty()) {
            AccumulatedParagraphStyle style = mParagraphStyles.peek();
            ParagraphType type = style.getType();

            if ((orderedList && (type.isNumbering() || type == ParagraphType.INDENTATION_OL)) ||
                    (!orderedList && (type.isBullet() || type == ParagraphType.INDENTATION_UL))) {

                // the end tag matches the current style
                int indent = style.getRelativeIndent();
                if (indent > 1) {
                    style.setRelativeIndent(indent - 1);
                    style.setAbsoluteIndent(style.getAbsoluteIndent() - 1);
                } else {
                    mParagraphStyles.pop();
                }

            } else {

                // the end tag doesn't match the current style
                mParagraphStyles.pop();
                endList(orderedList);    // find the next matching style

            }
        }
    }

    /**
     * Handles LI tags
     */
    private void startList(Attributes attributes) {
        List listTag = null;

        if (!mParagraphStyles.isEmpty()) {
            AccumulatedParagraphStyle currentStyle = mParagraphStyles.peek();
            ParagraphType type = currentStyle.getType();
            int indent = currentStyle.getAbsoluteIndent();
            boolean isIndentation = isIndentation(attributes);

            if (type.isIndentation() || isIndentation) {
                listTag = new UL(indent, true);
            } else if (type.isNumbering()) {
                listTag = new OL(indent, false);
            } else if (type.isBullet()) {
                listTag = new UL(indent, false);
            }
        } else {
            listTag = new UL(0, false);
        }

        if (listTag != null) start(listTag);
    }

    /**
     * Handles LI tags
     */
    private void endList() {
        List list = (List) getLast(List.class);
        if (list != null) {
            if (mResult.length() == 0 || mResult.charAt(mResult.length() - 1) != '\n') {
                mResult.append('\n');
            }
            int start = mResult.getSpanStart(list);
            int end = mResult.length();

            int nrOfIndents = list.mNrOfIndents;
            if (!list.mIsIndentation) {
                nrOfIndents--;
                int margin = Helper.getLeadingMarging();
                // use SPAN_EXCLUSIVE_EXCLUSIVE here, will be replaced later anyway when the cleanup function is called
                Object span = list instanceof UL ?
                        new BulletSpan(margin, start == end, false, false) :
                        new NumberSpan(1, margin, start == end, false, false);
                mResult.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (nrOfIndents > 0) {
                int margin = nrOfIndents * Helper.getLeadingMarging();
                // use SPAN_EXCLUSIVE_EXCLUSIVE here, will be replaced later anyway when the cleanup function is called
                IndentationSpan span = new IndentationSpan(margin, start == end, false, false);
                mResult.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            mResult.removeSpan(list);
        }
    }

    private boolean isIndentation(Attributes attributes) {
        String style = attributes.getValue("style");
        return style != null && style.toLowerCase(Locale.US).contains("list-style-type:none");
    }

    private boolean checkDuplicateSpan(SpannableStringBuilder text, int where, Class<?> kind) {
        Object[] spans = text.getSpans(where, where, kind);
        if (spans != null && spans.length > 0) {
            for (int i = 0; i < spans.length; i++) {
                if (text.getSpanStart(spans[i]) == where) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object getLast(Spanned text, Class<?> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        Object[] objs = text.getSpans(0, text.length(), kind);
        return objs.length == 0 ? null : objs[objs.length - 1];
    }

    private void handleP() {
        int len = mResult.length();
        if (len >= 1 && mResult.charAt(len - 1) == '\n') {
            if (len < 2 || mResult.charAt(len - 2) != '\n') {
                mResult.append("\n");
            }
        } else if (len != 0) {
            mResult.append("\n\n");
        }
    }

    private void handleBr() {
        mResult.append("\n");
    }

    private Object getLast(Class<? extends Object> kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        Object[] objs = mResult.getSpans(0, mResult.length(), kind);
        return objs.length == 0 ? null : objs[objs.length - 1];
    }

    private void start(Object mark) {
        int len = mResult.length();
        mResult.setSpan(mark, len, len, Spanned.SPAN_MARK_MARK);
    }

    private void end(Class<? extends Object> kind, Object repl) {
        int len = mResult.length();
        Object obj = getLast(kind);
        int where = mResult.getSpanStart(obj);

        mResult.removeSpan(obj);

        if (where != len) {
            // Note: use SPAN_EXCLUSIVE_EXCLUSIVE, the TemporarySpan will be replaced by a SPAN_EXCLUSIVE_INCLUSIVE span
            mResult.setSpan(new TemporarySpan(repl), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void startImg(Attributes attributes) {
        int len = mResult.length();
        String src = attributes.getValue("", "src");
        RTImage image = mMediaFactory.createImage(src);

        if (image != null && image.exists()) {
            String path = image.getFilePath(RTFormat.SPANNED);
            File file = new File(path);
            if (file.isDirectory()) {
                // there were crashes when an image was a directory all of a sudden...
                // the root cause is unknown and this is a desparate work around
                return;
            }

            // Unicode Character 'OBJECT REPLACEMENT CHARACTER' (U+FFFC)
            // see http://www.fileformat.info/info/unicode/char/fffc/index.htm
            mResult.append("\uFFFC");
            ImageSpan imageSpan = new ImageSpan(image, true);
            mResult.setSpan(imageSpan, len, len + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void startVideo(Attributes attributes) {
    }

    private void startAudio(Attributes attributes) {
    }

    /*
     * Examples:
     * <font style="font-size:25px;background-color:#00ff00;color:#ff0000">This is heading 1</font>
     * <font style="font-size:50px;background-color:#0000FF;color:#FFFF00">This is heading 2</font>
     */
    private static final Pattern FONT_SIZE = Pattern.compile("\\d+");
    private static final Pattern FONT_COLOR = Pattern.compile("#[a-f0-9]+");

    private void startFont(Attributes attributes) {
        int size = Integer.MIN_VALUE;
        String fgColor = null;
        String bgColor = null;
        String fontName = null;

        String style = attributes.getValue("", "style");
        if (style != null) {
            for (String part : style.toLowerCase(Locale.ENGLISH).split(";")) {
                if (part.startsWith("font-size")) {
                    Matcher matcher = FONT_SIZE.matcher(part);
                    if (matcher.find(0)) {
                        int start = matcher.start();
                        int end = matcher.end();
                        try {
                            size = Integer.parseInt(part.substring(start, end));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                } else if (part.startsWith("color")) {
                    Matcher matcher = FONT_COLOR.matcher(part);
                    if (matcher.find(0)) {
                        int start = matcher.start();
                        int end = matcher.end();
                        fgColor = part.substring(start, end);
                    }
                } else if (part.startsWith("background-color")) {
                    Matcher matcher = FONT_COLOR.matcher(part);
                    if (matcher.find(0)) {
                        int start = matcher.start();
                        int end = matcher.end();
                        bgColor = part.substring(start, end);
                    }
                }
            }
        }

        fontName = attributes.getValue("", "face");

        int len = mResult.length();
        Font font = new Font()
                .setSize(size)
                .setFGColor(fgColor)
                .setBGColor(bgColor)
                .setFontFace(fontName);
        mResult.setSpan(font, len, len, Spanned.SPAN_MARK_MARK);
    }

    private void endFont() {
        int len = mResult.length();
        Object obj = getLast(Font.class);
        int where = mResult.getSpanStart(obj);

        mResult.removeSpan(obj);

        if (where != len) {
            Font font = (Font) obj;

            // font type face
            if (font.hasFontFace()) {
                // Note: use SPAN_EXCLUSIVE_EXCLUSIVE, the TemporarySpan will be replaced by a SPAN_EXCLUSIVE_INCLUSIVE span
                RTTypeface typeface = FontManager.getTypeface(font.mFontFace);
                if (typeface != null) {
                    TemporarySpan span = new TemporarySpan(new TypefaceSpan(typeface));
                    mResult.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // text size
            if (font.hasSize()) {
                // Note: use SPAN_EXCLUSIVE_EXCLUSIVE, the TemporarySpan will later be replaced by a SPAN_EXCLUSIVE_INCLUSIVE span
                int size = Helper.convertPxToSp(font.mSize);
                TemporarySpan span = new TemporarySpan(new AbsoluteSizeSpan(size));
                mResult.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // font color
            if (font.hasFGColor()) {
                int c = getHtmlColor(font.mFGColor);
                if (c != -1) {
                    // Note: use SPAN_EXCLUSIVE_EXCLUSIVE, the TemporarySpan will be replaced by a SPAN_EXCLUSIVE_INCLUSIVE span
                    TemporarySpan span = new TemporarySpan(new ForegroundColorSpan(c | 0xFF000000));
                    mResult.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // font background color
            if (font.hasBGColor()) {
                int c = getHtmlColor(font.mBGColor);
                if (c != -1) {
                    // Note: use SPAN_EXCLUSIVE_EXCLUSIVE, the TemporarySpan will be replaced by a SPAN_EXCLUSIVE_INCLUSIVE span
                    TemporarySpan span = new TemporarySpan(new BackgroundColorSpan(c | 0xFF000000));
                    mResult.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private void startAHref(Attributes attributes) {
        String href = attributes.getValue("", "href");
        int len = mResult.length();
        mResult.setSpan(new Href(href), len, len, Spanned.SPAN_MARK_MARK);
    }

    private void endAHref() {
        int len = mResult.length();
        Object obj = getLast(Href.class);
        int where = mResult.getSpanStart(obj);

        mResult.removeSpan(obj);

        if (where != len) {
            Href h = (Href) obj;
            if (h.mHref != null) {
                mResult.setSpan(new LinkSpan(h.mHref), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void endHeader() {
        int len = mResult.length();
        Object obj = getLast(Header.class);

        int where = mResult.getSpanStart(obj);

        mResult.removeSpan(obj);

        // Back off not to change only the text, not the blank line.
        while (len > where && mResult.charAt(len - 1) == '\n') {
            len--;
        }

        if (where != len) {
            Header h = (Header) obj;
            mResult.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mResult.setSpan(new BoldSpan(), where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    // ****************************************** Helper Data Structures *******************************************

    /*
     * While the spanned text is build we need to use SPAN_EXCLUSIVE_EXCLUSIVE instead of SPAN_EXCLUSIVE_INCLUSIVE
     * or each span would expand to the end of the text as we append more text.
     * Therefore we use a TemporarySpan which will be replaced by the "real" span once the full spanned text is built.
     */
    private static class TemporarySpan {
        Object mSpan;

        TemporarySpan(Object span) {
            mSpan = span;
        }

        void swapIn(SpannableStringBuilder builder) {
            int start = builder.getSpanStart(this);
            int end = builder.getSpanEnd(this);
            builder.removeSpan(this);
            if (start >= 0 && end > start && end <= builder.length()) {
                builder.setSpan(mSpan, start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
        }
    }

    private static class Div {
        String mAlign = "left";

        Div(String align) {
            if (align != null) mAlign = align;
        }
    }

    private static class Bold {}
    private static class Italic {}
    private static class Underline {}
    private static class Strikethrough {}
    private static class Super {}
    private static class Sub {}
    private static class Big {}
    private static class Small {}
    private static class Monospace {}
    private static class Blockquote {}

    private abstract static class List {
        int mNrOfIndents;
        boolean mIsIndentation;

        List(int nrOfIndents, boolean isIndentation) {
            mNrOfIndents = nrOfIndents;
            mIsIndentation = isIndentation;
        }
    }

    private static class UL extends List {
        UL(int nrOfIndents, boolean isIndentation) {
            super(nrOfIndents, isIndentation);
        }
    }

    private static class OL extends List {
        OL(int nrOfIndents, boolean isIndentation) {
            super(nrOfIndents, isIndentation);
        }
    }

    private static class Font {
        int mSize = Integer.MIN_VALUE;
        String mFGColor;
        String mBGColor;
        String mFontFace;

        Font setSize(int size) {
            mSize = size;
            return this;
        }

        Font setFGColor(String color) {
            mFGColor = color;
            return this;
        }

        Font setBGColor(String color) {
            mBGColor = color;
            return this;
        }
        
        private Font setFontFace(String fontFace) {
            mFontFace = fontFace;
            return this;
        }

        boolean hasSize() {
            return mSize > 0;
        }

        boolean hasFGColor() {
            return !TextUtils.isEmpty(mFGColor);
        }

        boolean hasBGColor() {
            return !TextUtils.isEmpty(mBGColor);
        }

        boolean hasFontFace() {
            return !TextUtils.isEmpty(mFontFace);
        }
    }

    private static class Href {
        String mHref;

        Href(String href) {
            mHref = href;
        }
    }

    private static class Header {
        int mLevel;

        Header(int level) {
            mLevel = level;
        }
    }

    // ****************************************** Color Methods *******************************************

    private static HashMap<String, Integer> COLORS = new HashMap<String, Integer>();

    static {
        COLORS.put("aqua", 0x00FFFF);
        COLORS.put("black", 0x000000);
        COLORS.put("blue", 0x0000FF);
        COLORS.put("fuchsia", 0xFF00FF);
        COLORS.put("green", 0x008000);
        COLORS.put("grey", 0x808080);
        COLORS.put("lime", 0x00FF00);
        COLORS.put("maroon", 0x800000);
        COLORS.put("navy", 0x000080);
        COLORS.put("olive", 0x808000);
        COLORS.put("purple", 0x800080);
        COLORS.put("red", 0xFF0000);
        COLORS.put("silver", 0xC0C0C0);
        COLORS.put("teal", 0x008080);
        COLORS.put("white", 0xFFFFFF);
        COLORS.put("yellow", 0xFFFF00);
    }

    /**
     * Converts an HTML color (named or numeric) to an integer RGB value.
     *
     * @param color Non-null color string.
     * @return A color value, or {@code -1} if the color string could not be interpreted.
     */
    @SuppressLint("DefaultLocale")
    private static int getHtmlColor(String color) {
        Integer i = COLORS.get(color.toLowerCase());
        if (i != null) {
            return i;
        } else {
            try {
                return convertValueToInt(color, -1);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }
    }

    private static final int convertValueToInt(CharSequence charSeq, int defaultValue) {
        if (null == charSeq)
            return defaultValue;

        String nm = charSeq.toString();

        // XXX This code is copied from Integer.decode() so we don't
        // have to instantiate an Integer!

        int sign = 1;
        int index = 0;
        int len = nm.length();
        int base = 10;

        if ('-' == nm.charAt(0)) {
            sign = -1;
            index++;
        }

        if ('0' == nm.charAt(index)) {
            // Quick check for a zero by itself
            if (index == (len - 1))
                return 0;

            char c = nm.charAt(index + 1);

            if ('x' == c || 'X' == c) {
                index += 2;
                base = 16;
            } else {
                index++;
                base = 8;
            }
        } else if ('#' == nm.charAt(index)) {
            index++;
            base = 16;
        }

        return Integer.parseInt(nm.substring(index), base) * sign;
    }

}