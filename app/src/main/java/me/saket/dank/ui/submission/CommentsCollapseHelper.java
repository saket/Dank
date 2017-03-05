package me.saket.dank.ui.submission;

import net.dean.jraw.models.CommentNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Helps in flattening a comments tree with collapsed child comments ignored.
 */
public class CommentsCollapseHelper {

    private CommentNode rootCommentNode;

    // TODO: Replace this with comment IDs.
    private List<CommentNode> collapsedCommentNodes = new LinkedList<>();    // Note: !CommentNode.hashCode() crashes so using a Set isn't possible.

    /**
     * @param rootCommentNode Root comment of a submission.
     */
    public void setupWith(CommentNode rootCommentNode) {
        this.rootCommentNode = rootCommentNode;
    }

    public void reset() {
        rootCommentNode = null;
        collapsedCommentNodes.clear();
    }

    public List<SubmissionCommentItem> toggleCollapseAndGet(CommentNode commentNode) {
        if (isCollapsed(commentNode)) {
            collapsedCommentNodes.remove(commentNode);
        } else {
            collapsedCommentNodes.add(commentNode);
        }
        return flattenExpandedComments();
    }

    private boolean isCollapsed(CommentNode commentNode) {
        return collapsedCommentNodes.contains(commentNode);
    }

    /**
     * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
     */
    public List<SubmissionCommentItem> flattenExpandedComments() {
        return flattenExpandedComments(new ArrayList<>(rootCommentNode.getTotalSize()), rootCommentNode);
    }

    /**
     * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
     */
    private List<SubmissionCommentItem> flattenExpandedComments(List<SubmissionCommentItem> flattenComments, CommentNode nextNode) {
        String indentation = "";
        if (nextNode.getDepth() != 0) {
            for (int step = 0; step < nextNode.getDepth(); step++) {
                indentation += "  ";
            }
        }

        boolean isCommentNodeCollapsed = isCollapsed(nextNode);
        if (nextNode.getDepth() != 0) {
//            Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getId(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
            flattenComments.add(DankUserCommentNode.create(nextNode, isCommentNodeCollapsed));
        }

        if (nextNode.isEmpty() && !nextNode.hasMoreComments()) {
            return flattenComments;

        } else {
            // Ignore collapsed children.
            if (!isCommentNodeCollapsed) {
                List<CommentNode> childCommentsTree = nextNode.getChildren();
                for (CommentNode node : childCommentsTree) {
                    flattenExpandedComments(flattenComments, node);
                }

                if (nextNode.hasMoreComments()) {
//                    Timber.d("%s(%s) %s has %d MORE ---------->",
//                            indentation, nextNode.getComment().getId(), nextNode.getComment().getAuthor(), nextNode.getMoreChildren().getCount()
//                    );
//                    Timber.d("%s %s", indentation, nextNode.getMoreChildren().getChildrenIds());
                    flattenComments.add(LoadMoreCommentsItem.create(nextNode));
                }
            }
            return flattenComments;
        }
    }

}
