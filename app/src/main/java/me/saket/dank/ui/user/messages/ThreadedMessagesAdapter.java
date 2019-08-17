package me.saket.dank.ui.user.messages;

import androidx.annotation.CheckResult;
import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

/**
 * Provides messages in a {@link InboxFolder#PRIVATE_MESSAGES} thread.
 */
public class ThreadedMessagesAdapter extends RecyclerViewArrayAdapter<PrivateMessageUiModel, ThreadedMessagesAdapter.MessageViewHolder> {

  private BetterLinkMovementMethod linkMovementMethod;
  private Relay<PrivateMessageUiModel> messageClickStream = PublishRelay.create();

  private static final int VIEW_TYPE_RECEIVED = 1;
  private static final int VIEW_TYPE_SENT = 2;

  public ThreadedMessagesAdapter(BetterLinkMovementMethod linkMovementMethod) {
    this.linkMovementMethod = linkMovementMethod;
    setHasStableIds(true);
  }

  @CheckResult
  public Observable<PrivateMessageUiModel> streamMessageClicks() {
    return messageClickStream;
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).adapterId();
  }

  @Override
  public int getItemViewType(int position) {
    PrivateMessageUiModel item = getItem(position);
    return item.senderType() == PrivateMessageUiModel.Direction.RECEIVED ? VIEW_TYPE_RECEIVED : VIEW_TYPE_SENT;
  }

  @Override
  protected MessageViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    @LayoutRes int layoutRes;
    switch (viewType) {
      case VIEW_TYPE_RECEIVED:
        layoutRes = R.layout.list_item_threaded_message_start_aligned;
        break;

      case VIEW_TYPE_SENT:
        layoutRes = R.layout.list_item_threaded_message_end_aligned;
        break;

      default:
        throw new AssertionError();
    }
    return new MessageViewHolder(inflater.inflate(layoutRes, parent, false), linkMovementMethod, this, messageClickStream);
  }

  @Override
  public void onBindViewHolder(MessageViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  static class MessageViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.threadedmessage_item_byline) TextView bylineView;
    @BindView(R.id.threadedmessage_item_body) TextView messageBodyView;

    public MessageViewHolder(View itemView, BetterLinkMovementMethod linkMovementMethod, ThreadedMessagesAdapter adapter,
        Relay<PrivateMessageUiModel> messageClickStream)
    {
      super(itemView);
      ButterKnife.bind(this, itemView);

      itemView.setOnClickListener(v -> messageClickStream.accept(adapter.getItem(getAdapterPosition())));

      // Bug workaround: TextView with clickable spans consume all touch events. Manually
      // transfer them to the parent so that the background touch indicator shows up +
      // click listener works.
      messageBodyView.setOnTouchListener((__, event) -> {
        boolean handledByMovementMethod = messageBodyView.getMovementMethod().onTouchEvent(
            messageBodyView, ((Spannable) messageBodyView.getText()),
            event
        );
        return handledByMovementMethod || itemView.onTouchEvent(event);
      });
      messageBodyView.setMovementMethod(linkMovementMethod);
    }

    public void bind(PrivateMessageUiModel messageUiModel) {
      //authorNameView.setText(messageUiModel.senderName());
      bylineView.setText(messageUiModel.byline());
      messageBodyView.setText(messageUiModel.messageBody());
      itemView.setClickable(messageUiModel.isClickable());
    }
  }
}
