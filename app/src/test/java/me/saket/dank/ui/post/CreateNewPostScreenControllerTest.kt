package me.saket.dank.ui.post

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import dagger.Lazy
import io.reactivex.subjects.PublishSubject
import me.saket.dank.RxErrorsRule
import me.saket.dank.ui.UiEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateNewPostScreenControllerTest {

  @get:Rule
  val rxErrorsRule = RxErrorsRule()

  private val uiEvents: PublishSubject<UiEvent> = PublishSubject.create()
  private lateinit var controller: CreateNewPostScreenController

  private val screen = mock<CreateNewPostActivity>()
  private val urlValidator = mock<UrlValidator>()
  private val outboxRepository = mock<PostOutboxRepository>()

  @Before
  fun setUp() {
    controller = CreateNewPostScreenController(
        urlValidator = Lazy { urlValidator },
        outboxRepository = Lazy { outboxRepository })

    uiEvents
        .compose(controller)
        .subscribe { uiChange -> uiChange(screen) }
  }

  @Test
  fun `when body can be parsed as a link, resolve post type to link`() {
    val body = "https://link.com"
    whenever(urlValidator.validate(body)).thenReturn(UrlValidator.Result.Valid)

    uiEvents.run {
      onNext(NewPostBodyTextChanged(body))
      onNext(NewPostImageSelectionUpdated(images = emptyList()))
      onNext(NewPostImageSelectionUpdated(images = listOf(ImageToUpload())))
      onNext(NewPostImageSelectionUpdated(images = emptyList()))
    }

    verify(screen, times(2)).setDetectedPostKind(NewPostKind.LINK)
  }

  @Test
  fun `when image(s) are selected, resolve post type to image`() {
    val body = "https://link.com"
    whenever(urlValidator.validate(body)).thenReturn(UrlValidator.Result.Valid)

    uiEvents.run {
      onNext(NewPostBodyTextChanged(body))
      onNext(NewPostImageSelectionUpdated(images = listOf(ImageToUpload())))
    }

    verify(screen).setDetectedPostKind(NewPostKind.IMAGE)
  }

  @Test
  fun `when neither image nor link are present, resolve post type to self-text`() {
    val body = "https://link.com this is the greatest link ever"
    whenever(urlValidator.validate(body)).thenReturn(UrlValidator.Result.Invalid)

    uiEvents.run {
      onNext(NewPostBodyTextChanged(body))
      onNext(NewPostImageSelectionUpdated(images = emptyList()))
      onNext(NewPostImageSelectionUpdated(images = listOf(ImageToUpload())))
      onNext(NewPostImageSelectionUpdated(images = emptyList()))
    }

    verify(screen, times(2)).setDetectedPostKind(NewPostKind.SELF_TEXT)
  }

  @Test
  fun `when backspace is clicked on body and body is empty, move focus to title field`() {
    uiEvents.run {
      onNext(NewPostTitleTextChanged(""))
      onNext(NewPostBodyTextChanged(""))
      onNext(NewPostTitleTextChanged("Mods are asleep"))
      onNext(NewPostBodyTextChanged("Post pictures of..."))
      onNext(NewPostBodyBackspaceClicked)
      onNext(NewPostBodyTextChanged(""))
      onNext(NewPostBodyBackspaceClicked)
    }

    verify(screen).requestFocusOnTitleField()
  }

  @Test
  fun `submit button should remain disabled while title and subreddit are missing`() {
    uiEvents.run {
      onNext(NewPostTitleTextChanged(""))
      onNext(NewPostSubredditSelected("androiddev"))
      onNext(NewPostTitleTextChanged("Mods are asleep"))
      onNext(NewPostTitleTextChanged(""))
    }

    verify(screen, times(3)).setSubmitButtonEnabled(false)
    verify(screen, times(1)).setSubmitButtonEnabled(true)
  }

  @Test
  fun `when submit is clicked then post details should be saved and screen closed`() {
    val title = "Mods are asleep"
    val body = "Post pictures of..."
    val options = NewPostOptions(
        isNsfw = true,
        hasSpoilers = true,
        sendRepliesToInbox = true,
        flair = "flair")

    uiEvents.run {
      onNext(NewPostSubredditSelected("androiddev"))
      onNext(NewPostTitleTextChanged(title))
      onNext(NewPostBodyTextChanged(body))
      onNext(NewPostOptionsUpdated(options))
      onNext(NewPostSubmitClicked)
    }

    verify(outboxRepository).submit(PostToSubmit(title= title, body = body, options = options))
  }

  @Test
  fun `when screen is opened, and a draft exists for the subreddit then pre-fill the draft`() {
    TODO()
  }

  @Test
  fun `when screen is opened, and an outgoing post exists for the subreddit then show progress indicator`() {
    TODO()
  }
}
