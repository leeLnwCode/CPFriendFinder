package com.cp.friend.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

class WebSocketAuthChannelInterceptorTest {

    private MessageChannel channel;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        channel = mock(MessageChannel.class);
        interceptor = new WebSocketAuthChannelInterceptor(mock(com.cp.friend.service.RoomMembershipService.class));
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

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> interceptor.preSend(message, channel));
    }

    private Message<byte[]> frame(StompCommand command, UUID user, String destination) {
        var a=StompHeaderAccessor.create(command);if(user!=null)a.setUser(()->user.toString());a.setDestination(destination);a.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0],a.getMessageHeaders());
    }
    @Test void cannotSubscribeToAnotherUsersNotifications() {
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class,
          ()->interceptor.preSend(frame(StompCommand.SUBSCRIBE,UUID.randomUUID(),"/topic/notifications/"+UUID.randomUUID()),channel));
    }
    @Test void ownNotificationsAndRoomListUpdatesAllowed() {
        UUID me=UUID.randomUUID();assertNotNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE,me,"/topic/notifications/"+me),channel));
        assertNotNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE,me,"/topic/rooms/updates"),channel));
    }
    @Test void roomSubscriptionChecksMembership() {
        var membership=mock(com.cp.friend.service.RoomMembershipService.class);var secured=new WebSocketAuthChannelInterceptor(membership);
        UUID me=UUID.randomUUID(),room=UUID.randomUUID();var message=frame(StompCommand.SUBSCRIBE,me,"/topic/rooms/"+room+"/message-updates");
        secured.preSend(message,channel);org.mockito.Mockito.verify(membership).requireActive(room,me);
    }
    @Test void clientsCannotPublishBrokerTopics() {
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class,
          ()->interceptor.preSend(frame(StompCommand.SEND,UUID.randomUUID(),"/topic/rooms/updates"),channel));
    }
}