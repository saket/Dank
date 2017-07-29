package me.saket.dank.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Size;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import timber.log.Timber;

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

  public abstract Size getPadding(int imageWidth, int imageHeight);

  @Override
  public Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
    Bitmap source = resource.get();

    Size padding = getPadding(source.getWidth(), source.getHeight());
    int verticalPadding = padding.getHeight();
    int horizontalPadding = padding.getWidth();

    if (verticalPadding == 0 && horizontalPadding == 0) {
      // Nothing to do here.
      return BitmapResource.obtain(source, bitmapPool);
    }

    int targetWidth = source.getWidth() + horizontalPadding * 2;
    int targetHeight = source.getHeight() + verticalPadding * 2;

    Bitmap bitmap = bitmapPool.get(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
    if (bitmap == null) {
      bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
    }

    Canvas canvas = new Canvas(bitmap);

    canvas.drawRect(0, 0, targetWidth, targetHeight, paint);              // Padding.
    if (!source.isRecycled()) {
      canvas.drawBitmap(source, horizontalPadding, verticalPadding, null);  // Original bitmap.
    }
    return BitmapResource.obtain(bitmap, bitmapPool);
  }

  @Override
  public String getId() {
    return "GlidePaddingTransformation()";
  }
}
