package me.saket.dank.walkthrough;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.f2prateek.rx.preferences2.Preference;
import com.google.auto.value.AutoValue;
import io.reactivex.Observable;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import me.saket.dank.R;
import me.saket.dank.ui.subreddit.SubmissionSwipeActionsProvider;
import me.saket.dank.ui.subreddit.SubredditSubmissionsAdapter;
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel;
import me.saket.dank.utils.Optional;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout.SwipeActionIconProvider;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public class SubmissionGesturesWalkthrough {

  private final Preference<Boolean> hasUserLearnedPref;

  @Inject
  public SubmissionGesturesWalkthrough(@Named("user_learned_submission_gestures") Preference<Boolean> hasUserLearnedPref) {
    this.hasUserLearnedPref = hasUserLearnedPref;
  }

  public Observable<Optional<UiModel>> walkthroughRows() {
    return hasUserLearnedPref.asObservable()
        .flatMap(hasLearned -> hasLearned
            ? Observable.just(Optional.<UiModel>empty())
            : Observable.just(Optional.of(UiModel.create())));
  }

  @AutoValue
  public abstract static class UiModel implements SubredditScreenUiModel.SubmissionRowUiModel {

    @Override
    public Type type() {
      return Type.GESTURES_WALKTHROUGH;
    }

    @Override
    public long adapterId() {
      return SubredditSubmissionsAdapter.ADAPTER_ID_GESTURES_WALKTHROUGH;
    }

    public static UiModel create() {
      return new AutoValue_SubmissionGesturesWalkthrough_UiModel();
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {

    private final ImageView thumbnailView;
    private final TextView titleView;
    private final TextView bylineView;

    protected ViewHolder(View itemView) {
      super(itemView);
      thumbnailView = itemView.findViewById(R.id.submission_item_icon);
      titleView = itemView.findViewById(R.id.submission_item_title);
      bylineView = itemView.findViewById(R.id.submission_item_byline);

      titleView.setText("Hey there! Welcome to Dank.");
      thumbnailView.setImageResource(R.drawable.ic_adb_24dp);
      thumbnailView.setBackgroundResource(R.color.blue_gray_800);
      bylineView.setText("Swipe here horizontally to reveal gestures");
    }

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      View itemView = inflater.inflate(R.layout.list_item_submission, parent, false);
      return new ViewHolder(itemView);
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return ((SwipeableLayout) itemView);
    }
  }

  public static class Adapter implements SubredditScreenUiModel.SubmissionRowUiChildAdapter<UiModel, ViewHolder> {

    private final WalkthroughSwipeActionsProvider swipeActionsProvider;

    @Inject
    public Adapter(WalkthroughSwipeActionsProvider swipeActionsProvider) {
      this.swipeActionsProvider = swipeActionsProvider;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
      SwipeableLayout swipeableLayout = holder.getSwipeableLayout();
      swipeableLayout.setSwipeActions(swipeActionsProvider.actions());
      swipeableLayout.setSwipeActionIconProvider(swipeActionsProvider);
      swipeableLayout.setOnPerformSwipeActionListener(action ->
          swipeActionsProvider.perform(action, holder)
      );
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {

    }
  }

  public static class WalkthroughSwipeActionsProvider implements SwipeActionIconProvider {

    private final SubmissionSwipeActionsProvider submissionSwipeActionsProvider;

    @Inject
    public WalkthroughSwipeActionsProvider(SubmissionSwipeActionsProvider submissionSwipeActionsProvider) {
      this.submissionSwipeActionsProvider = submissionSwipeActionsProvider;
    }

    public SwipeActions actions() {
      return submissionSwipeActionsProvider.actionsWithSave();
    }

    @Override
    public void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction) {
      submissionSwipeActionsProvider.showSwipeActionIcon(imageView, oldAction, newAction);
    }

    public void perform(SwipeAction action, ViewHolder holder) {

    }
  }
}
