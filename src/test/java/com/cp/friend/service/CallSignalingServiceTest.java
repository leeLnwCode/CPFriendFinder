package com.cp.friend.service;

import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.dto.request.CallSignalRequest;
import com.cp.friend.dto.response.CallSignalResponse;
import com.cp.friend.dto.response.ChatMessageResponse;
import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.Message;
import com.cp.friend.model.RoomMember;
import com.cp.friend.model.User;
import com.cp.friend.repository.ChatRoomRepository;
import com.cp.friend.repository.MessageRepository;
import com.cp.friend.repository.RoomMemberRepository;
import com.cp.friend.repository.UserRepository;
import com.cp.friend.service.strategy.MessageContentStrategy;
import com.cp.friend.service.strategy.MessageContentStrategyResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallSignalingServiceTest {
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    private CallSignalingService callSignalingService;
    @BeforeEach void setUp() {
        callSignalingService = new CallSignalingService(roomMemberRepository, userRepository, messagingTemplate);
    }
    private User user(UUID id, String firstname) {
        User user = new User();
        setUserId(user, id);
        user.setFirstname(firstname);
        user.setLastname("User");
        user.setImageUrl("avatar.png");
        return user;
    }

    private ChatRoom room(
            UUID id,
            ChatRoom.RoomType type
    ) {
        ChatRoom room = new ChatRoom();
        room.setId(id);
        room.setRoomName(
                type == ChatRoom.RoomType.GROUP
                        ? "Group Room"
                        : "DIRECT"
        );
        room.setRoomType(type);
        return room;
    }

    private RoomMember member(
            ChatRoom room,
            User user
    ) {
        RoomMember member = new RoomMember();
        member.setRoom(room);
        member.setUser(user);
        member.setRole(RoomMember.Role.MEMBER);
        return member;
    }

        @Test
        void relayCallSignal_leave_afterMembershipEnded_stillBroadcasts() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User user = user(userId, "Former");
        ChatRoom room = room(roomId, ChatRoom.RoomType.DIRECT);

        RoomMember formerMember = member(room, user);
        formerMember.setLeftAt(Instant.now());

        CallSignalRequest signal = new CallSignalRequest();
        signal.setType("LEAVE");

        when(roomMemberRepository
                .findFirstByRoomIdAndUserIdOrderByJoinedAtDesc(roomId, userId))
                .thenReturn(Optional.of(formerMember));

        assertDoesNotThrow(
                () -> callSignalingService.relayCallSignal(
                        userId,
                        roomId,
                        signal
                )
        );

        verify(messagingTemplate).convertAndSend(
                eq("/topic/rooms/" + roomId + "/call"),
                any(CallSignalResponse.class)
        );
        }
        @Test
        void relayCallSignal_leave_neverMember_returnsForbidden() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        CallSignalRequest signal = new CallSignalRequest();
        signal.setType("LEAVE");

        when(roomMemberRepository
                .findFirstByRoomIdAndUserIdOrderByJoinedAtDesc(roomId, userId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> callSignalingService.relayCallSignal(
                        userId,
                        roomId,
                        signal
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        verifyNoInteractions(messagingTemplate);
        }


    @Test
    void relayCallInvite_videoMode_reachesReceiver() {
        UUID callerId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        User caller = user(callerId, "Caller");
        when(roomMemberRepository.findActiveMember(roomId, callerId))
                .thenReturn(Optional.of(member(room(roomId, ChatRoom.RoomType.DIRECT), caller)));
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        var request = new com.cp.friend.dto.request.CallInviteRequest();
        request.setType("INVITE"); request.setToUserId(receiverId); request.setRoomId(roomId); request.setMode("VIDEO");
        callSignalingService.relayCallInvite(callerId, request);
        var captured = ArgumentCaptor.forClass(com.cp.friend.dto.response.CallInviteSignal.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/call/" + receiverId), captured.capture());
        assertEquals("VIDEO", captured.getValue().mode());
        assertEquals(roomId, captured.getValue().roomId());
        assertEquals("Caller User", captured.getValue().fromName());
    }

    @Test
    void relayCallInvite_accept_preservesModeAndOldClientsDefaultToVoice() {
        UUID callerId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        var request = new com.cp.friend.dto.request.CallInviteRequest();
        request.setType("ACCEPT"); request.setToUserId(receiverId); request.setMode("VIDEO");
        callSignalingService.relayCallInvite(callerId, request);
        request.setMode(null);
        callSignalingService.relayCallInvite(callerId, request);
        var captured = ArgumentCaptor.forClass(com.cp.friend.dto.response.CallInviteSignal.class);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/call/" + receiverId), captured.capture());
        assertEquals("VIDEO", captured.getAllValues().get(0).mode());
        assertEquals("VOICE", captured.getAllValues().get(1).mode());
    }

    @Test
    void relayCallInvite_invalidMode_isRejected() {
        var request = new com.cp.friend.dto.request.CallInviteRequest();
        request.setType("ACCEPT"); request.setToUserId(UUID.randomUUID()); request.setMode("UNKNOWN");
        var error = assertThrows(ResponseStatusException.class,
                () -> callSignalingService.relayCallInvite(UUID.randomUUID(), request));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(messagingTemplate);
    }

    private void setUserId(
            User user,
            UUID id
    ) {
        try {
            var field =
                    User.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    @ParameterizedTest
    @ValueSource(strings={"OFFER","ANSWER","ICE"})
    void forwardsTargetedSignalsWithUnchangedPayload(String type) {
        UUID me=UUID.randomUUID(), roomId=UUID.randomUUID(), peer=UUID.randomUUID();
        when(roomMemberRepository.findActiveMember(roomId,me)).thenReturn(Optional.of(new RoomMember()));
        CallSignalRequest signal=new CallSignalRequest();
        signal.setType(type);signal.setTargetUserId(peer);signal.setPayload("opaque-sdp-or-ice");
        callSignalingService.relayCallSignal(me,roomId,signal);
        var captor=ArgumentCaptor.forClass(CallSignalResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/"+roomId+"/call"),captor.capture());
        assertEquals(type,captor.getValue().type());
        assertEquals(me,captor.getValue().fromUserId());
        assertEquals(peer,captor.getValue().toUserId());
        assertEquals("opaque-sdp-or-ice",captor.getValue().payload());
    }
    @ParameterizedTest
    @ValueSource(strings={"OFFER","ANSWER","ICE"})
    void rejectsTargetedSignalsWithoutRecipient(String type) {
        UUID me=UUID.randomUUID(), roomId=UUID.randomUUID();
        when(roomMemberRepository.findActiveMember(roomId,me)).thenReturn(Optional.of(new RoomMember()));
        CallSignalRequest signal=new CallSignalRequest();signal.setType(type);
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            ()->callSignalingService.relayCallSignal(me,roomId,signal)).getStatusCode());
        verifyNoInteractions(messagingTemplate);
    }
    @Test void rejectsJoinFromNonMember() {
        CallSignalRequest signal=new CallSignalRequest();signal.setType("JOIN");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
            ()->callSignalingService.relayCallSignal(UUID.randomUUID(),UUID.randomUUID(),signal)).getStatusCode());
        verifyNoInteractions(messagingTemplate);
    }
    @Test void broadcastsMediaStateWithoutTarget() {
        UUID me=UUID.randomUUID(), roomId=UUID.randomUUID();
        when(roomMemberRepository.findActiveMember(roomId,me)).thenReturn(Optional.of(new RoomMember()));
        CallSignalRequest signal=new CallSignalRequest();signal.setType("MEDIA");signal.setPayload("camera-state");
        callSignalingService.relayCallSignal(me,roomId,signal);
        var captor=ArgumentCaptor.forClass(CallSignalResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/"+roomId+"/call"),captor.capture());
        assertEquals("MEDIA",captor.getValue().type());assertNull(captor.getValue().toUserId());
        assertEquals("camera-state",captor.getValue().payload());
    }
}
