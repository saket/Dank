package me.saket.dank.ui.subreddit;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.TimePeriod;

import me.saket.dank.R;
import me.saket.dank.ui.submission.SortingAndTimePeriod;

public class SubmissionsSortingModePopupMenu extends PopupMenu {

  private final Context context;
  private OnSortingModeSelectListener onSortingModeSelectListener;

  public interface OnSortingModeSelectListener {
    void onSortingModeSelect(SortingAndTimePeriod sortingAndTimePeriod);
  }

  public SubmissionsSortingModePopupMenu(Context context, View anchorView) {
    super(context, anchorView, Gravity.NO_GRAVITY, 0, R.style.DankPopupMenu_SubmissionSortingMode);
    this.context = context;

    setOnMenuItemClickListener(menuItem -> {
      switch (menuItem.getItemId()) {
        case R.id.action_subreddit_sorting_best:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.BEST));
          return true;

        case R.id.action_subreddit_sorting_hot:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.HOT));
          return true;

        case R.id.action_subreddit_sorting_new:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.NEW));
          return true;

        case R.id.action_subreddit_sorting_rising:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.RISING));
          return true;

        case R.id.action_subreddit_sorting_controversial_hour:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.CONTROVERSIAL, TimePeriod.HOUR));
          return true;

        case R.id.action_subreddit_sorting_controversial_day:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.CONTROVERSIAL, TimePeriod.DAY));
          return true;

        case R.id.action_subreddit_sorting_controversial_week:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.CONTROVERSIAL, TimePeriod.WEEK));
          return true;

        case R.id.action_subreddit_sorting_controversial_month:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.CONTROVERSIAL, TimePeriod.MONTH));
          return true;

        case R.id.action_subreddit_sorting_controversial_year:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.CONTROVERSIAL, TimePeriod.YEAR));
          return true;

        case R.id.action_subreddit_sorting_controversial_alltime:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.CONTROVERSIAL, TimePeriod.ALL));
          return true;

        case R.id.action_subreddit_sorting_top_hour:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.TOP, TimePeriod.HOUR));
          return true;

        case R.id.action_subreddit_sorting_top_day:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.TOP, TimePeriod.DAY));
          return true;

        case R.id.action_subreddit_sorting_top_week:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.TOP, TimePeriod.WEEK));
          return true;

        case R.id.action_subreddit_sorting_top_month:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.TOP, TimePeriod.MONTH));
          return true;

        case R.id.action_subreddit_sorting_top_year:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.TOP, TimePeriod.YEAR));
          return true;

        case R.id.action_subreddit_sorting_top_alltime:
          onSortingModeSelectListener.onSortingModeSelect(SortingAndTimePeriod.create(SubredditSort.TOP, TimePeriod.ALL));
          return true;

        case R.id.action_subreddit_sorting_controversial:
        case R.id.action_subreddit_sorting_top:
          // Submenu.
          return false;

        default:
          throw new UnsupportedOperationException();
      }
    });
  }

  public void setOnSortingModeSelectListener(OnSortingModeSelectListener listener) {
    onSortingModeSelectListener = listener;
  }

  // NOTE: Keep this in sync with CommentSortingModePopupMenu.
  public void highlightActiveSortingAndTImePeriod(SortingAndTimePeriod highlightedSortingAndTimePeriod) {
    String highlightedSorting = context.getString(highlightedSortingAndTimePeriod.getSortingDisplayTextRes());
    String highlightedTimePeriod = context.getString(highlightedSortingAndTimePeriod.getTimePeriodDisplayTextRes());

    for (int i = 0; i < getMenu().size(); i++) {
      MenuItem menuItem = getMenu().getItem(i);

      boolean isSortActive = highlightedSorting.equalsIgnoreCase(menuItem.getTitle().toString());
      if (isSortActive) {
        if (menuItem.hasSubMenu()) {
          SubMenu subMenu = menuItem.getSubMenu();
          for (int j = 0; j < subMenu.size(); j++) {
            MenuItem subMenuItem = subMenu.getItem(j);
            boolean isTimePeriodActive = highlightedTimePeriod.equalsIgnoreCase(subMenuItem.getTitle().toString());
            if (isTimePeriodActive) {
              // PopupMenu won't let us apply spans. So we'll just gray the selected items in the meanwhile.
              subMenuItem.setEnabled(false);
            }
          }

        } else {
          menuItem.setEnabled(false);
        }
      }
    }
  }
}
