package me.saket.dank.ui.compose;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.jakewharton.rxbinding2.widget.RxTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankDialogFragment;
import me.saket.dank.utils.Views;

/**
 * TODO: Offer to paste URL from clipboard.
 */
public class AddLinkDialog extends DankDialogFragment {

  private static final String KEY_PRE_FILLED_TITLE = "preFilledTitle";

  @BindView(R.id.addlinkdialog_title_inputlayout) TextInputLayout titleInputLayout;
  @BindView(R.id.addlinkdialog_title) EditText titleField;
  @BindView(R.id.addlinkdialog_url_inputlayout) TextInputLayout urlInputLayout;
  @BindView(R.id.addlinkdialog_url) EditText urlField;
  @BindView(R.id.addlinkdialog_paste_from_clipboard_hint) TextView pasteUrlFromClipboardHintView;

  public static void showPreFilled(FragmentManager fragmentManager, @Nullable String preFilledTitle) {
    String tag = AddLinkDialog.class.getSimpleName();

    AddLinkDialog dialog = (AddLinkDialog) fragmentManager.findFragmentByTag(tag);
    if (dialog != null) {
      dialog.dismiss();
    }

    Bundle arguments = new Bundle();
    if (preFilledTitle != null) {
      arguments.putString(KEY_PRE_FILLED_TITLE, preFilledTitle);
    }
    dialog = new AddLinkDialog();
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

    View dialogLayout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_link, null);
    ButterKnife.bind(this, dialogLayout);

    if (savedInstanceState == null && getArguments().containsKey(KEY_PRE_FILLED_TITLE)) {
      String preFilledTitle = getArguments().getString(KEY_PRE_FILLED_TITLE);
      Views.setTextWithCursor(titleField, preFilledTitle);
    }

//    pasteUrlFromClipboardHintView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
//      @Override
//      public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//        pasteUrlFromClipboardHintView.removeOnLayoutChangeListener(this);
//
//        if (urlField.getLineCount() > 2) {
//          int lineEndIndex = urlField.getLayout().getLineEnd(1);
//          String text = urlField.getText().subSequence(0, lineEndIndex - 2) + "...";
//          urlField.setText(text);
//        }
//      }
//    });

    AlertDialog dialog = new AlertDialog.Builder(getContext())
        .setView(dialogLayout)
        .create();
    // Show keyboard automatically on start. Doesn't happen on its own.
    //noinspection ConstantConditions
    dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    return dialog;
  }

  @Override
  public void onStart() {
    super.onStart();

    Optional<String> urlInClipboard = getUrlInClipboard();

    RxTextView.textChanges(urlField)
        .skipInitialValue()
        .takeUntil(lifecycle().onStop())
        .take(1)
        .subscribe(o -> pasteUrlFromClipboardHintView.setVisibility(View.GONE));

    boolean isUrlFieldEmpty = urlField.getText().toString().trim().isEmpty();
    boolean canOfferPastingFromClipboard = isUrlFieldEmpty && urlInClipboard.isPresent();
    pasteUrlFromClipboardHintView.setVisibility(canOfferPastingFromClipboard ? View.VISIBLE : View.GONE);

    // TODO: Elipsize manually with "...".
    if (urlInClipboard.isPresent()) {
      pasteUrlFromClipboardHintView.setText(getString(
          R.string.composereply_paste_from_clipboard_hint,
          urlInClipboard.get()
      ));
      pasteUrlFromClipboardHintView.setOnClickListener(o -> {
        Views.setTextWithCursor(urlField, urlInClipboard.get());
        pasteUrlFromClipboardHintView.setVisibility(View.GONE);
      });
    }
  }

  private Optional<String> getUrlInClipboard() {
    ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    //noinspection ConstantConditions
    String textInClipboard = clipboardManager.getPrimaryClip().getItemAt(0).coerceToText(getContext()).toString();
    boolean hasUrlInClipboard = Patterns.WEB_URL.matcher(textInClipboard).matches();
    return hasUrlInClipboard ? Optional.of(textInClipboard) : Optional.absent();
  }

  @OnClick(R.id.addlinkdialog_cancel)
  void onClickCancel() {
    dismiss();
  }

  @OnClick(R.id.addlinkdialog_insert)
  void onClickInsert() {
    String title = titleField.getText().toString().trim();
    String url = urlField.getText().toString().trim();

    titleInputLayout.setError(
        title.isEmpty()
            ? getString(R.string.composereply_addlink_error_empty_field)
            : null
    );
    urlInputLayout.setError(
        url.isEmpty()
            ? getString(R.string.composereply_addlink_error_empty_field)
            : null
    );

    if (!title.isEmpty() && !url.isEmpty()) {
      ((OnLinkInsertListener) getActivity()).onLinkInsert(title, url);
      dismiss();
    }
  }
}
