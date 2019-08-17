/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.saket.dank.utils.itemanimators;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Copied from https://github.com/mikepenz/ItemAnimators
 * <p>
 * This implementation of {@link RecyclerView.ItemAnimator} provides basic
 * animations on remove, add, and move events that happen to the items in
 * a RecyclerView. RecyclerView uses a CollapsingItemAnimator by default.
 *
 * @see RecyclerView#setItemAnimator(RecyclerView.ItemAnimator)
 */
public abstract class BaseItemAnimator<T> extends SimpleItemAnimator {
  private static final boolean DEBUG = false;
  private static TimeInterpolator defaultInterpolator;

  private ArrayList<ViewHolder> mPendingRemovals = new ArrayList<>();
  private ArrayList<ViewHolder> mPendingAdditions = new ArrayList<>();
  private ArrayList<MoveInfo> mPendingMoves = new ArrayList<>();
  private ArrayList<ChangeInfo> mPendingChanges = new ArrayList<>();

  private ArrayList<ArrayList<ViewHolder>> mAdditionsList = new ArrayList<>();
  private ArrayList<ArrayList<MoveInfo>> mMovesList = new ArrayList<>();
  private ArrayList<ArrayList<ChangeInfo>> mChangesList = new ArrayList<>();

  private ArrayList<ViewHolder> mAddAnimations = new ArrayList<>();
  private ArrayList<ViewHolder> mMoveAnimations = new ArrayList<>();
  private ArrayList<ViewHolder> mRemoveAnimations = new ArrayList<>();
  private ArrayList<ViewHolder> mChangeAnimations = new ArrayList<>();

  Interpolator mInterpolator;

  private static class MoveInfo {
    public ViewHolder holder;
    public int fromX, fromY, toX, toY;

    private MoveInfo(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
      this.holder = holder;
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }
  }

  protected static class ChangeInfo {
    public ViewHolder oldHolder, newHolder;
    public int fromX, fromY, toX, toY;

    private ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder) {
      this.oldHolder = oldHolder;
      this.newHolder = newHolder;
    }

