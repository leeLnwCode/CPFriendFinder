package com.cp.friend.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.security.Principal;
import java.util.UUID;
import com.cp.friend.service.RoomMembershipService;
import org.springframework.security.access.AccessDeniedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

class WebSocketAuthChannelInterceptorTest {

    private MessageChannel channel;
    private WebSocketAuthChannelInterceptor interceptor;
    private RoomMembershipService membership;

    @BeforeEach
    void setUp() {
        channel = mock(MessageChannel.class);
        membership = mock(RoomMembershipService.class);
        interceptor = new WebSocketAuthChannelInterceptor(membership);
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

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
        assertNull(accessor.getUser());
        verifyNoInteractions(membership);
    }

    private Message<byte[]> frame(StompCommand command, UUID userId, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (userId != null) accessor.setUser(() -> userId.toString());
        if (destination != null) accessor.setDestination(destination);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void subscribeOwnNotificationsAndCall_isAllowed() {
        UUID me = UUID.randomUUID();
        for (String topic : new String[]{"/topic/notifications/", "/topic/call/"}) {
            Message<byte[]> message = frame(StompCommand.SUBSCRIBE, me, topic + me);
            assertSame(message, interceptor.preSend(message, channel));
        }
        verifyNoInteractions(membership);
    }

    @Test
    void subscribeOtherUsersPrivateTopics_isDenied() {
        UUID me = UUID.randomUUID(), other = UUID.randomUUID();
        for (String topic : new String[]{"/topic/notifications/", "/topic/call/"}) {
            Message<byte[]> message = frame(StompCommand.SUBSCRIBE, me, topic + other);
            assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
        }
        verifyNoInteractions(membership);
    }

    @Test
    void subscribeRoomTopics_requiresMembershipForEachTopic() {
        UUID me = UUID.randomUUID(), room = UUID.randomUUID();
        for (String suffix : new String[]{"", "/call", "/members", "/presence", "/message-updates"}) {
            Message<byte[]> message = frame(StompCommand.SUBSCRIBE, me, "/topic/rooms/" + room + suffix);
            assertSame(message, interceptor.preSend(message, channel));
        }
        org.mockito.Mockito.verify(membership, org.mockito.Mockito.times(5)).requireActive(room, me);
    }

    @Test
    void subscribeRoomWithoutMembership_isDenied() {
        UUID me = UUID.randomUUID(), room = UUID.randomUUID();
        doThrow(new AccessDeniedException("Not a member")).when(membership).requireActive(room, me);
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, me, "/topic/rooms/" + room + "/call");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void subscribeRoomDiscoveryUpdates_isAllowed() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, UUID.randomUUID(), "/topic/rooms/updates");
        assertSame(message, interceptor.preSend(message, channel));
        verifyNoInteractions(membership);
    }

    @Test
    void sendDirectlyToBrokerTopic_isDenied() {
        Message<byte[]> message = frame(StompCommand.SEND, UUID.randomUUID(), "/topic/rooms/updates");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void sendToApplicationDestination_isAllowed() {
        Message<byte[]> message = frame(StompCommand.SEND, UUID.randomUUID(), "/app/call");
        assertSame(message, interceptor.preSend(message, channel));
    }

    @Test
    void disconnectWithoutPrincipal_isAllowedForCleanup() {
        Message<byte[]> message = frame(StompCommand.DISCONNECT, null, null);
        assertSame(message, interceptor.preSend(message, channel));
    }
}
