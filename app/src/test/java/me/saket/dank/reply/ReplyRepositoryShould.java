package me.saket.dank.reply;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Identifiable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.utils.AutoValueMoshiAdapterFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplyRepositoryShould {

  private static final int RECYCLE_DRAFTS_IN_DAYS = 14;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock SharedPreferences sharedPrefs;
  @Mock SharedPreferences.Editor sharedPrefsEditor;
  @Mock ErrorResolver errorResolver;

  @Captor ArgumentCaptor<String> stringArgCaptor;

  private ReplyRepository replyRepository;
  private JsonAdapter<ReplyDraft> replyDraftJsonAdapter;

  @Before
  @SuppressLint("CommitPrefEdits")
  public void setUp() {
    Moshi moshi = new Moshi.Builder().add(AutoValueMoshiAdapterFactory.create()).build();
    replyRepository = spy(new ReplyRepository(
        null,
        null,
        null,
        sharedPrefs,
        moshi,
        RECYCLE_DRAFTS_IN_DAYS,
        () -> errorResolver));
    replyDraftJsonAdapter = moshi.adapter(ReplyDraft.class);

    when(sharedPrefs.edit()).thenReturn(sharedPrefsEditor);
    when(sharedPrefsEditor.putString(anyString(), anyString())).thenReturn(sharedPrefsEditor);
    when(sharedPrefsEditor.remove(anyString())).thenReturn(sharedPrefsEditor);
  }

  @Test
  @SuppressLint("CommitPrefEdits")
  public void onSaveDraft_shouldSaveDraft_shouldCallRecycleDrafts() throws Exception {
    Identifiable parentComment = mock(Identifiable.class);
    when(parentComment.getFullName()).thenReturn("fullName");
    when(sharedPrefs.getAll()).thenReturn(Collections.emptyMap());

    replyRepository.saveDraft(parentComment, "draft").subscribe();

    // Verify that the draft was saved.
    verify(sharedPrefsEditor).putString(stringArgCaptor.capture(), stringArgCaptor.capture());
    assertEquals(stringArgCaptor.getAllValues().get(0), ReplyRepository.keyForDraft(parentComment));
    ReplyDraft savedReplyDraft = replyDraftJsonAdapter.fromJson(stringArgCaptor.getAllValues().get(1));
    //noinspection ConstantConditions
    assertEquals(savedReplyDraft.body(), "draft");

    verify(replyRepository).recycleOldDrafts(any());
  }

  @Test
  public void onRecycleOldDrafts_shouldCorrectlyRecycleStaleDrafts() {
    Map<String, ReplyDraft> savedDrafts = new HashMap<>();
    DateTime twoWeeksOldDate = DateTime.forInstant(System.currentTimeMillis(), TimeZone.getTimeZone("UTC")).minusDays(RECYCLE_DRAFTS_IN_DAYS + 1);
    long twoWeeksOldTimeMillis = twoWeeksOldDate.getMilliseconds(TimeZone.getTimeZone("UTC"));
    savedDrafts.put("oldKey", ReplyDraft.create("oldDraft", twoWeeksOldTimeMillis));
    savedDrafts.put("newKey", ReplyDraft.create("newDraft", System.currentTimeMillis()));

    replyRepository.recycleOldDrafts(savedDrafts);

    verify(sharedPrefsEditor).remove("oldKey");
    verify(sharedPrefsEditor, never()).remove("newKey");
  }
}
