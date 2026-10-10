package com.cp.friend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "AWS_ENDPOINT_URL_S3=http://localhost:9000",
        "AWS_ACCESS_KEY_ID=test-access-key",
        "AWS_SECRET_ACCESS_KEY=test-secret-key",
        "AWS_REGION=us-east-1",

        "spring.datasource.url=jdbc:h2:mem:cpfriendfinder;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",

        // schema.sql เป็น PostgreSQL-only (partial index) — ให้ Hibernate สร้าง schema จาก entity แทน
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FriendApplicationTests {

    @Test
    void contextLoads() {
    }
}
