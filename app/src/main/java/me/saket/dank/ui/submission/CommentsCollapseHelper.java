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
    private List<CommentNode> collapsedCommentNodes = new LinkedList<>();

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

    public List<CommentNode> toggleCollapseAndGet(CommentNode commentNode) {
        if (isCollapsed(commentNode)) {
            return expandAndGet(commentNode);
        } else {
            return collapseAndGet(commentNode);
        }
    }

    private List<CommentNode> collapseAndGet(CommentNode commentNode) {
        collapsedCommentNodes.add(commentNode);
        return flattenExpandedComments();
    }

    private List<CommentNode> expandAndGet(CommentNode commentNode) {
        collapsedCommentNodes.remove(commentNode);
        return flattenExpandedComments();
    }

    private boolean isCollapsed(CommentNode commentNode) {
        return collapsedCommentNodes.contains(commentNode);
    }

    /**
     * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
     */
    public List<CommentNode> flattenExpandedComments() {
        return flattenExpandedComments(new ArrayList<>(rootCommentNode.getTotalSize()), rootCommentNode);
    }

    /**
     * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
     */
    private List<CommentNode> flattenExpandedComments(List<CommentNode> flattenComments, CommentNode nextNode) {
        List<CommentNode> childCommentsTree = nextNode.getChildren();

        if (childCommentsTree.isEmpty() && nextNode.getDepth() != 0) {
            flattenComments.add(nextNode);
            return flattenComments;

        } else {
            // Ignore the root node. It is the root of all top-level comments,
            // but doesn't have any comment of its own.
            if (nextNode.getDepth() != 0) {
                flattenComments.add(nextNode);
            }

            // Ignore collapsed children.
            if (!isCollapsed(nextNode)) {
                for (CommentNode node : childCommentsTree) {
                    flattenExpandedComments(flattenComments, node);
                }
            }

            return flattenComments;
        }
    }

}
