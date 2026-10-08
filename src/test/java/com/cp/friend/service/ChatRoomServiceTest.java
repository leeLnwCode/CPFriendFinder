package com.cp.friend.service;

import com.cp.friend.dto.request.CreateChatRoomRequest;
import com.cp.friend.dto.request.UpdateChatRoomRequest;
import com.cp.friend.dto.response.ChatRoomSummaryResponse;
import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.Interest;
import com.cp.friend.model.RoomMember;
import com.cp.friend.model.User;
import com.cp.friend.repository.ChatRoomRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.InterestRepository;
import com.cp.friend.repository.MessageRepository;
import com.cp.friend.repository.RoomInterestRepository;
import com.cp.friend.repository.RoomMemberRepository;
import com.cp.friend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private RoomInterestRepository roomInterestRepository;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(
                chatRoomRepository,
                roomMemberRepository,
                roomInterestRepository,
                interestRepository,
                messageRepository,
                friendshipRepository,
                userRepository,
                passwordEncoder
        );
    }

    private User user(UUID id, String firstname) {
        User user = new User();
        setUserId(user, id);
        user.setFirstname(firstname);
        user.setLastname("User");
        user.setStatus(User.Status.ACTIVE);
        return user;
    }

    private ChatRoom room(
            UUID id,
            String name,
            ChatRoom.RoomType type
    ) {
        ChatRoom room = new ChatRoom();
        room.setId(id);
        room.setRoomName(name);
        room.setRoomType(type);
        room.setMaxMembers((short) 10);
        return room;
    }

    @Test
    void createGroupRoom_publicRoom_appliesDefaultsAndOwner() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User creator = user(userId, "Owner");

        CreateChatRoomRequest request =
                new CreateChatRoomRequest();

        request.setRoomName("  Study Room  ");
        request.setDescription("  Java group  ");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(creator));

        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> {
                    ChatRoom saved =
                            invocation.getArgument(0);

                    saved.setId(roomId);
                    return saved;
                });

        when(roomInterestRepository
                .findByRoomIdWithInterest(roomId))
                .thenReturn(List.of());

        ChatRoomSummaryResponse result =
                chatRoomService.createGroupRoom(
                        userId,
                        request
                );

        assertEquals(roomId, result.id());
        assertEquals("Study Room", result.roomName());
        assertEquals(
                ChatRoom.RoomType.GROUP,
                result.roomType()
        );
        assertEquals(10, result.maxMembers());
        assertEquals(1, result.memberCount());
        assertFalse(result.isPrivate());

        ArgumentCaptor<RoomMember> memberCaptor =
                ArgumentCaptor.forClass(
                        RoomMember.class
                );

        verify(roomMemberRepository)
                .save(memberCaptor.capture());

        assertEquals(
                RoomMember.Role.OWNER,
                memberCaptor.getValue().getRole()
        );

        assertSame(
                creator,
                memberCaptor.getValue().getUser()
        );
    }

    @Test
    void createGroupRoom_privateWithoutPassword_returnsBadRequest() {
        CreateChatRoomRequest request =
                new CreateChatRoomRequest();

        request.setRoomName("Private");
        request.setPrivate(true);
        request.setPassword("   ");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.createGroupRoom(
                        UUID.randomUUID(),
                        request
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        assertEquals(
                "Password is required for private rooms",
                ex.getReason()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createGroupRoom_private_encodesPassword() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        User creator = user(userId, "Owner");

        CreateChatRoomRequest request =
                new CreateChatRoomRequest();

        request.setRoomName("Private Room");
        request.setPrivate(true);
        request.setPassword("secret123");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(creator));

        when(passwordEncoder.encode("secret123"))
                .thenReturn("HASHED");

        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> {
                    ChatRoom saved =
                            invocation.getArgument(0);

                    saved.setId(roomId);
                    return saved;
                });

        when(roomInterestRepository
                .findByRoomIdWithInterest(roomId))
                .thenReturn(List.of());

        ChatRoomSummaryResponse result =
                chatRoomService.createGroupRoom(
                        userId,
                        request
                );

        assertTrue(result.isPrivate());

        ArgumentCaptor<ChatRoom> roomCaptor =
                ArgumentCaptor.forClass(
                        ChatRoom.class
                );

        verify(chatRoomRepository)
                .save(roomCaptor.capture());

        assertEquals(
                "HASHED",
                roomCaptor.getValue()
                        .getPasswordHash()
        );

        verify(passwordEncoder)
                .encode("secret123");
    }

    @Test
    void createGroupRoom_invalidInterest_returnsBadRequest() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID validId = UUID.randomUUID();
        UUID invalidId = UUID.randomUUID();

        User creator = user(userId, "Owner");

        CreateChatRoomRequest request =
                new CreateChatRoomRequest();

        request.setRoomName("Room");
        request.setInterestIds(
                List.of(validId, invalidId)
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(creator));

        when(chatRoomRepository.save(any(ChatRoom.class)))
                .thenAnswer(invocation -> {
                    ChatRoom saved =
                            invocation.getArgument(0);

                    saved.setId(roomId);
                    return saved;
                });

        when(interestRepository
                .findByIdInAndIsActiveTrue(
                        anyCollection()
                ))
                .thenReturn(
                        List.of(mock(Interest.class))
                );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.createGroupRoom(
                        userId,
                        request
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        assertEquals(
                "One or more interests do not exist or are inactive",
                ex.getReason()
        );
    }

    @Test
    void discoverRooms_searchAndInterests_intersectsResults() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        UUID cId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        ChatRoom roomA =
                room(aId, "Java A", ChatRoom.RoomType.GROUP);

        ChatRoom roomB =
                room(bId, "Java B", ChatRoom.RoomType.GROUP);

        ChatRoom roomC =
                room(cId, "Other", ChatRoom.RoomType.GROUP);

        when(chatRoomRepository
                .searchGroupRoomsByName("Java"))
                .thenReturn(
                        List.of(roomA, roomB)
                );

        when(chatRoomRepository
                .findGroupRoomsByInterestIds(
                        Set.of(interestId)
                ))
                .thenReturn(
                        List.of(roomB, roomC)
                );

        when(roomMemberRepository
                .countActiveMembers(bId))
                .thenReturn(1L);

        when(roomInterestRepository
                .findByRoomIdInWithInterest(
                        anyCollection()
                ))
                .thenReturn(List.of());

        List<ChatRoomSummaryResponse> result =
                chatRoomService.discoverRooms(
                        "  Java  ",
                        Set.of(interestId),
                        0,
                        20,
                        0
                );

        assertEquals(1, result.size());
        assertEquals(bId, result.get(0).id());
    }

    @Test
    void joinRoom_existingMember_returnsConflict() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ChatRoom room =
                room(
                        roomId,
                        "Room",
                        ChatRoom.RoomType.GROUP
                );

        User user = user(userId, "Member");

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(roomMemberRepository
                .isActiveMember(roomId, userId))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.joinRoom(
                        userId,
                        roomId,
                        null
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );

        assertEquals(
                "Already a member of this room",
                ex.getReason()
        );
    }

    @Test
    void joinRoom_privateWrongPassword_returnsForbidden() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ChatRoom room =
                room(
                        roomId,
                        "Private",
                        ChatRoom.RoomType.GROUP
                );

        room.setPrivate(true);
        room.setPasswordHash("HASHED");

        User user = user(userId, "Member");

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(roomMemberRepository
                .isActiveMember(roomId, userId))
                .thenReturn(false);

        when(passwordEncoder.matches(
                "wrong",
                "HASHED"
        )).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.joinRoom(
                        userId,
                        roomId,
                        "wrong"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                ex.getStatusCode()
        );

        assertEquals(
                "Invalid room password",
                ex.getReason()
        );
    }

    @Test
    void joinRoom_fullRoom_returnsConflict() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ChatRoom room =
                room(
                        roomId,
                        "Full",
                        ChatRoom.RoomType.GROUP
                );

        room.setMaxMembers((short) 2);

        User user = user(userId, "Member");

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(roomMemberRepository
                .isActiveMember(roomId, userId))
                .thenReturn(false);

        when(roomMemberRepository
                .countActiveMembers(roomId))
                .thenReturn(2L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.joinRoom(
                        userId,
                        roomId,
                        null
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );

        assertEquals(
                "Room is full",
                ex.getReason()
        );
    }

    @Test
    void leaveRoom_nonMember_returnsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        when(roomMemberRepository
                .findActiveMember(roomId, userId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.leaveRoom(
                        userId,
                        roomId
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        assertEquals(
                "You are not a member of this room",
                ex.getReason()
        );
    }

    @Test
    void updateRoom_permissionAndMemberLimit_areEnforced() {
        UUID userId = UUID.randomUUID();
        UUID deniedRoomId = UUID.randomUUID();
        UUID limitedRoomId = UUID.randomUUID();

        UpdateChatRoomRequest deniedRequest =
                new UpdateChatRoomRequest();

        deniedRequest.setRoomName("New Name");

        when(roomMemberRepository.hasAnyRole(
                eq(deniedRoomId),
                eq(userId),
                anyCollection()
        )).thenReturn(false);

        ResponseStatusException denied = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.updateRoom(
                        userId,
                        deniedRoomId,
                        deniedRequest
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                denied.getStatusCode()
        );

        ChatRoom limitedRoom =
                room(
                        limitedRoomId,
                        "Room",
                        ChatRoom.RoomType.GROUP
                );

        UpdateChatRoomRequest limitRequest =
                new UpdateChatRoomRequest();

        limitRequest.setMaxMembers((short) 2);

        when(roomMemberRepository.hasAnyRole(
                eq(limitedRoomId),
                eq(userId),
                anyCollection()
        )).thenReturn(true);

        when(chatRoomRepository
                .findById(limitedRoomId))
                .thenReturn(
                        Optional.of(limitedRoom)
                );

        when(roomMemberRepository
                .countActiveMembers(limitedRoomId))
                .thenReturn(3L);

        ResponseStatusException tooSmall = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService.updateRoom(
                        userId,
                        limitedRoomId,
                        limitRequest
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                tooSmall.getStatusCode()
        );

        assertEquals(
                "maxMembers is lower than the current member count",
                tooSmall.getReason()
        );
    }

    @Test
    void directRoom_selfOrNonFriend_isRejected() {
        UUID userId = UUID.randomUUID();

        ResponseStatusException self = assertThrows(
                ResponseStatusException.class,
                () -> chatRoomService
                        .getOrCreateDirectRoom(
                                userId,
                                userId
                        )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                self.getStatusCode()
        );

        UUID friendId = UUID.randomUUID();

        when(friendshipRepository.findBetween(
                userId,
                friendId
        )).thenReturn(Optional.empty());

        ResponseStatusException nonFriend =
                assertThrows(
                        ResponseStatusException.class,
                        () -> chatRoomService
                                .getOrCreateDirectRoom(
                                        userId,
                                        friendId
                                )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                nonFriend.getStatusCode()
        );

        assertEquals(
                "You can only chat directly with friends",
                nonFriend.getReason()
        );
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
