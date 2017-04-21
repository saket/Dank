package me.saket.dank.ui.user.messages;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.Message;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import rx.functions.Action1;

public class MessagesAdapter extends RecyclerViewArrayAdapter<Message, MessagesAdapter.MessageViewHolder> implements Action1<List<Message>> {

    @Override
    protected MessageViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return MessageViewHolder.create(inflater, parent);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = getItem(position);
        holder.bind(message);
    }

    @Override
    public void call(List<Message> messages) {
        updateData(messages);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_reply_post_title) TextView postTitleView;
        @BindView(R.id.message_reply_timestamp) TextView timestampView;
        @BindView(R.id.message_reply_author_name) TextView authorNameView;
        @BindView(R.id.message_reply_subreddit_name) TextView subredditNameView;
        @BindView(R.id.message_reply_body) TextView messageBodyView;

        public static MessageViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new MessageViewHolder(inflater.inflate(R.layout.list_item_message, parent, false));
        }

        public MessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Message message) {
            authorNameView.setText(message.getAuthor());
            messageBodyView.setText(message.getBody());
            postTitleView.setText(message.getParentId());
            subredditNameView.setText(message.getSubreddit());

            long createdTimeMs = message.getCreated().getTime();
            if (createdTimeMs < DateUtils.MINUTE_IN_MILLIS) {
                timestampView.setText(R.string.messages_timestamp_just_now);
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
