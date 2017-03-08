package me.saket.dank.utils;

import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;

import org.sufficientlysecure.htmltextview.HtmlTagHandler;
import org.xml.sax.XMLReader;

import java.util.Stack;
import java.util.Vector;

import timber.log.Timber;

/**
 *
 */
@SuppressWarnings({ "StatementWithEmptyBody", "deprecation" })
public class Markdown {

    public static CharSequence parseRedditMarkdownHtml(String markdown, TextPaint textPaint) {
        String source = Html.fromHtml(markdown).toString();
        Timber.i("source: %s", source);

        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler(textPaint);
//        htmlTagHandler.setClickableTableSpan(clickableTableSpan);
//        htmlTagHandler.setDrawTableLinkSpan(drawTableLinkSpan);

        source = overrideTags(source);
        Spanned spanned = Html.fromHtml(source, null, htmlTagHandler);
        return trimTrailingWhitespace(spanned);
    }

    /**
     * Newer versions of the Android SDK's {@link Html.TagHandler} handles &lt;ul&gt; and &lt;li&gt;
     * tags itself which means they never get delegated to this class. We want to handle the tags
     * ourselves so before passing the string html into Html.fromHtml(), we can use this method to
     * replace the &lt;ul&gt; and &lt;li&gt; tags with tags of our own.
     *
     * @param html String containing HTML, for example: "<b>Hello world!</b>"
     * @return html with replaced <ul> and <li> tags
     * @see <a href="https://github.com/android/platform_frameworks_base/commit/8b36c0bbd1503c61c111feac939193c47f812190">Specific Android SDK Commit</a>
     */
    static String overrideTags(@Nullable String html) {
        if (html == null) {
            return null;
        }

        html = html.replace("<ul", "<" + HtmlTagHandler.UNORDERED_LIST);
        html = html.replace("</ul>", "</" + HtmlTagHandler.UNORDERED_LIST + ">");
        html = html.replace("<ol", "<" + HtmlTagHandler.ORDERED_LIST);
        html = html.replace("</ol>", "</" + HtmlTagHandler.ORDERED_LIST + ">");
        html = html.replace("<li", "<" + HtmlTagHandler.LIST_ITEM);
        html = html.replace("</li>", "</" + HtmlTagHandler.LIST_ITEM + ">");

        return html;
    }

    /**
     * @return "" if source is null, otherwise string with all trailing whitespace removed
     */
    public static CharSequence trimTrailingWhitespace(CharSequence source) {
        if (source == null) {
            return null;
        }

        // Loop back to the first non-whitespace character.
        int len = source.length();
        while (--len >= 0 && Character.isWhitespace(source.charAt(len))) {
        }
        return source.subSequence(0, len + 1);
    }

