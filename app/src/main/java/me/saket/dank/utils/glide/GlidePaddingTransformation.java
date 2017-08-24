package me.saket.dank.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.Size;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

/**
 * Adds empty spaces to inflate the height of images.
 */
public abstract class GlidePaddingTransformation extends BitmapTransformation {
  private static final String ID = "me.saket.dank.utils.glide.GlidePaddingTransformation";
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  private final BitmapPool bitmapPool;
  private final Paint paint;

  public GlidePaddingTransformation(Context context, int paddingColor) {
    this(Glide.get(context).getBitmapPool(), paddingColor);
  }

  public GlidePaddingTransformation(BitmapPool pool, int paddingColor) {
    this.bitmapPool = pool;
    paint = new Paint();
    paint.setColor(paddingColor);
  }

  public abstract Size getPadding(int imageWidth, int imageHeight);

  @Override
  protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
    Size padding = getPadding(toTransform.getWidth(), toTransform.getHeight());
    int verticalPadding = padding.getHeight();
    int horizontalPadding = padding.getWidth();

    if (verticalPadding == 0 && horizontalPadding == 0) {
      // Nothing to do here.
      return toTransform;
    }

    int targetWidth = toTransform.getWidth() + horizontalPadding * 2;
    int targetHeight = toTransform.getHeight() + verticalPadding * 2;

    Bitmap bitmapWithPadding = bitmapPool.get(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmapWithPadding);

    canvas.drawRect(0, 0, targetWidth, targetHeight, paint);              // Padding.
    if (!toTransform.isRecycled()) {
      canvas.drawBitmap(toTransform, horizontalPadding, verticalPadding, null);  // Original bitmap.
    }
    return bitmapWithPadding;
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }
}
