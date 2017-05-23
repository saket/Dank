package me.saket.dank.ui.user.messages;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.dean.jraw.models.Message;

import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

/**
 * Provides messages in a {@link InboxFolder#PRIVATE_MESSAGES} thread.
 */
public class ThreadedMessagesAdapter extends RecyclerViewArrayAdapter<Message, ThreadedMessagesAdapter.MessageViewHolder> {

  public ThreadedMessagesAdapter() {
    setHasStableIds(true);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).getId().hashCode();
  }

  @Override
  protected MessageViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return MessageViewHolder.create(inflater, parent);
  }

  @Override
  public void onBindViewHolder(MessageViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  static class MessageViewHolder extends RecyclerView.ViewHolder {
    public static MessageViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new MessageViewHolder(inflater.inflate(R.layout.list_item_threaded_message, parent, false));
    }

    public MessageViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(Message message) {

    }
  }

}
