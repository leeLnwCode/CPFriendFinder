package com.cp.friend.controller;

import com.cp.friend.dto.request.CallInviteRequest;
import com.cp.friend.dto.request.CallSignalRequest;
import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.service.CallSignalingService;
import com.cp.friend.service.ChatMessageService;
import com.cp.friend.service.RoomRealtimeService;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatWsControllerTest {
    private final ChatMessageService messages = mock(ChatMessageService.class);
    private final CallSignalingService calls = mock(CallSignalingService.class);
    private final RoomRealtimeService realtime = mock(RoomRealtimeService.class);
    private final UUID userId = UUID.randomUUID();
    private final Principal principal = () -> userId.toString();
    private ChatWsController controller;

    @BeforeEach void setUp() { controller = new ChatWsController(messages, calls, realtime); }

    @Test void incomingCallUsesAuthenticatedIdentityAndCallService() {
        var request = new CallInviteRequest();
        controller.handleCallInvite(request, principal);
        verify(calls).relayCallInvite(userId, request);
        verifyNoInteractions(messages, realtime);
    }

    @Test void roomSignalIsRelayedBeforeRealtimePresenceUpdate() {
        UUID roomId = UUID.randomUUID();
        var signal = new CallSignalRequest(); signal.setType("MEDIA"); signal.setPayload("state");
        controller.handleCallSignal(roomId, signal, "session-a", principal);
        var ordered = inOrder(calls, realtime);
        ordered.verify(calls).relayCallSignal(userId, roomId, signal);
        ordered.verify(realtime).signal(roomId, userId, "session-a", "MEDIA", "state");
        verifyNoInteractions(messages);
    }

    @Test void rejectedSignalDoesNotCreateRealtimePresence() {
        UUID roomId = UUID.randomUUID(); var signal = new CallSignalRequest();signal.setType("JOIN");
        doThrow(new IllegalStateException("rejected")).when(calls).relayCallSignal(userId, roomId, signal);
        assertThrows(IllegalStateException.class,
                () -> controller.handleCallSignal(roomId, signal, "session-a", principal));
        verifyNoInteractions(realtime, messages);
    }

    @Test void unauthenticatedInvitesAndSignalsAreRejected() {
        assertThrows(IllegalStateException.class, () -> controller.handleCallInvite(new CallInviteRequest(), null));
        assertThrows(IllegalStateException.class,
                () -> controller.handleCallSignal(UUID.randomUUID(), new CallSignalRequest(), "session-a", null));
        verifyNoInteractions(calls, messages, realtime);
    }

    @Test void textMessagesStillUseMessageService() {
        UUID roomId = UUID.randomUUID();var payload = new ChatMessagePayload();payload.setMessageType("TEXT");
        controller.sendMessage(roomId, payload, principal);
        verify(messages).send(userId, roomId, payload);
        verifyNoInteractions(calls, realtime);
    }
}
