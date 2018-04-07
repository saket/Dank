package me.saket.dank.utils.markdown;

import android.app.Application;
import android.arch.core.util.Function;
import android.support.v4.content.ContextCompat;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.saket.dank.R;
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownSpanPool;

@Module
public class MarkdownModule {

  @Provides
  @Singleton
  static MarkdownSpanPool provideMarkdownSpanPool() {
    return new MarkdownSpanPool();
  }

  @Provides
  static MarkdownHintOptions provideMarkdownHintOptions(Application appContext) {
    Function<Integer, Integer> colors = resId -> ContextCompat.getColor(appContext, resId);
    Function<Integer, Integer> dimens = resId -> appContext.getResources().getDimensionPixelSize(resId);

    return MarkdownHintOptions.builder()
        .syntaxColor(colors.apply(R.color.markdown_syntax))
        .blockQuoteIndentationRuleColor(colors.apply(R.color.markdown_blockquote_indentation_rule))
        .blockQuoteTextColor(colors.apply(R.color.markdown_blockquote_text))
        .blockQuoteVerticalRuleStrokeWidth(dimens.apply(R.dimen.markdown_blockquote_vertical_rule_stroke_width))
        .linkUrlColor(colors.apply(R.color.markdown_link_url))
        .textBlockIndentationMargin(dimens.apply(R.dimen.markdown_text_block_indentation_margin))
        .horizontalRuleColor(colors.apply(R.color.markdown_horizontal_rule))
        .horizontalRuleStrokeWidth(dimens.apply(R.dimen.markdown_horizontal_rule_stroke_width))
        .build();
  }
}
