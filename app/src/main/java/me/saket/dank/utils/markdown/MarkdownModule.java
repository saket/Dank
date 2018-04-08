package me.saket.dank.utils.markdown;

import android.app.Application;
import android.support.v4.content.ContextCompat;

import com.commonsware.cwac.anddown.AndDown;
import com.nytimes.android.external.cache3.Cache;
import com.nytimes.android.external.cache3.CacheBuilder;

import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.saket.dank.R;
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownSpanPool;
import me.saket.dank.utils.SafeFunction;

@Module
public class MarkdownModule {

  @Provides
  @Singleton
  static MarkdownSpanPool provideMarkdownSpanPool() {
    return new MarkdownSpanPool();
  }

  @Provides
  static MarkdownHintOptions provideMarkdownHintOptions(Application appContext) {
    SafeFunction<Integer, Integer> colors = resId -> ContextCompat.getColor(appContext, resId);
    SafeFunction<Integer, Integer> dimens = resId -> appContext.getResources().getDimensionPixelSize(resId);

    return MarkdownHintOptions.builder()
        .syntaxColor(colors.apply(R.color.markdown_syntax))

        .blockQuoteIndentationRuleColor(colors.apply(R.color.markdown_blockquote_indentation_rule))
        .blockQuoteTextColor(colors.apply(R.color.markdown_blockquote_text))
        .blockQuoteVerticalRuleStrokeWidth(dimens.apply(R.dimen.markdown_blockquote_vertical_rule_stroke_width))

        .linkUrlColor(colors.apply(R.color.markdown_link_url))
        .linkTextColor(colors.apply(R.color.markdown_link_text))
        .spoilerTextColor(colors.apply(R.color.markdown_spoiler_text))

        .textBlockIndentationMargin(dimens.apply(R.dimen.markdown_text_block_indentation_margin))
        .listMarginBetweenListIndicatorAndText(dimens.apply(R.dimen.markdown_list_margin_with_list_indicator))

        .horizontalRuleColor(colors.apply(R.color.markdown_horizontal_rule))
        .horizontalRuleStrokeWidth(dimens.apply(R.dimen.markdown_horizontal_rule_stroke_width))

        .inlineCodeBackgroundColor(colors.apply(R.color.markdown_inline_code_background))
        .indentedCodeBlockBackgroundColor(colors.apply(R.color.markdown_indented_code_background))
        .indentedCodeBlockBackgroundRadius(dimens.apply(R.dimen.markdown_indented_code_background_radius))

        .build();
  }

  @Provides
  @Singleton
  @Named("markdown_from_html")
  static Cache<String, CharSequence> markdownCache() {
    return CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();
  }

  @Provides
  @Singleton
  @Named("markdown_from_markdown")
  static Cache<String, String> provideMarkdownCache() {
    return CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();
  }

  @Provides
  @Singleton
  @Named("markdown_to_html")
  static SafeFunction<String, String> markdownToHtmlParser() {
    AndDown andDown = new AndDown();
    return textWithMarkdown -> andDown.markdownToHtml(
        textWithMarkdown,
        AndDown.HOEDOWN_EXT_FENCED_CODE
            | AndDown.HOEDOWN_EXT_STRIKETHROUGH
            | AndDown.HOEDOWN_EXT_UNDERLINE
            | AndDown.HOEDOWN_EXT_HIGHLIGHT
            | AndDown.HOEDOWN_EXT_QUOTE
            | AndDown.HOEDOWN_EXT_SUPERSCRIPT
            | AndDown.HOEDOWN_EXT_SPACE_HEADERS
            | AndDown.HOEDOWN_EXT_TABLES,
        0);
  }

  @Provides
  static HtmlToSpansParser htmlToSpansParser(AndroidBasedHtmlToSpansParser androidBasedParser) {
    return androidBasedParser;
  }
}
