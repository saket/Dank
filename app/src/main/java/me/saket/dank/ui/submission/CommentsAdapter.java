package me.saket.dank.ui.submission;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Flair;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Views;

public class CommentsAdapter extends RecyclerViewArrayAdapter<SubmissionCommentsRow, RecyclerView.ViewHolder>
        implements Consumer<List<SubmissionCommentsRow>>
{

    private static final int VIEW_TYPE_USER_COMMENT = 100;
    private static final int VIEW_TYPE_LOAD_MORE = 101;

    private static int startPaddingForRootComment;
    private static int startPaddingPerDepthLevel;
    private final BetterLinkMovementMethod linkMovementMethod;

    private PublishRelay<CommentNode> commentClickSubject = PublishRelay.create();
    private PublishRelay<LoadMoreCommentsClickEvent> loadMoreCommentsClickSubject = PublishRelay.create();

    class LoadMoreCommentsClickEvent {
        /**
         * Node whose more comments have to be fetched.
         */
        CommentNode parentCommentNode;

        /**
         * Clicked itemView.
         */
        View loadMoreItemView;

        public LoadMoreCommentsClickEvent(CommentNode parentNode, View loadMoreItemView) {
            this.parentCommentNode = parentNode;
            this.loadMoreItemView = loadMoreItemView;
        }
    }

    public CommentsAdapter(Resources resources, BetterLinkMovementMethod commentsLinkMovementMethod) {
        setHasStableIds(true);
        startPaddingForRootComment = resources.getDimensionPixelSize(R.dimen.comment_start_padding_for_root_comment);
        startPaddingPerDepthLevel = resources.getDimensionPixelSize(R.dimen.comment_start_padding_per_depth_level);

        linkMovementMethod = commentsLinkMovementMethod;
    }

    /**
     * Emits a CommentNodes when it's clicked.
     */
    public PublishRelay<CommentNode> commentClicks() {
        return commentClickSubject;
    }

    /**
     * Emits a CommentNode whose "load more comments" is clicked.
     */
    public PublishRelay<LoadMoreCommentsClickEvent> loadMoreCommentsClicks() {
        return loadMoreCommentsClickSubject;
    }

    @Override
    public void accept(List<SubmissionCommentsRow> commentNodes) {
        updateData(commentNodes);
    }

    @Override
    public int getItemViewType(int position) {
        SubmissionCommentsRow commentItem = getItem(position);
        return commentItem.type() == SubmissionCommentsRow.Type.USER_COMMENT ? VIEW_TYPE_USER_COMMENT : VIEW_TYPE_LOAD_MORE;
    }

    @Override
    protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER_COMMENT) {
            return UserCommentViewHolder.create(inflater, parent, linkMovementMethod);
        } else {
            return LoadMoreCommentViewHolder.create(inflater, parent);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SubmissionCommentsRow commentItem = getItem(position);

        if (commentItem.type() == SubmissionCommentsRow.Type.USER_COMMENT) {
            CommentNode commentNode = ((DankCommentNode) commentItem).commentNode();
            ((UserCommentViewHolder) holder).bind(commentNode);
            ((UserCommentViewHolder) holder).itemView.setOnClickListener(v -> {
                commentClickSubject.accept(commentNode);
            });

        } else {
            LoadMoreCommentsItem loadMoreItem = ((LoadMoreCommentsItem) commentItem);
            ((LoadMoreCommentViewHolder) holder).bind(loadMoreItem);

            holder.itemView.setOnClickListener(__ -> {
                loadMoreCommentsClickSubject.accept(new LoadMoreCommentsClickEvent(loadMoreItem.parentCommentNode(), holder.itemView));
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
        private BetterLinkMovementMethod linkMovementMethod;

        public static UserCommentViewHolder create(LayoutInflater inflater, ViewGroup parent, BetterLinkMovementMethod linkMovementMethod) {
            return new UserCommentViewHolder(inflater.inflate(R.layout.list_item_comment, parent, false), linkMovementMethod);
        }

        public UserCommentViewHolder(View itemView, BetterLinkMovementMethod linkMovementMethod) {
            super(itemView);
            this.linkMovementMethod = linkMovementMethod;

            ButterKnife.bind(this, itemView);

            // Bug workaround: TextView with clickable spans consume all touch events. Manually
            // transfer them to the parent so that the background touch indicator shows up +
            // click listener works.
            commentBodyView.setOnTouchListener((__, event) -> {
                boolean handledByMovementMethod = linkMovementMethod.onTouchEvent(commentBodyView, ((Spannable) commentBodyView.getText()), event);
                return handledByMovementMethod || itemView.onTouchEvent(event);
            });
        }

        public void bind(CommentNode commentNode) {
            applyDepthIndentation(itemView, commentNode.getDepth());

            // Author name, comment.
            authorNameView.setText(String.format("%s (%s)", commentNode.getComment().getAuthor(), commentNode.getComment().getScore()));

            String commentBody = commentNode.getComment().getDataNode().get("body_html").asText();
            commentBodyView.setText(Markdown.parseRedditMarkdownHtml(commentBody, commentBodyView.getPaint()));
            commentBodyView.setMovementMethod(linkMovementMethod);

            // Flair.
            Flair authorFlair = commentNode.getComment().getAuthorFlair();
            authorFlairView.setText(authorFlair != null ? authorFlair.getText() : null);
        }
    }

    public static class LoadMoreCommentViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.item_loadmorecomments_load_more) TextView loadMoreView;

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

            if (loadMoreCommentsItem.progressVisible()) {
                loadMoreView.setText(R.string.submission_loading_more_comments);
            } else {
                if (parentCommentNode.isThreadContinuation()) {
                    loadMoreView.setText(R.string.submission_continue_this_thread);
                } else {
                    loadMoreView.setText(itemView.getResources().getString(
                            R.string.submission_load_more_comments,
                            parentCommentNode.getMoreChildren().getCount()
                    ));
                }
            }
            Views.setCompoundDrawableEnd(loadMoreView, parentCommentNode.isThreadContinuation() ? R.drawable.ic_arrow_forward_12dp : 0);
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
