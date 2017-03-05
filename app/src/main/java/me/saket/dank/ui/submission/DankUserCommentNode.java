package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

@AutoValue
public abstract class DankUserCommentNode implements SubmissionCommentItem {

    public abstract CommentNode commentNode();

    public abstract boolean isCollapsed();

    @Override
    public abstract long id();

    @Override
    public SubmissionCommentItem.Type type() {
        return Type.USER_COMMENT;
    }

    public static DankUserCommentNode create(CommentNode commentNode, boolean isCollapsed) {
        int commentId = commentNode.getComment().getId().hashCode();
        return new AutoValue_DankUserCommentNode(commentNode, isCollapsed, commentId);
    }

}
