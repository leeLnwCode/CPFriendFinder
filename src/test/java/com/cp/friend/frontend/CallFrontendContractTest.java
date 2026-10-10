package com.cp.friend.frontend;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CallFrontendContractTest {

    private static String homeHtml;
    private static String randomHtml;
    private static String notificationHtml;
    private static String settingHtml;
    private static String roomHtml;
    private static String callJs;
    private static String friendJs;

    @BeforeAll
    static void loadResources() throws IOException {
        homeHtml = readResource("templates/home.html");
        randomHtml = readResource("templates/random.html");
        notificationHtml = readResource("templates/notification.html");
        settingHtml = readResource("templates/setting.html");
        roomHtml = readResource("templates/room.html");
        callJs = readResource("static/js/call.js");
        friendJs = readResource("static/js/friend.js");
    }

    @Test
    void cameraToggle_publishesMediaState() {
        assertTrue(
            callJs.contains("type: \"MEDIA\""),
            "Camera toggle should publish MEDIA state to other participants"
        );
    }

    // @Test
    // void callJs_handlesIncomingMediaSignal() {
    //     assertTrue(
    //             callJs.contains("signal.type === \"MEDIA\""),
    //             "Call client should handle incoming MEDIA signal"
    //     );
    // }

    @Test
    void callJs_leaveSignal_notifiesUiThatCallEnded() {
        assertTrue(
                callJs.contains("cp-call-ended"),
                "Remote LEAVE should notify the UI that the call has ended"
        );
    }

    @Test
    void friendJs_handlesRemoteCallEndedEvent() {
        assertTrue(
                friendJs.contains("addEventListener(\"cp-call-ended\""),
                "Call UI should close when the remote participant leaves"
        );
    }

    @Test
    void voiceCallEnd_cancelsPendingOutgoingCall() {
        int start = friendJs.indexOf("voiceCallEnd.addEventListener");
        int end = friendJs.indexOf("if (videoCallClose)", start);

        assertTrue(start >= 0 && end > start, "Voice call end handler should exist");

        String handler = friendJs.substring(start, end);

        assertTrue(handler.contains("if (CPCall.isActive?.())"));
        assertTrue(handler.contains("CPCall.leaveCall();"));
        assertTrue(handler.contains("else if (currentCallMode === \"VOICE\")"));
        assertTrue(handler.contains("await cancelOutgoingCall();"));
    }

    @Test
    void videoCallEnd_cancelsPendingOutgoingCall() {
        int start = friendJs.indexOf("videoCallEnd.addEventListener");
        int end = friendJs.indexOf("if (voiceCallMute)", start);

        assertTrue(start >= 0 && end > start, "Video call end handler should exist");

        String handler = friendJs.substring(start, end);

        assertTrue(handler.contains("if (CPCall.isActive?.())"));
        assertTrue(handler.contains("CPCall.leaveCall();"));
        assertTrue(handler.contains("else if (currentCallMode === \"VIDEO\")"));
        assertTrue(handler.contains("await cancelOutgoingCall();"));
    }
    @Test
    void callJs_doesNotContainHardcodedTurnCredentials() {
        boolean hasHardcodedTurnCredentials = callJs.matches(
                "(?s).*username\\s*:\\s*[\"'][^\"']+[\"'].*"
            + "credential\\s*:\\s*[\"'][^\"']+[\"'].*"
        );

        assertFalse(
                hasHardcodedTurnCredentials,
                "TURN username and credential must not be hardcoded in public frontend source"
        );
    }

    @Test
    void authenticatedPages_loadGlobalIncomingCallSupport() {
        String[] pages = {
            homeHtml,
            randomHtml,
            notificationHtml,
            settingHtml,
            roomHtml
        };

        for (String page : pages) {
            assertTrue(
                    page.contains("/js/call.js"),
                    "Authenticated pages should load the call client"
            );
        }
    }

    @Test
    void globalCall_acceptRedirectsToFriendWithPendingCall() throws IOException {
        String globalCallJs = readResource("static/js/global-call.js");

        assertTrue(
                globalCallJs.contains("sessionStorage.setItem"),
                "Global call accept should preserve the incoming call before navigation"
        );

        assertTrue(
                globalCallJs.contains("window.location.href = \"/friend\"")
                        || globalCallJs.contains("window.location.assign(\"/friend\")"),
                "Global call accept should navigate to the Friend page"
        );
    }

    @Test
    void friendPage_resumesPendingIncomingCall() {
        assertTrue(
                friendJs.contains("sessionStorage.getItem"),
                "Friend page should restore a call accepted from another page"
        );
    }

    private static String readResource(String path) throws IOException {
        ClassLoader classLoader =
                CallFrontendContractTest.class.getClassLoader();

        try (InputStream inputStream =
                     classLoader.getResourceAsStream(path)) {

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
