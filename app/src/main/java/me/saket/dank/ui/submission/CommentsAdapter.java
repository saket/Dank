package me.saket.dank.ui.submission;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Flair;
import net.dean.jraw.models.VoteDirection;

import java.util.List;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.VotingManager;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.IndentedLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;
import timber.log.Timber;

public class CommentsAdapter extends RecyclerViewArrayAdapter<SubmissionCommentsRow, RecyclerView.ViewHolder>
    implements Consumer<List<SubmissionCommentsRow>>
{

  private static final int VIEW_TYPE_USER_COMMENT = 100;
  private static final int VIEW_TYPE_LOAD_MORE = 101;

  private final BetterLinkMovementMethod linkMovementMethod;
  private VotingManager votingManager;
  private final CommentSwipeActionsProvider swipeActionsProvider;

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

  public CommentsAdapter(BetterLinkMovementMethod commentsLinkMovementMethod, VotingManager votingManager,
      CommentSwipeActionsProvider swipeActionsProvider)
  {
    this.linkMovementMethod = commentsLinkMovementMethod;
    this.votingManager = votingManager;
    this.swipeActionsProvider = swipeActionsProvider;
    setHasStableIds(true);
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
      UserCommentViewHolder holder = UserCommentViewHolder.create(inflater, parent, linkMovementMethod);
      holder.getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider);
      return holder;

    } else {
      return LoadMoreCommentViewHolder.create(inflater, parent);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    SubmissionCommentsRow commentItem = getItem(position);

    if (commentItem.type() == SubmissionCommentsRow.Type.USER_COMMENT) {
      CommentNode commentNode = ((DankCommentNode) commentItem).commentNode();
      Comment comment = commentNode.getComment();

      VoteDirection pendingOrDefaultVoteDirection = votingManager.getPendingOrDefaultVote(comment, comment.getVote());
      int commentScore = votingManager.getScoreAfterAdjustingPendingVote(comment);

      UserCommentViewHolder commentViewHolder = (UserCommentViewHolder) holder;
      commentViewHolder.bind(commentNode, pendingOrDefaultVoteDirection, commentScore);

      commentViewHolder.itemView.setOnClickListener(v -> {
        commentClickSubject.accept(commentNode);
      });

      SwipeableLayout swipeableLayout = commentViewHolder.getSwipeableLayout();
      swipeableLayout.setSwipeActions(swipeActionsProvider.getSwipeActions());
      swipeableLayout.setOnPerformSwipeActionListener(action -> {
        swipeActionsProvider.performSwipeAction(action, commentNode, swipeableLayout);

        // We should ideally only be updating the backing data-set and let onBind() handle the
        // changes, but RecyclerView's item animator reset's the View's x-translation which we
        // don't want. So we manually update the Views here.
        onBindViewHolder(holder, position);
      });

    } else {
      LoadMoreCommentsItem loadMoreItem = ((LoadMoreCommentsItem) commentItem);
      ((LoadMoreCommentViewHolder) holder).bind(loadMoreItem);

      holder.itemView.setOnClickListener(__ -> {
        Timber.i("load more click");
        loadMoreCommentsClickSubject.accept(new LoadMoreCommentsClickEvent(loadMoreItem.parentCommentNode(), holder.itemView));
      });
    }
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).id();
  }

  public static class UserCommentViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    @BindView(R.id.item_comment_indented_container) IndentedLayout indentedContainer;
    @BindView(R.id.item_comment_byline) TextView bylineView;
    @BindView(R.id.item_comment_body) TextView commentBodyView;

    @BindColor(R.color.submission_comment_byline_author) int bylineAuthorNameColor;
    @BindString(R.string.submission_comment_byline_item_separator) String bylineItemSeparator;
    @BindString(R.string.submission_comment_byline_item_score) String bylineItemScore;

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

    public void bind(CommentNode commentNode, VoteDirection voteDirection, int commentScore) {
      indentedContainer.setIndentationDepth(commentNode.getDepth() - 1);

      // Byline: author, flair, score and timestamp.
      Flair authorFlair = commentNode.getComment().getAuthorFlair();

      Truss bylineBuilder = new Truss();
      bylineBuilder.pushSpan(new ForegroundColorSpan(bylineAuthorNameColor));
      bylineBuilder.append(commentNode.getComment().getAuthor());
      bylineBuilder.popSpan();
      if (authorFlair != null) {
        bylineBuilder.append(bylineItemSeparator);
        bylineBuilder.append(authorFlair.getText());
      }
      bylineBuilder.append(bylineItemSeparator);
      bylineBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), Commons.voteColor(voteDirection))));
      bylineBuilder.append(String.format(bylineItemScore, Strings.abbreviateScore(commentScore)));
      bylineBuilder.popSpan();
      bylineBuilder.append(bylineItemSeparator);
      bylineBuilder.append(Dates.createTimestamp(itemView.getResources(), JrawUtils.createdTimeUtc(commentNode.getComment())));
      bylineView.setText(bylineBuilder.build());

      // Body.
      String commentBody = commentNode.getComment().getDataNode().get("body_html").asText();
      commentBodyView.setText(Markdown.parseRedditMarkdownHtml(commentBody, commentBodyView.getPaint()));
      commentBodyView.setMovementMethod(linkMovementMethod);
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }

  public static class LoadMoreCommentViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_loadmorecomments_load_more) TextView loadMoreView;
    @BindView(R.id.item_loadmorecomments_indented_container) IndentedLayout indentedContainer;

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
      indentedContainer.setIndentationDepth(parentCommentNode.getDepth());

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
}
