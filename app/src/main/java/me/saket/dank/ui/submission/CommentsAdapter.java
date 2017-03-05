package me.saket.dank.ui.submission;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Flair;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Views;
import rx.functions.Action1;

public class CommentsAdapter extends RecyclerViewArrayAdapter<SubmissionCommentItem, RecyclerView.ViewHolder>
        implements Action1<List<SubmissionCommentItem>>
{

    private static final int VIEW_TYPE_USER_COMMENT = 100;
    private static final int VIEW_TYPE_LOAD_MORE = 101;

    private static int startPaddingForRootComment;
    private static int startPaddingPerDepthLevel;

    private ClickListeners clickListeners;

    interface ClickListeners {
        /**
         * Called when a comment is clicked.
         */
        void onCommentClick(CommentNode clickedCommentNode);

        /**
         * Called when more comments have to be loaded for <var>parentCommentNode</var>.
         */
        void onLoadMoreCommentsClick(CommentNode parentCommentNode);
    }

    public CommentsAdapter(Resources resources) {
        setHasStableIds(true);
        startPaddingForRootComment = resources.getDimensionPixelSize(R.dimen.comment_start_padding_for_root_comment);
        startPaddingPerDepthLevel = resources.getDimensionPixelSize(R.dimen.comment_start_padding_per_depth_level);
    }

    public void setClickListeners(ClickListeners listener) {
        clickListeners = listener;
    }

    @Override
    public void call(List<SubmissionCommentItem> commentNodes) {
        updateData(commentNodes);
    }

    @Override
    public int getItemViewType(int position) {
        SubmissionCommentItem commentItem = getItem(position);
        return commentItem.type() == SubmissionCommentItem.Type.USER_COMMENT ? VIEW_TYPE_USER_COMMENT : VIEW_TYPE_LOAD_MORE;
    }

    @Override
    protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER_COMMENT) {
            return UserCommentViewHolder.create(inflater, parent);

        } else {
            return LoadMoreCommentViewHolder.create(inflater, parent);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SubmissionCommentItem commentItem = getItem(position);

        if (commentItem.type() == SubmissionCommentItem.Type.USER_COMMENT) {
            CommentNode commentNode = ((DankUserCommentNode) commentItem).commentNode();
            ((UserCommentViewHolder) holder).bind(commentNode);

            holder.itemView.setOnClickListener(__ -> {
                clickListeners.onCommentClick(commentNode);
            });

        } else {
            LoadMoreCommentsItem loadMoreItem = ((LoadMoreCommentsItem) commentItem);
            ((LoadMoreCommentViewHolder) holder).bind(loadMoreItem);

            holder.itemView.setOnClickListener(__ -> {
                clickListeners.onLoadMoreCommentsClick(loadMoreItem.parentCommentNode());
            });
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id();
    }

    public static class UserCommentViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_comment_author_username) TextView authorNameView;
        @BindView(R.id.item_comment_author_flair) TextView authorFlairView;
        @BindView(R.id.item_comment_body) TextView commentBodyView;

        public static UserCommentViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new UserCommentViewHolder(inflater.inflate(R.layout.list_item_comment, parent, false));
        }

        public UserCommentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(CommentNode commentNode) {
            applyDepthIndentation(itemView, commentNode.getDepth());

            // Author name, comment.
            authorNameView.setText(String.format("%s (%s)", commentNode.getComment().getAuthor(), commentNode.getComment().getScore()));
            commentBodyView.setText(commentNode.getComment().getBody().trim());

            // Flair.
            Flair authorFlair = commentNode.getComment().getAuthorFlair();
            authorFlairView.setText(authorFlair != null ? authorFlair.getText() : null);
        }
    }

    public static class LoadMoreCommentViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_loadmorecomments_reply_count) TextView replyCountView;

        public static LoadMoreCommentViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new LoadMoreCommentViewHolder(inflater.inflate(R.layout.list_item_load_more_comments, parent, false));
        }

        public LoadMoreCommentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(LoadMoreCommentsItem loadMoreCommentsItem) {
            CommentNode parentCommentNode = loadMoreCommentsItem.parentCommentNode();

            // Add a +1 depth to align it with sibling comments.
            applyDepthIndentation(itemView, parentCommentNode.getDepth() + 1);

            Integer replyCount = parentCommentNode.getMoreChildren().getCount();
            replyCountView.setText(itemView.getResources().getString(R.string.submission_loadmorecomments_reply_count, replyCount));
        }
    }

    public static void applyDepthIndentation(View itemView, int depth) {
        if (depth == 1) {
            Views.setPaddingStart(itemView, startPaddingForRootComment);
        } else {
            Views.setPaddingStart(itemView, startPaddingPerDepthLevel * depth);
        }
    }

}
