package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.jakewharton.rxbinding2.widget.RxTextView;

import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Subreddit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankDialogFragment;
import me.saket.dank.utils.Keyboards;
import timber.log.Timber;

/**
 * Let's the user subscribe to a new subreddit by manually entering the sub name.
 */
public class NewSubredditSubscriptionDialog extends DankDialogFragment {

  @BindView(R.id.newsubredditdialog_subreddit) EditText subredditView;
  @BindView(R.id.newsubredditdialog_subreddit_inputlayout) TextInputLayout subredditViewInputLayout;
  @BindView(R.id.newsubredditdialog_progress) View progressView;

  public interface Callback {
    void onEnterNewSubredditForSubscription(Subreddit newSubreddit);
  }

  /**
   * Show this dialog.
   */
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

    if (!(getActivity() instanceof Callback)) {
      throw new AssertionError();
    }
  }

  @NonNull
  @Override
  @SuppressLint("InflateParams")
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View dialogLayout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_subscribe_to_new_subreddit, null);
    ButterKnife.bind(this, dialogLayout);

    AlertDialog dialog = new AlertDialog.Builder(getActivity())
        .setView(dialogLayout)
        .setTitle(R.string.newsubredditdialog_title)
        .create();

    // Clear any error when user starts typing.
    unsubscribeOnDestroy(RxTextView.textChanges(subredditView)
        .subscribe(o -> {
          subredditViewInputLayout.setError(null);
          subredditViewInputLayout.setErrorEnabled(false);
        }));

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
    Keyboards.hide(getContext(), subredditView);

    unsubscribeOnDestroy(Dank.reddit().findSubreddit(subredditName)
        .compose(applySchedulersSingle())
        .subscribe(subreddit -> {
          ((Callback) getActivity()).onEnterNewSubredditForSubscription(subreddit);
          dismissWithKeyboardDismissDelay();

        }, error -> {
          // TODO: 17/04/17 Network error?
          progressView.setVisibility(View.GONE);

          if (error instanceof IllegalArgumentException && error.getMessage().contains("is private")) {
            subredditViewInputLayout.setError(getString(R.string.newsubredditdialog_error_private_subreddit));

          } else if (error instanceof IllegalArgumentException && error.getMessage().contains("does not exist")
              || (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 404)) {
            subredditViewInputLayout.setError(getString(R.string.newsubredditdialog_error_subreddit_doesnt_exist));

          } else {
            Timber.e(error, "Couldn't subscribe");
            subredditViewInputLayout.setError(getString(R.string.newsubredditdialog_error_unknown));
          }

          Timber.e(error, "Couldn't subscribe");
        })
    );
  }

  /**
   * Assuming that the keyboard was visible, we dismiss the dialog after dismissing the keyboard.
   */
  private void dismissWithKeyboardDismissDelay() {
    Keyboards.hide(getContext(), subredditView);
    subredditView.postDelayed(() -> dismiss(), 150);
  }

}
