package me.saket.dank.utils;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.ui.submission.PendingSyncReply;

public class MoshiPostedOrInFlightContributionAdapterFactoryTest {

  @Test
  public void test() throws IOException {
    Moshi moshi = new Moshi.Builder()
        .add(AutoValueMoshiAdapterFactory.create())
        .build();

    MoshiPostedOrInFlightContributionAdapterFactory factory = new MoshiPostedOrInFlightContributionAdapterFactory();
    //noinspection unchecked
    JsonAdapter<PostedOrInFlightContribution> jsonAdapter = (JsonAdapter<PostedOrInFlightContribution>) factory.create(
        PostedOrInFlightContribution.class,
        Collections.emptySet(),
        moshi
    );

    PendingSyncReply mockPendingSyncReply = mock(PendingSyncReply.class);
    when(mockPendingSyncReply.postedFullName()).thenReturn("postedFullName");
    when(mockPendingSyncReply.parentContributionFullName()).thenReturn("parentContributionFullName");
    when(mockPendingSyncReply.state()).thenReturn(PendingSyncReply.State.FAILED);
    when(mockPendingSyncReply.createdTimeMillis()).thenReturn(System.currentTimeMillis());

    PostedOrInFlightContribution value = PostedOrInFlightContribution.createLocal(mockPendingSyncReply);
    //noinspection ConstantConditions
    PostedOrInFlightContribution deserializedValue = jsonAdapter.fromJson(jsonAdapter.toJson(value));

    assertEquals(value, deserializedValue);
  }
}
