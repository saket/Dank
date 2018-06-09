package me.saket.dank.ui.user.messages

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Types
import me.saket.dank.data.MoshiAdapter
import me.saket.dank.di.StorageModule
import net.dean.jraw.models.Message
import org.junit.Test
import java.lang.reflect.Type

class CachedMessageTest {

  private val json = """
    {"distinguished":null,"id":"c4xo11","score":0,"author":"Intersebbtor","body":"Hey there,\nas i said in the comment, I'd be happy to help you out. so as soon as i get the chance to I'll give you some feedback :)","context":"","created_utc":1528532315,"dest":"Saketme","name":"t4_c4xo11","was_comment":false,"new":true,"subject":"usability","likes":null,"replies":{"kind":"Listing","data":{"after":null,"children":[]}}}
  """.trimIndent()

  private val jsonArray = """
    [
      $json
    ]
  """.trimIndent()

  @Test
  fun serialization() {
    val moshiAdapter = MoshiAdapter(StorageModule().provideMoshi())

    val messagesAdapter = moshiAdapter.create(Array<Message>::class.java)
    val messages: Array<Message> = messagesAdapter.fromJson(jsonArray)!!
    assertThat(messages).isNotEmpty()

    val messageAdapter = moshiAdapter.create(Message::class.java)
    val message = messageAdapter.fromJson(json)!!
    val serializedMessage = messageAdapter.toJson(message)
    assertThat(serializedMessage).isEqualTo(json)

    val parameterizedType: Type = Types.newParameterizedType(Set::class.java, Message::class.java)
    moshiAdapter.create<Set<Message>>(parameterizedType)!!
  }
}
