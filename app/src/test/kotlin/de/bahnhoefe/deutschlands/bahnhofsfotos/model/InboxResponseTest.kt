package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import com.google.gson.Gson
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.InboxResponse.InboxResponseState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InboxResponseTest {
    @Test
    fun defaultConstructor() {
        val inboxResponse = InboxResponse()
        assertThat(inboxResponse.state).isSameAs(InboxResponseState.ERROR)
    }

    @Test
    fun defaultConstructorNullState() {
        val inboxResponse = InboxResponse(null)
        assertThat(inboxResponse.state).isSameAs(InboxResponseState.ERROR)
    }

    @Test
    fun defaultConstructorWithState() {
        val inboxResponse = InboxResponse(InboxResponseState.REVIEW)
        assertThat(inboxResponse.state).isSameAs(InboxResponseState.REVIEW)
    }

    @Test
    fun fromJsonWithoutState() {
        val inboxResponse = Gson().fromJson("{'message':'some message'}", InboxResponse::class.java)
        assertThat(inboxResponse.state).isSameAs(InboxResponseState.ERROR)
        assertThat(inboxResponse.message).isEqualTo("some message")
    }

    @Test
    fun fromJsonWithState() {
        val inboxResponse = Gson().fromJson(
            "{'state': 'REVIEW', 'message':'some message'}",
            InboxResponse::class.java
        )
        assertThat(inboxResponse.state).isSameAs(InboxResponseState.REVIEW)
        assertThat(inboxResponse.message).isEqualTo("some message")
    }
}