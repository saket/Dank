package me.saket.dank.ui.compose;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankDialogFragment;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.widgets.AnimatedProgressBar;

public class InsertGifDialog extends DankDialogFragment {

  private static final String KEY_GIPHY_GIF = "giphyGif";
  private static final String KEY_PAYLOAD = "payload";

  @BindView(R.id.uploadimage_image) ImageView imageView;
  @BindView(R.id.uploadimage_file_size) TextView fileSizeView;
  @BindView(R.id.uploadimage_state_viewflipper) ViewFlipper stateViewFlipper;
  @BindView(R.id.uploadimage_upload_progress_bar) AnimatedProgressBar progressBar;
  @BindView(R.id.uploadimage_title) EditText titleField;
  @BindView(R.id.uploadimage_url) TextView urlView;
  @BindView(R.id.uploadimage_state_failed_tap_to_retry) TextView errorView;
  @BindView(R.id.uploadimage_insert) Button insertButton;

  public interface OnGifInsertListener {
    void onGifInsert(String title, GiphyGif gif, @Nullable Parcelable payload);
  }

  public static void showWithPayload(FragmentManager fragmentManager, GiphyGif imageUrl, @Nullable Parcelable payload) {
    String tag = InsertGifDialog.class.getSimpleName();
    InsertGifDialog dialog = (InsertGifDialog) fragmentManager.findFragmentByTag(tag);

    if (dialog != null) {
      dialog.dismiss();
    }

    dialog = new InsertGifDialog();

    Bundle arguments = new Bundle();
    arguments.putParcelable(KEY_GIPHY_GIF, imageUrl);
    if (payload != null) {
      arguments.putParcelable(KEY_PAYLOAD, payload);
    }
    dialog.setArguments(arguments);
    dialog.show(fragmentManager, tag);
  }

  public static void show(FragmentManager fragmentManager, GiphyGif imageUrl) {
    showWithPayload(fragmentManager, imageUrl, null);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (!(getHost() instanceof OnGifInsertListener)) {
      throw new AssertionError();
    }
  }

  @NonNull
  @Override
  @SuppressLint("InflateParams")
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);

    View dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_image, null);
    ButterKnife.bind(this, dialogLayout);

    //noinspection ConstantConditions
    GiphyGif giphyGif = getArguments().getParcelable(KEY_GIPHY_GIF);
    Parcelable payload = getArguments().getParcelable(KEY_PAYLOAD);

    //noinspection ConstantConditions
    displayPickedImage(giphyGif.previewUrl());
    urlView.setText(giphyUrlWithoutScheme(giphyGif.url()));
    stateViewFlipper.setDisplayedChild(stateViewFlipper.indexOfChild(stateViewFlipper.findViewById(R.id.uploadimage_state_uploaded)));

    insertButton.setEnabled(true);
    insertButton.setOnClickListener(v -> {
      String title = titleField.getText().toString().trim();
      ((OnGifInsertListener) requireHost()).onGifInsert(title, giphyGif, payload);
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
        .into(imageView);
  }

  private static String giphyUrlWithoutScheme(String giphyUrl) {
    String scheme = Uri.parse(giphyUrl).getScheme();
    return giphyUrl.substring((scheme + "://").length(), giphyUrl.length());
  }
}
