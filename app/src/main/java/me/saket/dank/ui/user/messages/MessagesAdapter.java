package me.saket.dank.ui.user.messages;

import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.CommentMessage;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.UrlParser;
import timber.log.Timber;

public class MessagesAdapter extends RecyclerViewArrayAdapter<Message, MessagesAdapter.MessageViewHolder> implements Consumer<List<Message>> {

    private BetterLinkMovementMethod linkMovementMethod;

    public MessagesAdapter(BetterLinkMovementMethod linkMovementMethod) {
        this.linkMovementMethod = linkMovementMethod;
    }

    @Override
    protected MessageViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return MessageViewHolder.create(inflater, parent, linkMovementMethod);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = getItem(position);
        holder.bind(message);

        holder.itemView.setOnClickListener(o -> {
            // TODO: Check if message is a comment.
            // TODO: Send callback.
            String commentUrl = "https://reddit.com" + message.getDataNode().get("context").asText();
            Timber.i("Clicked url: %s", UrlParser.parse(commentUrl));
        });
    }

    @Override
    public void accept(@NonNull List<Message> messages) {
        updateData(messages);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_reply_post_title) TextView linkTitleView;
        @BindView(R.id.message_reply_timestamp) TextView timestampView;
        @BindView(R.id.message_reply_author_name) TextView authorNameView;
        @BindView(R.id.message_reply_from) TextView fromView;
        @BindView(R.id.message_reply_body) TextView messageBodyView;

        private final BetterLinkMovementMethod linkMovementMethod;

        public static MessageViewHolder create(LayoutInflater inflater, ViewGroup parent, BetterLinkMovementMethod linkMovementMethod) {
            return new MessageViewHolder(inflater.inflate(R.layout.list_item_message, parent, false), linkMovementMethod);
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
            if (message instanceof CommentMessage) {
                // TODO: Send PR for these custom fields to JRAW.
                linkTitleView.setText(message.getDataNode().get("link_title").asText());
                fromView.setText(itemView.getResources().getString(R.string.subreddit_name_r_prefix, message.getSubreddit()));

            } else if (message instanceof PrivateMessage) {
                linkTitleView.setText(message.getSubject());
            }

            // TODO: Should we cache these markdown?
            String bodyHtml = message.getDataNode().get("body_html").asText();
            messageBodyView.setText(Markdown.parseRedditMarkdownHtml(bodyHtml, messageBodyView.getPaint()));
            messageBodyView.setMovementMethod(linkMovementMethod);

            authorNameView.setText(message.getAuthor());

            long createdTimeMs = message.getCreated().getTime();
            if (createdTimeMs < DateUtils.MINUTE_IN_MILLIS) {
                timestampView.setText(R.string.inbox_timestamp_just_now);
            } else {
                timestampView.setText(DateUtils.getRelativeTimeSpanString(
                        createdTimeMs,
                        System.currentTimeMillis(),
                        0,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                ));
            }
        }
    }

}
