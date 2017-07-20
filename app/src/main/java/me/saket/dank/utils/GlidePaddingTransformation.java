package me.saket.dank.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

/**
 * Adds empty spaces to inflate the height of images.
 */
public abstract class GlidePaddingTransformation implements Transformation<Bitmap> {
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

  public abstract int getVerticalPadding(int imageWidth, int imageHeight);

  @Override
  public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
    Bitmap source = resource.get();

    int verticalPadding = getVerticalPadding(source.getWidth(), source.getHeight());

    if (verticalPadding == 0) {
      // Nothing to do here.
      return BitmapResource.obtain(source, bitmapPool);
    }

    int targetWidth = source.getWidth();
    int targetHeight = source.getHeight() + verticalPadding * 2;

    Bitmap bitmap = bitmapPool.get(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
    if (bitmap == null) {
      bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
    }

    Canvas canvas = new Canvas(bitmap);

    // Draw original image.
    canvas.drawBitmap(source, 0, verticalPadding, null);

    // Draw paddings.
    canvas.drawRect(0, 0, targetWidth, verticalPadding, paint);
    int bottomPaddingStartY = verticalPadding + source.getHeight() + 1;
    canvas.drawRect(0, bottomPaddingStartY, targetWidth, bottomPaddingStartY + verticalPadding, paint);

    return BitmapResource.obtain(bitmap, bitmapPool);
  }

  @Override
  public String getId() {
    return "GlidePaddingTransformation()";
  }
}
