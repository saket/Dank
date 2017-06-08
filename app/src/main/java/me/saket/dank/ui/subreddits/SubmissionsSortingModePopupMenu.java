package me.saket.dank.ui.subreddits;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import me.saket.dank.R;
import me.saket.dank.ui.submission.SortingAndTimePeriod;

public class SubmissionsSortingModePopupMenu extends PopupMenu {

  private final Context context;

  public SubmissionsSortingModePopupMenu(Context context, View anchorView) {
    super(context, anchorView, Gravity.NO_GRAVITY, 0, R.style.DankPopupMenu_SubmissionSortingMode);
    this.context = context;
  }

  public void highlightActiveSortingAndTImePeriod(SortingAndTimePeriod highlightedSortingAndTimePeriod) {
    String highlightedSorting = getSortingModeText(highlightedSortingAndTimePeriod.sortOrder());
    String highlightedTimePeriod = getTimePeriodText(highlightedSortingAndTimePeriod.timePeriod());

    // TODO: Highlight currently selected sorting mode and time period.
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

  private String getSortingModeText(Sorting sorting) {
    switch (sorting) {
      case HOT:
        return context.getString(R.string.sorting_mode_hot);

      case NEW:
        return context.getString(R.string.sorting_mode_new);

      case RISING:
        return context.getString(R.string.sorting_mode_rising);

      case CONTROVERSIAL:
        return context.getString(R.string.sorting_mode_controversial);

      case TOP:
        return context.getString(R.string.sorting_mode_top);

      default:
      case GILDED:
        throw new UnsupportedOperationException();
    }
  }

  private String getTimePeriodText(TimePeriod timePeriod) {
    switch (timePeriod) {
      case HOUR:
        return context.getString(R.string.sorting_time_period_hour);

      case DAY:
        return context.getString(R.string.sorting_time_period_day);

      case WEEK:
        return context.getString(R.string.sorting_time_period_week);

      case MONTH:
        return context.getString(R.string.sorting_time_period_month);

      case YEAR:
        return context.getString(R.string.sorting_time_period_year);

      case ALL:
        return context.getString(R.string.sorting_time_period_all_time);

      default:
        throw new UnsupportedOperationException();
    }
  }
}
