package me.saket.dank.ui.user.messages;

import androidx.annotation.CheckResult;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class MessagesAdapter extends RecyclerViewArrayAdapter<InboxFolderScreenUiModel, RecyclerView.ViewHolder>
    implements Consumer<Pair<List<InboxFolderScreenUiModel>, DiffUtil.DiffResult>>
{

  private static final InboxFolderScreenUiModel.Type[] VIEW_TYPES = InboxFolderScreenUiModel.Type.values();

  private final Map<InboxFolderScreenUiModel.Type, InboxFolderScreenUiModel.Adapter> childAdapters;
  private final InboxIndividualMessage.Adapter individualMessageAdapter;
  private final InboxMessageThread.Adapter messageThreadAdapter;

  @Inject
  public MessagesAdapter(InboxIndividualMessage.Adapter individualMessageAdapter, InboxMessageThread.Adapter messageThreadAdapter) {
    this.individualMessageAdapter = individualMessageAdapter;
    this.messageThreadAdapter = messageThreadAdapter;

    childAdapters = new HashMap<>(3);
    childAdapters.put(InboxFolderScreenUiModel.Type.INDIVIDUAL_MESSAGE, individualMessageAdapter);
    childAdapters.put(InboxFolderScreenUiModel.Type.MESSAGE_THREAD, messageThreadAdapter);
    setHasStableIds(true);
  }

  @CheckResult
  Observable<MessageClickEvent> streamMessageClicks() {
    return individualMessageAdapter.messageClicks.mergeWith(messageThreadAdapter.messageClicks);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).adapterId();
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).type().ordinal();
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return childAdapters.get(VIEW_TYPES[viewType]).onCreate(inflater, parent);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position, payloads);
    } else {
      //noinspection unchecked
      childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBind(holder, getItem(position), payloads);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    //noinspection unchecked
    childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBind(holder, getItem(position));
  }

  @Override
  public void accept(Pair<List<InboxFolderScreenUiModel>, DiffUtil.DiffResult> pair) throws Exception {
    updateData(pair.first());
    pair.second().dispatchUpdatesTo(this);
  }
}
