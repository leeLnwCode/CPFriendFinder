package com.cp.friend.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FriendNotificationChatContractTest {

    private static String friendJs;
    private static String notificationJs;

    @BeforeAll
    static void loadResources() throws IOException {
        friendJs = readResource(
                "static/js/friend.js"
        );

        notificationJs = readResource(
                "static/js/notification.js"
        );
    }

    @Test
    void friendJs_loadsCurrentUserFromUsersMe() {
        assertTrue(
                friendJs.contains(
                        "fetch(\"/api/users/me\""
                )
        );

        assertTrue(
                friendJs.contains(
                        "credentials: \"include\""
                )
        );

        assertTrue(
                friendJs.contains(
                        "user.id || user.userId || user.user?.id"
                )
        );
    }

    @Test
    void friendJs_callsFriendsApi() {
        assertTrue(
                friendJs.contains(
                        "fetch(\"/api/friends\""
                )
        );

        assertTrue(
                friendJs.contains(
                        "friend.friendId || friend.id"
                )
        );

        assertTrue(
                friendJs.contains(
                        "friend.interests"
                )
        );
    }

    @Test
    void friendJs_opensDirectRoom() {
        assertTrue(
                friendJs.contains(
                        "fetch(`/api/chats/direct/${friendId}`"
                )
        );

        assertTrue(
                friendJs.contains(
                        "method: \"POST\""
                )
        );

        assertTrue(
                friendJs.contains(
                        "currentRoomId = room.id"
                )
        );
    }

    @Test
    void friendJs_loadsMessageHistory() {
        assertTrue(
                friendJs.contains(
                        "`/api/chats/${currentRoomId}/messages?limit=50`"
                )
        );

        assertTrue(
                friendJs.contains(
                        "method: \"GET\""
                )
        );

        assertTrue(
                friendJs.contains(
                        "messages.forEach"
                )
        );
    }

    @Test
    void friendJs_sendsTextMessage() {
        assertTrue(
                friendJs.contains(
                        "fetch(`/api/chats/${sendingRoomId}/messages`"
                )
        );

        assertTrue(
                friendJs.contains(
                        "content: text"
                )
        );

        assertTrue(
                friendJs.contains(
                        "messageType: \"TEXT\""
                )
        );

        assertTrue(
                friendJs.contains(
                        "\"Content-Type\": \"application/json\""
                )
        );
    }

    @Test
    void notificationJs_loadsIncomingFriendRequests() {
        assertTrue(
                notificationJs.contains(
                        "fetch(\"/api/friend-requests/incoming\""
                )
        );

        assertTrue(
                notificationJs.contains(
                        "requests.forEach"
                )
        );

        assertTrue(
                notificationJs.contains(
                        "request.id"
                )
        );
    }

    @Test
    void notificationJs_acceptDecline_endpointsMatch() {
        assertTrue(
                notificationJs.contains(
                        "`/api/friend-requests/${requestId}/accept`"
                )
        );

        assertTrue(
                notificationJs.contains(
                        "`/api/friend-requests/${requestId}/decline`"
                )
        );

        assertTrue(
                notificationJs.contains(
                        "window.acceptRequest"
                )
        );

        assertTrue(
                notificationJs.contains(
                        "window.declineRequest"
                )
        );
    }

    @Test
    void notificationJs_loadsMessageNotifications() {
        assertTrue(
                notificationJs.contains(
                        "\"/api/notifications?unread=false&page=0&size=20\""
                )
        );

        assertTrue(
                notificationJs.contains(
                        "notification.type === \"NEW_MESSAGE\""
                )
        );

        assertTrue(
                notificationJs.contains(
                        "notification.roomId"
                )
        );
    }

    private static String readResource(
            String path
    ) throws IOException {

        ClassLoader classLoader =
                FriendNotificationChatContractTest.class
                        .getClassLoader();

        try (
                InputStream inputStream =
                        classLoader.getResourceAsStream(
                                path
                        )
        ) {
            assertNotNull(
                    inputStream,
                    "Missing classpath resource: " + path
            );

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }
}
