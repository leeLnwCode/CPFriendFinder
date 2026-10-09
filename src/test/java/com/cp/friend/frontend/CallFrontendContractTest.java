package com.cp.friend.frontend;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CallFrontendContractTest {

    private static String callJs;
    private static String friendJs;

    @BeforeAll
    static void loadResources() throws IOException {
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

    @Test
    void callJs_handlesIncomingMediaSignal() {
        assertTrue(
                callJs.contains("signal.type === \"MEDIA\""),
                "Call client should handle incoming MEDIA signal"
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