    private ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder,
        int fromX, int fromY, int toX, int toY)
    {
      this(oldHolder, newHolder);
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }

    @Override
    public String toString() {
      return "ChangeInfo{" +
          "oldHolder=" + oldHolder +
          ", newHolder=" + newHolder +
          ", fromX=" + fromX +
          ", fromY=" + fromY +
          ", toX=" + toX +
          ", toY=" + toY +
          '}';
    }
  }

  /**
   * defines the interpolator used for the animations
   *
   * @param interpolator the interpolator used for the animations
   * @return the implementing class T
   */
  public T withInterpolator(Interpolator interpolator) {
    this.mInterpolator = interpolator;
    //noinspection unchecked
    return (T) this;
  }

  /**
   * @return the interpolator used for the animations
   */
  public Interpolator getInterpolator() {
    return this.mInterpolator;
  }

  @Override
  public void runPendingAnimations() {
    boolean removalsPending = !mPendingRemovals.isEmpty();
    boolean movesPending = !mPendingMoves.isEmpty();
    boolean changesPending = !mPendingChanges.isEmpty();
    boolean additionsPending = !mPendingAdditions.isEmpty();
    if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
      // nothing to animate
      return;
    }
    // First, remove stuff
    for (ViewHolder holder : mPendingRemovals) {
      animateRemoveImpl(holder);
    }
    mPendingRemovals.clear();
    // Next, move stuff
    if (movesPending) {
      final ArrayList<MoveInfo> moves = new ArrayList<>();
      moves.addAll(mPendingMoves);
      mMovesList.add(moves);
      mPendingMoves.clear();
      Runnable mover = new Runnable() {
        @Override
        public void run() {
          for (MoveInfo moveInfo : moves) {
            animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY,
                moveInfo.toX, moveInfo.toY);
          }
          moves.clear();
          mMovesList.remove(moves);
        }
      };
      if (removalsPending) {
        View view = moves.get(0).holder.itemView;
        ViewCompat.postOnAnimationDelayed(view, mover, 0);
      } else {
        mover.run();
      }
    }
    // Next, change stuff, to run in parallel with move animations
    if (changesPending) {
      final ArrayList<ChangeInfo> changes = new ArrayList<>();
      changes.addAll(mPendingChanges);
      mChangesList.add(changes);
      mPendingChanges.clear();
      Runnable changer = new Runnable() {
        @Override
        public void run() {
          for (ChangeInfo change : changes) {
            animateChangeImpl(change);
          }
          changes.clear();
          mChangesList.remove(changes);
        }
      };
      if (removalsPending) {
        ViewHolder holder = changes.get(0).oldHolder;

        long moveDuration = movesPending ? getMoveDuration() : 0;
        ViewCompat.postOnAnimationDelayed(holder.itemView, changer, getRemoveDelay(getRemoveDuration(), moveDuration, getChangeDuration()));
      } else {
        changer.run();
      }
    }
    // Next, add stuff
    if (additionsPending) {
      final ArrayList<ViewHolder> additions = new ArrayList<>();
      additions.addAll(mPendingAdditions);
      mAdditionsList.add(additions);
      mPendingAdditions.clear();
      Runnable adder = new Runnable() {
        public void run() {
          for (ViewHolder holder : additions) {
            animateAddImpl(holder);
          }
          additions.clear();
          mAdditionsList.remove(additions);
        }
      };
      if (removalsPending || movesPending || changesPending) {
        long removeDuration = removalsPending ? getRemoveDuration() : 0;
        long moveDuration = movesPending ? getMoveDuration() : 0;
        long changeDuration = changesPending ? getChangeDuration() : 0;
        View view = additions.get(0).itemView;
        ViewCompat.postOnAnimationDelayed(view, adder, getAddDelay(removeDuration, moveDuration, changeDuration));
      } else {
        adder.run();
      }
    }
  }

  /**
   * used to calculated the delay until the remove animation should start
   *
   * @param remove the remove duration
   * @param move   the move duration
   * @param change the change duration
   * @return the calculated delay for the remove items animation
   */
  public long getRemoveDelay(long remove, long move, long change) {
    return remove + Math.max(move, change);
  }

  /**
   * used to calculated the delay until the add animation should start
   *
   * @param remove the remove duration
   * @param move   the move duration
   * @param change the change duration
   * @return the calculated delay for the add items animation
   */
  public long getAddDelay(long remove, long move, long change) {
    return remove + Math.max(move, change);
  }

  @Override
  public boolean animateRemove(final ViewHolder holder) {
    resetAnimation(holder);
    removeAnimationPrepare(holder);
    mPendingRemovals.add(holder);
    return true;
  }

  private void animateRemoveImpl(final ViewHolder holder) {
    final ViewPropertyAnimatorCompat animation = removeAnimation(holder);
    mRemoveAnimations.add(holder);
    animation.setListener(new VpaListenerAdapter() {
      @Override
      public void onAnimationStart(View view) {
        dispatchRemoveStarting(holder);
      }

      @Override
      public void onAnimationEnd(View view) {
        animation.setListener(null);
        removeAnimationCleanup(holder);
        dispatchRemoveFinished(holder);
        mRemoveAnimations.remove(holder);
        dispatchFinishedWhenDone();
      }
    }).start();
  }

  public void removeAnimationPrepare(ViewHolder holder) {

  }

  abstract public ViewPropertyAnimatorCompat removeAnimation(ViewHolder holder);

  abstract public void removeAnimationCleanup(ViewHolder holder);

  @Override
  public boolean animateAdd(final ViewHolder holder) {
    resetAnimation(holder);
    addAnimationPrepare(holder);
    mPendingAdditions.add(holder);
    return true;
  }

  private void animateAddImpl(final ViewHolder holder) {
    final ViewPropertyAnimatorCompat animation = addAnimation(holder);
    mAddAnimations.add(holder);
    animation.
        setListener(new VpaListenerAdapter() {
          @Override
          public void onAnimationStart(View view) {
            dispatchAddStarting(holder);
          }

          @Override
          public void onAnimationCancel(View view) {
            addAnimationCleanup(holder);
          }

          @Override
          public void onAnimationEnd(View view) {
            animation.setListener(null);
            dispatchAddFinished(holder);
            mAddAnimations.remove(holder);
            dispatchFinishedWhenDone();
            addAnimationCleanup(holder);
          }
        }).start();
  }

  /**
   * the animation to prepare the view before the add animation is run
   *
   * @param holder
   */
  abstract public void addAnimationPrepare(ViewHolder holder);

  /**
   * the animation for adding a view
   *
   * @param holder
   * @return
   */
  abstract public ViewPropertyAnimatorCompat addAnimation(ViewHolder holder);

  /**
   * the cleanup method if the animation needs to be stopped. and tro prepare for the next view
   *
   * @param holder
   */
  abstract void addAnimationCleanup(ViewHolder holder);

  @Override
  public boolean animateMove(final ViewHolder holder, int fromX, int fromY,
      int toX, int toY)
  {
    final View view = holder.itemView;
    fromX += ViewCompat.getTranslationX(holder.itemView);
    fromY += ViewCompat.getTranslationY(holder.itemView);
    resetAnimation(holder);
    int deltaX = toX - fromX;
    int deltaY = toY - fromY;
    if (deltaX == 0 && deltaY == 0) {
      dispatchMoveFinished(holder);
      return false;
    }
    if (deltaX != 0) {
      ViewCompat.setTranslationX(view, -deltaX);
    }
    if (deltaY != 0) {
      ViewCompat.setTranslationY(view, -deltaY);
    }
    mPendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
    return true;
  }

  private void animateMoveImpl(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    final View view = holder.itemView;
    final int deltaX = toX - fromX;
    final int deltaY = toY - fromY;
    if (deltaX != 0) {
      ViewCompat.animate(view).translationX(0);
    }
    if (deltaY != 0) {
      ViewCompat.animate(view).translationY(0);
    }
    // TODO: make EndActions end listeners instead, since end actions aren't called when
    // vpas are canceled (and can't end them. why?)
    // need listener functionality in VPACompat for this. Ick.
    final ViewPropertyAnimatorCompat animation = ViewCompat.animate(view);
    mMoveAnimations.add(holder);
    animation.setDuration(getMoveDuration()).setListener(new VpaListenerAdapter() {
      @Override
      public void onAnimationStart(View view) {
        dispatchMoveStarting(holder);
      }

      @Override
      public void onAnimationCancel(View view) {
        if (deltaX != 0) {
          ViewCompat.setTranslationX(view, 0);
        }
        if (deltaY != 0) {
          ViewCompat.setTranslationY(view, 0);
        }
      }

      @Override
      public void onAnimationEnd(View view) {
        animation.setListener(null);
        dispatchMoveFinished(holder);
        mMoveAnimations.remove(holder);
        dispatchFinishedWhenDone();
      }
    }).start();
  }

  @Override
  public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder,
      int fromX, int fromY, int toX, int toY)
  {
    if (oldHolder == newHolder) {
      // Don't know how to run change animations when the same view holder is re-used.
      // run a move animation to handle position changes.
      return animateMove(oldHolder, fromX, fromY, toX, toY);
    }
    changeAnimation(oldHolder, newHolder,
        fromX, fromY, toX, toY);
    mPendingChanges.add(new ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY));
    return true;
  }

  private void animateChangeImpl(final ChangeInfo changeInfo) {
    final ViewHolder holder = changeInfo.oldHolder;
    final View view = holder == null ? null : holder.itemView;
    final ViewHolder newHolder = changeInfo.newHolder;
    final View newView = newHolder != null ? newHolder.itemView : null;
    if (view != null) {
      final ViewPropertyAnimatorCompat oldViewAnim = changeOldAnimation(holder, changeInfo);
      mChangeAnimations.add(changeInfo.oldHolder);
      oldViewAnim.setListener(new VpaListenerAdapter() {
        @Override
        public void onAnimationStart(View view) {
          dispatchChangeStarting(changeInfo.oldHolder, true);
        }

        @Override
        public void onAnimationEnd(View view) {
          oldViewAnim.setListener(null);
          changeAnimationCleanup(holder);
          ViewCompat.setTranslationX(view, 0);
          ViewCompat.setTranslationY(view, 0);
          dispatchChangeFinished(changeInfo.oldHolder, true);
          mChangeAnimations.remove(changeInfo.oldHolder);
          dispatchFinishedWhenDone();
        }
      }).start();
    }
    if (newView != null) {
      final ViewPropertyAnimatorCompat newViewAnimation = changeNewAnimation(newHolder);
      mChangeAnimations.add(changeInfo.newHolder);
      newViewAnimation.setListener(new VpaListenerAdapter() {
        @Override
        public void onAnimationStart(View view) {
          dispatchChangeStarting(changeInfo.newHolder, false);
        }

        @Override
        public void onAnimationEnd(View view) {
          newViewAnimation.setListener(null);
          changeAnimationCleanup(newHolder);
          ViewCompat.setTranslationX(newView, 0);
          ViewCompat.setTranslationY(newView, 0);
          dispatchChangeFinished(changeInfo.newHolder, false);
          mChangeAnimations.remove(changeInfo.newHolder);
          dispatchFinishedWhenDone();
        }
      }).start();
    }
  }

  /**
   * the whole change animation if we have to cross animate two views
   *
   * @param oldHolder
   * @param newHolder
   * @param fromX
   * @param fromY
   * @param toX
   * @param toY
   */
  public void changeAnimation(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
    final float prevTranslationX = ViewCompat.getTranslationX(oldHolder.itemView);
    final float prevTranslationY = ViewCompat.getTranslationY(oldHolder.itemView);
    final float prevValue = ViewCompat.getAlpha(oldHolder.itemView);
    resetAnimation(oldHolder);
    int deltaX = (int) (toX - fromX - prevTranslationX);
    int deltaY = (int) (toY - fromY - prevTranslationY);
    // recover prev translation state after ending animation
    ViewCompat.setTranslationX(oldHolder.itemView, prevTranslationX);
    ViewCompat.setTranslationY(oldHolder.itemView, prevTranslationY);

    ViewCompat.setAlpha(oldHolder.itemView, prevValue);
    if (newHolder != null) {
      // carry over translation values
      resetAnimation(newHolder);
      ViewCompat.setTranslationX(newHolder.itemView, -deltaX);
      ViewCompat.setTranslationY(newHolder.itemView, -deltaY);
      ViewCompat.setAlpha(newHolder.itemView, 0);
    }
  }

  /**
   * the animation for removing the old view
   *
   * @param holder
   * @return
   */
  abstract public ViewPropertyAnimatorCompat changeOldAnimation(ViewHolder holder, ChangeInfo changeInfo);

  /**
   * the animation for changing the new view
   *
   * @param holder
   * @return
   */
  abstract public ViewPropertyAnimatorCompat changeNewAnimation(ViewHolder holder);

  /**
   * the cleanup method if the animation needs to be stopped. and tro prepare for the next view
   *
   * @param holder
   */
  abstract public void changeAnimationCleanup(ViewHolder holder);

  private void endChangeAnimation(List<ChangeInfo> infoList, ViewHolder item) {
    for (int i = infoList.size() - 1; i >= 0; i--) {
      ChangeInfo changeInfo = infoList.get(i);
      if (endChangeAnimationIfNecessary(changeInfo, item)) {
        if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
          infoList.remove(changeInfo);
        }
      }
    }
  }

  private void endChangeAnimationIfNecessary(ChangeInfo changeInfo) {
    if (changeInfo.oldHolder != null) {
      endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder);
    }
    if (changeInfo.newHolder != null) {
      endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder);
    }
  }

  private boolean endChangeAnimationIfNecessary(ChangeInfo changeInfo, ViewHolder item) {
    boolean oldItem = false;
    if (changeInfo.newHolder == item) {
      changeInfo.newHolder = null;
    } else if (changeInfo.oldHolder == item) {
      changeInfo.oldHolder = null;
      oldItem = true;
    } else {
      return false;
    }
    changeAnimationCleanup(item);
    ViewCompat.setTranslationX(item.itemView, 0);
    ViewCompat.setTranslationY(item.itemView, 0);
    dispatchChangeFinished(item, oldItem);
    return true;
  }

  @Override
  public void endAnimation(final ViewHolder item) {
    final View view = item.itemView;
    // this will trigger end callback which should set properties to their target values.
    ViewCompat.animate(view).cancel();
    // TODO if some other animations are chained to end, how do we cancel them as well?
    for (int i = mPendingMoves.size() - 1; i >= 0; i--) {
      MoveInfo moveInfo = mPendingMoves.get(i);
      if (moveInfo.holder == item) {
        ViewCompat.setTranslationY(view, 0);
        ViewCompat.setTranslationX(view, 0);
        dispatchMoveFinished(item);
        mPendingMoves.remove(i);
      }
    }
    endChangeAnimation(mPendingChanges, item);
    if (mPendingRemovals.remove(item)) {
      removeAnimationCleanup(item);
      dispatchRemoveFinished(item);
    }
    if (mPendingAdditions.remove(item)) {
      addAnimationCleanup(item);
      dispatchAddFinished(item);
    }

    for (int i = mChangesList.size() - 1; i >= 0; i--) {
      ArrayList<ChangeInfo> changes = mChangesList.get(i);
      endChangeAnimation(changes, item);
      if (changes.isEmpty()) {
        mChangesList.remove(i);
      }
    }
    for (int i = mMovesList.size() - 1; i >= 0; i--) {
      ArrayList<MoveInfo> moves = mMovesList.get(i);
      for (int j = moves.size() - 1; j >= 0; j--) {
        MoveInfo moveInfo = moves.get(j);
        if (moveInfo.holder == item) {
          ViewCompat.setTranslationY(view, 0);
          ViewCompat.setTranslationX(view, 0);
          dispatchMoveFinished(item);
          moves.remove(j);
          if (moves.isEmpty()) {
            mMovesList.remove(i);
          }
          break;
        }
      }
    }
    for (int i = mAdditionsList.size() - 1; i >= 0; i--) {
      ArrayList<ViewHolder> additions = mAdditionsList.get(i);
      if (additions.remove(item)) {
        addAnimationCleanup(item);
        dispatchAddFinished(item);
        if (additions.isEmpty()) {
          mAdditionsList.remove(i);
        }
      }
    }

    // animations should be ended by the cancel above.
    //noinspection PointlessBooleanExpression,ConstantConditions
    if (mRemoveAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException("after animation is cancelled, item should not be in "
          + "mRemoveAnimations list");
    }

    //noinspection PointlessBooleanExpression,ConstantConditions
    if (mAddAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException("after animation is cancelled, item should not be in "
          + "mAddAnimations list");
    }

    //noinspection PointlessBooleanExpression,ConstantConditions
    if (mChangeAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException("after animation is cancelled, item should not be in "
          + "mChangeAnimations list");
    }

    //noinspection PointlessBooleanExpression,ConstantConditions
    if (mMoveAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException("after animation is cancelled, item should not be in "
          + "mMoveAnimations list");
    }
    dispatchFinishedWhenDone();
  }

  public void resetAnimation(ViewHolder holder) {
    if (defaultInterpolator == null) {
      defaultInterpolator = new ValueAnimator().getInterpolator();
    }
    holder.itemView.animate().setInterpolator(defaultInterpolator);
    endAnimation(holder);
  }

  @Override
  public boolean isRunning() {
    return (!mPendingAdditions.isEmpty() ||
        !mPendingChanges.isEmpty() ||
        !mPendingMoves.isEmpty() ||
        !mPendingRemovals.isEmpty() ||
        !mMoveAnimations.isEmpty() ||
        !mRemoveAnimations.isEmpty() ||
        !mAddAnimations.isEmpty() ||
        !mChangeAnimations.isEmpty() ||
        !mMovesList.isEmpty() ||
        !mAdditionsList.isEmpty() ||
        !mChangesList.isEmpty());
  }

  /**
   * Check the state of currently pending and running animations. If there are none
   * pending/running, call {@link #dispatchAnimationsFinished()} to notify any
   * listeners.
   */
  private void dispatchFinishedWhenDone() {
    if (!isRunning()) {
      dispatchAnimationsFinished();
    }
  }

  @Override
  public void endAnimations() {
    int count = mPendingMoves.size();
    for (int i = count - 1; i >= 0; i--) {
      MoveInfo item = mPendingMoves.get(i);
      View view = item.holder.itemView;
      ViewCompat.setTranslationY(view, 0);
      ViewCompat.setTranslationX(view, 0);
      dispatchMoveFinished(item.holder);
      mPendingMoves.remove(i);
    }
    count = mPendingRemovals.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = mPendingRemovals.get(i);
      dispatchRemoveFinished(item);
      mPendingRemovals.remove(i);
    }
    count = mPendingAdditions.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = mPendingAdditions.get(i);
      View view = item.itemView;
      addAnimationCleanup(item);
      dispatchAddFinished(item);
      mPendingAdditions.remove(i);
    }
    count = mPendingChanges.size();
    for (int i = count - 1; i >= 0; i--) {
      endChangeAnimationIfNecessary(mPendingChanges.get(i));
    }
    mPendingChanges.clear();
    if (!isRunning()) {
      return;
    }

    int listCount = mMovesList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<MoveInfo> moves = mMovesList.get(i);
      count = moves.size();
      for (int j = count - 1; j >= 0; j--) {
        MoveInfo moveInfo = moves.get(j);
        ViewHolder item = moveInfo.holder;
        View view = item.itemView;
        ViewCompat.setTranslationY(view, 0);
        ViewCompat.setTranslationX(view, 0);
        dispatchMoveFinished(moveInfo.holder);
        moves.remove(j);
        if (moves.isEmpty()) {
          mMovesList.remove(moves);
        }
      }
    }
    listCount = mAdditionsList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<ViewHolder> additions = mAdditionsList.get(i);
      count = additions.size();
      for (int j = count - 1; j >= 0; j--) {
        ViewHolder item = additions.get(j);
        View view = item.itemView;
        addAnimationCleanup(item);
        dispatchAddFinished(item);
        additions.remove(j);
        if (additions.isEmpty()) {
          mAdditionsList.remove(additions);
        }
      }
    }
    listCount = mChangesList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<ChangeInfo> changes = mChangesList.get(i);
      count = changes.size();
      for (int j = count - 1; j >= 0; j--) {
        endChangeAnimationIfNecessary(changes.get(j));
        if (changes.isEmpty()) {
          mChangesList.remove(changes);
        }
      }
    }

    cancelAll(mRemoveAnimations);
    cancelAll(mMoveAnimations);
    cancelAll(mAddAnimations);
    cancelAll(mChangeAnimations);

    dispatchAnimationsFinished();
  }

  void cancelAll(List<ViewHolder> viewHolders) {
    for (int i = viewHolders.size() - 1; i >= 0; i--) {
      ViewCompat.animate(viewHolders.get(i).itemView).cancel();
    }
  }

  @Override
  public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder, List<Object> payloads) {
    boolean hasPayloads = !payloads.isEmpty();
    return hasPayloads || super.canReuseUpdatedViewHolder(viewHolder, payloads);
  }

  private static class VpaListenerAdapter implements ViewPropertyAnimatorListener {
    @Override
    public void onAnimationStart(View view) {
    }

    @Override
    public void onAnimationEnd(View view) {
    }

    @Override
    public void onAnimationCancel(View view) {
    }
  }
}
