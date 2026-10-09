package com.cp.friend.schema;
import org.junit.jupiter.api.Test;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SchemaExportTest {
 @Test void exportPostgresSchemaFromActualMappings() throws Exception {
  Path target=Path.of("target/schema.sql");Files.deleteIfExists(target);
  var registry=new StandardServiceRegistryBuilder().applySettings(Map.of(
   "hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect",
   "hibernate.boot.allow_jdbc_metadata_access","false",
   "hibernate.hbm2ddl.auto","none",
   "hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl",
   "jakarta.persistence.schema-generation.database.action","none",
   "jakarta.persistence.schema-generation.scripts.action","create",
   "jakarta.persistence.schema-generation.scripts.create-target",target.toString()
  )).build();
  try {
   MetadataSources sources=new MetadataSources(registry);
   for(String name:List.of("User","UserProfile","Interest","UserInterest","ChatRoom","RoomMember","RoomInterest","Message","FriendRequest","Friendship","Notification","RandomChatSession","RandomChatMessage")) sources.addAnnotatedClass(Class.forName("com.cp.friend.model."+name));
   try(var factory=sources.buildMetadata().buildSessionFactory()) {
    String sql=Files.readString(target);assertTrue(sql.contains("user_profiles"));assertTrue(sql.contains("foreign key"));assertTrue(sql.contains("chat_rooms"));
   }
  } finally {StandardServiceRegistryBuilder.destroy(registry);}
 }
}