    public static class MyTagHandler implements TagHandler {
        boolean first = true;
        String parent = null;
        int index = 1;

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equals("ul")) {
                parent = "ul";

            } else if (tag.equals("ol")) {
                parent = "ol";
            }
            if (tag.equals("li")) {
                if (parent.equals("ul")) {
                    if (first) {
                        output.append("\n\t•");
                        first = false;
                    } else {
                        first = true;
                    }
                } else {
                    if (first) {
                        output.append("\n\t").append(String.valueOf(index)).append(". ");
                        first = false;
                        index++;
                    } else {
                        first = true;
                    }
                }
            }
        }
    }

    /*
 * HTML.formHtml() Source Code :
 * http://www.netmite.com/android/mydroid/frameworks/base/core/java/android/text/Html.java
 * http://stackoverflow.com/questions/3150400/html-list-tag-not-working-in-android-textview-what-can-i-do/16169511#16169511
 * http://mohammedlakkadshaw.com/blog/Handling_Custom_Tags_Using_Html.tagHandler().html#.V9FpmRB94Y3
 */
    public static class HtmlTagHan2dler implements Html.TagHandler {
        private int mListItemCount = 0;
        private Vector<String> mListParents = new Vector<String>();

        @Override
        public void handleTag(final boolean opening, final String tag, Editable output, final XMLReader xmlReader) {

            if (tag.equals("ul") || tag.equals("ol") || tag.equals("dd")) {
                if (opening) {
                    mListParents.add(tag);
                } else
                    mListParents.remove(tag);

                mListItemCount = 0;
            } else if (tag.equals("li") && !opening) {
                handleListTag(output);
            } else if (tag.equalsIgnoreCase("code")) {
                if (opening) {
                    output.setSpan(new TypefaceSpan("monospace"), output.length(), output.length(), Spannable.SPAN_MARK_MARK);
                } else {
                    Log.d("COde Tag", "Code tag encountered");
                    Object obj = getLast(output, TypefaceSpan.class);
                    int where = output.getSpanStart(obj);

                    output.setSpan(new TypefaceSpan("monospace"), where, output.length(), 0);
                }
            }

        }

        private Object getLast(Editable text, Class kind) {
            Object[] objs = text.getSpans(0, text.length(), kind);
            if (objs.length == 0) {
                return null;
            } else {
                for (int i = objs.length; i > 0; i--) {
                    if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                        return objs[i - 1];
                    }
                }
                return null;
            }
        }

        private void handleListTag(Editable output) {
            if (mListParents.lastElement().equals("ul")) {
                output.append("\n");
                String[] split = output.toString().split("\n");

                int lastIndex = split.length - 1;
                int start = output.length() - split[lastIndex].length() - 1;
                output.setSpan(new BulletSpan(15 * mListParents.size()), start, output.length(), 0);
            } else if (mListParents.lastElement().equals("ol")) {
                mListItemCount++;

                output.append("\n");
                String[] split = output.toString().split("\n");

                int lastIndex = split.length - 1;
                int start = output.length() - split[lastIndex].length() - 1;
                output.insert(start, mListItemCount + ". ");
                output.setSpan(new LeadingMarginSpan.Standard(15 * mListParents.size()), start, output.length(), 0);
            }
        }
    }

    /**
     * Implements support for ordered and unordered lists in to Android TextView.
     * <p>
     * Some code taken from inner class android.text.Html.HtmlToSpannedConverter. If you find this code useful,
     * please vote my answer at <a href="http://stackoverflow.com/a/17365740/262462">StackOverflow</a> up.
     */
    public static class MyTagHandler2 implements Html.TagHandler {
        /**
         * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list
         * and on top of Stack is the most nested list
         */
        Stack<String> lists = new Stack<>();
        /**
         * Tracks indexes of ordered lists so that after a nested list ends
         * we can continue with correct index of outer list
         */
        Stack<Integer> olNextIndex = new Stack<>();
        /**
         * List indentation in pixels. Nested lists use multiple of this.
         */
        private static final int indent = 10;
        private static final int listItemIndent = indent * 2;
        private static final BulletSpan bullet = new BulletSpan(indent);

        @Override
        public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
            if (tag.equalsIgnoreCase("ul")) {
                if (opening) {
                    lists.push(tag);
                } else {
                    lists.pop();
                }
            } else if (tag.equalsIgnoreCase("ol")) {
                if (opening) {
                    lists.push(tag);
                    olNextIndex.push(1).toString();//TODO: add support for lists starting other index than 1
                } else {
                    lists.pop();
                    olNextIndex.pop().toString();
                }
            } else if (tag.equalsIgnoreCase("li")) {
                if (opening) {
                    if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                        output.append("\n");
                    }
                    String parentList = lists.peek();
                    if (parentList.equalsIgnoreCase("ol")) {
                        start(output, new Ol());
                        output.append(olNextIndex.peek().toString()).append(". ");
                        olNextIndex.push(olNextIndex.pop() + 1);
                    } else if (parentList.equalsIgnoreCase("ul")) {
                        start(output, new Ul());
                    }
                } else {
                    if (lists.peek().equalsIgnoreCase("ul")) {
                        if (output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }
                        // Nested BulletSpans increases distance between bullet and text, so we must prevent it.
                        int bulletMargin = indent;
                        if (lists.size() > 1) {
                            bulletMargin = indent - bullet.getLeadingMargin(true);
                            if (lists.size() > 2) {
                                // This get's more complicated when we add a LeadingMarginSpan into the same line:
                                // we have also counter it's effect to BulletSpan
                                bulletMargin -= (lists.size() - 2) * listItemIndent;
                            }
                        }
                        BulletSpan newBullet = new BulletSpan(bulletMargin);
                        end(output,
                                Ul.class,
                                new LeadingMarginSpan.Standard(listItemIndent * (lists.size() - 1)),
                                newBullet);
                    } else if (lists.peek().equalsIgnoreCase("ol")) {
                        if (output.charAt(output.length() - 1) != '\n') {
                            output.append("\n");
                        }
                        int numberMargin = listItemIndent * (lists.size() - 1);
                        if (lists.size() > 2) {
                            // Same as in ordered lists: counter the effect of nested Spans
                            numberMargin -= (lists.size() - 2) * listItemIndent;
                        }
                        end(output,
                                Ol.class,
                                new LeadingMarginSpan.Standard(numberMargin));
                    }
                }
            } else {
                if (opening)
                    Log.d("TagHandler", "Found an unsupported tag " + tag);
            }
        }

        /** @see android.text.Html */
        private static void start(Editable text, Object mark) {
            int len = text.length();
            text.setSpan(mark, len, len, Spanned.SPAN_MARK_MARK);
        }

        /** Modified from {@link android.text.Html} */
        private static void end(Editable text, Class<?> kind, Object... replaces) {
            int len = text.length();
            Object obj = getLast(text, kind);
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where != len) {
                for (Object replace : replaces) {
                    text.setSpan(replace, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        /** @see android.text.Html */
        private static Object getLast(Spanned text, Class<?> kind) {
        /*
         * This knows that the last returned object from getSpans()
		 * will be the most recently added.
		 */
            Object[] objs = text.getSpans(0, text.length(), kind);
            if (objs.length == 0) {
                return null;
            }
            return objs[objs.length - 1];
        }

        private static class Ul {
        }

        private static class Ol {
        }

    }

}
