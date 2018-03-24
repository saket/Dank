package me.saket.dank.ui.compose;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankDialogFragment;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.utils.glide.GlideUtils;
import me.saket.dank.widgets.AnimatedProgressBar;
import timber.log.Timber;

public class InsertGifDialog extends DankDialogFragment {

  private static final String KEY_GIPHY_GIF = "giphyGif";

  @BindView(R.id.uploadimage_image) ImageView imageView;
  @BindView(R.id.uploadimage_file_size) TextView fileSizeView;
  @BindView(R.id.uploadimage_state_viewflipper) ViewFlipper stateViewFlipper;
  @BindView(R.id.uploadimage_upload_progress_bar) AnimatedProgressBar progressBar;
  @BindView(R.id.uploadimage_title) EditText titleField;
  @BindView(R.id.uploadimage_url) TextView urlView;
  @BindView(R.id.uploadimage_state_failed_tap_to_retry) TextView errorView;
  @BindView(R.id.uploadimage_insert) Button insertButton;

  public static void show(FragmentManager fragmentManager, GiphyGif imageUrl) {
    String tag = InsertGifDialog.class.getSimpleName();
    InsertGifDialog dialog = (InsertGifDialog) fragmentManager.findFragmentByTag(tag);

    if (dialog != null) {
      dialog.dismiss();
    }

    dialog = new InsertGifDialog();

    Bundle arguments = new Bundle();
    arguments.putParcelable(KEY_GIPHY_GIF, imageUrl);
    dialog.setArguments(arguments);
    dialog.show(fragmentManager, tag);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (!(getActivity() instanceof OnLinkInsertListener)) {
      throw new AssertionError();
    }
  }

  @NonNull
  @Override
  @SuppressLint("InflateParams")
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);

    View dialogLayout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_upload_image, null);
    ButterKnife.bind(this, dialogLayout);

    //noinspection ConstantConditions
    GiphyGif giphyGif = getArguments().getParcelable(KEY_GIPHY_GIF);

    //noinspection ConstantConditions
    displayPickedImage(giphyGif.previewUrl());
    urlView.setText(giphyUrlWithoutScheme(giphyGif.url()));
    stateViewFlipper.setDisplayedChild(stateViewFlipper.indexOfChild(stateViewFlipper.findViewById(R.id.uploadimage_state_uploaded)));

    insertButton.setEnabled(true);
    insertButton.setOnClickListener(v -> {
      String title = titleField.getText().toString().trim();
      ((OnLinkInsertListener) requireActivity()).onLinkInsert(title, giphyGif.url());
      dismiss();
    });

    AlertDialog dialog = new AlertDialog.Builder(requireContext())
        .setView(dialogLayout)
        .create();

    titleField.requestFocus();
    //noinspection ConstantConditions
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

    return dialog;
  }

  @OnClick(R.id.uploadimage_cancel)
  void onClickCancel() {
    dismiss();
  }

  private void displayPickedImage(String imageUrl) {
    Glide.with(this)
        .load(imageUrl)
        .transition(DrawableTransitionOptions.withCrossFade())
        .listener(new GlideUtils.SimpleRequestListener<Drawable>() {
          @Override
          public void onResourceReady(Drawable resource) {
            Timber.i("Image loaded: %s", resource);
          }
        })
        .into(imageView);
  }

  private static String giphyUrlWithoutScheme(String giphyUrl) {
    String scheme = Uri.parse(giphyUrl).getScheme();
    return giphyUrl.substring((scheme + "://").length(), giphyUrl.length());
  }
}
