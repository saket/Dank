package me.saket.dank.ui.submission;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Flair;

import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Views;
import rx.functions.Action1;

public class CommentsAdapter extends RecyclerViewArrayAdapter<CommentNode, CommentsAdapter.CommentViewHolder> implements Action1<List<CommentNode>> {

    private OnCommentClickListener onCommentClickListener;

    interface OnCommentClickListener {
        void onCommentClick(CommentNode clickedComment);
    }

    public CommentsAdapter() {
        setHasStableIds(true);
    }

    public void setOnCommentClickListener(OnCommentClickListener listener) {
        onCommentClickListener = listener;
    }

    @Override
    public void call(List<CommentNode> commentNodes) {
        updateData(commentNodes);
    }

    @Override
    protected CommentViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return CommentViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(CommentViewHolder holder, int position) {
        CommentNode commentNode = getItem(position);
        holder.bind(commentNode);

        holder.itemView.setOnClickListener(v -> {
            onCommentClickListener.onCommentClick(commentNode);
        });
    }

    @Override
    public long getItemId(int position) {
        CommentNode commentNode = getItem(position);
        return commentNode.getComment().getId().hashCode();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.comment_item_author_username) TextView authorNameView;
        @BindView(R.id.comment_item_author_flair) TextView authorFlairView;
        @BindView(R.id.comment_item_body) TextView commentBodyView;

        @BindDimen(R.dimen.comment_start_padding_for_root_comment) int startPaddingForRootComment;
        @BindDimen(R.dimen.comment_start_padding_per_depth_level) int startPaddingPerDepthLevel;

        public static CommentViewHolder create(ViewGroup parent) {
            return new CommentViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_comment, parent, false));
        }

        public CommentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(CommentNode commentNode) {
            // Depth.
            if (commentNode.isTopLevel()) {
                Views.setPaddingStart(itemView, startPaddingForRootComment);
            } else {
                Views.setPaddingStart(itemView, startPaddingPerDepthLevel * commentNode.getDepth());
            }

            // Author name, comment.
            authorNameView.setText(commentNode.getComment().getAuthor());
            commentBodyView.setText(commentNode.getComment().getBody().trim());

            // Flair.
            Flair authorFlair = commentNode.getComment().getAuthorFlair();
            authorFlairView.setText(authorFlair != null ? authorFlair.getText() : null);
        }

    }

}
