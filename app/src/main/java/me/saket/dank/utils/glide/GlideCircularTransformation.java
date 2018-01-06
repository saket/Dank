package me.saket.dank.utils.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class GlideCircularTransformation extends BitmapTransformation {
  private static final String ID = "me.saket.dank.utils.glide.GlideCircularTransformation";
  private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

  public static final GlideCircularTransformation INSTANCE = new GlideCircularTransformation();

  @Override
  protected Bitmap transform(BitmapPool bitmapPool, Bitmap toTransform, int outWidth, int outHeight) {
    int size = Math.min(toTransform.getWidth(), toTransform.getHeight());

    int width = (toTransform.getWidth() - size) / 2;
    int height = (toTransform.getHeight() - size) / 2;

    Bitmap circularBitmap = bitmapPool.get(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(circularBitmap);
    Paint paint = new Paint();

    BitmapShader shader = new BitmapShader(toTransform, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
    if (width != 0 || height != 0) {
      // source isn't square, move viewport to center
      Matrix matrix = new Matrix();
      matrix.setTranslate(-width, -height);
      shader.setLocalMatrix(matrix);
    }
    paint.setShader(shader);
    paint.setAntiAlias(true);

    float r = size / 2f;
    canvas.drawCircle(r, r, r, paint);

    return circularBitmap;
  }

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {
    messageDigest.update(ID_BYTES);
  }
}
