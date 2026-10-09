package com.cp.friend.service;

import com.cp.friend.dto.response.UserDiscoveryResponse;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.User;
import com.cp.friend.repository.FriendRequestRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDiscoveryServiceTest {

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    private UserDiscoveryService userDiscoveryService;

    @BeforeEach
    void setUp() {
        userDiscoveryService = new UserDiscoveryService(
                userInterestRepository,
                friendshipRepository,
                friendRequestRepository,
                userRepository
        );

        lenient()
                .when(friendshipRepository
                        .findAllByMember(any(UUID.class)))
                .thenReturn(List.of());

        lenient()
                .when(friendRequestRepository
                        .findBySenderIdAndStatus(
                                any(UUID.class),
                                eq(FriendRequest.Status.PENDING)
                        ))
                .thenReturn(List.of());

        lenient()
                .when(friendRequestRepository
                        .findByReceiverIdAndStatus(
                                any(UUID.class),
                                eq(FriendRequest.Status.PENDING)
                        ))
                .thenReturn(List.of());
    }

    private User user(
            UUID id,
            String firstname,
            User.Status status,
            String department,
            short year
    ) {
        User user = new User();
        setUserId(user, id);

        user.setFirstname(firstname);
        user.setLastname("User");
        user.setImageUrl("avatar.png");
        user.setStatus(status);
        user.setDepartment(department);
        user.setYear(year);

        return user;
    }

    private UserInterestRepository.CommonInterestCount match(
            UUID userId,
            long commonCount
    ) {
        return new UserInterestRepository.CommonInterestCount() {
            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public Long getCommonCount() {
                return commonCount;
            }
        };
    }

    @Test
    void discover_noMatches_returnsEmpty() {
        UUID userId = UUID.randomUUID();

        when(userInterestRepository
                .findUsersWithCommonInterests(userId))
                .thenReturn(List.of());

        List<UserDiscoveryResponse> result =
                userDiscoveryService.discover(
                        userId,
                        null,
                        null
                );

        assertTrue(result.isEmpty());

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void discover_excludesSelfFriendsAndPending() {
        UUID meId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        UUID outgoingId = UUID.randomUUID();
        UUID incomingId = UUID.randomUUID();
        UUID validId = UUID.randomUUID();

        when(userInterestRepository
                .findUsersWithCommonInterests(meId))
                .thenReturn(List.of(
                        match(meId, 5),
                        match(friendId, 4),
                        match(outgoingId, 3),
                        match(incomingId, 2),
                        match(validId, 1)
                ));

        User me = user(
                meId,
                "Me",
                User.Status.ACTIVE,
                "CS",
                (short) 3
        );

        User friend = user(
                friendId,
                "Friend",
                User.Status.ACTIVE,
                "CS",
                (short) 3
        );

        Friendship friendship = new Friendship();
        friendship.setUser(me);
        friendship.setFriend(friend);

        when(friendshipRepository
                .findAllByMember(meId))
                .thenReturn(List.of(friendship));

        User outgoing = user(
                outgoingId,
                "Outgoing",
                User.Status.ACTIVE,
                "CS",
                (short) 3
        );

        FriendRequest outgoingRequest =
                new FriendRequest();

        outgoingRequest.setSender(me);
        outgoingRequest.setReceiver(outgoing);
        outgoingRequest.setStatus(
                FriendRequest.Status.PENDING
        );

        when(friendRequestRepository
                .findBySenderIdAndStatus(
                        meId,
                        FriendRequest.Status.PENDING
                ))
                .thenReturn(
                        List.of(outgoingRequest)
                );

        User incoming = user(
                incomingId,
                "Incoming",
                User.Status.ACTIVE,
                "CS",
                (short) 3
        );

        FriendRequest incomingRequest =
                new FriendRequest();

        incomingRequest.setSender(incoming);
        incomingRequest.setReceiver(me);
        incomingRequest.setStatus(
                FriendRequest.Status.PENDING
        );

        when(friendRequestRepository
                .findByReceiverIdAndStatus(
                        meId,
                        FriendRequest.Status.PENDING
                ))
                .thenReturn(
                        List.of(incomingRequest)
                );

        User valid = user(
                validId,
                "Valid",
                User.Status.ACTIVE,
                "CS",
                (short) 3
        );

        when(userRepository.findAllById(any()))
                .thenAnswer(invocation -> {
                    Iterable<UUID> ids =
                            invocation.getArgument(0);

                    List<UUID> captured =
                            new ArrayList<>();

                    ids.forEach(captured::add);

                    assertEquals(
                            Set.of(validId),
                            new HashSet<>(captured)
                    );

                    return List.of(valid);
                });

        List<UserDiscoveryResponse> result =
                userDiscoveryService.discover(
                        meId,
                        null,
                        null
                );

        assertEquals(1, result.size());
        assertEquals(validId, result.get(0).id());
    }

    @Test
    void discover_excludesInactiveCandidates() {
        UUID meId = UUID.randomUUID();
        UUID activeId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();

        when(userInterestRepository
                .findUsersWithCommonInterests(meId))
                .thenReturn(List.of(
                        match(activeId, 3),
                        match(blockedId, 2)
                ));

        User active = user(
                activeId,
                "Active",
                User.Status.ACTIVE,
                "CS",
                (short) 3
        );

        User blocked = user(
                blockedId,
                "Blocked",
                User.Status.BLOCKED,
                "CS",
                (short) 3
        );

        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(active, blocked)
                );

        List<UserDiscoveryResponse> result =
                userDiscoveryService.discover(
                        meId,
                        null,
                        null
                );

        assertEquals(1, result.size());
        assertEquals(activeId, result.get(0).id());
    }

    @Test
    void discover_departmentFilter_isTrimmedAndCaseInsensitive() {
        UUID meId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        when(userInterestRepository
                .findUsersWithCommonInterests(meId))
                .thenReturn(List.of(
                        match(candidateId, 2)
                ));

        User candidate = user(
                candidateId,
                "Candidate",
                User.Status.ACTIVE,
                "Computer Science",
                (short) 3
        );

        when(userRepository.findAllById(any()))
                .thenReturn(List.of(candidate));

        List<UserDiscoveryResponse> result =
                userDiscoveryService.discover(
                        meId,
                        "  computer science  ",
                        null
                );

        assertEquals(1, result.size());
        assertEquals(
                candidateId,
                result.get(0).id()
        );

        assertEquals(
                2,
                result.get(0).commonInterestCount()
        );
    }

    @Test
    void discover_yearFilter_returnsMatchingYear() {
        UUID meId = UUID.randomUUID();
        UUID year2Id = UUID.randomUUID();
        UUID year3Id = UUID.randomUUID();

        when(userInterestRepository
                .findUsersWithCommonInterests(meId))
                .thenReturn(List.of(
                        match(year2Id, 3),
                        match(year3Id, 2)
                ));

        User year2 = user(
                year2Id,
                "YearTwo",
                User.Status.ACTIVE,
                "CS",
                (short) 2
        );

        User year3 = user(
                year3Id,
                "YearThree",
                User.Status.ACTIVE,
                "CS",
                (short) 3
        );

        when(userRepository.findAllById(any()))
                .thenReturn(
                        List.of(year2, year3)
                );

        List<UserDiscoveryResponse> result =
                userDiscoveryService.discover(
                        meId,
                        null,
                        (short) 3
                );

        assertEquals(1, result.size());
        assertEquals(year3Id, result.get(0).id());
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
