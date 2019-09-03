package me.saket.dank.ui.compose;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.jakewharton.rxbinding2.view.RxView;
import com.squareup.moshi.Moshi;

import java.io.File;
import java.io.InputStream;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.FileUploadProgressEvent;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankDialogFragment;
import me.saket.dank.ui.media.ImgurUploadResponse;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.utils.FileSizeUnit;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.widgets.AnimatedProgressBar;
import okio.Okio;
import retrofit2.HttpException;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

public class UploadImageDialog extends DankDialogFragment {

  private static final String KEY_IMAGE_URI = "imageUri";

  @BindView(R.id.uploadimage_image) ImageView imageView;
  @BindView(R.id.uploadimage_file_size) TextView fileSizeView;
  @BindView(R.id.uploadimage_state_viewflipper) ViewFlipper stateViewFlipper;
  @BindView(R.id.uploadimage_upload_progress_bar) AnimatedProgressBar progressBar;
  @BindView(R.id.uploadimage_title) EditText titleField;
  @BindView(R.id.uploadimage_url) TextView urlView;
  @BindView(R.id.uploadimage_state_failed_tap_to_retry) TextView errorView;
  @BindView(R.id.uploadimage_insert) Button insertButton;

  @Inject MediaHostRepository mediaHostRepository;
  @Inject ErrorResolver errorResolver;
  @Inject Moshi moshi;

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

    if (!(requireActivity() instanceof OnLinkInsertListener)) {
      throw new AssertionError();
    }
  }

  @NonNull
  @Override
  @SuppressLint("InflateParams")
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    Dank.dependencyInjector().inject(this);

    View dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_image, null);
    ButterKnife.bind(this, dialogLayout);

    displayPickedImage();
    uploadImage();
    setCancelable(false);

    return new AlertDialog.Builder(requireContext())
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
    Uri imageContentUri = getArguments().getParcelable(KEY_IMAGE_URI);
    final long startTime = System.currentTimeMillis();
    String imageMimeType = requireContext().getContentResolver().getType(imageContentUri);
    Timber.i("Mime-type in: %sms", System.currentTimeMillis() - startTime);

    Single<File> imageFileStream = copyImageToTempFile(imageContentUri)
        .subscribeOn(io())
        .observeOn(mainThread())
        .doOnSuccess(tempFile -> {
          fileSizeView.setVisibility(View.VISIBLE);
          fileSizeView.setText(FileSizeUnit.formatForDisplay(fileSizeView.getResources(), tempFile.length(), FileSizeUnit.BYTES));
        })
        .cache();

    // Start upload.
    imageFileStream
        .flatMapObservable(file -> RxView.clicks(errorView).map(o -> file).startWith(file))
        .doOnNext(o -> showUiState(UploadState.IN_FLIGHT))
        .flatMap(fileToUpload -> mediaHostRepository.uploadImage(fileToUpload, imageMimeType)
            .subscribeOn(io())
            .observeOn(mainThread())
            .onErrorResumeNext(handleImageUploadError())
        )
        .takeUntil(lifecycle().onDestroy())
        .subscribe(handleImageUploadUpdate());

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
      InputStream inputStream = requireContext().getContentResolver().openInputStream(imageContentUri);

      // Copy image to a temporary location in case the original gets deleted before upload.
      // For instance, I have a habit of accidentally deleting screenshots from notification
      // as soon as they're shared, but before the upload is complete.
      File temporaryFile = new File(requireContext().getFilesDir(), "Temp-image-" + System.currentTimeMillis());

      //noinspection ConstantConditions
      Okio.buffer(Okio.source(inputStream)).readAll(Okio.sink(temporaryFile));
      return temporaryFile;
    });
  }

  private Consumer<FileUploadProgressEvent<ImgurUploadResponse>> handleImageUploadUpdate() {
    return uploadEvent -> {
      if (uploadEvent.isInFlight()) {
        float progress = uploadEvent.progress();
        progressBar.setIndeterminate(progress == 0f || progress == 1f);
        progressBar.setProgressWithAnimation((int) (progress * 100));

      } else {
        ImgurUploadResponse uploadResponse = uploadEvent.uploadResponse();
        //noinspection ConstantConditions
        String link = uploadResponse.data().link();
        //noinspection ConstantConditions
        String linkWithoutScheme = link.substring("https://".length(), link.length());
        urlView.setText(linkWithoutScheme);

        showUiState(UploadState.UPLOADED);
        Keyboards.show(titleField);
        insertButton.setEnabled(true);
        insertButton.setOnClickListener(v -> {
          String title = titleField.getText().toString().trim();
          String url = uploadResponse.data().link();
          ((OnLinkInsertListener) requireActivity()).onLinkInsert(title, url);
          dismiss();
        });
      }
    };
  }

  private Function<Throwable, Observable<FileUploadProgressEvent<ImgurUploadResponse>>> handleImageUploadError() {
    return error -> {
      boolean handled = false;

      if (error instanceof HttpException && ((HttpException) error).code() == 400 /* Bad request */) {
        InputStream errorBodyStream = ((HttpException) error).response().errorBody().byteStream();
        ImgurUploadResponse errorResponse = ImgurUploadResponse.jsonAdapter(moshi).fromJson(Okio.buffer(Okio.source(errorBodyStream)));
        //noinspection ConstantConditions
        if (errorResponse.data().error().contains("File is over the size limit")) {
          errorView.setText(R.string.composereply_uploadimage_error_file_size_too_large);
          handled = true;
        }
      }

      if (!handled) {
        ResolvedError resolvedError = errorResolver.resolve(error);
        if (resolvedError.isUnknown()) {
          Timber.e(error, "Error while uploading image");
        }

        String emoji = getResources().getString(resolvedError.errorEmojiRes());
        String errorMessage = getResources().getString(resolvedError.errorMessageRes());

        if (!resolvedError.isImgurRateLimitError()) {
          String tapToRetryText = getResources().getString(R.string.composereply_uploadimage_tap_to_retry);
          if (!errorMessage.endsWith(getResources().getString(R.string.composereply_uploadimage_error_message_period))) {
            errorMessage += getResources().getString(R.string.composereply_uploadimage_error_message_period);
          }
          errorMessage += " " + tapToRetryText;
        }

        errorView.setText(String.format("%s\n\n%s", emoji, errorMessage));
      }

      // TODO: Handle known HTTP error codes.
      showUiState(UploadState.FAILED);
      return Observable.never();
    };
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
