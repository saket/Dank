package me.saket.dank.ui.submission;

import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;

import net.dean.jraw.models.CommentSort;

import me.saket.dank.R;
import me.saket.dank.utils.CommentSortUtils;

public class CommentSortingModePopupMenu extends PopupMenu {

  private final Context context;
  private OnSortingModeSelectListener onSortingModeSelectListener;

  public interface OnSortingModeSelectListener {
    void onSortingModeSelect(CommentSort sorting);
  }

  public CommentSortingModePopupMenu(Context context, View anchorView) {
    super(context, anchorView, Gravity.NO_GRAVITY, 0, R.style.DankPopupMenu_SubmissionSortingMode);
    this.context = context;

    inflate(R.menu.menu_comment_sorting_mode);

    setOnMenuItemClickListener(menuItem -> {
      switch (menuItem.getItemId()) {
        case R.id.action_comment_sorting_best:
          onSortingModeSelectListener.onSortingModeSelect(CommentSort.CONFIDENCE);
          return true;

        case R.id.action_comment_sorting_top:
          onSortingModeSelectListener.onSortingModeSelect(CommentSort.TOP);
          return true;

        case R.id.action_comment_sorting_new:
          onSortingModeSelectListener.onSortingModeSelect(CommentSort.NEW);
          return true;

        case R.id.action_comment_sorting_controversial:
          onSortingModeSelectListener.onSortingModeSelect(CommentSort.CONTROVERSIAL);
          return true;

        case R.id.action_comment_sorting_old:
          onSortingModeSelectListener.onSortingModeSelect(CommentSort.OLD);
          return true;

        case R.id.action_comment_sorting_qna:
          onSortingModeSelectListener.onSortingModeSelect(CommentSort.QA);
          return true;

        default:
          throw new UnsupportedOperationException();
      }
    });
  }

  public void setOnSortingModeSelectListener(OnSortingModeSelectListener listener) {
    onSortingModeSelectListener = listener;
  }

  // NOTE: Keep this in sync with SubmissionsSortingModePopupMenu.
  public void highlightActiveSorting(CommentSort highlightedSorting) {
    String highlightedSortingText = context.getString(CommentSortUtils.INSTANCE.sortingDisplayTextRes(highlightedSorting));

    for (int i = 0; i < getMenu().size(); i++) {
      MenuItem menuItem = getMenu().getItem(i);

      boolean isSortActive = highlightedSortingText.equalsIgnoreCase(menuItem.getTitle().toString());
      if (isSortActive) {
        menuItem.hasSubMenu();
        menuItem.setEnabled(false);
      }
    }
  }
}
