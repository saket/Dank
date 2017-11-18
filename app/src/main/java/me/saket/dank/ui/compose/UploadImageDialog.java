package me.saket.dank.ui.compose;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.File;
import java.io.InputStream;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankDialogFragment;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.utils.FileSizeUnit;
import okio.Okio;
import timber.log.Timber;

public class UploadImageDialog extends DankDialogFragment {

  private static final String KEY_IMAGE_URI = "imageUri";

  @BindView(R.id.uploadimage_image) ImageView imageView;
  @BindView(R.id.uploadimage_progress) TextView fileSizeView;
  @BindView(R.id.uploadimage_state_viewflipper) ViewFlipper stateViewFlipper;
  @BindView(R.id.uploadimage_title) EditText titleField;
  @BindView(R.id.uploadimage_state_failed_tap_to_retry) TextView retryButton;
  @BindView(R.id.uploadimage_insert) Button insertButton;

  @Inject MediaHostRepository mediaHostRepository;
  @Inject ErrorResolver errorResolver;

  private enum UploadState {
    IN_FLIGHT,
    UPLOADED,
    FAILED
  }

  /**
   * Show this dialog.
   */
  public static void show(FragmentManager fragmentManager, Uri imageUri) {
    String tag = UploadImageDialog.class.getSimpleName();
    UploadImageDialog dialog = (UploadImageDialog) fragmentManager.findFragmentByTag(tag);

    if (dialog != null) {
      dialog.dismiss();
    }

    dialog = new UploadImageDialog();

    Bundle arguments = new Bundle();
    arguments.putParcelable(KEY_IMAGE_URI, imageUri);
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
    Dank.dependencyInjector().inject(this);

    View dialogLayout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_upload_image, null);
    ButterKnife.bind(this, dialogLayout);

    displayPickedImage();
    uploadImage();
    setCancelable(false);

    return new AlertDialog.Builder(getContext())
        .setView(dialogLayout)
        .create();
  }

  private void displayPickedImage() {
    Glide.with(this)
        .load(getArguments().<Uri>getParcelable(KEY_IMAGE_URI))
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageView);
  }

  private void uploadImage() {
    Single<File> imageFileStream = copyImageToTempFile(getArguments().getParcelable(KEY_IMAGE_URI))
        .subscribeOn(io())
        .observeOn(mainThread())
        .doOnSuccess(tempFile -> {
          fileSizeView.setVisibility(View.VISIBLE);
          fileSizeView.setText(FileSizeUnit.formatForDisplay(fileSizeView.getResources(), tempFile.length(), FileSizeUnit.BYTES));
        })
        .cache();

    // Start upload.
    imageFileStream
        .flatMapObservable(file -> RxView.clicks(retryButton).map(o -> file).startWith(file))
        .doOnNext(o -> showUiState(UploadState.IN_FLIGHT))
        .flatMapSingle(fileToUpload -> mediaHostRepository.uploadImage(fileToUpload)
            .subscribeOn(io())
            .observeOn(mainThread())
            .onErrorResumeNext(error -> {
              ResolvedError resolvedError = errorResolver.resolve(error);
              if (resolvedError.isUnknown()) {
                Timber.e(error, "Error while uploading image");
              }

              showUiState(UploadState.FAILED);
              return Single.never();
            })
        )
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          showUiState(UploadState.UPLOADED);
          insertButton.setEnabled(true);
          insertButton.setOnClickListener(v -> {
            String title = titleField.getText().toString().trim();
            String url = "http://boop.poop"; // TODO.

            ((OnLinkInsertListener) getActivity()).onLinkInsert(title, url);
            dismiss();
          });
        });

    // Delete the temporary file on dialog exit.
    lifecycle().onDestroy()
        .withLatestFrom(imageFileStream.toObservable(), (o, tempFile) -> tempFile)
        .subscribe(tempFile -> {
          Timber.i("Deleting temp file: %s", tempFile);
          boolean deleted = tempFile.delete();
          if (!deleted) {
            Timber.e(new AssertionError(), "Couldn't delete temporary file for upload");
          }
        });
  }

  @CheckResult
  private Single<File> copyImageToTempFile(Uri imageContentUri) {
    return Single.fromCallable(() -> {
      InputStream inputStream = getContext().getContentResolver().openInputStream(imageContentUri);

      // Copy image to a temporary location in case the original gets deleted before upload.
      // For instance, I have a habit of accidentally deleting screenshots from notification
      // as soon as they're shared, but before the upload is complete.
      File temporaryFile = new File(getContext().getFilesDir(), "Temp-image-" + System.currentTimeMillis());

      //noinspection ConstantConditions
      Okio.buffer(Okio.source(inputStream)).readAll(Okio.sink(temporaryFile));
      return temporaryFile;
    });
  }

  private void showUiState(UploadState state) {
    switch (state) {
      case IN_FLIGHT:
        stateViewFlipper.setDisplayedChild(stateViewFlipper.indexOfChild(stateViewFlipper.findViewById(R.id.uploadimage_state_in_flight)));
        break;

      case UPLOADED:
        stateViewFlipper.setDisplayedChild(stateViewFlipper.indexOfChild(stateViewFlipper.findViewById(R.id.uploadimage_state_uploaded)));
        break;

      case FAILED:
        stateViewFlipper.setDisplayedChild(stateViewFlipper.indexOfChild(stateViewFlipper.findViewById(R.id.uploadimage_state_failed_tap_to_retry)));
        break;

      default:
        throw new AssertionError();
    }
  }

  @OnClick(R.id.uploadimage_cancel)
  void onClickCancel() {
    dismiss();
  }
}
