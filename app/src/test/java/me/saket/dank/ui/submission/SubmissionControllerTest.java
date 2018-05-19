package me.saket.dank.ui.submission;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.reactivex.subjects.PublishSubject;
import me.saket.dank.ui.UiEvent;

public class SubmissionControllerTest {

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private SubmissionController controller;
  private PublishSubject<UiEvent> events = PublishSubject.create();

  @Mock SubmissionUi screen;
  @Mock SubmissionRepository submissionRepository;

  @Before
  public void setUp() {
    controller = new SubmissionController(() -> submissionRepository);

    events
        .compose(controller)
        .subscribe(uiChanges -> uiChanges.render(screen));
  }
}
