package me.saket.dank.utils;

import android.graphics.Canvas;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and
 * swipe-to-dismiss. Drag events are automatically started by an item long-press.<br/>
 * </br/>
 * Copied from https://github.com/ipaulpro/Android-ItemTouchHelper-Demo.
 *
 * @author Paul Burke (ipaulpro)
 */
public abstract class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

  public static final float ALPHA_FULL = 1.0f;

  public interface DraggableViewHolder {
    /**
     * Called when the {@link ItemTouchHelper} first registers an item as being moved or swiped.
     * Implementations should update the item view to indicate it's active state.
     */
    void onDragStart();

    /**
     * Called when the {@link ItemTouchHelper} has completed the move or swipe, and the active item
     * state should be cleared.
     */
    void onDragEnd();
  }

  @Override
  public boolean isLongPressDragEnabled() {
    return true;
  }

  @Override
  public boolean isItemViewSwipeEnabled() {
    return true;
  }

  @Override
  public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
    if (!(viewHolder instanceof DraggableViewHolder)) {
      return makeMovementFlags(0, 0);
    }

    // Set movement flags based on the layout manager
    if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
      final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
      final int swipeFlags = 0;
      return makeMovementFlags(dragFlags, swipeFlags);
    } else {
      final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
      final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
      return makeMovementFlags(dragFlags, swipeFlags);
    }
  }

  @Override
  public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
    if (source.getItemViewType() != target.getItemViewType()) {
      return false;
    }
    onItemMove(source, target);
    return true;
  }

  protected abstract void onItemMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target);

  @Override
  public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
    onItemDismiss(viewHolder.getAdapterPosition());
  }

  protected void onItemDismiss(int adapterPosition) {
    throw new AbstractMethodError();
  }

  @Override
  public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
      boolean isCurrentlyActive)
  {
    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
      // Fade out the view as it is swiped out of the parent's bounds
      final float alpha = ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
      viewHolder.itemView.setAlpha(alpha);
      viewHolder.itemView.setTranslationX(dX);
    } else {
      viewHolder.itemView.setTranslationY(dY);
    }
  }

  @Override
  public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
    // We only want the active item to change
    if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
      if (viewHolder instanceof DraggableViewHolder) {
        // Let the view holder know that this item is being moved or dragged
        DraggableViewHolder itemViewHolder = (DraggableViewHolder) viewHolder;
        itemViewHolder.onDragStart();
      }
    }
  }

  @Override
  public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
    viewHolder.itemView.setAlpha(ALPHA_FULL);

    if (viewHolder instanceof DraggableViewHolder) {
      // Tell the view holder it's time to restore the idle state
      DraggableViewHolder itemViewHolder = (DraggableViewHolder) viewHolder;
      itemViewHolder.onDragEnd();
    }
  }
}
