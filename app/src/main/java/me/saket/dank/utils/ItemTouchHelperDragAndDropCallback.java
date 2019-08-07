package me.saket.dank.utils;

import android.graphics.Canvas;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop.
 * Drag events are automatically started by an item long-press.<br/>.
 * </br/>
 * Copied from https://github.com/ipaulpro/Android-ItemTouchHelper-Demo.
 */
public abstract class ItemTouchHelperDragAndDropCallback extends ItemTouchHelper.Callback {

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
  public final boolean isItemViewSwipeEnabled() {
    return false;
  }

  @Override
  public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
    if (!(viewHolder instanceof DraggableViewHolder)) {
      return makeMovementFlags(0, 0);
    }

    if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
      final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
      return makeMovementFlags(dragFlags, 0);
    } else {
      final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
      return makeMovementFlags(dragFlags, 0);
    }
  }

  @Override
  public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
    if (source.getItemViewType() != target.getItemViewType()) {
      return false;
    }
    return onItemMove(source, target);
  }

  protected abstract boolean onItemMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target);

  @Override
  public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void onChildDraw(
      Canvas c,
      RecyclerView recyclerView,
      RecyclerView.ViewHolder viewHolder,
      float dX,
      float dY,
      int actionState,
      boolean isCurrentlyActive)
  {
    viewHolder.itemView.setTranslationY(dY);
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
    if (viewHolder instanceof DraggableViewHolder) {
      // Tell the view holder it's time to restore the idle state
      DraggableViewHolder itemViewHolder = (DraggableViewHolder) viewHolder;
      itemViewHolder.onDragEnd();
    }
  }
}
