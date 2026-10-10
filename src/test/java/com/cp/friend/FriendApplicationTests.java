package com.cp.friend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "AWS_ENDPOINT_URL_S3=http://localhost:9000",
        "AWS_ACCESS_KEY_ID=test-access-key",
        "AWS_SECRET_ACCESS_KEY=test-secret-key",
        "AWS_REGION=us-east-1",

        "spring.datasource.url=jdbc:h2:mem:cpfriendfinder;MODE=PostgreSQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",

        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class FriendApplicationTests {
    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;
    private java.net.http.HttpResponse<String> get(String path) throws Exception {
        return java.net.http.HttpClient.newHttpClient().send(
            java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:"+port+path)).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    }
    @Test void loginPageIsServedByRunningWebApplication() throws Exception {
        var response=get("/login");
        org.junit.jupiter.api.Assertions.assertEquals(200,response.statusCode());
        org.junit.jupiter.api.Assertions.assertTrue(response.body().contains("Login"));
    }
    @Test void callAndFriendScriptsAreServed() throws Exception {
        for(String path: java.util.List.of("/js/call.js","/js/friend.js")) {
            var response=get(path);
            org.junit.jupiter.api.Assertions.assertEquals(200,response.statusCode(),path);
            org.junit.jupiter.api.Assertions.assertFalse(response.body().isBlank(),path);
        }
    }
    @Test void anonymousProfileRequestStillRequiresLogin() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(401,get("/api/users/me").statusCode());
    }


    @Test
    void contextLoads() {
    }
}
