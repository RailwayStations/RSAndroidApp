package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InboxResponseTest {

    @Test
    void defaultConstructor() {
        var inboxResponse = new InboxResponse();

        assertThat(inboxResponse.getState()).isSameAs(InboxResponse.InboxResponseState.ERROR);
    }

    @Test
    void defaultConstructorNullState() {
        var inboxResponse = new InboxResponse(null);

        assertThat(inboxResponse.getState()).isSameAs(InboxResponse.InboxResponseState.ERROR);
    }

    @Test
    void defaultConstructorWithState() {
        var inboxResponse = new InboxResponse(InboxResponse.InboxResponseState.REVIEW);

        assertThat(inboxResponse.getState()).isSameAs(InboxResponse.InboxResponseState.REVIEW);
    }

}