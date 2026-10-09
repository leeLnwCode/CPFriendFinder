package com.cp.friend.service;

import com.cp.friend.dto.response.FriendResponse;
import com.cp.friend.mapper.InterestMapper;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.Interest;
import com.cp.friend.model.User;
import com.cp.friend.model.UserInterest;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.InterestRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private InterestRepository interestRepository;

    private FriendshipService friendshipService;

    @BeforeEach
    void setUp() {
        friendshipService = new FriendshipService(
                friendshipRepository,
                userRepository,
                userInterestRepository,
                interestRepository,
                new InterestMapper()
        );
    }

    private User user(
            UUID id,
            String firstname,
            User.Status status
    ) {
        User user = new User();
        setUserId(user, id);

        user.setFirstname(firstname);
        user.setLastname("User");
        user.setImageUrl("avatar.png");
        user.setYear((short) 3);
        user.setDepartment("Computer Science");
        user.setBio("Bio");
        user.setStatus(status);

        return user;
    }

    @Test
    void listFriends_returnsMappedFriends() {
        UUID meId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        User me = user(
                meId,
                "Me",
                User.Status.ACTIVE
        );

        User friend = user(
                friendId,
                "Friend",
                User.Status.ACTIVE
        );

        Friendship friendship = new Friendship();
        friendship.setUser(me);
        friendship.setFriend(friend);
        friendship.setCreatedAt(Instant.now());

        UserInterest relation = new UserInterest();
        relation.setUserId(friendId);
        relation.setInterestId(interestId);

        Interest interest = new Interest();
        interest.setId(interestId);
        interest.setName("Java");
        interest.setActive(true);

        when(userRepository.findById(meId))
                .thenReturn(Optional.of(me));

        when(friendshipRepository.findAllByMember(meId))
                .thenReturn(List.of(friendship));

        when(userInterestRepository.findByUserId(friendId))
                .thenReturn(List.of(relation));

        when(interestRepository
                .findByIdInAndIsActiveTrue(anyCollection()))
                .thenReturn(List.of(interest));

        List<FriendResponse> result =
                friendshipService.listFriends(meId);

        assertEquals(1, result.size());

        FriendResponse response = result.get(0);

        assertEquals(friendId, response.friendId());
        assertEquals("Friend", response.firstname());
        assertEquals("Computer Science", response.department());
        assertEquals(1, response.interests().size());
        assertEquals("Java", response.interests().get(0).name());
    }

    @Test
    void listFriends_inactiveOrMissingUser_returnsNotFound() {
        UUID missingId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();

        User blocked = user(
                blockedId,
                "Blocked",
                User.Status.BLOCKED
        );

        when(userRepository.findById(missingId))
                .thenReturn(Optional.empty());

        when(userRepository.findById(blockedId))
                .thenReturn(Optional.of(blocked));

        ResponseStatusException missing = assertThrows(
                ResponseStatusException.class,
                () -> friendshipService.listFriends(missingId)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                missing.getStatusCode()
        );

        assertEquals(
                "User not found",
                missing.getReason()
        );

        ResponseStatusException inactive = assertThrows(
                ResponseStatusException.class,
                () -> friendshipService.listFriends(blockedId)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                inactive.getStatusCode()
        );

        assertEquals(
                "User not found",
                inactive.getReason()
        );

        verifyNoInteractions(friendshipRepository);
    }

    @Test
    void unfriend_existingFriendship_deletesRelation() {
        UUID meId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        User me = user(
                meId,
                "Me",
                User.Status.ACTIVE
        );

        when(userRepository.findById(meId))
                .thenReturn(Optional.of(me));

        when(friendshipRepository.deleteBetween(
                meId,
                friendId
        )).thenReturn(1);

        friendshipService.unfriend(
                meId,
                friendId
        );

        verify(friendshipRepository)
                .deleteBetween(
                        meId,
                        friendId
                );
    }

    @Test
    void unfriend_missingFriendship_returnsNotFound() {
        UUID meId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        User me = user(
                meId,
                "Me",
                User.Status.ACTIVE
        );

        when(userRepository.findById(meId))
                .thenReturn(Optional.of(me));

        when(friendshipRepository.deleteBetween(
                meId,
                friendId
        )).thenReturn(0);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendshipService.unfriend(
                        meId,
                        friendId
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        assertEquals(
                "Friendship not found",
                ex.getReason()
        );
    }

    private void setUserId(
            User user,
            UUID id
    ) {
        try {
            java.lang.reflect.Field field =
                    User.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
