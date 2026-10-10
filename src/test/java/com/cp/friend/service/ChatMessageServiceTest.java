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
class ChatMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MessageContentStrategyResolver contentStrategyResolver;

    @Mock
    private MessageContentStrategy contentStrategy;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        chatMessageService = new ChatMessageService(
                messageRepository,
                chatRoomRepository,
                roomMemberRepository,
                userRepository,
                notificationService,
                contentStrategyResolver,
                messagingTemplate
        );
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

    private ChatMessagePayload payload(
            String content,
            String messageType
    ) {
        ChatMessagePayload payload =
                new ChatMessagePayload();

        payload.setContent(content);
        payload.setMessageType(messageType);

        return payload;
    }

    private Message message(
            UUID id,
            ChatRoom room,
            User sender,
            String content
    ) {
        Message message = new Message();
        message.setId(id);
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(
                Message.MessageType.TEXT
        );
        return message;
    }

    @Test
    void getMessages_nonMember_returnsForbidden() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatMessageService.getMessages(
                        userId,
                        roomId,
                        null,
                        50
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                ex.getStatusCode()
        );

        assertEquals(
                "You are not a member of this room",
                ex.getReason()
        );

        verifyNoInteractions(
                chatRoomRepository,
                messageRepository
        );
    }

    @Test
    void getMessages_groupRoom_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User user = user(userId, "Member");
        ChatRoom room =
                room(roomId, ChatRoom.RoomType.GROUP);

        RoomMember member =
                member(room, user);

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(Optional.of(member));

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        List<ChatMessageResponse> result =
                chatMessageService.getMessages(
                        userId,
                        roomId,
                        null,
                        50
                );

        assertTrue(result.isEmpty());

        verifyNoInteractions(messageRepository);
    }

    @Test
    void getMessages_directRoom_reversesAndMarksRead() {
        UUID userId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User user = user(userId, "Reader");
        User sender = user(senderId, "Sender");

        ChatRoom room =
                room(roomId, ChatRoom.RoomType.DIRECT);

        RoomMember member =
                member(room, user);

        Message newer = message(
                UUID.randomUUID(),
                room,
                sender,
                "newer"
        );

        Message older = message(
                UUID.randomUUID(),
                room,
                sender,
                "older"
        );

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(Optional.of(member));

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(messageRepository.findLatestByRoomId(
                eq(roomId),
                any(Pageable.class)
        )).thenReturn(
                List.of(newer, older)
        );

        List<ChatMessageResponse> result =
                chatMessageService.getMessages(
                        userId,
                        roomId,
                        null,
                        50
                );

        assertEquals(2, result.size());
        assertEquals("older", result.get(0).content());
        assertEquals("newer", result.get(1).content());

        assertNotNull(member.getLastReadAt());
    }

    @Test
    void getMessages_limitAndBefore_areHandled() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User user = user(userId, "Reader");

        ChatRoom room =
                room(roomId, ChatRoom.RoomType.DIRECT);

        RoomMember member =
                member(room, user);

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(Optional.of(member));

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(messageRepository.findLatestByRoomId(
                eq(roomId),
                any(Pageable.class)
        )).thenReturn(List.of());

        Instant before =
                Instant.parse(
                        "2026-10-07T12:00:00Z"
                );

        when(messageRepository.findByRoomIdBefore(
                eq(roomId),
                eq(before),
                any(Pageable.class)
        )).thenReturn(List.of());

        chatMessageService.getMessages(
                userId,
                roomId,
                null,
                0
        );

        chatMessageService.getMessages(
                userId,
                roomId,
                before,
                1000
        );

        ArgumentCaptor<Pageable> latestPage =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(messageRepository)
                .findLatestByRoomId(
                        eq(roomId),
                        latestPage.capture()
                );

        assertEquals(
                1,
                latestPage.getValue().getPageSize()
        );

        ArgumentCaptor<Pageable> beforePage =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(messageRepository)
                .findByRoomIdBefore(
                        eq(roomId),
                        eq(before),
                        beforePage.capture()
                );

        assertEquals(
                100,
                beforePage.getValue().getPageSize()
        );
    }

    @Test
    void unreadCount_usesCorrectRepositoryQuery() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        UUID room1Id = UUID.randomUUID();
        UUID room2Id = UUID.randomUUID();

        ChatRoom room1 =
                room(room1Id, ChatRoom.RoomType.DIRECT);

        ChatRoom room2 =
                room(room2Id, ChatRoom.RoomType.DIRECT);

        RoomMember neverRead =
                member(
                        room1,
                        user(user1Id, "One")
                );

        RoomMember alreadyRead =
                member(
                        room2,
                        user(user2Id, "Two")
                );

        Instant lastReadAt =
                Instant.parse(
                        "2026-10-07T10:00:00Z"
                );

        alreadyRead.setLastReadAt(lastReadAt);

        when(roomMemberRepository.findActiveMember(
                room1Id,
                user1Id
        )).thenReturn(
                Optional.of(neverRead)
        );

        when(roomMemberRepository.findActiveMember(
                room2Id,
                user2Id
        )).thenReturn(
                Optional.of(alreadyRead)
        );

        when(messageRepository
                .countByRoomIdAndDeletedAtIsNullAndSenderIdNot(
                        room1Id,
                        user1Id
                ))
                .thenReturn(4L);

        when(messageRepository
                .countByRoomIdAndDeletedAtIsNullAndSenderIdNotAndCreatedAtAfter(
                        room2Id,
                        user2Id,
                        lastReadAt
                ))
                .thenReturn(2L);

        assertEquals(
                4L,
                chatMessageService.unreadCount(
                        user1Id,
                        room1Id
                )
        );

        assertEquals(
                2L,
                chatMessageService.unreadCount(
                        user2Id,
                        room2Id
                )
        );
    }

    @Test
    void send_direct_persistsBroadcastsAndNotifies() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User sender = user(userId, "Sender");

        ChatRoom room =
                room(roomId, ChatRoom.RoomType.DIRECT);

        RoomMember senderMember =
                member(room, sender);

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(
                        Optional.of(senderMember)
                );

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(contentStrategyResolver.resolve(
                Message.MessageType.TEXT
        )).thenReturn(contentStrategy);

        when(contentStrategy.process("  hello  "))
                .thenReturn("hello");

        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> {
                    Message saved =
                            invocation.getArgument(0);

                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        ChatMessageResponse result =
                chatMessageService.send(
                        userId,
                        roomId,
                        payload(
                                "  hello  ",
                                "TEXT"
                        )
                );

        assertEquals("hello", result.content());
        assertEquals(
                Message.MessageType.TEXT,
                result.messageType()
        );

        assertNotNull(senderMember.getLastReadAt());

        verify(messageRepository)
                .save(any(Message.class));

        verify(notificationService)
                .notifyNewMessage(
                        any(Message.class)
                );

        verify(messagingTemplate)
                .convertAndSend(
                        eq("/topic/rooms/" + roomId),
                        any(ChatMessageResponse.class)
                );
    }

    @Test
    void send_group_broadcastsWithoutPersistence() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User sender = user(userId, "Sender");

        ChatRoom room =
                room(roomId, ChatRoom.RoomType.GROUP);

        RoomMember senderMember =
                member(room, sender);

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(
                        Optional.of(senderMember)
                );

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(contentStrategyResolver.resolve(
                Message.MessageType.TEXT
        )).thenReturn(contentStrategy);

        when(contentStrategy.process("hello"))
                .thenReturn("hello");

        ChatMessageResponse result =
                chatMessageService.send(
                        userId,
                        roomId,
                        payload("hello", "TEXT")
                );

        assertNotNull(result.id());
        assertEquals("hello", result.content());

        verify(messageRepository, never())
                .save(any());

        verifyNoInteractions(notificationService);

        verify(messagingTemplate)
                .convertAndSend(
                        eq("/topic/rooms/" + roomId),
                        any(ChatMessageResponse.class)
                );
    }

    @Test
    void send_messageType_defaultAndInvalid_areHandled() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User sender = user(userId, "Sender");

        ChatRoom room =
                room(roomId, ChatRoom.RoomType.GROUP);

        RoomMember senderMember =
                member(room, sender);

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(
                        Optional.of(senderMember)
                );

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(contentStrategyResolver.resolve(
                Message.MessageType.TEXT
        )).thenReturn(contentStrategy);

        when(contentStrategy.process("hello"))
                .thenReturn("hello");

        ChatMessageResponse defaultResult =
                chatMessageService.send(
                        userId,
                        roomId,
                        payload("hello", "   ")
                );

        assertEquals(
                Message.MessageType.TEXT,
                defaultResult.messageType()
        );

        ResponseStatusException invalid =
                assertThrows(
                        ResponseStatusException.class,
                        () -> chatMessageService.send(
                                userId,
                                roomId,
                                payload(
                                        "hello",
                                        "UNKNOWN"
                                )
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                invalid.getStatusCode()
        );

        assertEquals(
                "Invalid messageType",
                invalid.getReason()
        );
    }

    @Test
    void delete_ownMessage_softDeletes() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        User sender = user(userId, "Sender");

        ChatRoom room =
                room(roomId, ChatRoom.RoomType.DIRECT);

        RoomMember member =
                member(room, sender);

        Message message =
                message(
                        messageId,
                        room,
                        sender,
                        "hello"
                );

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(Optional.of(member));

        when(messageRepository.findById(messageId))
                .thenReturn(Optional.of(message));

        when(messageRepository.save(message))
                .thenReturn(message);

        ChatMessageResponse result =
                chatMessageService.delete(
                        userId,
                        roomId,
                        messageId
                );

        assertNotNull(message.getDeletedAt());
        assertEquals(messageId, result.id());

        verify(messageRepository).save(message);
    }

    @Test
    void delete_otherUsersMessage_returnsForbidden() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        User me = user(userId, "Me");
        User other = user(otherId, "Other");

        ChatRoom room =
                room(roomId, ChatRoom.RoomType.DIRECT);

        RoomMember member =
                member(room, me);

        Message message =
                message(
                        messageId,
                        room,
                        other,
                        "hello"
                );

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(Optional.of(member));

        when(messageRepository.findById(messageId))
                .thenReturn(Optional.of(message));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatMessageService.delete(
                        userId,
                        roomId,
                        messageId
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                ex.getStatusCode()
        );

        assertEquals(
                "You can only delete your own messages",
                ex.getReason()
        );

        verify(messageRepository, never())
                .save(any());
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
                () -> chatMessageService.relayCallSignal(
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
                () -> chatMessageService.relayCallSignal(
                        userId,
                        roomId,
                        signal
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

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
}
