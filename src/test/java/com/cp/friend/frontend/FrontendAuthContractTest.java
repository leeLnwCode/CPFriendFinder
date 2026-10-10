package com.cp.friend.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FrontendAuthContractTest {

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);

        assertTrue(resource.exists(), path + " should exist");

        try (var input = resource.getInputStream()) {
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    @Test
    void loginTemplate_containsInputsUsedByJavascript()
            throws IOException {

        String html = readResource("templates/login.html");

        assertTrue(html.contains("id=\"loginForm\""));
        assertTrue(html.contains("id=\"email\""));
        assertTrue(html.contains("id=\"password\""));
        assertTrue(html.contains("id=\"showPassword\""));
    }

    @Test
    void loginJavascript_callsCorrectBackendEndpoint()
            throws IOException {

        String js = readResource("static/js/login.js");

        assertTrue(js.contains("fetch(\"/api/auth/login\""));
        assertTrue(js.contains("method: \"POST\""));
        assertTrue(js.contains("\"Content-Type\": \"application/json\""));
    }

    @Test
    void loginJavascript_storesUserAndRedirectsHome()
            throws IOException {

        String js = readResource("static/js/login.js");

        assertTrue(js.contains(
                "sessionStorage.setItem(\"currentUser\""
        ));

        assertTrue(js.contains(
                "window.location.assign(\"/home\")"
        ));
    }

    @Test
    void registerTemplate_containsInputsUsedByJavascript()
            throws IOException {

        String html = readResource("templates/register.html");

        String[] requiredIds = {
                "email",
                "password",
                "confirmPassword",
                "firstname",
                "lastname",
                "dateOfBirth",
                "year",
                "department",
                "profileImage"
        };

        for (String id : requiredIds) {
            assertTrue(
                    html.contains("id=\"" + id + "\""),
                    "Missing frontend element: " + id
            );
        }
    }

    @Test
    void registerJavascript_callsCorrectBackendEndpoint()
            throws IOException {

        String js = readResource("static/js/register.js");

        assertTrue(js.contains(
                "fetch(\"/api/auth/register\""
        ));

        assertTrue(js.contains("method: \"POST\""));

        assertTrue(js.contains(
                "\"Content-Type\": \"application/json\""
        ));
    }

    @Test
    void registerJavascript_validatesPasswordAndRedirectsLogin()
            throws IOException {

        String js = readResource("static/js/register.js");

        assertTrue(js.contains("password.length < 8"));
        assertTrue(js.contains("password !== confirmPassword"));

        assertTrue(js.contains(
                "\"/login?registered=1&email=\" + encodeURIComponent(email)"
        ));
    }
}
