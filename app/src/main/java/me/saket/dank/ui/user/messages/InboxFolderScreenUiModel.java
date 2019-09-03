package me.saket.dank.ui.user.messages;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import net.dean.jraw.models.Message;

import java.util.List;

import me.saket.dank.utils.SimpleDiffUtilsCallbacks;

public interface InboxFolderScreenUiModel {

  enum Type {
    INDIVIDUAL_MESSAGE,
    MESSAGE_THREAD
  }

  long adapterId();

  Type type();

  Message message();

  interface Adapter<T extends InboxFolderScreenUiModel, VH extends RecyclerView.ViewHolder> {
    VH onCreate(LayoutInflater inflater, ViewGroup parent);

    void onBind(VH holder, T uiModel);

    void onBind(VH holder, T uiModel, List<Object> payloads);
  }

  class ItemDiffer extends SimpleDiffUtilsCallbacks<InboxFolderScreenUiModel> {

    public ItemDiffer(List<InboxFolderScreenUiModel> oldItems, List<InboxFolderScreenUiModel> newItems) {
      super(oldItems, newItems);
    }

    @Override
    public boolean areItemsTheSame(InboxFolderScreenUiModel oldItem, InboxFolderScreenUiModel newItem) {
      return oldItem.adapterId() == newItem.adapterId();
    }

    @Override
    protected boolean areContentsTheSame(InboxFolderScreenUiModel oldItem, InboxFolderScreenUiModel newItem) {
      return oldItem.equals(newItem);
    }
  }
}
