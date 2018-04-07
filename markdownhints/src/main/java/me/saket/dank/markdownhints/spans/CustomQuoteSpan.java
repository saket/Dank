/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.saket.dank.markdownhints.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.Px;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;

/**
 * Copied from {@link QuoteSpan} with customizable rule color and margin.
 */
public class CustomQuoteSpan implements LeadingMarginSpan {

  private final @ColorInt int indentationRuleColor;
  private final @Px int indentationMargin;
  private final @Px int verticalRuleStrokeWidth;

  public CustomQuoteSpan(@ColorInt int indentationRuleColor, @Px int indentationMargin, @Px int verticalRuleStrokeWidth) {
    this.indentationRuleColor = indentationRuleColor;
    this.indentationMargin = indentationMargin;
    this.verticalRuleStrokeWidth = verticalRuleStrokeWidth;
  }

  public int getLeadingMargin(boolean first) {
    return verticalRuleStrokeWidth + indentationMargin;
  }

  public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
      int top, int baseline, int bottom,
      CharSequence text, int start, int end,
      boolean first, Layout layout)
  {
    Paint.Style style = p.getStyle();
    int color = p.getColor();

    p.setStyle(Paint.Style.FILL);
    p.setColor(this.indentationRuleColor);

    c.drawRect(x, top, x + dir * verticalRuleStrokeWidth, bottom, p);

    p.setStyle(style);
    p.setColor(color);
  }

  @ColorInt
  public int getIndentationRuleColor() {
    return indentationRuleColor;
  }

  @Px
  public int getIndentationMargin() {
    return indentationMargin;
  }

  @Px
  public int getVerticalRuleStrokeWidth() {
    return verticalRuleStrokeWidth;
  }
}
