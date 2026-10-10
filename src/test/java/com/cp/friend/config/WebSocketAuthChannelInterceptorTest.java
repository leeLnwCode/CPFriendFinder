package com.cp.friend.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

import com.cp.friend.service.RoomMembershipService;

class WebSocketAuthChannelInterceptorTest {

    private MessageChannel channel;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        channel = mock(MessageChannel.class);
        interceptor = new WebSocketAuthChannelInterceptor(
                mock(RoomMembershipService.class));
    }

    @Test
    void connect_withAuthenticatedPrincipal_preservesPrincipal() {
        UUID userId = UUID.randomUUID();
        Principal principal = () -> userId.toString();

        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);

        Message<byte[]> message =
                MessageBuilder.createMessage(
                        new byte[0],
                        accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(
                        result,
                        StompHeaderAccessor.class);

        assertNotNull(resultAccessor);
        assertSame(principal, resultAccessor.getUser());
    }

    @Test
    void connect_withoutAuthenticatedPrincipal_mustNotTrustLoginHeader() {
        UUID claimedUserId = UUID.randomUUID();

        StompHeaderAccessor accessor =
                StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLogin(claimedUserId.toString());
        accessor.setLeaveMutable(true);

        Message<byte[]> message =
                MessageBuilder.createMessage(
                        new byte[0],
                        accessor.getMessageHeaders());

        assertThrows(
                AccessDeniedException.class,
                () -> interceptor.preSend(message, channel),
                "Unauthenticated client must not be trusted from STOMP login header");
    }
}