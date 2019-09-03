package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.f2prateek.rx.preferences2.Preference;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import me.saket.dank.R;
import me.saket.dank.di.Dank;

public class IndentedLayout extends LinearLayout {
  private static final int DEFAULT_SPACING_PER_DEPTH_DP = 10;
  private static final int DEFAULT_LINE_WIDTH = 6;

  private final Paint indentationLinePaint;
  private final int defaultIndentationLineColor;
  private final int originalPaddingStart;
  private final int spacePerDepthPx;
  private final int indentationLineWidth;
  private final int[] indentationColors;

  private int indentationDepth;
  private List<ColoredTree> trees = new ArrayList<>();
  @Inject @Named("show_colored_comments_tree") Preference<Boolean> coloredDepthPreference;

  public IndentedLayout(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setWillNotDraw(false);

    Dank.dependencyInjector().inject(this);

    coloredDepthPreference.asObservable()
        .skip(1)
        .takeUntil(RxView.detaches(this))
        .subscribe(colored -> {
          updateLineColors(colored);
          invalidate();
        });

    originalPaddingStart = getPaddingStart();
    indentationColors = getResources().getIntArray(R.array.indentation_colors);

    TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.IndentedLayout);
    spacePerDepthPx = attributes.getDimensionPixelSize(R.styleable.IndentedLayout_spacePerDepth, dpToPx(DEFAULT_SPACING_PER_DEPTH_DP, context));
    indentationLineWidth = attributes.getDimensionPixelSize(R.styleable.IndentedLayout_indentationLineWidth, dpToPx(DEFAULT_LINE_WIDTH, context));
    defaultIndentationLineColor = attributes.getColor(R.styleable.IndentedLayout_indentationLineColor, Color.LTGRAY);
    attributes.recycle();

    // Using a Path so that dashes can be rendered
    indentationLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    indentationLinePaint.setStrokeWidth(indentationLineWidth);
    indentationLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
    indentationLinePaint.setPathEffect(new DashPathEffect(new float[] { indentationLineWidth * 2, indentationLineWidth * 2 }, 0));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);

    for (int i = 0; i < indentationDepth; i++) {
      float lineStartX = spacePerDepthPx * (i + 1) + indentationLinePaint.getStrokeWidth();

      ColoredTree tree = trees.get(i);

      tree.path.reset();
      tree.path.moveTo(lineStartX, 0);
      tree.path.lineTo(lineStartX, getHeight());
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    for (ColoredTree tree : trees) {
      indentationLinePaint.setColor(tree.color);
      canvas.drawPath(tree.path, indentationLinePaint);
    }
  }

  public void setIndentationDepth(int depth) {
    indentationDepth = depth;

    setupDepthLines(coloredDepthPreference.get());

    int indentationSpacing = (int) (indentationDepth * spacePerDepthPx + indentationLinePaint.getStrokeWidth());
    setPaddingRelative(originalPaddingStart + indentationSpacing, getPaddingTop(), getPaddingEnd(), getPaddingBottom());

    invalidate();
  }

  private void setupDepthLines(Boolean colored) {
    trees = new ArrayList<>();

    for (int i = 0; i < indentationDepth; i++) {
      int depthColor = getDepthColor(i, colored);
      trees.add(new ColoredTree(depthColor, new Path()));
    }
  }

  private int getDepthColor(int index, Boolean shouldBeColored) {
    int colorIndex = index % indentationColors.length;
    return shouldBeColored ? indentationColors[colorIndex] : defaultIndentationLineColor;
  }

  private void updateLineColors(Boolean colored) {
    for (int i = 0; i < trees.size(); i++) {
      trees.get(i).color = getDepthColor(i, colored);
    }
  }

  @Px
  public static int dpToPx(float dpValue, Context context) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
  }

  private static class ColoredTree {
    public int color;
    public final Path path;

    public ColoredTree(int color, Path path) {
      this.color = color;
      this.path = path;
    }
  }
}
