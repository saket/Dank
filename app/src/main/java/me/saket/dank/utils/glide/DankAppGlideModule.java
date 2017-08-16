package me.saket.dank.utils.glide;

import com.bumptech.glide.annotation.Excludes;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Glide requires atleast one app module if library modules are used.
 */
@GlideModule
@Excludes({ OkHttpLibraryGlideModule.class })
public class DankAppGlideModule extends AppGlideModule {

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
