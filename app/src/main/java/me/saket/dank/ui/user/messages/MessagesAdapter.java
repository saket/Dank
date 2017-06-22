package me.saket.dank.ui.user.messages;

import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Message;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Dates;
import timber.log.Timber;

public class MessagesAdapter extends RecyclerViewArrayAdapter<Message, RecyclerView.ViewHolder> implements Consumer<List<Message>> {

  private static final int VIEW_TYPE_PRIVATE_MESSAGE_THREAD = 100;
  private static final int VIEW_TYPE_INDIVIDUAL_MESSAGE = 101;

  private BetterLinkMovementMethod linkMovementMethod;
  private boolean showMessageThreads;
  private String loggedInUserName;
  private OnMessageClickListener onMessageClickListener;

  interface OnMessageClickListener {
    void onClickMessage(Message message, View messageItemView);
  }

  /**
   * @param showMessageThreads used for {@link InboxFolder#PRIVATE_MESSAGES}.
   * @param loggedInUserName Used for prefixing the body with "You: " for user sent messages.
   */
  public MessagesAdapter(BetterLinkMovementMethod linkMovementMethod, boolean showMessageThreads, String loggedInUserName) {
    this.linkMovementMethod = linkMovementMethod;
    this.showMessageThreads = showMessageThreads;
    this.loggedInUserName = loggedInUserName;
    setHasStableIds(true);
  }

  public void setOnMessageClickListener(OnMessageClickListener listener) {
    onMessageClickListener = listener;
  }

  @Override
  public void accept(@NonNull List<Message> messages) {
    updateDataAndNotifyDatasetChanged(messages);
  }

  @Override
  public int getItemViewType(int position) {
    return showMessageThreads ? VIEW_TYPE_PRIVATE_MESSAGE_THREAD : VIEW_TYPE_INDIVIDUAL_MESSAGE;
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_INDIVIDUAL_MESSAGE) {
      return IndividualMessageViewHolder.create(inflater, parent, linkMovementMethod);
    } else {
      return MessageThreadViewHolder.create(inflater, parent);
    }
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).getId().hashCode();
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    Message message = getItem(position);

    if (getItemViewType(position) == VIEW_TYPE_INDIVIDUAL_MESSAGE) {
      ((IndividualMessageViewHolder) holder).bind(message);
    } else {
      ((MessageThreadViewHolder) holder).bind(message, loggedInUserName);
    }

    holder.itemView.setOnClickListener(o -> {
      onMessageClickListener.onClickMessage(message, holder.itemView);
    });
  }

  static class MessageThreadViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.messagethread_second_party) TextView secondPartyNameView;
    @BindView(R.id.messagethread_subject) TextView subjectView;
    @BindView(R.id.messagethread_snippet) TextView snippetView;
    @BindView(R.id.messagethread_timestamp) TextView timestampView;

    public static MessageThreadViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new MessageThreadViewHolder(inflater.inflate(R.layout.list_item_message_thread, parent, false));
    }

    public MessageThreadViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(Message message, String loggedInUserName) {
      List<Message> messageReplies = JrawUtils.messageReplies(message);
      Message latestMessageInThread = messageReplies.isEmpty()
          ? message
          : messageReplies.get(messageReplies.size() - 1);
      bind(message, latestMessageInThread, loggedInUserName);
    }

    private void bind(Message parentMessage, Message latestMessageInThread, String loggedInUserName) {
      String secondPartyName = JrawUtils.secondPartyName(itemView.getResources(), parentMessage, loggedInUserName);
      secondPartyNameView.setText(secondPartyName);
      secondPartyNameView.setVisibility(secondPartyName != null && !secondPartyName.isEmpty() ? View.VISIBLE : View.GONE);

      if (secondPartyName == null || secondPartyName.isEmpty()) {
        Timber.e(new IllegalStateException("Second party name is null. Message: " + parentMessage.getDataNode().toString()), "Empty second party");
      }

      subjectView.setText(parentMessage.getSubject());
      timestampView.setText(Dates.createTimestamp(itemView.getResources(), JrawUtils.createdTimeUtc(latestMessageInThread)));

      // TODO Cache this: This is taking too long to bind.
      String bodyHtml = JrawUtils.messageBodyHtml(latestMessageInThread);
      String bodyWithoutHtml = Markdown.stripMarkdown(bodyHtml);
      bodyWithoutHtml = bodyWithoutHtml.replace("\n", " ");

      boolean wasLastMessageBySelf = loggedInUserName.equalsIgnoreCase(latestMessageInThread.getAuthor());  // Author can be null.
      snippetView.setText(wasLastMessageBySelf
          ? itemView.getResources().getString(R.string.inbox_snippet_sent_by_logged_in_user, bodyWithoutHtml)
          : bodyWithoutHtml
      );
    }
  }

  static class IndividualMessageViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.individualmessage_reply_post_title) TextView linkTitleView;
    @BindView(R.id.individualmessage_reply_timestamp) TextView timestampView;
    @BindView(R.id.individualmessage_reply_author_name) TextView authorNameView;
    @BindView(R.id.individualmessage_reply_from) TextView fromView;
    @BindView(R.id.individualmessage_reply_body) TextView messageBodyView;

    private final BetterLinkMovementMethod linkMovementMethod;

    public static IndividualMessageViewHolder create(LayoutInflater inflater, ViewGroup parent, BetterLinkMovementMethod linkMovementMethod) {
      return new IndividualMessageViewHolder(inflater.inflate(R.layout.list_item_individual_message, parent, false), linkMovementMethod);
    }

    public IndividualMessageViewHolder(View itemView, BetterLinkMovementMethod linkMovementMethod) {
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
      if (message.isComment()) {
        linkTitleView.setText(((CommentMessage) message).getLinkTitle());
        fromView.setText(itemView.getResources().getString(R.string.subreddit_name_r_prefix, message.getSubreddit()));
      } else {
        linkTitleView.setText(message.getSubject());
      }

      // TODO: Send PR for these custom fields to JRAW.
      // TODO: Should we cache these markdown?
      String bodyHtml = JrawUtils.messageBodyHtml(message);
      messageBodyView.setText(Markdown.parseRedditMarkdownHtml(bodyHtml, messageBodyView.getPaint()));
      messageBodyView.setMovementMethod(linkMovementMethod);

      authorNameView.setText(message.getAuthor());
      timestampView.setText(Dates.createTimestamp(itemView.getResources(), JrawUtils.createdTimeUtc(message)));
    }

  }
}
