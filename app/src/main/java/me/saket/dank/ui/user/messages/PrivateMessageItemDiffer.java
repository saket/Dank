package me.saket.dank.ui.user.messages;

import java.util.List;

import me.saket.dank.utils.SimpleDiffUtilsCallbacks;

public class PrivateMessageItemDiffer extends SimpleDiffUtilsCallbacks<PrivateMessageUiModel> {

  public static PrivateMessageItemDiffer create(List<PrivateMessageUiModel> oldItems, List<PrivateMessageUiModel> newItems) {
    return new PrivateMessageItemDiffer(oldItems, newItems);
  }

  private PrivateMessageItemDiffer(List<PrivateMessageUiModel> oldItems, List<PrivateMessageUiModel> newItems) {
    super(oldItems, newItems);
  }

  @Override
  public boolean areItemsTheSame(PrivateMessageUiModel oldItem, PrivateMessageUiModel newItem) {
    return oldItem.adapterId() == newItem.adapterId();
  }

  @Override
  protected boolean areContentsTheSame(PrivateMessageUiModel oldItem, PrivateMessageUiModel newItem) {
    return oldItem.equals(newItem);
  }
}
