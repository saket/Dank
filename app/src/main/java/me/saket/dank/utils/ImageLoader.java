package me.saket.dank.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;

/**
 * Wrapper around our image loading library so that we can easily swith libraries in the future.
 */
public class ImageLoader {

    private void load(Context context, String imageUrl, ImageView imageView, boolean circular) {
        DrawableTypeRequest<String> loadRequest = Glide.with(context).load(imageUrl);
        if (circular) {
            loadRequest.bitmapTransform(new GlideCircularTransformation(context));
        }
        loadRequest.into(imageView);
    }

    public void load(Context context, String imageUrl, ImageView imageView) {
        load(context, imageUrl, imageView, false);
    }

    public void loadCircular(Context context, String imageUrl, ImageView imageView) {
        load(context, imageUrl, imageView, true);
    }

}
