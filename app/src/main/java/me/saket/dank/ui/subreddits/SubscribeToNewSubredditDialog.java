package me.saket.dank.ui.subreddits;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.utils.Keyboards;

/**
 * Let's the user subscribe to a new subreddit by manually entering the sub name.
 */
public class SubscribeToNewSubredditDialog extends DialogFragment {

    @BindView(R.id.newsubredditdialog_subreddit) EditText subredditView;

    /**
     * Show this dialog.
     */
    public static void show(FragmentManager fragmentManager) {
        String tag = SubscribeToNewSubredditDialog.class.getSimpleName();
        SubscribeToNewSubredditDialog dialog = (SubscribeToNewSubredditDialog) fragmentManager.findFragmentByTag(tag);

        if (dialog != null) {
            dialog.dismiss();
        }

        dialog = new SubscribeToNewSubredditDialog();
        dialog.show(fragmentManager, tag);
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

        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return dialog;
    }

    @OnClick(R.id.newsubredditdialog_cancel)
    void onClickCancel() {
        dismissWithKeyboardDismissDelay();
    }

    @OnClick(R.id.newsubredditdialog_subscribe)
    void onClickSubscribe() {
        // TODO: 14/04/17 Subscribe to this sub
        dismissWithKeyboardDismissDelay();
    }

    /**
     * Assuming that the keyboard was visible, we dismiss the dialog after dismissing the keyboard.
     */
    private void dismissWithKeyboardDismissDelay() {
        Keyboards.hide(getContext(), subredditView);
        subredditView.postDelayed(() -> dismiss(), 150);
    }

}
