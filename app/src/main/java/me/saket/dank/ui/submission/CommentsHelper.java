package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.ioIfNeeded;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Helps in flattening a comments tree with collapsed child comments ignored.
 */
public class CommentsHelper {

    private Set<String> collapsedCommentNodeIds = new HashSet<>();    // Note: !CommentNode.hashCode() crashes so using a Set isn't possible.
    private CommentNode rootCommentNode;

    /**
     * Set the root comment of a submission.
     */
    public Action1<Submission> setupWith() {
        return submission -> rootCommentNode = submission.getComments();
    }

    public void reset() {
        rootCommentNode = null;
        collapsedCommentNodeIds.clear();
    }

    public Action1<CommentNode> toggleCollapse() {
        return commentNode -> {
            if (isCollapsed(commentNode)) {
                collapsedCommentNodeIds.remove(commentNode.getComment().getId());
            } else {
                collapsedCommentNodeIds.add(commentNode.getComment().getId());
            }
        };
    }

    private boolean isCollapsed(CommentNode commentNode) {
        return collapsedCommentNodeIds.contains(commentNode.getComment().getId());
    }

    /**
     * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
     */
    public Func1<Object, Observable<List<SubmissionCommentsRow>>> constructComments() {
        return __ -> Observable
                .fromCallable(() -> constructComments(new ArrayList<>(rootCommentNode.getTotalSize()), rootCommentNode))
                .subscribeOn(ioIfNeeded())
                .compose(applySchedulers());
    }

    /**
     * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
     */
    private List<SubmissionCommentsRow> constructComments(List<SubmissionCommentsRow> flattenComments, CommentNode nextNode) {
        String indentation = "";
        if (nextNode.getDepth() != 0) {
            for (int step = 0; step < nextNode.getDepth(); step++) {
                indentation += "  ";
            }
        }

        boolean isCommentNodeCollapsed = isCollapsed(nextNode);
        if (nextNode.getDepth() != 0) {
//            Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getId(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
            flattenComments.add(DankCommentNode.create(nextNode, isCommentNodeCollapsed));
        }

        if (nextNode.isEmpty() && !nextNode.hasMoreComments()) {
            return flattenComments;

        } else {
            // Ignore collapsed children.
            if (!isCommentNodeCollapsed) {
                List<CommentNode> childCommentsTree = nextNode.getChildren();
                for (CommentNode node : childCommentsTree) {
                    constructComments(flattenComments, node);
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
