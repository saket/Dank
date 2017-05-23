package me.saket.dank.ui.user.messages;

import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.Message;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

/**
 * Provides messages in a {@link InboxFolder#PRIVATE_MESSAGES} thread.
 */
public class ThreadedMessagesAdapter extends RecyclerViewArrayAdapter<Message, ThreadedMessagesAdapter.MessageViewHolder>
    implements Consumer<List<Message>>
{

  private BetterLinkMovementMethod linkMovementMethod;

  public ThreadedMessagesAdapter(BetterLinkMovementMethod linkMovementMethod) {
    this.linkMovementMethod = linkMovementMethod;
    setHasStableIds(true);
  }

  @Override
  public void accept(@NonNull List<Message> messages) {
    updateData(messages);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).getId().hashCode();
  }

  @Override
  protected MessageViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return MessageViewHolder.create(inflater, parent, linkMovementMethod);
  }

  @Override
  public void onBindViewHolder(MessageViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  static class MessageViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.threadedmessage_item_author) TextView authorNameView;
    @BindView(R.id.threadedmessage_item_timestamp) TextView timestampView;
    @BindView(R.id.threadedmessage_item_body) TextView messageBodyView;

    private final BetterLinkMovementMethod linkMovementMethod;

    public static MessageViewHolder create(LayoutInflater inflater, ViewGroup parent, BetterLinkMovementMethod linkMovementMethod) {
      return new MessageViewHolder(inflater.inflate(R.layout.list_item_threaded_message, parent, false), linkMovementMethod);
    }

    public MessageViewHolder(View itemView, BetterLinkMovementMethod linkMovementMethod) {
      super(itemView);
      this.linkMovementMethod = linkMovementMethod;
      ButterKnife.bind(this, itemView);

      // Bug workaround: TextView with clickable spans consume all touch events. Manually
      // transfer them to the parent so that the background touch indicator shows up +
      // click listener works.
      messageBodyView.setOnTouchListener((__, event) -> {
        boolean handledByMovementMethod = linkMovementMethod.onTouchEvent(messageBodyView, ((Spannable) messageBodyView.getText()), event);
        return handledByMovementMethod || itemView.onTouchEvent(event);
      });
    }

    public void bind(Message message) {
      String senderName = message.getAuthor() == null
          ? itemView.getResources().getString(R.string.subreddit_name_r_prefix, message.getSubreddit())
          : message.getAuthor();
      authorNameView.setText(senderName);

      timestampView.setText(Dates.createTimestamp(itemView.getResources(), JrawUtils.createdTimeUtc(message)));

      String bodyHtml = JrawUtils.messageBodyHtml(message);
      messageBodyView.setText(Markdown.parseRedditMarkdownHtml(bodyHtml, messageBodyView.getPaint()));
      messageBodyView.setMovementMethod(linkMovementMethod);
    }
  }

}
