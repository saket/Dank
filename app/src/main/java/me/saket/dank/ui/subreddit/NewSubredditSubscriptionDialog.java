package me.saket.dank.ui.subreddit;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding2.widget.RxTextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.DankDialogFragment;
import me.saket.dank.utils.Keyboards;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

/**
 * Let's the user subscribe to a new subreddit by manually entering the sub name.
 */
public class NewSubredditSubscriptionDialog extends DankDialogFragment {

  @BindView(R.id.newsubredditdialog_subreddit) EditText subredditView;
  @BindView(R.id.newsubredditdialog_subreddit_inputlayout) TextInputLayout subredditViewInputLayout;
  @BindView(R.id.newsubredditdialog_progress) View progressView;

  @Inject Lazy<ErrorResolver> errorResolver;
  @Inject Lazy<Reddit> reddit;

  public interface Callback {
    void onEnterNewSubredditForSubscription(Subscribeable newSubreddit);
  }

  public static void show(FragmentManager fragmentManager) {
    String tag = NewSubredditSubscriptionDialog.class.getSimpleName();
    NewSubredditSubscriptionDialog dialog = (NewSubredditSubscriptionDialog) fragmentManager.findFragmentByTag(tag);

    if (dialog != null) {
      dialog.dismiss();
    }

    dialog = new NewSubredditSubscriptionDialog();
    dialog.show(fragmentManager, tag);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    if (!(requireActivity() instanceof Callback)) {
      throw new AssertionError();
    }
  }

  @NonNull
  @Override
  @SuppressLint("InflateParams")
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    View dialogLayout = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_subscribe_to_new_subreddit, null);
    ButterKnife.bind(this, dialogLayout);

    AlertDialog dialog = new AlertDialog.Builder(requireActivity())
        .setView(dialogLayout)
        .setTitle(R.string.newsubredditdialog_title)
        .create();

    // Clear any error when user starts typing.
    RxTextView.textChanges(subredditView)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          subredditViewInputLayout.setError(null);
          subredditViewInputLayout.setErrorEnabled(false);
        });

    // Force show keyboard on start.
    //noinspection ConstantConditions
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

    return dialog;
  }

  @OnClick(R.id.newsubredditdialog_cancel)
  void onClickCancel() {
    dismissWithKeyboardDismissDelay();
  }

  @OnEditorAction(R.id.newsubredditdialog_subreddit)
  boolean onClickEnterOnSubredditField(int actionId) {
    if (actionId == EditorInfo.IME_ACTION_DONE) {
      // Proxy enter key to the subscribe button.
      onClickSubscribe();
      return true;
    }

    return false;
  }

  @OnClick(R.id.newsubredditdialog_subscribe)
  void onClickSubscribe() {
    String subredditName = subredditView.getText().toString();

    if (subredditName.trim().isEmpty()) {
      subredditViewInputLayout.setError(getString(R.string.newsubredditdialog_error_empty_field));
      return;
    }

    progressView.setVisibility(View.VISIBLE);
    //noinspection ConstantConditions
    Keyboards.hide(requireContext(), subredditView);

    reddit.get().subreddits().find(subredditName)
        .subscribeOn(io())
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroyCompletable())
        .subscribe(searchResult -> {
          switch (searchResult.type()) {
            case SUCCESS:
              Subscribeable subreddit = ((SubredditSearchResult.Success) searchResult).subscribeable();
              ((Callback) requireActivity()).onEnterNewSubredditForSubscription(subreddit);
              dismissWithKeyboardDismissDelay();
              break;

            case ERROR_PRIVATE:
              subredditViewInputLayout.setError(getString(R.string.newsubredditdialog_error_private_subreddit));
              break;

            case ERROR_NOT_FOUND:
              subredditViewInputLayout.setError(getString(R.string.newsubredditdialog_error_subreddit_doesnt_exist));
              break;

            case ERROR_UNKNOWN:
              subredditViewInputLayout.setError(getString(R.string.newsubredditdialog_error_unknown));

              Throwable error = ((SubredditSearchResult.UnknownError) searchResult).error();
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't subscribe"));
              break;

            default:
              throw new AssertionError("Unknown result: " + searchResult);
          }

          if (!(searchResult instanceof SubredditSearchResult.Success)) {
            progressView.setVisibility(View.GONE);
          }
        });
  }

  /**
   * Assuming that the keyboard was visible, we dismiss the dialog after dismissing the keyboard.
   */
  private void dismissWithKeyboardDismissDelay() {
    Keyboards.hide(requireContext(), subredditView);
    subredditView.postDelayed(() -> dismiss(), 150);
  }
}
