package com.cp.friend.schema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import com.cp.friend.model.User;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest(properties={"spring.datasource.url=jdbc:h2:mem:profiles;MODE=PostgreSQL","spring.datasource.driver-class-name=org.h2.Driver","spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.jpa.properties.hibernate.auto_quote_keyword=true"})
class ProfilePersistenceTest {
 @Autowired EntityManager em;
 @Test void biographyCascadesWithSharedUserIdAndSurvivesReload() {
  User u=new User();u.setEmail("profile-persistence@example.invalid");u.setPasswordHash("test-only-hash");u.setBio("Coding and music");em.persist(u);em.flush();var id=u.getId();em.clear();
  assertEquals("Coding and music",em.find(User.class,id).getBio());
  assertEquals(1L,em.createQuery("select count(p) from UserProfile p where p.user.id=:id",Long.class).setParameter("id",id).getSingleResult());
 }
}
